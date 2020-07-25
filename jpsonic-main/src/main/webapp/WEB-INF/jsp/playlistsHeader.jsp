<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<c:set var="categories" value="playlists import more"/>

<section>
	<h1><img src="<spring:theme code='playlistImage'/>"><fmt:message key="left.playlists"/></h1>

	<ul class="subMenu">
	    <c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
	        <c:choose>
	            <c:when test="${loopStatus.count > 1}"></li><li></c:when>
	            <c:otherwise><li></c:otherwise>
	        </c:choose>
	        <c:choose>
	            <c:when test="${'playlists' == cat}"><c:url var="url" value="playlists.view?"/></c:when>
	            <c:when test="${'import' == cat}"><c:url var="url" value="importPlaylist.view?"/></c:when>
	            <c:when test="${'more' == cat}"><c:url var="url" value="more.view?"/></c:when>
	            <c:otherwise></c:otherwise>
	        </c:choose>
	        <c:choose>
	            <c:when test="${param.cat eq cat}">
	                <span class="menuItemSelected"><fmt:message key="playlistsheader.${cat}"/></span>
	            </c:when>
	            <c:otherwise>
	                <span class="menuItem"><a href="${url}"><fmt:message key="playlistsheader.${cat}"/></a></span>
	            </c:otherwise>
	        </c:choose>
	    </c:forTokens>
	</ul>
</section>