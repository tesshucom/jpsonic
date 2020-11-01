<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--
  ~ This file is part of Airsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2015 (C) Sindre Mehus
  --%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
<script src="<c:url value='/script/jpsonic/coverartContainer.js'/>"></script>
<script src="<c:url value='/script/jpsonic/truncate.js'/>"></script>
<script>
$(document).ready(function(){
    initTruncate(".mainframe", ".tabular.channels", 2, ["name", "description"]);
    initTruncate(".mainframe", ".tabular.episodes", 3, ["episodeTitle", "channelTitle"]);
});
</script>
</head><body class="mainframe podcastChannels">

<c:import url="podcastsHeader.jsp">
    <c:param name="cat" value="podcasts"/>
    <c:param name="restricted" value="${not model.user.adminRole}"/>
</c:import>

<div class="actions">
    <ul class="controls">
        <c:import url="viewAsListSelector.jsp">
            <c:param name="targetView" value="podcastChannels.view"/>
            <c:param name="viewAsList" value="${model.viewAsList}"/>
            <c:param name="directoryId" value="${model.dir.id}"/>
        </c:import>
    </ul>
</div>

<c:if test="${empty model.channels}">
    <p><strong><fmt:message key="podcastreceiver.empty"/></strong></p>
</c:if>

<c:if test="${not empty model.channels}">
    <c:choose>
        <c:when test="${model.viewAsList}">
            <table class="tabular channels">
                <thead>
                    <tr>
                        <th></th><%-- play --%>
                        <th class="name"><fmt:message key="podcast.channelname" /></th>
                        <th class="description"><fmt:message key="podcast.description" /></th>
                        <th class="count"><fmt:message key="podcast.count" /></th>
                        <th class="status"><fmt:message key="podcast.status" /></th>
                        <th class="url"><fmt:message key="podcast.url" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.channels}" var="channel" varStatus="loopStatus">
                        <tr>
                            <td>
                                <div onclick="top.playQueue.onPlayPodcastChannel(${channel.key.id}); return false;" title="<fmt:message key='common.play'/>" class="control play">
                                    <fmt:message key='common.play'/>
                                </div>
                            </td>
                            <c:url value="podcastChannel.view" var="targetUrl">
                                <c:param name="id" value="${channel.key.id}"/>
                            </c:url>
                            <td class="name"><span><a href="${targetUrl}" title="${fn:escapeXml(channel.key.title)}">${fn:escapeXml(channel.key.title)}</a></span></td>
                            <td class="description"><span><str:truncateNicely upper="${50}" lower="${70}" >${fn:escapeXml(channel.key.description)}</str:truncateNicely></span></td>
                            <td class="count">${fn:length(channel.value)}</td>
                            <td class="status"><fmt:message key="podcastreceiver.status.${fn:toLowerCase(channel.key.status)}"/></td>
                            <td class="url"><input type="text" value="${channel.key.url}" readonly class="url"></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise>
            <div class="coverart-container">
                <c:forEach items="${model.channels}" var="channel" varStatus="loopStatus">
                    <c:set var="caption2">
                        <fmt:message key="podcastreceiver.episodes"><fmt:param value="${fn:length(channel.value)}"/></fmt:message>
                    </c:set>
                    <div class="albumThumb">
                        <c:import url="coverArt.jsp">
                            <c:param name="podcastChannelId" value="${channel.key.id}"/>
                            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
                            <c:param name="caption1" value="${fn:escapeXml(channel.key.title)}"/>
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

<c:if test="${not empty model.newestEpisodes}">
    <h2><fmt:message key="podcastreceiver.newestepisodes"/></h2>
    <c:if test="${model.user.podcastRole}">
        <div class="actions">
            <ul class="controls">
                <li><a href="podcastReceiverAdmin.view?refresh" title="<fmt:message key='podcastreceiver.check'/>" class="control refresh"><fmt:message key='podcastreceiver.check'/></a></li>
            </ul>
        </div>
    </c:if>
    
    <table class="tabular episodes">
        <thead>
            <tr>
                <th></th><%-- play --%>
                <th></th><%-- add --%>
                <th></th><%-- next --%>
                <th><fmt:message key="podcast.episodename" /></th>
                <th><fmt:message key="podcast.channelname" /></th>
                <th class="duration"><fmt:message key="podcast.duration" /></th>
                <th class="date"><fmt:message key="podcast.publishdate" /></th>
            </tr>
        </thead>
        <tbody>
	        <c:forEach items="${model.newestEpisodes}" var="episode" varStatus="i">
	            <tr>
	                <c:import url="playButtons.jsp">
	                    <c:param name="id" value="${episode.mediaFileId}"/>
	                    <c:param name="podcastEpisodeId" value="${episode.id}"/>
	                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
	                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
	                    <c:param name="asTable" value="true"/>
	                    <c:param name="onPlay" value="top.playQueue.onPlayNewestPodcastEpisode(${i.index})"/>
	                </c:import>
	                <td class="episodeTitle"><span>${episode.title}</span></td>
	                <c:set var="channelTitle" value="${model.channelMap[episode.channelId].title}"/>
	                <td class="channelTitle"><span><a href="podcastChannel.view?id=${episode.channelId}">${channelTitle}</a></span></td>
	                <td class="duration">${episode.duration}</td>
	                <td class="date"><fmt:formatDate value="${episode.publishDate}" dateStyle="medium"/></td>
	            </tr>
	        </c:forEach>
        </tbody>
    </table>
</c:if>

<c:if test="${model.user.podcastRole}">

    <h2><fmt:message key="podcastreceiver.subscribe"/></h2>

    <form:form method="post" action="podcastReceiverAdmin.view?">
        <dl class="single">
            <dt></dt>
            <dd>
                <input type="text" name="add" value="http://" onclick="select()"/>
                <input type="submit" value="<fmt:message key='common.ok'/>"/>
            </dd>
        </dl>
    </form:form>
</c:if>

</body>
</html>
