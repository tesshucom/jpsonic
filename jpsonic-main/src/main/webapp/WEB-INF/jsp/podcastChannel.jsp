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
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script>

$(document).ready(function(){
    $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
        buttons: {
            "<fmt:message key="common.delete"/>": function() {location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}&deleteChannel=${model.channel.id}";},
            "<fmt:message key="common.cancel"/>": function() {$(this).dialog("close");}}});
    checkTruncate();
	function onResize(c,t){onresize=function(){clearTimeout(t);t=setTimeout(c,100)};return c};
	onResize(function() {checkTruncate();})();
});

function downloadSelected() {
    location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}" +
            "&downloadEpisode=" + getSelectedEpisodes();
}

function deleteChannel() {
    $("#dialog-delete").dialog("open");
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

function checkTruncate() {
    $('.tabular.episodes tr td.truncate').each(function(index , e) {
        $(e).removeClass('truncate');
        $(e).children('span').removeAttr('title');
    });
    if($('.mainframe').width() < $('.tabular.episodes').width() + 10){
        const threshold = $('.mainframe').width() / 4;
        function writeTruncate($clazz){
            $('.tabular.episodes tr td.' + $clazz).each(function(index , e) {
                if(threshold < $(e).width()){
                    $(e).addClass('truncate');
                    $(e).children('span').attr('title', $(e).text());
                }
            });
        }
        writeTruncate('description');
        writeTruncate('episode-title');
    }
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
    <div>
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
    </div>
    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="podcastChannelId" value="${model.channel.id}"/>
            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
        </c:import>
    </div>
</div>

<div id="dialog-delete" title="<fmt:message key='common.confirm'/>">
    <p>
        <span class="ui-icon ui-icon-alert"></span>
        <fmt:message key="podcastreceiver.confirmdelete"/>
    </p>
</div>

</body></html>
