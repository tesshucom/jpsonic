<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ page trimDirectiveWhitespaces="true" %>
<html>
<head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/multiService.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>

<script>

let previousQuery = "";
let instantSearchTimeout;
let playlists;
let drawer;
let toggler;
let main;
let playQueue;

$(document).ready(function(){

  drawer = document.getElementById('drawer');
  toggler = document.getElementById("toggler");
  main = document.getElementById("main");
  playQueue = window.top.document.getElementById('playQueue');

  const closeDrawer = ${model.closeDrawer};
  if(closeDrawer){
    toggler.checked = false;
  }
  toggleDrawer();

  toggler.onclick = function() {
    toggleDrawer();
  };

  $("#keyboardShortcuts").dialog({
    autoOpen: false,
    width: 640,
    height: 480,
    modal: false,
    open : function(){
      $("#keyboardShortcuts").append('<iframe id="iframeDiv"></iframe>');
      $("#iframeDiv").attr({
        src : "keyboardShortcuts.view?",
        width : '100%',
        height : '100%'
      });
    },
    close : function(){
      $("#iframeDiv").remove();
    }
  });

  dwr.engine.setErrorHandler(null);

  updatePlaylists();

  main.addEventListener("load", (e) => {
    if (${model.musicFolderChanged}) {
      // TODO ... if Home was displayed before refresh
    }
  });

  $('.radio-play').on('click', function(evt) {
    top.playQueue.onPlayInternetRadio($(this).data("id"), 0);
    evt.preventDefault();
  });

});

function toggleDrawer() {
  let mainWidth;
  if(toggler.checked) {
    mainWidth = "calc(100vw - 240px - " + ${model.showRight ? 240 : 0} + "px)";
    playQueue.style.left = '240px';
  } else {
    mainWidth = "calc(100vw - " + ${model.showRight ? 240 : 0} + "px)";
    playQueue.style.left = '0';
  }
  main.style.width = mainWidth;
  playQueue.style.width = mainWidth;
};

function onToggleDrawer() {
  toggler.checked = !toggler.checked;
  toggleDrawer();
}

function onQueryFocus() {
  $("#query").focus();
}

function onChangeMainLocation(location) {
  $('#main')[0].contentDocument.location = location;
}

function onShowKeyboardShortcuts() {
  $('#keyboardShortcuts').dialog('open');
}

function triggerInstantSearch() {
  if (instantSearchTimeout) {
    window.clearTimeout(instantSearchTimeout);
  }
  instantSearchTimeout = window.setTimeout(executeInstantSearch, 300);
}

function executeInstantSearch() {
  const query = $("#query").val().trim();
  if (query.length > 1 && query != previousQuery) {
    previousQuery = query;
    document.searchForm.submit();
  }
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
    const playlist = playlists[i];
    const overflow = i > 9;
      $("<p class='dense" + (i == playlists.length -1 ? " last" : "") +
        "'><a target='main' href='playlist.view?id=" + playlist.id + "'>" +
        escapeHtml(playlist.name) + "&nbsp;(" + playlist.fileCount + ")</a></p>")
          .appendTo(overflow ? "#playlistOverflow" : "#playlists");
    }
    if (playlists.length > 10 && !$('#playlistOverflow').is(":visible")) {
      $('#showAllPlaylists').show();
  }
}

<%-- Provisional : Events are relayed due to frame configuration changes. #631 --%>
function onToggleStartStop() {window.parent.onToggleStartStop();};
function onPrevious() {window.parent.onPrevious();};
function onNext() {window.parent.onNext();};
function onStarCurrent() {window.parent.onStarCurrent();};
function onGainAdd() {window.parent.onGainAdd(val);};
function onGainAdd() {window.parent.onGainAdd(val);};
function onTogglePlayQueue() {window.parent.onTogglePlayQueue();};

</script>
</head>

