<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ page trimDirectiveWhitespaces="true" %>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script type="text/javascript" language="javascript">
function refresh() {
    window.parent.main.location.href = location.href;
}

function playShuffle() {
  top.playQueue.onPlayShuffle('${model.listType}', ${model.listOffset}, ${model.listSize}, '${model.genre}', '${model.decade}')
}

$(document).ready(function(){
  <c:if test="${not model.musicFoldersExist}">
    $().toastmessage("showNoticeToast", "<fmt:message key="top.missing"/>");
  </c:if>
  <c:if test="${model.isIndexBeingCreated}">
    $().toastmessage("showNoticeToast", "<fmt:message key="home.scan"/>");
  </c:if>
});
</script>
</head>

<body class="mainframe home">

<section>

    <c:if test="${not empty model.welcomeTitle}">
        <h1><img src="<spring:theme code='homeImage'/>">${model.welcomeTitle}</h1>
    </c:if>
    <c:if test="${not empty model.welcomeSubtitle}">
        <h2>${model.welcomeSubtitle}</h2>
    </c:if>

    <ul class="subMenu">
        <c:forTokens items="random newest starred highest frequent recent decade genre alphabetical index" delims=" " var="cat" varStatus="loopStatus">
            <c:choose>
               <c:when test="${loopStatus.count > 1}"></li><li></c:when>
               <c:otherwise><li></c:otherwise>
            </c:choose>
            <sub:url var="url" value="home.view">
                <sub:param name="listType" value="${cat}"/>
            </sub:url>
            <c:choose>
                <c:when test="${model.listType eq cat}">
                    <span class="menuItemSelected"><fmt:message key="home.${cat}.title"/></span>
                </c:when>
                <c:otherwise>
                    <span class="menuItem"><a href="${url}"><fmt:message key="home.${cat}.title"/></a></span>
                </c:otherwise>
            </c:choose>
        </c:forTokens>
    </ul>

    <%@ include file="homePager.jsp" %>

    <c:if test="${not empty model.welcomeMessage}">
        <div class="welcome">${model.welcomeMessage}</div>
    </c:if>

</section>

<div class="albums">
    <c:forEach items="${model.albums}" var="album" varStatus="loopStatus">

        <c:set var="albumTitle">
            <c:choose>
                <c:when test="${empty album.albumTitle}">
                    <fmt:message key="common.unknown"/>
                </c:when>
                <c:otherwise>
                    ${album.albumTitle}
                </c:otherwise>
            </c:choose>
        </c:set>

        <c:set var="captionCount" value="2"/>

        <c:if test="${not empty album.playCount}">
            <c:set var="caption3"><fmt:message key="home.playcount"><fmt:param value="${album.playCount}"/></fmt:message></c:set>
            <c:set var="captionCount" value="3"/>
        </c:if>
        <c:if test="${not empty album.lastPlayed}">
            <fmt:formatDate value="${album.lastPlayed}" dateStyle="short" var="lastPlayedDate"/>
            <c:set var="caption3"><fmt:message key="home.lastplayed"><fmt:param value="${lastPlayedDate}"/></fmt:message></c:set>
            <c:set var="captionCount" value="3"/>
        </c:if>
        <c:if test="${not empty album.created}">
            <fmt:formatDate value="${album.created}" dateStyle="short" var="creationDate"/>
            <c:set var="caption3"><fmt:message key="home.created"><fmt:param value="${creationDate}"/></fmt:message></c:set>
            <c:set var="captionCount" value="3"/>
        </c:if>
        <c:if test="${not empty album.year}">
            <c:set var="caption3" value="${album.year}"/>
            <c:set var="captionCount" value="3"/>
        </c:if>
        <div class="albumThumb">
            <c:import url="coverArt.jsp">
                <c:param name="albumId" value="${album.id}"/>
                <c:param name="caption1" value="${fn:escapeXml(album.albumTitle)}"/>
                <c:param name="caption2" value="${fn:escapeXml(album.artist)}"/>
                <c:param name="caption3" value="${caption3}"/>
                <c:param name="captionCount" value="${captionCount}"/>
                <c:param name="coverArtSize" value="${model.coverArtSize}"/>
                <c:param name="showLink" value="true"/>
                <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                <c:param name="hideOverflow" value="true"/>
            </c:import>

            <c:if test="${not empty album.rating}">
                <c:import url="rating.jsp">
                    <c:param name="readonly" value="true"/>
                    <c:param name="rating" value="${album.rating}"/>
                </c:import>
            </c:if>
    
        </div>
    </c:forEach>
</div>

<c:set var="isOpen" value='${model.isOpenDetailIndex ? "open" : ""}' />

<c:if test="${not empty model.indexedArtists}">
    <c:forEach items="${model.indexedArtists}" var="entry" varStatus="status">
        <details ${isOpen}>
            <c:set var="accesskey" value="${fn:escapeXml(entry.key.index)}" />
            <c:if test="${model.assignAccesskeyToNumber}">
                <c:set var="accesskey" value="${status.index%5 == 0 ? Integer.toString(status.index/5) : null}" />
            </c:if>
            <c:choose>
                <c:when test="${!empty accesskey}">
                    <summary accesskey="${fn:escapeXml(accesskey)}">${fn:escapeXml(entry.key.index)}</summary>
                </c:when>
                <c:otherwise>
                    <summary>${fn:escapeXml(entry.key.index)}</summary>
                </c:otherwise>
            </c:choose>
            <ul>
                <c:forEach items="${entry.value}" var="artist" varStatus="loop">
                    <sub:url value="main.view" var="mainUrl">
                        <c:forEach items="${artist.mediaFiles}" var="mediaFile">
                            <sub:param name="id" value="${mediaFile.id}"/>
                        </c:forEach>
                    </sub:url>
                    <li><a target="main" href="${mainUrl}" title="${artist.sortableName}">${fn:escapeXml(artist.name)}</a></li>
                </c:forEach>
            </ul>
        </details>
    </c:forEach>
</c:if>

<c:if test="${not empty model.singleSongs}">
    <details>
        <summary><fmt:message key="home.index.child"/></summary>
        <ul>
            <c:forEach items="${model.singleSongs}" var="song">
                <li>
                    ${fn:escapeXml(song.title)}
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${song.id}"/>
                        <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                        <c:param name="addEnabled" value="${model.user.streamRole}"/>
                        <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyMode}"/>
                        <c:param name="video" value="${song.video and model.player.web}"/>
                    </c:import>
                </li>
            </c:forEach>
        </ul>
    </details>
</c:if>

</body></html>
