<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<c:set var="categories" value="${param.restricted ? 'help' : 'help internalhelp'}"/>

<section>
    <h1 class="info"><fmt:message key="help.title"><fmt:param value="${model.brand}"/></fmt:message></h1>
    <ul class="sibling-pages">
        <c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
            <c:choose>
                <c:when test="${loopStatus.count > 1}"></li><li></c:when>
                <c:otherwise><li></c:otherwise>
            </c:choose>
            <c:url var="url" value="${cat}.view?"/>
            <c:choose>
                <c:when test="${param.cat eq cat}">
                    <span class="selected"><fmt:message key="settingsheader.${cat}"/></span>
                </c:when>
                <c:otherwise>
                    <a href="${url}"><fmt:message key="settingsheader.${cat}"/></a>
                </c:otherwise>
            </c:choose>
        </c:forTokens>
    </ul>
</section>