<body>

    <fmt:message key="top.home" var="home"/>
    <fmt:message key="top.now_playing" var="nowPlaying"/>
    <fmt:message key="top.starred" var="starred"/>
    <fmt:message key="top.playlists" var="playlists"/>
    <fmt:message key="top.settings" var="settings"/>
    <fmt:message key="top.status" var="status" />
    <fmt:message key="top.podcast" var="podcast"/>
    <fmt:message key="top.more" var="more"/>
    <fmt:message key="top.help" var="help"/>
    <fmt:message key="top.search" var="search"/>
    <fmt:message key="top.upload" var="upload"/>

    <%-- toggler --%>
    <input type="checkbox" id="toggler" class="jps-input-toggler" value="1" autofocus="true" checked/>
    <label for="toggler" class="jps-toggler" role="button" aria-pressed="false" aria-expanded="false" aria-label="Navigation button">
        <span class="jps-toggler-line"></span>
        <span class="jps-toggler-line"></span>
        <span class="jps-toggler-line"></span>
    </label>

    <%-- drawer --%>
    <div id="drawer" class="jps-drawer">

        <div class="jps-musicfolder">
            <c:if test="${fn:length(model.musicFolders) > 1}">
                <select name="musicFolderId" onchange="location='top.view?musicFolderId=' + options[selectedIndex].value;">
                    <option value="-1"><fmt:message key="left.allfolders"/></option>
                    <c:forEach items="${model.musicFolders}" var="musicFolder">
                        <option ${model.selectedMusicFolder.id == musicFolder.id ? "selected" : ""} value="${musicFolder.id}">${fn:escapeXml(musicFolder.name)}</option>
                    </c:forEach>
                </select>
            </c:if>
        </div>

        <c:set var="showIndex" value="${model.showIndex ? '' : 'checked'}" />
        <input type="checkbox" class="jps-input-without-index" value="1" autofocus="false" ${showIndex}/>
        <ul class="jps-index">
            <c:forEach items="${model.indexedArtists}" var="entry" varStatus="status">
                <c:set var="accesskey" value="${fn:escapeXml(entry.key.index)}" />
                <c:if test="${model.assignAccesskeyToNumber}">
                    <c:set var="accesskey" value="${status.index%5 == 0 ? Integer.toString(status.index/5) : null}" />
                </c:if>
                <c:choose>
                    <c:when test="${!empty accesskey}">
                        <li><a href="#${index.index}" accesskey="${fn:escapeXml(accesskey)}" class="jps-index-key">${fn:escapeXml(entry.key.index)}</a>
                    </c:when>
                    <c:otherwise>
                        <li><a href="#${index.index}" class="jps-index-key">${fn:escapeXml(entry.key.index)}</a>
                    </c:otherwise>
                </c:choose>
                    <ul>
                        <c:forEach items="${entry.value}" var="artist" varStatus="loop">
                            <sub:url value="main.view" var="mainUrl">
                                <c:forEach items="${artist.mediaFiles}" var="mediaFile">
                                    <sub:param name="id" value="${mediaFile.id}"/>
                                </c:forEach>
                            </sub:url>
                            <li><a target="main" href="${mainUrl}" title="${artist.sortableName}"><str:truncateNicely upper="${18}" lower="${25}" >${fn:escapeXml(artist.name)}</str:truncateNicely></a></li>
                        </c:forEach>
                    </ul>
                </li>
            </c:forEach>
            <c:if test="${not empty model.singleSongs}">
                <li><a target="main" href="home.view?listType=index" class="jps-index-key">more</a></li>
            </c:if>
        </ul>

        <div class="jps-side-menu">
            <nav>
                <ul>
                    <li>
                        <c:choose>
                            <c:when test="${model.scanning}">
                                <a href="top.view"><img src="<spring:theme code='settingsImage'/>" title="${settings}" alt="${settings}"><fmt:message key="common.refresh"/></a>
                            </c:when>
                            <c:otherwise>
                                <a href="top.view?refresh=true"><img src="<spring:theme code='settingsImage'/>" title="${settings}" alt="${settings}"><fmt:message key="common.refresh"/></a>
                            </c:otherwise>
                        </c:choose>
                    </li>
                    <c:if test="${model.user.settingsRole}">
                        <li><a href="settings.view?" target="main"><img src="<spring:theme code='settingsImage'/>" title="${settings}" alt="${settings}">${settings}</a></li>
                    </c:if>
                    <li>
                        <c:if test="${model.user.settingsRole}"><a href="personalSettings.view" target="main"></c:if>
                        <c:choose>
                            <c:when test="${model.showAvatar}">
                                <sub:url value="avatar.view" var="avatarUrl">
                                    <sub:param name="username" value="${model.user.username}"/>
                                </sub:url>
                                <img src="${avatarUrl}" alt="User">
                            </c:when>
                            <c:otherwise>
                                <img src="<spring:theme code='userImage'/>" alt="User">
                            </c:otherwise>
                        </c:choose>
                        <fmt:message key="settingsheader.personal"/><br>
                        (<c:out value="${model.user.username}" escapeXml="true"/>)
                        <c:if test="${model.user.settingsRole}"></a></c:if>
                    </li>
                    <li><a href="status.view?" target="main"><img src="<spring:theme code='statusImage'/>" title="${status}" alt="${status}">${status}</a></li>
                    <c:if test="${model.user.uploadRole}">
                        <li><a href="uploadEntry.view?" target="main"><img src="<spring:theme code='uploadImage'/>" title="${upload}" alt="${upload}">${upload}</a></li>
                    </c:if>
                    <li><a href="help.view?" target="main"><img src="<spring:theme code='helpImage'/>" title="${help}" alt="${help}">${help}</a></li>
                    <li>
                        <a href="<c:url value='/logout'/>" target="_top"><img src="<spring:theme code='logoutImage'/>" alt="logout"><fmt:message key="top.logout" var="logout"></fmt:message><c:out value="${logout}"/></a>
                    </li>
                </ul>
            </nav>
        </div>

        <aside>
            <c:if test="${not empty model.shortcuts}">
                <ul class="jps-shortcuts">
                    <c:forEach items="${model.shortcuts}" var="shortcut">
                        <sub:url value="main.view" var="mainUrl">
                            <sub:param name="id" value="${shortcut.id}"/>
                        </sub:url>
                        <li><a target="main" href="${mainUrl}">${fn:escapeXml(shortcut.name)}</a></li>
                    </c:forEach>
                </ul>
            </c:if>
            <c:if test="${not empty model.radios and model.useRadio eq true}">
                <ul class="jps-radio">
                    <c:forEach items="${model.radios}" var="radio" varStatus="loop">
                        <li>
                            <a target="hidden" href="${radio.streamUrl}" data-id="${radio.id}">
                                <img src="<spring:theme code='playImage'/>" alt="<fmt:message key='common.play'/>" title="<fmt:message key='common.play'/>">
                            </a>
                             <c:choose>
                                 <c:when test="${empty radio.homepageUrl}">
                                     ${fn:escapeXml(radio.name)}
                                 </c:when>
                                 <c:otherwise>
                                     <a target="_blank" rel="noopener" href="${radio.homepageUrl}">${fn:escapeXml(radio.name)}</a>
                                 </c:otherwise>
                             </c:choose>
                        </li>
                    </c:forEach>
                </ul>
            </c:if>
        </aside>

    </div>

    <%-- topHeader --%>
    <div class="jps-topHeader">
        <nav>
            <ul class="jps-nav">
                <li><a href="home.view?" target="main"><img src="<spring:theme code='homeImage'/>" title="${home}" alt="${home}">${home}</a></li>
                <li><a href="nowPlaying.view?" target="main"><img src="<spring:theme code='nowPlayingImage'/>" title="${nowPlaying}" alt="${nowPlaying}">${nowPlaying}</a></li>
                <li><a href="starred.view?" target="main"><img src="<spring:theme code='starredImage'/>" title="${starred}" alt="${starred}">${starred}</a></li>
                <li><a href="playlists.view?" target="main" class="plalist"><img src="<spring:theme code='playlistImage'/>" title="${playlists}" alt="${playlists}">${playlists}</a></li>
                <li><a href="podcastChannels.view?" target="main"><img src="<spring:theme code='podcastLargeImage'/>" title="${podcast}" alt="${podcast}">${podcast}</a></li>
            </ul>
        </nav>
        <form method="post" action="search.view" target="main" name="searchForm">
            <input required type="text" name="query" id="query" size="28" placeholder="${search}" onclick="select();" onkeyup="triggerInstantSearch();">
            <a href="javascript:document.searchForm.submit()"><img src="<spring:theme code='searchImage'/>" alt="${search}" title="${search}"></a>
        </form>
    </div>

    <%-- main --%>
    <iframe name="main" id="main" src="nowPlaying.view?" frameborder="no"></iframe>

    <%-- keyboardShortcuts --%>
    <div id="keyboardShortcuts" style="display: none; z-index:100 "></div>

</body></html>
