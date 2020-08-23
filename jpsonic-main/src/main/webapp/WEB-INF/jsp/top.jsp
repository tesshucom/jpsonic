<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/multiService.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>

<script>

let previousQuery = "";
let instantSearchTimeout;
let drawer;
let toggler;
let main;
let playQueue;

$(document).ready(function(){

    drawer = document.getElementById('drawer');
    toggler = document.getElementById("toggler");
    main = document.getElementById("main");
    playQueue = window.top.document.getElementById('playQueue');

    onToggleDrawer();

    $("#keyboardShortcuts").dialog({
        autoOpen: false,
        width: 840,
        height: 480,
        modal: false,
        open : function() {
            $("#keyboardShortcuts").append('<iframe id="iframeDiv" scrolling="no" frameborder="no"></iframe>');
            $("#iframeDiv").attr({src : "keyboardShortcuts.view?", width : '100%', height : '100%' });},
        close : function() {$("#iframeDiv").remove();}
    });

    dwr.engine.setErrorHandler(null);

    $('.radio-play').on('click', function(evt) {
        top.playQueue.onPlayInternetRadio($(this).data("id"), 0);
        evt.preventDefault();
    });

});

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

function toggleDrawer() {
    window.top.setDrawerOpened(toggler.checked);
    // >> It will be modified when right.view is modified.
    let mainWidth;
    if(toggler.checked) {
        mainWidth = "calc(100vw - 240px - " + ${model.showRight ? 240 : 0} + "px)";
        <c:if test="${model.alternativeDrawer}">
            top.onCloseQueue();
        </c:if>
    } else {
        mainWidth = "calc(100vw - " + ${model.showRight ? 240 : 0} + "px)";
    }
    main.style.width = mainWidth;
    playQueue.style.width = mainWidth;
    // <<
}

window.onToggleDrawer = function() {
    toggler.checked = !toggler.checked;
    toggleDrawer();
}

window.onCloseDrawer = function() {
    if (toggler.checked) {
        onToggleDrawer();
    }
};

window.onTryCloseDrawer = function() {
    <c:if test="${model.closeDrawer}">
        if (toggler.checked) {
            onToggleDrawer();
        }
    </c:if>
    document.activeElement.blur();
};

window.onQueryFocus = function() {
    $("#query").focus();
}

window.onChangeMainLocation = function(location) {
    $('#main')[0].contentDocument.location = location;
}

window.onShowKeyboardShortcuts = function() {
    $('#keyboardShortcuts').dialog('open');
}

</script>
</head>

