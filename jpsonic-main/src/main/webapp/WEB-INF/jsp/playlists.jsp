<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe">

<c:import url="playlistsHeader.jsp">
    <c:param name="cat" value="playlists"/>
</c:import>

<c:if test="${empty model.playlists}">
    <p><em><fmt:message key="playlist2.noplaylists"/></em></p>
</c:if>

<c:forEach items="${model.playlists}" var="playlist" varStatus="loopStatus">

    <c:set var="caption2">
        ${playlist.fileCount} <fmt:message key="playlist2.songs"/> &ndash; ${playlist.durationAsString}
    </c:set>
    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="playlistId" value="${playlist.id}"/>
            <c:param name="coverArtSize" value="200"/>
            <c:param name="caption1" value="${fn:escapeXml(playlist.name)}"/>
            <c:param name="caption2" value="${caption2}"/>
            <c:param name="captionCount" value="2"/>
            <c:param name="showLink" value="true"/>
            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
        </c:import>
    </div>
</c:forEach>

<h2><img src="<spring:theme code='podcastImage'/>" alt=""/><fmt:message key="more.podcast.title"/></h2>
<fmt:message key="more.podcast.text"/>

</body>
</html>
