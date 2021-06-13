package com.tesshu.jpsonic.taglib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.servlet.jsp.JspException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

class UrlTagTest {

    private final UrlTag urlTag = new UrlTag();

    private String doFormatUrl() throws ExecutionException {
        try {
            Method method = urlTag.getClass().getDeclaredMethod("formatUrl");
            method.setAccessible(true);
            Object o = method.invoke(urlTag);
            if (o != null) {
                return (String) o;
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    @Test
    void testormatUrl() throws JspException, ExecutionException {
        MockPageContext pageContext = new MockPageContext();
        urlTag.setPageContext(pageContext);
        String value = "/test";

        urlTag.setValue(value);
        assertEquals("/test", doFormatUrl());
        urlTag.release();

        urlTag.setValue(value);
        urlTag.addParameter("id", "999");
        urlTag.addParameter("user", "admin");
        assertEquals("/test?id=999&user=admin", doFormatUrl());
        urlTag.release();

        urlTag.setValue(value);
        urlTag.addParameter("id", "999");
        urlTag.addParameter("user", "ａｄｍｉｎ");
        assertEquals("/test?id=999&userUtf8Hex=efbd81efbd84efbd8defbd89efbd8e", doFormatUrl());
        urlTag.release();
    }
}