<body class="top-frame">

    <fmt:message key="top.home" var="home" />
    <fmt:message key="top.now_playing" var="nowPlaying" />
    <fmt:message key="top.starred" var="starred" />
    <fmt:message key="top.playlists" var="playlists" />
    <fmt:message key="top.settings" var="settings" />
    <fmt:message key="top.status" var="status" />
    <fmt:message key="top.podcast" var="podcast" />
    <fmt:message key="top.more" var="more" />
    <fmt:message key="top.help" var="help" />
    <fmt:message key="top.search" var="search" />
    <fmt:message key="top.upload" var="upload" />

    <%-- toggler --%>
    <input type="checkbox" id="toggler" class="jps-input-toggler" value="1" autofocus="true" checked onchange="toggleDrawer()" />
    <label for="toggler" class="jps-toggler" role="button" aria-pressed="false" aria-expanded="false" aria-label="Navigation button"> <span class="jps-toggler-line"></span> <span
        class="jps-toggler-line"></span> <span class="jps-toggler-line"></span>
    </label>

    <%-- drawer --%>
    <div id="drawer" class="jps-drawer">

        <div class="jps-musicfolder">
            <c:if test="${fn:length(model.musicFolders) > 1}">
                <select name="musicFolderId" onchange="location='top.view?musicFolderId=' + options[selectedIndex].value;">
                    <option value="-1"><fmt:message key="left.allfolders" /></option>
                    <c:forEach items="${model.musicFolders}" var="musicFolder">
                        <option ${model.selectedMusicFolder.id == musicFolder.id ? "selected" : ""} value="${musicFolder.id}">${fn:escapeXml(musicFolder.name)}</option>
                    </c:forEach>
                </select>
            </c:if>
        </div>

        <input type="checkbox" class="jps-input-without-index" value="1" autofocus="false" ${model.showIndex ? '' : 'checked'} />
        <c:if test="${not empty model.indexedArtists}">
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
                                    <sub:param name="id" value="${mediaFile.id}" />
                                </c:forEach>
                            </sub:url>
                            <li><a target="main" href="${mainUrl}" title="${artist.sortableName}"><str:truncateNicely upper="${18}" lower="${25}">${fn:escapeXml(artist.name)}</str:truncateNicely></a></li>
                        </c:forEach>
                    </ul>
                    </li>
                </c:forEach>
                <c:if test="${not empty model.singleSongs}">
                    <li><a target="main" href="home.view?listType=index" class="jps-index-key">more</a></li>
                </c:if>
            </ul>
        </c:if>

        <div class="jps-side-menu">
            <nav>
                <ul class="menu">
                    <c:if test="${model.putMenuInDrawer}">
                        <li><a href="home.view?" target="main" title="${home}" class="menu-item home">${home}</a></li>
                        <li><a href="starred.view?" target="main" title="${starred}" class="menu-item star">${starred}</a></li>
                        <li><a href="playlists.view?" target="main" title="${playlists}" class="menu-item playlists">${playlists}</a></li>
                        <li><a href="podcastChannels.view?" target="main" title="${podcast}" class="menu-item podcast">${podcast}</a></li>
                    </c:if>
                    <li><c:choose>
                            <c:when test="${model.scanning}">
                                <a href="top.view" title="<fmt:message key='common.refresh'/>" class="menu-item refresh"><fmt:message key="common.refresh" /></a>
                            </c:when>
                            <c:otherwise>
                                <a href="top.view?refresh=true" title="<fmt:message key='common.refresh'/>" class="menu-item refresh"><fmt:message key="common.refresh" /></a>
                            </c:otherwise>
                        </c:choose></li>
                    <li><a href="nowPlaying.view?" target="main" title="${nowPlaying}" class="menu-item pulse">${nowPlaying}</a></li>
                    <c:if test="${model.user.settingsRole}">
                        <li><a href="settings.view?" target="main" title="${settings}" class="menu-item settings">${settings}</a></li>
                    </c:if>
                    <c:if test="${model.user.settingsRole}">
                        <a href="personalSettings.view" target="main"> <c:choose>
                                <c:when test="${model.showAvatar}">
                                    <sub:url value="avatar.view" var="avatarUrl">
                                        <sub:param name="username" value="${model.user.username}" />
                                    </sub:url>
                                    <li class="avatar"><a href="personalSettings.view" target="main" title="<fmt:message key='settingsheader.personal'/>(<c:out value='${model.user.username}' escapeXml='true'/>)"> <img
                                            src="${avatarUrl}">
                                            <div>
                                                <fmt:message key="settingsheader.personal" />
                                            </div>
                                    </a></li>
                                </c:when>
                                <c:otherwise>
                                    <li><a href="personalSettings.view" target="main" title="<fmt:message key='settingsheader.personal'/>(<c:out value='${model.user.username}' escapeXml='true'/>)" class="menu-item personal"><fmt:message key="settingsheader.personal" /></a></li>
                                </c:otherwise>
                            </c:choose>
                    </c:if>
                    <li><a href="status.view?" target="main" title="<fmt:message key='top.status'/>" class="menu-item graph"><fmt:message key='top.status' /></a></li>
                    <c:if test="${model.user.uploadRole}">
                        <li><a href="uploadEntry.view?" target="main" title="${upload}" class="menu-item upload">${upload}</a></li>
                    </c:if>
                    <li><a href="help.view?" target="main" title="${help}" class="menu-item about">${help}</a></li>
                    <li><a href="<c:url value='/logout'/>" target="_top" title="<fmt:message key='top.logout'/>" class="menu-item logout"><fmt:message key='top.logout' /></a></li>
                </ul>
            </nav>
        </div>

        <aside>
            <c:if test="${not empty model.shortcuts}">
                <ul class="jps-shortcuts">
                    <c:forEach items="${model.shortcuts}" var="shortcut">
                        <sub:url value="main.view" var="mainUrl">
                            <sub:param name="id" value="${shortcut.id}" />
                        </sub:url>
                        <li><a target="main" href="${mainUrl}">${fn:escapeXml(shortcut.name)}</a></li>
                    </c:forEach>
                </ul>
            </c:if>
            <c:if test="${not empty model.radios and model.useRadio eq true}">
                <ul class="jps-radio">
                    <c:forEach items="${model.radios}" var="radio" varStatus="loop">
                        <li><a href="${radio.streamUrl}" target="hidden" data-id="${radio.id}" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play' /></a> <c:choose>
                                <c:when test="${empty radio.homepageUrl}">
                                    ${fn:escapeXml(radio.name)}
                                </c:when>
                                <c:otherwise>
                                    <a target="_blank" rel="noopener" href="${radio.homepageUrl}">${fn:escapeXml(radio.name)}</a>
                                </c:otherwise>
                            </c:choose></li>
                    </c:forEach>
                </ul>
            </c:if>
        </aside>
    </div>

    <%-- topHeader --%>
    <div class="jps-topHeader">
        <c:if test="${not model.putMenuInDrawer}">
            <nav>
                <ul class="menu">
                    <li><a href="home.view?" target="main" title="${home}" class="menu-item home">${home}</a></li>
                    <li><a href="starred.view?" target="main" title="${starred}" class="menu-item star">${starred}</a></li>
                    <li><a href="playlists.view?" target="main" title="${playlists}" class="menu-item playlists">${playlists}</a></li>
                    <li><a href="podcastChannels.view?" target="main" title="${podcast}" class="menu-item podcast">${podcast}</a></li>
                </ul>
            </nav>
        </c:if>
        <form method="post" action="search.view" target="main" name="searchForm">
            <input required type="text" name="query" id="query" size="20" placeholder="${search}" onclick="select();" onkeyup="triggerInstantSearch();">
            <a href="javascript:document.searchForm.submit()" title="${search}" class="control search">${search}</a>
        </form>
    </div>

    <%-- main --%>
    <c:set var="mainHref" value="nowPlaying.view?" />
    <c:if test="${not empty model.mainView}">
        <c:set var="mainHref" value="${model.mainView}?toast=yes" />
    </c:if>
    <iframe name="main" id="main" src="${mainHref}" frameborder="no"></iframe>

    <%-- keyboardShortcuts --%>
    <div id="keyboardShortcuts"></div>

    <c:if test="${model.newVersionAvailable}">
        <fmt:message key="top.upgradeshort" var="versionNotice"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message>
        <script>
        $().toastmessage('showToast', {
            text     : '${fn:escapeXml(versionNotice)}',
            stayTime : 10000,
            sticky   : false,
            type     : 'warning',
            allowToastClose: true,
            close    : function () {window.open('https://github.com/jpsonic/jpsonic/releases/');}
        });
        </script>
    </c:if>

</body>
</html>
