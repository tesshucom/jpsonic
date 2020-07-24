<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/multiService.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
<script>

var previousQuery = "";
var instantSearchTimeout;
var showSideBar = ${model.showSideBar ? 'true' : 'false'};
var playlists;

function init() {
    dwr.engine.setErrorHandler(null);
    updatePlaylists();

    var mainLocation = top.main.location.href;
    if (${model.musicFolderChanged}) {
        if (mainLocation.indexOf("/home.view") != -1) {
            top.main.location.href = mainLocation;
        }
    }

    $('.radio-play').on('click', function(evt) {
        top.playQueue.onPlayInternetRadio($(this).data("id"), 0);
        evt.preventDefault();
    });
}

function triggerInstantSearch() {
    if (instantSearchTimeout) {
        window.clearTimeout(instantSearchTimeout);
    }
    instantSearchTimeout = window.setTimeout(executeInstantSearch, 300);
}

function executeInstantSearch() {
    var query = $("#query").val().trim();
    if (query.length > 1 && query != previousQuery) {
        previousQuery = query;
        document.searchForm.submit();
    }
}

function showLeftFrame() {
    $("#show-left-frame").hide();
    $("#hide-left-frame").show();

    multiService.setShowSideBar(true);
    showSideBar = true;
}

function hideLeftFrame() {
    $("#hide-left-frame").hide();
    $("#show-left-frame").show();
  
    multiService.setShowSideBar(false);
    showSideBar = false;
}

function toggleLeftFrameVisible() {
    if (showSideBar) hideLeftFrame();
    else showLeftFrame();
}

function toggleLeftFrame(width) {

}

function updatePlaylists() {
    playlistService.getReadablePlaylists(playlistCallback);
}

function createEmptyPlaylist() {
    showAllPlaylists();
    playlistService.createEmptyPlaylist(playlistCallback);
}

function showAllPlaylists() {
    $('#playlistOverflow').show('blind');
    $('#showAllPlaylists').hide('blind');
}

function playlistCallback(playlists) {
    this.playlists = playlists;

    $("#playlists").empty();
    $("#playlistOverflow").empty();
    for (var i = 0; i < playlists.length; i++) {
        var playlist = playlists[i];
        var overflow = i > 9;
        $("<p class='dense" + (i == playlists.length -1 ? " last" : "") + "'><a target='main' href='playlist.view?id=" +
                playlist.id + "'>" + escapeHtml(playlist.name) + "&nbsp;(" + playlist.fileCount + ")</a></p>").appendTo(overflow ? "#playlistOverflow" : "#playlists");
    }

    if (playlists.length > 10 && !$('#playlistOverflow').is(":visible")) {
        $('#showAllPlaylists').show();
    }
}

</script>
</head>

<body class="bgcolor2 topframe" style="margin:0.4em 1em 0 1em;" onload="init()">

<span id="dummy-animation-target" style="max-width:0;display: none"></span>

<fmt:message key="top.home" var="home"/>
<fmt:message key="top.now_playing" var="nowPlaying"/>
<fmt:message key="top.starred" var="starred"/>
<fmt:message key="left.playlists" var="playlists"/>
<fmt:message key="top.settings" var="settings"/>
<fmt:message key="top.status" var="status" />
<fmt:message key="top.podcast" var="podcast"/>
<fmt:message key="top.more" var="more"/>
<fmt:message key="top.help" var="help"/>
<fmt:message key="top.search" var="search"/>

