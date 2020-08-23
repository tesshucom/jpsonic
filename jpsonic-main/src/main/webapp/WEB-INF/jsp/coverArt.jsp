<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  albumId: ID of album.
  playlistId: ID of playlist.
  podcastChannelId: ID of podcast channel
  coverArtSize: Height and width of cover art.
  caption1: Caption line 1
  caption2: Caption line 2
  caption3: Caption line 3
  captionCount: Number of caption lines to display (default 0)
  showLink: Whether to make the cover art image link to the album page.
  showZoom: Whether to display a link for zooming the cover art.
  appearAfter: Fade in after this many milliseconds, or nil if no fading in should happen.
  hideOverflow: Hide cover art overflow when height is greater than width
--%>
<c:choose>
    <c:when test="${empty param.coverArtSize}">
        <c:set var="size" value="auto"/>
    </c:when>
    <c:otherwise>
        <c:set var="size" value="${param.coverArtSize}px"/>
    </c:otherwise>
</c:choose>

<c:set var="captionCount" value="${empty param.captionCount ? 0 : param.captionCount}"/>

<str:randomString count="5" type="alphabet" var="divId"/>
<str:randomString count="5" type="alphabet" var="imgId"/>
<str:randomString count="5" type="alphabet" var="playId"/>
<str:randomString count="5" type="alphabet" var="addId"/>

<div class="coverart">
    <div style="width:${size};max-width:${size};min-width:${size};height:${size};max-height:${size};cursor:pointer;<c:if test='${param.hideOverflow}'>overflow:hidden</c:if>;" title="${param.caption1}" id="${divId}">

        <c:if test="${not empty param.albumId}">
            <c:url value="main.view" var="targetUrl">
                <c:param name="id" value="${param.albumId}"/>
            </c:url>
        </c:if>
        <c:if test="${not empty param.playlistId}">
            <c:url value="playlist.view" var="targetUrl">
                <c:param name="id" value="${param.playlistId}"/>
            </c:url>
        </c:if>
        <c:if test="${not empty param.podcastChannelId}">
            <c:url value="podcastChannel.view" var="targetUrl">
                <c:param name="id" value="${param.podcastChannelId}"/>
            </c:url>
        </c:if>

        <c:url value="/coverArt.view" var="coverArtUrl">
            <c:if test="${not empty param.coverArtSize}">
                <c:param name="size" value="${param.coverArtSize}"/>
            </c:if>
            <c:if test="${not empty param.albumId}">
                <c:param name="id" value="${param.albumId}"/>
            </c:if>
            <c:if test="${not empty param.podcastChannelId}">
                <c:param name="id" value="pod-${param.podcastChannelId}"/>
            </c:if>
            <c:if test="${not empty param.playlistId}">
                <c:param name="id" value="pl-${param.playlistId}"/>
            </c:if>
        </c:url>

        <c:url value="/coverArt.view" var="zoomCoverArtUrl">
            <c:param name="id" value="${param.albumId}"/>
        </c:url>

        <ul class="controls">
        	<li><div id="${playId}" title="<fmt:message key='main.playall'/>" class="control play"><fmt:message key="main.playall"/></div></li>
	        <c:if test="${not empty param.albumId or not empty param.playlistId}">
				<li><div id="${addId}" title="<fmt:message key='main.addall'/>" class="control plus"><fmt:message key="main.addall"/></div></li>
	        </c:if>
	    </ul>
        <c:choose>
        	<c:when test="${param.showLink}"><a tabindex="-1" href="${targetUrl}" title="${param.caption1}"></c:when>
        	<c:when test="${param.showZoom}"><a tabindex="-1" href="${zoomCoverArtUrl}" class="fancy" rel="zoom" title="${param.caption1}"></c:when>
        </c:choose>
        <img src="${coverArtUrl}" id="${imgId}" alt="${param.caption1}" style="display:none">
        <c:if test="${param.showLink or param.showZoom}"></a></c:if>
    </div>

    <c:if test="${captionCount gt 0}">
        <div class="caption1" style="width:${param.coverArtSize}px"><a href="${targetUrl}" title="${param.caption1}">${param.caption1}</a></div>
    </c:if>
    <c:if test="${captionCount gt 1}">
        <div class="caption2" style="width:${param.coverArtSize}px">${param.caption2}</div>
    </c:if>
    <c:if test="${captionCount gt 2}">
        <div class="caption3" style="width:${param.coverArtSize}px">${param.caption3}</div>
    </c:if>
</div>

<script>
    $(document).ready(function () {
        setTimeout("$('#${imgId}').fadeIn(500)", ${empty param.appearAfter ? 0 : param.appearAfter});
    });

    $("#${divId}").mouseenter(function () {
        $("#${playId}").show();
        $("#${addId}").show();
        $("#${imgId}").stop();
        $("#${imgId}").animate({opacity: 0.7}, 150);
    });
    $("#${divId}").mouseleave(function () {
        $("#${playId}").hide();
        $("#${addId}").hide();
        $("#${imgId}").stop();
        $("#${imgId}").animate({opacity: 1.0}, 150);
    });
    $("#${playId}").click(function () {
        <c:if test="${not empty param.albumId}">
        top.playQueue.onPlay(${param.albumId});
        </c:if>
        <c:if test="${not empty param.playlistId}">
        top.playQueue.onPlayPlaylist(${param.playlistId});
        </c:if>
        <c:if test="${not empty param.podcastChannelId}">
        top.playQueue.onPlayPodcastChannel(${param.podcastChannelId});
        </c:if>
    });
    $("#${addId}").click(function () {
        <c:if test="${not empty param.albumId}">
        top.playQueue.onAdd(${param.albumId});
        </c:if>
        <c:if test="${not empty param.playlistId}">
        top.playQueue.onAddPlaylist(${param.playlistId});
        </c:if>
    });
</script>
