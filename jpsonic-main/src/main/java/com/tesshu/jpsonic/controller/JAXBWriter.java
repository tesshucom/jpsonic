/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.util.XMLUtil.createSAXBuilder;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.tesshu.jpsonic.util.StringUtil;
import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subsonic.restapi.Error;
import org.subsonic.restapi.ObjectFactory;
import org.subsonic.restapi.Response;
import org.subsonic.restapi.ResponseStatus;

/**
 * @author Sindre Mehus
 */
public class JAXBWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JAXBWriter.class);

    private final javax.xml.bind.JAXBContext jaxbContext;
    private final DatatypeFactory datatypeFactory;
    private final String restProtocolVersion;

    public JAXBWriter() {
        try {
            jaxbContext = JAXBContext.newInstance(Response.class);
            datatypeFactory = DatatypeFactory.newInstance();
            restProtocolVersion = getRESTProtocolVersion();
        } catch (ExecutionException | JAXBException | DatatypeConfigurationException e) {
            throw new CompletionException("Fatal JAXBWriter initialization error.", e);
        }
    }

    private Marshaller createXmlMarshaller() {
        Marshaller marshaller;
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StringUtil.ENCODING_UTF8);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new CompletionException(e);
        }
    }

    private Marshaller createJsonMarshaller() {
        try {
            Marshaller marshaller;
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StringUtil.ENCODING_UTF8);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new CompletionException(e);
        }
    }

    private String getRESTProtocolVersion() throws ExecutionException {
        try (InputStream in = StringUtil.class.getResourceAsStream("/subsonic-rest-api.xsd")) {
            Document document = createSAXBuilder().build(in);
            Attribute version = document.getRootElement().getAttribute("version");
            return version.getValue();
        } catch (JDOMException | IOException e) {
            throw new ExecutionException("Unable to parse subsonic-rest-api.xsd.", e);
        }
    }

    public String getRestProtocolVersion() {
        return restProtocolVersion;
    }

    public Response createResponse(boolean ok) {
        Response response = new ObjectFactory().createResponse();
        response.setStatus(ok ? ResponseStatus.OK : ResponseStatus.FAILED);
        response.setVersion(restProtocolVersion);
        return response;
    }

    public void writeResponse(HttpServletRequest request, HttpServletResponse httpResponse, Response jaxbResponse) {

        String format = getStringParameter(request, Attributes.Request.F.value(), "xml");
        String jsonpCallback = request.getParameter(Attributes.Request.CALLBACK.value());
        boolean json = "json".equals(format);
        boolean jsonp = "jsonp".equals(format) && jsonpCallback != null;
        Marshaller marshaller;

        if (json) {
            marshaller = createJsonMarshaller();
            httpResponse.setContentType("application/json");
        } else if (jsonp) {
            marshaller = createJsonMarshaller();
            httpResponse.setContentType("text/javascript");
        } else {
            marshaller = createXmlMarshaller();
            httpResponse.setContentType("text/xml");
        }

        httpResponse.setCharacterEncoding(StringUtil.ENCODING_UTF8);

        try {
            StringWriter writer = new StringWriter();
            if (jsonp) {
                writer.append(escape(jsonpCallback)).append('(');
            }
            marshaller.marshal(new ObjectFactory().createSubsonicResponse(jaxbResponse), writer);
            if (jsonp) {
                writer.append(");");
            }
            httpResponse.getWriter().append(writer.getBuffer());
        } catch (JAXBException | IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to marshal JAXB", e);
            }
        }
    }

    private String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        return escaped;
    }

    public void writeErrorResponse(HttpServletRequest request, HttpServletResponse response,
            SubsonicRESTController.ErrorCode code, String message) {
        Response res = createResponse(false);
        Error error = new Error();
        res.setError(error);
        error.setCode(code.getCode());
        error.setMessage(message);
        writeResponse(request, response, res);
    }

    public XMLGregorianCalendar convertDate(Date date) {
        if (date == null) {
            return null;
        }

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        return datatypeFactory.newXMLGregorianCalendar(c).normalize();
    }

    public XMLGregorianCalendar convertCalendar(Calendar calendar) {
        if (calendar == null) {
            return null;
        }

        return datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) calendar).normalize();
    }
}
