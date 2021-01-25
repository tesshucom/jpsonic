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

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.FontLoader;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Controller for generating a chart showing bitrate vs time.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/userChart")
public class UserChartController extends AbstractChartController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private FontLoader fontLoader;

    public static final int IMAGE_WIDTH = 400;
    public static final int IMAGE_MIN_HEIGHT = 200;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    @Override
    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = request.getParameter(Attributes.Request.TYPE.value());
        CategoryDataset dataset = createDataset(type);
        JFreeChart chart = createChart(dataset, request);

        int imageHeight = Math.max(IMAGE_MIN_HEIGHT, 15 * dataset.getColumnCount());

        ChartUtils.writeChartAsPNG(response.getOutputStream(), chart, IMAGE_WIDTH, imageHeight);
        return null;
    }

    private CategoryDataset createDataset(String type) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<User> users = securityService.getAllUsers();
        for (User user : users) {
            double value;
            if ("stream".equals(type)) {
                value = user.getBytesStreamed();
            } else if ("download".equals(type)) {
                value = user.getBytesDownloaded();
            } else if ("upload".equals(type)) {
                value = user.getBytesUploaded();
            } else if ("total".equals(type)) {
                value = user.getBytesStreamed() + user.getBytesDownloaded() + user.getBytesUploaded();
            } else {
                throw new IllegalArgumentException("Illegal chart type: " + type);
            }

            value /= BYTES_PER_MB;
            dataset.addValue(value, "Series", user.getUsername());
        }

        return dataset;
    }

    private JFreeChart createChart(CategoryDataset dataset, HttpServletRequest request) {

        Color bgColor = getBackground(request);
        Color fgColor = getForeground(request);
        Color stColor = getStroke(request);

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.HORIZONTAL, false,
                false, false);
        StandardChartTheme theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
        Font font = fontLoader.getFont(12F);
        theme.setExtraLargeFont(font);
        theme.setLargeFont(font);
        theme.setRegularFont(font);
        theme.setSmallFont(font);
        theme.apply(chart);

        chart.setBackgroundPaint(bgColor);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(bgColor);
        plot.setOutlinePaint(fgColor);
        plot.setRangeGridlinePaint(fgColor);
        plot.setRangeGridlineStroke(new BasicStroke(0.2f));
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new BarPainter() {
            @Override
            public void paintBarShadow(Graphics2D g2, BarRenderer ren, int row, int col, RectangularShape shape,
                    RectangleEdge base, boolean pegShadow) {
                // to be none
            }

            @Override
            public void paintBar(Graphics2D g2, BarRenderer ren, int row, int col, RectangularShape shape,
                    RectangleEdge base) {
                int barMaxHeight = 15;
                double radius = 10.0;
                double barX = shape.getX() - radius;
                double barY = barMaxHeight < shape.getHeight() ? shape.getY() + (shape.getHeight() - barMaxHeight) / 2
                        : shape.getY();
                double barHeight = barMaxHeight < shape.getHeight() ? barMaxHeight : shape.getHeight();
                double barWidth = shape.getWidth() + radius;
                RoundRectangle2D rec = new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, radius, radius);
                g2.setPaint(new GradientPaint(0, 0, stColor, 0, 150, stColor));
                g2.fill(rec);
            }
        });

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelPaint(fgColor);
        domainAxis.setTickMarkPaint(fgColor);
        domainAxis.setAxisLinePaint(fgColor);

        LogarithmicAxis rangeAxis = new LogarithmicAxis(null);
        rangeAxis.setStrictValuesFlag(false);
        rangeAxis.setAllowNegativesFlag(true);
        rangeAxis.setTickLabelPaint(fgColor);
        rangeAxis.setTickMarkPaint(fgColor);
        rangeAxis.setAxisLinePaint(fgColor);

        return chart;
    }
}
