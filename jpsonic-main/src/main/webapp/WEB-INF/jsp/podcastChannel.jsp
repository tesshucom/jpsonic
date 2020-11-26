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

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
<script src="<c:url value='/script/jpsonic/truncate.js'/>"></script>
<script src="<c:url value='/script/jpsonic/dialogs.js'/>"></script>
<script>

$(document).ready(function(){

    initTruncate(".tabular-and-thumb", ".tabular.episodes", 4, ["description", "episode-title"]);

    const ps = new PrefferedSize(480, 180);
    top.$("#dialog-delete").dialog({
    	autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : ps.width,
        height  : ps.height,
        buttons: {
            "<fmt:message key="common.delete"/>": function() {
            	top.$("#dialog-delete").dialog("close");
            	location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}&deleteChannel=${model.channel.id}";
            },
            "<fmt:message key="common.cancel"/>": {
            	text: "<fmt:message key="common.cancel"/>",
            	id: 'ddCancelButton',
            	click: function() {top.$("#dialog-delete").dialog("close");}
            }
        },
	    open: function() {top.$("#ddCancelButton").focus();}
    });
    top.$("#dialog-delete").text("<fmt:message key='podcastreceiver.confirmdelete2'><fmt:param><sub:escapeJavaScript string='${model.channel.title}'/></fmt:param></fmt:message>");
});

function downloadSelected() {
    location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}" +
            "&downloadEpisode=" + getSelectedEpisodes();
}

function deleteChannel() {
    top.$("#dialog-delete").dialog("open");
}

function deleteSelected() {
    location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}" +
            "&deleteEpisode=" + getSelectedEpisodes();
}

function refreshChannels() {
    location.href = "podcastReceiverAdmin.view?refresh&channelId=${model.channel.id}";
}

function refreshPage() {
    location.href = "podcastChannel.view?id=${model.channel.id}";
}

function getSelectedEpisodes() {
    var result = "";
    for (var i = 0; i < ${fn:length(model.episodes)}; i++) {
        var checkbox = $("#episode" + i);
        if (checkbox.is(":checked")) {
            result += (checkbox.val() + " ");
        }
    }
    return result;
}

</script>
</head>
<body class="mainframe podcastChannel">

<nav>
    <ul class="breadcrumb">
        <li><a href="podcastChannels.view"><fmt:message key="podcastreceiver.title"/></a></li>
    </ul>
</nav>

<section>
    <c:choose>
        <c:when test="${empty model.channel.description}">
            <h1 class="podcast">${fn:escapeXml(model.channel.title)}</h1>
        </c:when>
        <c:otherwise>
            <details>
                <summary><h1 class="podcast">${fn:escapeXml(model.channel.title)}</h1></summary>
                <div class="description">${fn:escapeXml(model.channel.description)}</div>
            </details>
        </c:otherwise>
    </c:choose>
</section>

<div class="actions">
    <ul class="controls">
        <li><a href="javascript:top.playQueue.onPlayPodcastChannel(${model.channel.id})" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='podcastreceiver.check'/></a></li>
        <c:choose>
            <c:when test="${model.user.podcastRole}">
                <li><a href="javascript:deleteChannel()" title="<fmt:message key='common.delete'/>" class="control cross"><fmt:message key='common.delete'/></a></li>
                <li><a href="javascript:refreshChannels()" title="<fmt:message key='podcastreceiver.check'/>" class="control refresh"><fmt:message key='podcastreceiver.check'/></a></li>
            </c:when>
            <c:otherwise>
                <li><a href="javascript:refreshPage()" title="<fmt:message key='podcastreceiver.refresh'/>" class="control refresh"><fmt:message key='podcastreceiver.refresh'/></a></li>
            </c:otherwise>
        </c:choose>
    </ul>
</div>

<div class="tabular-and-thumb">

        <c:if test="${not empty model.episodes}">
            <table class="tabular episodes">
                <c:if test="${model.channel.status eq 'ERROR'}">
                    <caption><strong>${model.channel.errorMessage}</strong></caption>
                </c:if>
                <thead>
                    <th></th><%-- play --%>
                    <th></th><%-- add --%>
                    <th></th><%-- next --%>
                    <th></th><%-- check --%>
                    <th class="song"><fmt:message key="podcast.episodename" /></th>
                    <th class="description"><fmt:message key="podcast.description" /></th>
                    <th class="status"><fmt:message key="podcast.status" /></th>
                    <th class="duration"><fmt:message key="podcast.duration" /></th>
                    <th class="date"><fmt:message key="podcast.publishdate" /></th>
                </thead>
                <tbody>
                    <c:forEach items="${model.episodes}" var="episode" varStatus="i">
                        <tr>
                            <c:import url="playButtons.jsp">
                                <c:param name="id" value="${episode.mediaFileId}"/>
                                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                                <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                                <c:param name="asTable" value="true"/>
                                <c:param name="onPlay" value="top.playQueue.onPlayPodcastEpisode(${episode.id})"/>
                            </c:import>
                            <td><input type="checkbox" id="episode${i.index}" value="${episode.id}"/></td>
                            <td class="episode-title"><span>${episode.title}</span></td>
                            <td class="description">
                                <span>
                                    <c:choose>
                                        <c:when test="${episode.status eq 'ERROR'}">${episode.errorMessage}</c:when>
                                        <c:otherwise>${episode.description}</c:otherwise>
                                    </c:choose>
                                </span>
                            </td>
                            <td class="state">
                                <c:choose>
                                    <c:when test="${episode.status eq 'DOWNLOADING'}"><fmt:formatNumber type="percent" value="${episode.completionRate}"/></c:when>
                                    <c:otherwise><fmt:message key="podcastreceiver.status.${fn:toLowerCase(episode.status)}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="duration">${episode.duration}</td>
                            <td class="date"><fmt:formatDate value="${episode.publishDate}" dateStyle="medium"/></td>
                        </tr>
                    </c:forEach>
                </tbody>        
            </table>
            <c:if test="${model.user.podcastRole}">
                <div class="actions">
                    <ul class="controls">
                        <li><a href="javascript:downloadSelected()" title="<fmt:message key='podcastreceiver.downloadselected'/>" class="control download"><fmt:message key='podcastreceiver.downloadselected'/></a></li>
                        <li><a href="javascript:deleteSelected()" title="<fmt:message key='podcastreceiver.deleteselected'/>" class="control cross"><fmt:message key='podcastreceiver.deleteselected'/></a></li>
                    </ul>
                </div>
            </c:if>
        </c:if>

    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="podcastChannelId" value="${model.channel.id}"/>
            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
        </c:import>
    </div>
</div>

</body></html>
