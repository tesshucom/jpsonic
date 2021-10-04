package com.tesshu.jpsonic.taglib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import javax.servlet.jsp.JspException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

class UrlTagTest {

    private final UrlTag urlTag = new UrlTag();

    @Test
    void testormatUrl() throws JspException, ExecutionException {
        MockPageContext pageContext = new MockPageContext();
        urlTag.setPageContext(pageContext);
        String value = "/test";

        urlTag.setValue(value);
        assertEquals("/test", urlTag.formatUrl());
        urlTag.release();

        urlTag.setValue(value);
        urlTag.addParameter("id", "999");
        urlTag.addParameter("user", "admin");
        assertEquals("/test?id=999&user=admin", urlTag.formatUrl());
        urlTag.release();

        urlTag.setValue(value);
        urlTag.addParameter("id", "999");
        urlTag.addParameter("user", "ａｄｍｉｎ");
        assertEquals("/test?id=999&userUtf8Hex=efbd81efbd84efbd8defbd89efbd8e", urlTag.formatUrl());
        urlTag.release();
    }
}
