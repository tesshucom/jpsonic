<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<c:set var="categories" value="${param.restricted ? '' : 'podcasts settings'}"/>

<section>
    <h1><img src="<spring:theme code='podcastLargeImage'/>" alt=""><fmt:message key="podcastreceiver.title"/></h1>
	<ul class="subMenu">
	    <c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
	        <c:choose>
	            <c:when test="${loopStatus.count > 1}"></li><li></c:when>
	            <c:otherwise><li></c:otherwise>
	        </c:choose>
	        <c:choose>
	            <c:when test="${'podcasts' == cat}"><c:url var="url" value="podcastChannels.view?"/></c:when>
	            <c:when test="${'settings' == cat}"><c:url var="url" value="podcastSettings.view?"/></c:when>
	            <c:otherwise></c:otherwise>
	        </c:choose>
	        <c:choose>
	            <c:when test="${param.cat eq cat}">
	                <span class="menuItemSelected"><fmt:message key="podcastsheader.${cat}"/></span>
	            </c:when>
	            <c:otherwise>
	                <span class="menuItem"><a href="${url}"><fmt:message key="podcastsheader.${cat}"/></a></span>
	            </c:otherwise>
	        </c:choose>
	    </c:forTokens>
	</ul>
</section>