<table style="margin:0;padding-top:5px">
    <tr>
        <td style="padding-right:4.5em;">
            <img id="show-left-frame" src="<spring:theme code='sidebarImage'/>" onclick="showLeftFrame()" alt="" style="display:${model.showSideBar ? 'none' : 'inline'};cursor:pointer">
            <img id="hide-left-frame" src="<spring:theme code='sidebarImage'/>" onclick="hideLeftFrame()" alt="" style="display:${model.showSideBar ? 'inline' : 'none'};cursor:pointer">
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="home.view?" target="main"><img src="<spring:theme code='homeImage'/>" title="${home}" alt="${home}"></a>
            <div class="topHeader"><a href="home.view?" target="main">${home}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="nowPlaying.view?" target="main"><img src="<spring:theme code='nowPlayingImage'/>" title="${nowPlaying}" alt="${nowPlaying}"></a>
            <div class="topHeader"><a href="nowPlaying.view?" target="main">${nowPlaying}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="starred.view?" target="main"><img src="<spring:theme code='starredImage'/>" title="${starred}" alt="${starred}"></a>
            <div class="topHeader"><a href="starred.view?" target="main">${starred}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="playlists.view?" target="main"><img src="<spring:theme code='playlistImage'/>" title="${playlists}" alt="${playlists}"></a>
            <div class="topHeader"><a href="playlists.view?" target="main">${playlists}</a></div>
        </td>
        <td style="min-width:4em;padding-right:1em;text-align: center">
            <a href="podcastChannels.view?" target="main"><img src="<spring:theme code='podcastLargeImage'/>" title="${podcast}" alt="${podcast}"></a>
            <div class="topHeader"><a href="podcastChannels.view?" target="main">${podcast}</a></div>
        </td>
        <c:if test="${model.user.settingsRole}">
            <td style="min-width:3em;padding-right:1em;text-align: center">
                <a href="settings.view?" target="main"><img src="<spring:theme code='settingsImage'/>" title="${settings}" alt="${settings}"></a>
                <div class="topHeader"><a href="settings.view?" target="main">${settings}</a></div>
            </td>
        </c:if>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="status.view?" target="main"><img src="<spring:theme code='statusImage'/>" title="${status}" alt="${status}"></a>
            <div class="topHeader"><a href="status.view?" target="main">${status}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="more.view?" target="main"><img src="<spring:theme code='moreImage'/>" title="${more}" alt="${more}"></a>
            <div class="topHeader"><a href="more.view?" target="main">${more}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="help.view?" target="main"><img src="<spring:theme code='helpImage'/>" title="${help}" alt="${help}"></a>
            <div class="topHeader"><a href="help.view?" target="main">${help}</a></div>
        </td>

        <td style="padding-left:1em">
            <form method="post" action="search.view" target="main" name="searchForm">
                <td><input required type="text" name="query" id="query" size="28" placeholder="${search}" onclick="select();"
                           onkeyup="triggerInstantSearch();"></td>
                <td><a href="javascript:document.searchForm.submit()"><img src="<spring:theme code='searchImage'/>" alt="${search}" title="${search}"></a></td>
            </form>
        </td>
        <td style="width:30%;"><td>
        <td style="padding-right:5pt;vertical-align: middle;width:32px;text-align: center">
            <c:if test="${model.user.settingsRole}"><a href="personalSettings.view" target="main"></c:if>
            <c:choose>
                <c:when test="${model.showAvatar}">
                    <sub:url value="avatar.view" var="avatarUrl">
                        <sub:param name="username" value="${model.user.username}"/>
                    </sub:url>
                    <div style="padding-bottom: 4px">
                        <img src="${avatarUrl}" alt="User" width="30" height="30">
                    </div>
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='userImage'/>" alt="User" height="30">
                </c:otherwise>
            </c:choose>

            <div class="detail">
                <c:out value="${model.user.username}" escapeXml="true"/>
            </div>
            <c:if test="${model.user.settingsRole}"></a></c:if>
        </td>

        <td style="padding-left:5pt;padding-right:5pt;vertical-align: right;width:32px;text-align: center">
            <a href="<c:url value='/logout'/>" target="_top">
                <img src="<spring:theme code='logoutImage'/>" alt="logout" height="24">
                <div class="detail">
                    <fmt:message key="top.logout" var="logout"></fmt:message>
                    <c:out value="${logout}"/>
                </div>
            </a>
        </td>

    </tr></table>

