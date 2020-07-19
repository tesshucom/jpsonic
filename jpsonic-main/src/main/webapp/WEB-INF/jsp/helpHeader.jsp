<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<c:set var="categories" value="${param.restricted ? 'help' : 'help internalhelp'}"/>

<h1>
    <img src="<spring:theme code='helpImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="help.title"><fmt:param value="${model.brand}"/></fmt:message></span>
</h1>

<ul class="subMenu">
    <c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
        <c:choose>
            <c:when test="${loopStatus.count > 1}"></li><li></c:when>
            <c:otherwise><li></c:otherwise>
        </c:choose>
        <c:url var="url" value="${cat}.view?"/>
        <c:choose>
            <c:when test="${param.cat eq cat}">
                <span class="menuItemSelected"><fmt:message key="settingsheader.${cat}"/></span>
            </c:when>
            <c:otherwise>
                <span class="menuItem"><a href="${url}"><fmt:message key="settingsheader.${cat}"/></a></span>
            </c:otherwise>
        </c:choose>
    </c:forTokens>
</ul>

