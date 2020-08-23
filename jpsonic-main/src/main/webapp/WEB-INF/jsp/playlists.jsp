<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script src="<c:url value='/script/jpsonic/coverartContainer.js'/>"></script>
</head>
<body class="mainframe playlists">

<c:import url="playlistsHeader.jsp">
    <c:param name="cat" value="playlists"/>
</c:import>

<div class="actions">
    <ul class="controls">
        <c:import url="viewSelector.jsp">
            <c:param name="targetView" value="playlists.view"/>
        </c:import>
    </ul>
</div>

<c:if test="${empty model.playlists}">
    <p><strong><fmt:message key="playlist2.noplaylists"/></strong></p>
</c:if>

<c:if test="${not empty model.playlists}">
    <c:choose>
        <c:when test="${model.viewAsList}">
            <table class="tabular playlists">
                <thead>
                    <tr>
                        <th></th><%-- play --%>
                        <th></th><%-- add --%>
                        <th><fmt:message key="playlist2.name" /></th>
                        <th><fmt:message key="playlist2.numberofsongs" /></th>
                        <th><fmt:message key="playlist2.duration" /></th>
                        <th><fmt:message key="playlist2.created" /></th>
                        <th><fmt:message key="playlist2.author" /></th>
                        <th><fmt:message key="playlist2.visibility" /></th>
                        <th><fmt:message key="playlist2.comment" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.playlists}" var="playlist">
                        <tr>
                            <td>
                                <div onclick="top.playQueue.onPlayPlaylist(${playlist.id}); return false;" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/></div>
                            </td>
                            <td>
                                <div onclick="top.playQueue.onAddPlaylist(${playlist.id}); return false;" title="<fmt:message key='common.add'/>" class="control plus"><fmt:message key='common.add'/></div>
                            </td>
                            <c:url value="playlist.view" var="targetUrl">
                                <c:param name="id" value="${playlist.id}"/>
                            </c:url>
                            <td class="name"><a href="${targetUrl}" title="${fn:escapeXml(playlist.name)}">${fn:escapeXml(playlist.name)}</a></td>
                            <td class="numberofsongs">${playlist.fileCount} <fmt:message key="playlist2.songs"/></td>
                            <td class="duration">${playlist.durationAsString}</td>
                            <td class="created"><fmt:formatDate type="date" dateStyle="long" value="${playlist.created}"/></td>
                            <td class="author">${fn:escapeXml(playlist.username)}</td>
                            <td class="visibility">
                                <c:choose>
                                    <c:when test="${playlist.shared}">
                                        <strong><fmt:message key="playlist2.shared"/></strong>
                                    </c:when>
                                    <c:otherwise>
                                        <fmt:message key="playlist2.notshared"/>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="comment">${fn:escapeXml(playlist.comment)}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise>
            <div class="coverart-container">
                <c:forEach items="${model.playlists}" var="playlist" varStatus="loopStatus">
                    <c:set var="caption2">
                        ${playlist.fileCount} <fmt:message key="playlist2.songs"/> &ndash; ${playlist.durationAsString}
                    </c:set>
                    <div class="albumThumb">
                        <c:import url="coverArt.jsp">
                            <c:param name="playlistId" value="${playlist.id}"/>
                            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
                            <c:param name="caption1" value="${fn:escapeXml(playlist.name)}"/>
                            <c:param name="caption2" value="${caption2}"/>
                            <c:param name="captionCount" value="2"/>
                            <c:param name="showLink" value="true"/>
                            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                        </c:import>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<c:if test="${model.publishPodcast}">
    <h2><fmt:message key="more.podcast.title"/></h2>
    <fmt:message key="more.podcast.text"/>
</c:if>

</body>
</html>