<%-- Block that was previously "left.jsp" --%>
<div id="left" class="bgcolor2 leftframe" style="width:200px;position:absolute;z-index:99">

	<a name="top"></a>
	
	<div style="padding-bottom:1.5em">
	    <a href="home.view" target="main">
	      <img src="<spring:theme code='logoImage'/>" style="width:196px" title="<fmt:message key='top.help'/>" alt="">
	    </a>
	</div>
	
	<c:if test="${fn:length(model.musicFolders) > 1}">
	    <div style="padding-bottom:1.0em">
	    <select name="musicFolderId" style="width:100%" onchange="location='top.view?musicFolderId=' + options[selectedIndex].value;">
	            <option value="-1"><fmt:message key="left.allfolders"/></option>
	            <c:forEach items="${model.musicFolders}" var="musicFolder">
	                <option ${model.selectedMusicFolder.id == musicFolder.id ? "selected" : ""} value="${musicFolder.id}">${fn:escapeXml(musicFolder.name)}</option>
	            </c:forEach>
	        </select>
	    </div>
	</c:if>
	
	<div style="margin-bottom:0.5em;padding-left: 2px" class="bgcolor1">
	    <c:forEach items="${model.indexes}" var="index">
	        <a href="#${index.index}" accesskey="${index.index}">${index.index}</a>
	    </c:forEach>
	</div>
	
	<div style="padding-bottom:0.5em">
	    <div class="forward">
	        <c:choose>
	            <c:when test="${model.scanning}">
	                <a href="top.view"><fmt:message key="common.refresh"/></a>
	            </c:when>
	            <c:otherwise>
	                <a href="top.view?refresh=true"><fmt:message key="common.refresh"/></a>
	            </c:otherwise>
	        </c:choose>
	    </div>
	</div>
	
	<c:if test="${not empty model.shortcuts}">
	    <h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.shortcut"/></h2>
	    <c:forEach items="${model.shortcuts}" var="shortcut">
	        <p class="dense" style="padding-left:2px">
	            <sub:url value="main.view" var="mainUrl">
	                <sub:param name="id" value="${shortcut.id}"/>
	            </sub:url>
	            <a target="main" href="${mainUrl}">${fn:escapeXml(shortcut.name)}</a>
	        </p>
	    </c:forEach>
	</c:if>
	
	<h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.playlists"/></h2>
	<div id="playlistWrapper" style="padding-left:2px">
	    <div id="playlists"></div>
	    <div id="playlistOverflow" style="display:none"></div>
	    <div style="padding-top: 0.3em"></div>
	    <div class="forward" id="showAllPlaylists" style="display: none"><a href="#" onclick="showAllPlaylists()"><fmt:message key="left.showallplaylists"/></a></div>
	    <div class="forward"><a href="#" onclick="createEmptyPlaylist()"><fmt:message key="left.createplaylist"/></a></div>
	    <div class="forward"><a href="importPlaylist.view" target="main"><fmt:message key="left.importplaylist"/></a></div>
	</div>
	
	<c:if test="${not empty model.radios}">
	    <h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.radio"/></h2>
	    <iframe src="left.jsp" id="radio-playlist-data" style="display:none;"></iframe>
	    <c:forEach items="${model.radios}" var="radio" varStatus="loop">
	        <p class="dense<c:if test='${loop.last}'> last</c:if>" style="padding-left: 2px">
	        <a target="hidden" href="${radio.streamUrl}" class="radio-play" data-id="${radio.id}">
	            <img src="<spring:theme code='playImage'/>" alt="<fmt:message key='common.play'/>" title="<fmt:message key='common.play'/>"></a>
	            <span style="vertical-align: middle">
	                <c:choose>
	                    <c:when test="${empty radio.homepageUrl}">
	                        ${fn:escapeXml(radio.name)}
	                    </c:when>
	                    <c:otherwise>
	                        <a target="_blank" rel="noopener" href="${radio.homepageUrl}">${fn:escapeXml(radio.name)}</a>
	                    </c:otherwise>
	                </c:choose>
	            </span>
	        </p>
	    </c:forEach>
	</c:if>
	
	<c:forEach items="${model.indexedArtists}" var="entry">
	    <table class="bgcolor1" style="width:100%;padding:0;margin:1em 0 0 0;border:0">
	        <tr style="padding:0;margin:0;border:0">
	            <th style="text-align:left;padding:0;margin:0;border:0"><a name="${fn:escapeXml(entry.key.index)}"></a>
	                <h2 style="padding:0;margin:0;border:0">${fn:escapeXml(entry.key.index)}</h2>
	            </th>
	            <th style="text-align:right;">
	                <a href="#top"><img src="<spring:theme code='upImage'/>" alt="" style="height:18px;"></a>
	            </th>
	        </tr>
	    </table>
	
	    <c:forEach items="${entry.value}" var="artist" varStatus="loop">
	        <p class="dense<c:if test='${loop.last}'> last</c:if>" style="padding-left:2px">
	            <span title="${artist.sortableName}">
	                <sub:url value="main.view" var="mainUrl">
	                    <c:forEach items="${artist.mediaFiles}" var="mediaFile">
	                        <sub:param name="id" value="${mediaFile.id}"/>
	                    </c:forEach>
	                </sub:url>
	                <a target="main" href="${mainUrl}"><str:truncateNicely upper="${35}">${fn:escapeXml(artist.name)}</str:truncateNicely></a>
	            </span>
	        </p>
	    </c:forEach>
	</c:forEach>
	
	<div style="padding-top:1em"></div>
	
	<c:forEach items="${model.singleSongs}" var="song">
	    <p class="dense" style="padding-left:2px">
	        <span class="songTitle" title="${fn:escapeXml(song.title)}">
	            <c:import url="playButtons.jsp">
	                <c:param name="id" value="${song.id}"/>
	                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
	                <c:param name="addEnabled" value="${model.user.streamRole}"/>
	                <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyMode}"/>
	                <c:param name="video" value="${song.video and model.player.web}"/>
	            </c:import>
	            <str:truncateNicely upper="${35}">${fn:escapeXml(song.title)}</str:truncateNicely>
	        </span>
	    </p>
	</c:forEach>
	
	<c:if test="${model.statistics.songCount gt 0}">
	    <div class="detail" style="padding-top: 0.6em; padding-left: 2px">
	        <fmt:message key="left.statistics">
	            <fmt:param value="${model.statistics.artistCount}"/>
	            <fmt:param value="${model.statistics.albumCount}"/>
	            <fmt:param value="${model.statistics.songCount}"/>
	            <fmt:param value="${model.bytes}"/>
	            <fmt:param value="${model.hours}"/>
	        </fmt:message>
	    </div>
	</c:if>
	
	<div style="height:5em"></div>
	
	<div class="bgcolor2" style="opacity: 1.0; clear: both; position: fixed; bottom: 0; right: 0; left: 0;
	      padding: 0.25em 0.75em 0.25em 0.75em; border-top:1px solid black; max-width: 850px;">
	    <c:forEach items="${model.indexes}" var="index">
	        <a href="#${index.index}">${index.index}</a>
	    </c:forEach>
	</div>

<div>

<%-- TODO #630
<script>
$(document).ready(function(){
    const left = document.getElementById('left')
    $('body', parent.document).append(left);
    setTimeout(function(){
        $(left).css({
            top:"0px",
            left:"500px",
        });
    },1);
});
 --%>

</script>
</body></html>
