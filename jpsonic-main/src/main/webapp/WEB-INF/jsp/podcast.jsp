<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" contentType="text/xml; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@page trimDirectiveWhitespaces="true"%>
<rss xmlns:itunes="http://www.itunes.com/DTDs/Podcast-1.0.dtd" version="2.0">
    <channel>
        <title>Jpsonic Podcast</title>
        <link>${model.url}</link>
        <description>Jpsonic Podcast</description>
        <language>${model.lang}</language>
        <image>
            <url>${model.logo}</url>
            <title>Jpsonic Podcast</title>
        </image>
        <c:forEach var="podcast" items="${model.podcasts}">
            <item>
                <title>${fn:escapeXml(podcast.name)}</title>
                <link>${model.url}</link>
                <description>Jpsonic playlist "${fn:escapeXml(podcast.name)}"</description>
                <pubDate>${podcast.publishDate}</pubDate>
                <enclosure url="${podcast.enclosureUrl}" length="${podcast.length}" type="${podcast.type}"/>
            </item>
        </c:forEach>
    </channel>
</rss>
