/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import com.tesshu.jpsonic.controller.FontLoader;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.StatusService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.DateRange;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Controller for generating a chart showing bitrate vs time.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/statusChart")
public class StatusChartController extends AbstractChartController {

    @Autowired
    private StatusService statusService;

    @Autowired
    private FontLoader fontLoader;

    public static final int IMAGE_WIDTH = 240;
    public static final int IMAGE_HEIGHT = 150;

    public static final Object LOCK = new Object();

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Millisecond, Date) Not reusable
    @Override
    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = request.getParameter("type");
        int index = ServletRequestUtils.getIntParameter(request, "index", 0);

        List<TransferStatus> statuses = Collections.emptyList();
        if ("stream".equals(type)) {
            statuses = statusService.getAllStreamStatuses();
        } else if ("download".equals(type)) {
            statuses = statusService.getAllDownloadStatuses();
        } else if ("upload".equals(type)) {
            statuses = statusService.getAllUploadStatuses();
        }

        if (index < 0 || index >= statuses.size()) {
            return null;
        }
        TransferStatus status = statuses.get(index);

        TimeSeries series = new TimeSeries("Kbps");
        TransferStatus.SampleHistory history = status.getHistory();
        long to = System.currentTimeMillis();
        long from = to - status.getHistoryLengthMillis();

        if (!history.isEmpty()) {

            TransferStatus.Sample previous = history.get(0);

            for (int i = 1; i < history.size(); i++) {
                TransferStatus.Sample sample = history.get(i);

                long elapsedTimeMilis = sample.getTimestamp() - previous.getTimestamp();
                long bytesStreamed = Math.max(0L, sample.getBytesTransfered() - previous.getBytesTransfered());

                double kbps = (8.0 * bytesStreamed / 1024.0) / (elapsedTimeMilis / 1000.0);
                series.addOrUpdate(new Millisecond(new Date(sample.getTimestamp())), kbps);

                previous = sample;
            }
        }

        // Compute moving average.
        series = MovingAverage.createMovingAverage(series, "Kbps", 20000, 5000);

        // Find min and max values.
        double min = 100;
        double max = 250;
        for (Object obj : series.getItems()) {
            TimeSeriesDataItem item = (TimeSeriesDataItem) obj;
            double value = item.getValue().doubleValue();
            if (item.getPeriod().getFirstMillisecond() > from) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        // Add 10% to max value.
        max *= 1.1D;

        // Subtract 10% from min value.
        min *= 0.9D;

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        Color bgColor = getBackground(request);
        Color fgColor = getForeground(request);
        Color stColor = getStroke(request);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset, false, false, false);
        StandardChartTheme theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
        Font font = fontLoader.getFont(12F);
        theme.setExtraLargeFont(font);
        theme.setLargeFont(font);
        theme.setRegularFont(font);
        theme.setSmallFont(font);
        theme.apply(chart);

        chart.setBackgroundPaint(bgColor);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(bgColor);
        plot.setOutlinePaint(fgColor);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

        plot.setRangeGridlinePaint(fgColor);
        plot.setRangeGridlineStroke(new BasicStroke(0.2f));
        plot.setDomainGridlinePaint(fgColor);
        plot.setDomainGridlineStroke(new BasicStroke(0.2f));

        XYItemRenderer renderer = plot.getRendererForDataset(dataset);
        renderer.setSeriesPaint(0, stColor);
        renderer.setSeriesStroke(0, new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));

        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(new DateRange(from, to));
        domainAxis.setTickLabelPaint(fgColor);
        domainAxis.setTickMarkPaint(fgColor);
        domainAxis.setAxisLinePaint(fgColor);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(new Range(min, max));
        rangeAxis.setTickLabelPaint(fgColor);
        rangeAxis.setTickMarkPaint(fgColor);
        rangeAxis.setAxisLinePaint(fgColor);

        synchronized (LOCK) {
            ChartUtils.writeChartAsPNG(response.getOutputStream(), chart, IMAGE_WIDTH, IMAGE_HEIGHT);
        }

        return null;
    }

}
