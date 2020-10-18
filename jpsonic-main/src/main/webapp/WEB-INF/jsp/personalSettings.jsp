<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PersonalSettingsCommand"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>

function resetThemeAndLanguage() {
    $('[name="localeIndex"]').prop("selectedIndex", 0);
    $('[name="themeIndex"]').prop("selectedIndex", 0);
    $("#radio-" + "<fmt:message key='personalsettings.font.${fn:toLowerCase(command.defaultSettings.fontSchemeName)}'/>").prop('checked', true);
}

function setSettings4DesktopPC() {
    $('[name="keyboardShortcutsEnabled"]').prop('checked', ${command.defaultSettings.keyboardShortcutsEnabled});
    $('[name="albumListId"]').val('${command.defaultSettings.defaultAlbumList.id}');
    $('[name="putMenuInDrawer"]').prop('checked', ${command.defaultSettings.putMenuInDrawer});
    $('[name="showIndex"]').prop('checked', ${command.defaultSettings.showIndex});
    $('[name="closeDrawer"]').prop('checked', ${command.defaultSettings.closeDrawer});
    $('[name="closePlayQueue"]').prop('checked', ${command.defaultSettings.closePlayQueue});
    $('[name="alternativeDrawer"]').prop('checked', ${command.defaultSettings.alternativeDrawer});
    $('[name="breadcrumbIndex"]').prop('checked', ${command.defaultSettings.breadcrumbIndex});
    $('[name="assignAccesskeyToNumber"]').prop('checked', ${command.defaultSettings.assignAccesskeyToNumber});
    $('[name="simpleDisplay"]').prop('checked', ${command.defaultSettings.simpleDisplay});
    $('[name="queueFollowingSongs"]').prop('checked', ${command.defaultSettings.queueFollowingSongs});
    $('[name="openDetailSetting"]').prop('checked', ${command.defaultSettings.openDetailSetting});
    $('[name="openDetailStar"]').prop('checked', ${command.defaultSettings.openDetailStar});
    $('[name="openDetailIndex"]').prop('checked', ${command.defaultSettings.openDetailIndex});
    $('[name="songNotificationEnabled"]').prop('checked', ${command.defaultSettings.songNotificationEnabled});
}

function setSettings4Tablet() {
    $('[name="keyboardShortcutsEnabled"]').prop('checked', ${command.tabletSettings.keyboardShortcutsEnabled});
    $('[name="albumListId"]').val('${command.tabletSettings.defaultAlbumList.id}');
    $('[name="putMenuInDrawer"]').prop('checked', ${command.tabletSettings.putMenuInDrawer});
    $('[name="showIndex"]').prop('checked', ${command.tabletSettings.showIndex});
    $('[name="closeDrawer"]').prop('checked', ${command.tabletSettings.closeDrawer});
    $('[name="closePlayQueue"]').prop('checked', ${command.tabletSettings.closePlayQueue});
    $('[name="alternativeDrawer"]').prop('checked', ${command.tabletSettings.alternativeDrawer});
    $('[name="breadcrumbIndex"]').prop('checked', ${command.tabletSettings.breadcrumbIndex});
    $('[name="assignAccesskeyToNumber"]').prop('checked', ${command.tabletSettings.assignAccesskeyToNumber});
    $('[name="simpleDisplay"]').prop('checked', ${command.tabletSettings.simpleDisplay});
    $('[name="queueFollowingSongs"]').prop('checked', ${command.tabletSettings.queueFollowingSongs});
    $('[name="openDetailSetting"]').prop('checked', ${command.tabletSettings.openDetailSetting});
    $('[name="openDetailStar"]').prop('checked', ${command.tabletSettings.openDetailStar});
    $('[name="openDetailIndex"]').prop('checked', ${command.tabletSettings.openDetailIndex});
    $('[name="songNotificationEnabled"]').prop('checked', ${command.tabletSettings.songNotificationEnabled});
}

function setSettings4Smartphone() {
    $('[name="keyboardShortcutsEnabled"]').prop('checked', ${command.smartphoneSettings.keyboardShortcutsEnabled});
    $('[name="albumListId"]').val('${command.smartphoneSettings.defaultAlbumList.id}');
    $('[name="putMenuInDrawer"]').prop('checked', ${command.smartphoneSettings.putMenuInDrawer});
    $('[name="showIndex"]').prop('checked', ${command.smartphoneSettings.showIndex});
    $('[name="closeDrawer"]').prop('checked', ${command.smartphoneSettings.closeDrawer});
    $('[name="closePlayQueue"]').prop('checked', ${command.smartphoneSettings.closePlayQueue});
    $('[name="alternativeDrawer"]').prop('checked', ${command.smartphoneSettings.alternativeDrawer});
    $('[name="breadcrumbIndex"]').prop('checked', ${command.smartphoneSettings.breadcrumbIndex});
    $('[name="assignAccesskeyToNumber"]').prop('checked', ${command.smartphoneSettings.assignAccesskeyToNumber});
    $('[name="simpleDisplay"]').prop('checked', ${command.smartphoneSettings.simpleDisplay});
    $('[name="queueFollowingSongs"]').prop('checked', ${command.smartphoneSettings.queueFollowingSongs});
    $('[name="openDetailSetting"]').prop('checked', ${command.smartphoneSettings.openDetailSetting});
    $('[name="openDetailStar"]').prop('checked', ${command.smartphoneSettings.openDetailStar});
    $('[name="openDetailIndex"]').prop('checked', ${command.smartphoneSettings.openDetailIndex});
    $('[name="songNotificationEnabled"]').prop('checked', ${command.smartphoneSettings.songNotificationEnabled});
}

function resetDisplay() {
    $('[name="mainVisibility.trackNumberVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.trackNumberVisible});
    $('[name="mainVisibility.artistVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.artistVisible});
    $('[name="mainVisibility.albumVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.albumVisible});
    $('[name="mainVisibility.composerVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.composerVisible});
    $('[name="mainVisibility.genreVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.genreVisible});
    $('[name="mainVisibility.yearVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.yearVisible});
    $('[name="mainVisibility.bitRateVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.bitRateVisible});
    $('[name="mainVisibility.durationVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.durationVisible});
    $('[name="mainVisibility.formatVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.formatVisible});
    $('[name="mainVisibility.fileSizeVisible"]').prop('checked', ${command.defaultSettings.mainVisibility.fileSizeVisible});
    $('[name="playlistVisibility.trackNumberVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.trackNumberVisible});
    $('[name="playlistVisibility.artistVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.artistVisible});
    $('[name="playlistVisibility.albumVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.albumVisible});
    $('[name="playlistVisibility.composerVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.composerVisible});
    $('[name="playlistVisibility.genreVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.genreVisible});
    $('[name="playlistVisibility.yearVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.yearVisible});
    $('[name="playlistVisibility.bitRateVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.bitRateVisible});
    $('[name="playlistVisibility.durationVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.durationVisible});
    $('[name="playlistVisibility.formatVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.formatVisible});
    $('[name="playlistVisibility.fileSizeVisible"]').prop('checked', ${command.defaultSettings.playlistVisibility.fileSizeVisible});
}

function resetAdditionalDisplay() {
    $('[name="showNowPlayingEnabled"]').prop('checked', ${command.defaultSettings.showNowPlayingEnabled});
    $('[name="nowPlayingAllowed"]').prop('checked', ${command.defaultSettings.nowPlayingAllowed});
    $('[name="autoHidePlayQueue"]').prop('checked', ${command.defaultSettings.autoHidePlayQueue});
    $('[name="showArtistInfoEnabled"]').prop('checked', ${command.defaultSettings.showArtistInfoEnabled});
    $('[name="forceBio2Eng"]').prop('checked', ${command.defaultSettings.forceBio2Eng});
    $('[name="showTopSongs"]').prop('checked', ${command.defaultSettings.showTopSongs});
    $('[name="showSimilar"]').prop('checked', ${command.defaultSettings.showSimilar});
    $('[name="showSibling"]').prop('checked', ${command.defaultSettings.showSibling});
    $('[name="paginationSize"]').val(${command.defaultSettings.paginationSize});
    <c:if test="${command.user.downloadRole eq true}">
        $('[name="showDownload"]').prop('checked', ${command.defaultSettings.showDownload});
    </c:if>
    <c:if test="${command.user.coverArtRole eq true}">
        $('[name="showTag"]').prop('checked', ${command.defaultSettings.showTag});
        $('[name="showChangeCoverArt"]').prop('checked', ${command.defaultSettings.showChangeCoverArt});
    </c:if>
    <c:if test="${command.user.commentRole eq true}">
        $('[name="showComment"]').prop('checked', ${command.defaultSettings.showComment});
    </c:if>
    <c:if test="${command.user.shareRole eq true}">
        $('[name="showShare"]').prop('checked', ${command.defaultSettings.showShare});
    </c:if>
    <c:if test="${command.user.commentRole eq true}">
        $('[name="showRate"]').prop('checked', ${command.defaultSettings.showRate});
    </c:if>
    $('[name="showAlbumSearch"]').prop('checked', ${command.defaultSettings.showAlbumSearch});
    $('[name="showLastPlay"]').prop('checked', ${command.defaultSettings.showLastPlay});
    $('[name="showAlbumActions"]').prop('checked', ${command.defaultSettings.showAlbumActions});
    $('[name="partyModeEnabled"]').prop('checked', ${command.defaultSettings.partyModeEnabled});
}
</script>

</head>

<body class="mainframe settings personalSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="personal"/>
    <c:param name="restricted" value="${not command.user.adminRole}"/>
    <c:param name="toast" value="${command.showToast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="useSonos" value="${command.useSonos}"/>
</c:import>

<fmt:message key="common.default" var="defaultTitle"/>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="personalSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<form:form method="post" action="personalSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.themeandlanguage"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetThemeAndLanguage()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <dl>
            <dt><fmt:message key="generalsettings.language"/></dt>
            <dd>
                <form:select path="localeIndex">
                    <form:option value="-1" label="${defaultTitle}"/>
                       <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                           <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                       </c:forEach>
                </form:select>
            </dd>
            <dt><fmt:message key="personalsettings.theme"/></dt>
            <dd>
                <form:select path="themeIndex">
                    <form:option value="-1" label="${defaultTitle}"/>
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
            </dd>
            <dt><fmt:message key="personalsettings.font"/></dt>
            <dd>
                <ul class="playerSettings">
                    <c:forEach items="${command.fontSchemeHolders}" var="fontSchemeHolder">
                        <c:set var="fontSchemeName">
                            <fmt:message key="personalsettings.font.${fn:toLowerCase(fontSchemeHolder.name)}"/>
                        </c:set>
                        <li>
                            <form:radiobutton class="technologyRadio" id="radio-${fontSchemeName}" path="fontSchemeName" value="${fontSchemeHolder.name}"
                                checked="${fontSchemeHolder.name eq command.fontSchemeName ? 'checked' : ''}"
                                disabled="${fontSchemeHolder.name eq 'CUSTOM'}"/>
                            <label for="radio-${fontSchemeName}">${fontSchemeName}</label>
                            <c:if test="${fontSchemeHolder.name ne 'CUSTOM'}">
                                <c:import url="helpToolTip.jsp"><c:param name="topic" value="personalsettings.font.${fn:toLowerCase(fontSchemeHolder.name)}"/></c:import>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </dd>
        </dl>
    </details>

    <details open>
        <summary class="jpsonic"><fmt:message key="personalsettings.playinganddisplay"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:setSettings4DesktopPC()" title="<fmt:message key='personalsettings.desktoppc'/>" class="control desktop-pc"><fmt:message key="personalsettings.desktoppc"/></a></li>
                <li><a href="javascript:setSettings4Tablet()" title="<fmt:message key='personalsettings.tablet'/>" class="control tablet"><fmt:message key="personalsettings.tablet"/></a></li>
                <li><a href="javascript:setSettings4Smartphone()" title="<fmt:message key='personalsettings.smartphone'/>" class="control smartphone"><fmt:message key="personalsettings.smartphone"/></a></li>
            </ul>
        </div>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="personalsettings.playinganddisplayoutline"/>
            </div>
        </c:if>

        <dl>
            <dt><fmt:message key="personalsettings.pageall"/></dt>
            <dd>
                <form:checkbox path="keyboardShortcutsEnabled" id="keyboardShortcutsEnabled" />
                <label for="keyboardShortcutsEnabled"><fmt:message key="personalsettings.keyboardshortcutsenabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="keyboardshortcutsenabled"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.albumlist"/></dt>
            <dd>
                <form:select path="albumListId">
                    <c:forEach items="${command.albumLists}" var="albumList" varStatus="loopStatus">
                        <c:set var="label">
                            <fmt:message key="home.${albumList.id}.title"/>
                        </c:set>
                        <form:option value="${albumList.id}" label="${label}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="albumlist"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages.drawer"/></dt>
            <dd>
                <form:checkbox path="putMenuInDrawer" id="putMenuInDrawer" />
                <label for="putMenuInDrawer"><fmt:message key="personalsettings.menuindrawer"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages.drawer"/></dt>
            <dd>
                <form:checkbox path="showIndex" id="showIndex" />
                <label for="showIndex"><fmt:message key="personalsettings.showindex"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="showindex"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages.drawer"/></dt>
            <dd>
                <form:checkbox path="closeDrawer" id="closeDrawer" />
                <label for="closeDrawer"><fmt:message key="personalsettings.closedrawer"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="closedrawer"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages.playqueue"/></dt>
            <dd>
                <form:checkbox path="closePlayQueue" id="closePlayQueue" />
                <label for="closePlayQueue"><fmt:message key="personalsettings.closeplayqueue"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="closeplayqueue"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages.drawer"/></dt>
            <dd>
                <form:checkbox path="alternativeDrawer" id="alternativeDrawer" />
                <label for="alternativeDrawer"><fmt:message key="personalsettings.alternativedrawer"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="alternativedrawer"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/> / <fmt:message key="personalsettings.pages.album"/></dt>
            <dd>
                <form:checkbox path="breadcrumbIndex" id="breadcrumbIndex" />
                <label for="breadcrumbIndex"><fmt:message key="personalsettings.breadcrumbindex"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.drawer"/> / <fmt:message key="personalsettings.pages.home"/></dt>
            <dd>
                <form:checkbox path="assignAccesskeyToNumber" id="assignAccesskeyToNumber" />
                <label for="assignAccesskeyToNumber"><fmt:message key="personalsettings.numberaccesskey"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="numberaccesskey"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playlist"/> etc</dt>
            <dd>
                <form:checkbox path="simpleDisplay" id="simpleDisplay" />
                <label for="simpleDisplay"><fmt:message key="personalsettings.simpledisplay"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playlist"/> etc</dt>
            <dd>
                <form:checkbox path="queueFollowingSongs" id="queueFollowingSongs" />
                <label for="queueFollowingSongs"><fmt:message key="personalsettings.queuefollowingsongs"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.summary"/></dt>
            <dd>
                <form:checkbox path="openDetailSetting" id="openDetailSetting" />
                <label for="openDetailSetting"><fmt:message key="personalsettings.summary.opensettings"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.summary"/></dt>
            <dd>
                <form:checkbox path="openDetailStar" id="openDetailStar" />
                <label for="openDetailStar"><fmt:message key="personalsettings.summary.openstars"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.summary"/></dt>
            <dd>
                <form:checkbox path="openDetailIndex" id="openDetailIndex" />
                <label for="openDetailIndex"><fmt:message key="personalsettings.summary.openindexes"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.browser"/></dt>
            <dd>
                <form:checkbox path="songNotificationEnabled" id="song" />
                <label for="song"><fmt:message key="personalsettings.songnotification"/></label>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="personalsettings.display"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetDisplay()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="personalsettings.displayoutline"/>
            </div>
        </c:if>

        <table class="tabular personalsettings-display">
            <thead>
                <tr>
                    <th><fmt:message key="personalsettings.display"/></th>
                    <th><fmt:message key="personalsettings.browse"/></th>
                    <th><fmt:message key="personalsettings.playlist"/></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><fmt:message key="personalsettings.tracknumber"/></td>
                    <td><form:checkbox path="mainVisibility.trackNumberVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.trackNumberVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.artist"/></td>
                    <td><form:checkbox path="mainVisibility.artistVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.artistVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.album"/></td>
                    <td><form:checkbox path="mainVisibility.albumVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.albumVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.composer"/></td>
                    <td><form:checkbox path="mainVisibility.composerVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.composerVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.genre"/></td>
                    <td><form:checkbox path="mainVisibility.genreVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.genreVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.year"/></td>
                    <td><form:checkbox path="mainVisibility.yearVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.yearVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.bitrate"/></td>
                    <td><form:checkbox path="mainVisibility.bitRateVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.bitRateVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.duration"/></td>
                    <td><form:checkbox path="mainVisibility.durationVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.durationVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.format"/></td>
                    <td><form:checkbox path="mainVisibility.formatVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.formatVisible" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="personalsettings.filesize"/></td>
                    <td><form:checkbox path="mainVisibility.fileSizeVisible" /></td>
                    <td><form:checkbox path="playlistVisibility.fileSizeVisible" /></td>
                </tr>
            </tbody>
        </table>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="personalsettings.additionaldisplay"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetAdditionalDisplay()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="personalsettings.additionaldisplayoutline"/>
            </div>
        </c:if>

        <dl>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="showNowPlayingEnabled" id="nowPlaying" />
                <label for="nowPlaying"><fmt:message key="personalsettings.shownowplaying"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="shownowplaying"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="nowPlayingAllowed" id="nowPlayingAllowed" />
                <label for="nowPlayingAllowed"><fmt:message key="personalsettings.nowplayingallowed"/></label>
            </dd>
            <%-- There are many problems. Temporarily deleted. #622
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="autoHidePlayQueue" id="autoHidePlayQueue" />
                <label for="autoHidePlayQueue"><fmt:message key="personalsettings.autohideplayqueue"/></label>
            </dd>
             --%>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/></dt>
            <dd>
                <form:checkbox path="showArtistInfoEnabled" id="artistInfo" />
                <label for="artistInfo"><fmt:message key="personalsettings.showartistinfo"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/></dt>
            <dd>
                <form:checkbox path="forceBio2Eng" id="forceBio2Eng" />
                <label for="forceBio2Eng"><fmt:message key="personalsettings.forcebio2eng"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="forcebio2eng"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/></dt>
            <dd>
                <form:checkbox path="showTopSongs" id="showTopSongs" />
                <label for="showTopSongs"><fmt:message key="personalsettings.showtopsongs"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/></dt>
            <dd>
                <form:checkbox path="showSimilar" id="showSimilar" />
                <label for="showSimilar"><fmt:message key="personalsettings.showsimilar"/></label>
            </dd>      
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
            <dd>
                <form:checkbox path="showSibling" id="showSibling" />
                <label for="showSibling"><fmt:message key="personalsettings.showsibling"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
            <dd><form:input path="paginationSize"/>
                <fmt:message key="personalsettings.paginationsize"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="paginationsize"/></c:import>
            </dd>
            <c:if test="${command.user.downloadRole eq true}">
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playlist"/></dt>
                <dd>
                    <form:checkbox path="showDownload" id="showDownload" />
                    <label for="showDownload"><fmt:message key="personalsettings.showdownload"/></label>
                </dd>
            </c:if>
            <c:if test="${command.user.coverArtRole eq true}">
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
                <dd>
                    <form:checkbox path="showTag" id="showTag" />
                    <label for="showTag"><fmt:message key="personalsettings.showtag"/></label>
                </dd>
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
                <dd>
                    <form:checkbox path="showChangeCoverArt" id="showChangeCoverArt" />
                    <label for="showChangeCoverArt"><fmt:message key="personalsettings.showchangecoverart"/></label>
                </dd>  
            </c:if>
            <c:if test="${command.user.commentRole eq true}">
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.artist"/> / <fmt:message key="personalsettings.pages.album"/></dt>
                <dd>
                    <form:checkbox path="showComment" id="showComment" />
                    <label for="showComment"><fmt:message key="personalsettings.showcomment"/></label>
                </dd>
            </c:if>
            <c:if test="${command.user.shareRole eq true}">
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playlist"/></dt>
                <dd>
                    <form:checkbox path="showShare" id="showShare" />
                    <label for="showShare"><fmt:message key="personalsettings.showshare"/></label>
                </dd>
            </c:if>
            <c:if test="${command.user.commentRole eq true}">
                <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.home"/></dt>
                <dd>
                    <form:checkbox path="showRate" id="showRate" />
                    <label for="showRate"><fmt:message key="personalsettings.showrate"/></label>
                </dd>
            </c:if>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
            <dd>
                <form:checkbox path="showAlbumSearch" id="showAlbumSearch" />
                <label for="showAlbumSearch"><fmt:message key="personalsettings.showalbumsearch"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/></dt>
            <dd>
                <form:checkbox path="showLastPlay" id="showLastPlay" />
                <label for="showLastPlay"><fmt:message key="personalsettings.showlastplay"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playqueue"/></dt>
            <dd>
                <form:checkbox path="showAlbumActions" id="showAlbumActions" />
                <label for="showAlbumActions"><fmt:message key="personalsettings.showalbumactions"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="albumactions"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/> : <fmt:message key="personalsettings.pages.album"/> / <fmt:message key="personalsettings.pages.playlist"/> etc</dt>
            <dd>
                <form:checkbox path="partyModeEnabled" id="partyModeEnabled" />
                <label for="partyModeEnabled"><fmt:message key="personalsettings.partymode"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="partymode"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="legacy"><fmt:message key="personalsettings.musicsns"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="listenBrainzEnabled" id="listenBrainz" />
                <label for="listenBrainz"><fmt:message key="personalsettings.listenbrainzenabled"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.listenbrainztoken"/></dt>
            <dd><form:input path="listenBrainzToken"/></dd>
        </dl>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="lastFmEnabled" id="lastFm" />
                <label for="lastFm"><fmt:message key="personalsettings.lastfmenabled"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.lastfmusername"/></dt>
            <dd><form:input path="lastFmUsername"/></dd>
            <dt><fmt:message key="personalsettings.lastfmpassword"/></dt>
            <dd><form:password path="lastFmPassword"/></dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="legacy"><fmt:message key="personalsettings.updatenotification"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="finalVersionNotificationEnabled" id="final" />
                <label for="final"><fmt:message key="personalsettings.finalversionnotification"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="betaVersionNotificationEnabled" id="beta" />
                <label for="beta"><fmt:message key="personalsettings.betaversionnotification"/></label>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="legacy"><fmt:message key="personalsettings.avatar.title"/></summary>
         <dl>
            <dt></dt>
            <dd>
                <form:radiobutton id="noAvatar" path="avatarId" value="-1"/>
                <label for="noAvatar"><fmt:message key="personalsettings.avatar.none"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:radiobutton id="customAvatar" path="avatarId" value="-2"/>
                <label for="customAvatar"><fmt:message key="personalsettings.avatar.custom"/>
                    <c:if test="${not empty command.customAvatar}">
                        <sub:url value="avatar.view" var="avatarUrl">
                            <sub:param name="username" value="${command.user.username}"/>
                            <sub:param name="forceCustom" value="true"/>
                        </sub:url>
                        <img src="${avatarUrl}" alt="${fn:escapeXml(command.customAvatar.name)}" width="${command.customAvatar.width}" height="${command.customAvatar.height}"/>
                    </c:if>
                </label>
            </dd>
            <dt></dt>
            <dd class="avatarContainer">
                <c:forEach items="${command.avatars}" var="avatar">
                    <c:url value="avatar.view" var="avatarUrl">
                        <c:param name="id" value="${avatar.id}"/>
                    </c:url>
                    <span class="avatar">
                        <form:radiobutton id="avatar-${avatar.id}" path="avatarId" value="${avatar.id}"/>
                        <label for="avatar-${avatar.id}"><img src="${avatarUrl}" alt="${fn:escapeXml(avatar.name)}" width="${avatar.width}" height="${avatar.height}"/></label>
                    </span>
                </c:forEach>
            </dd>
        </dl>

    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>"/>
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

<details ${isOpen}>
    <summary class="legacy"><fmt:message key="personalsettings.avatar.changecustom"/></summary>
    <dl class="single">
        <dt></dt>
        <dd>
            <form method="post" enctype="multipart/form-data" action="avatarUpload.view?${_csrf.parameterName}=${_csrf.token}">
                <input type="file" id="file" name="file" mode="deleteText"/>
                <input type="submit" value="<fmt:message key='personalsettings.avatar.upload'/>" />
            </form>
        </dd>
    </dl>
</details>

<c:if test="${settings_reload}">
    <script>
        <c:choose>
            <c:when test="${index_reload}">
                <%--  There is a case of ON / OFF of right.view, so index.view must be reloaded. --%>
                window.top.location.reload(true);
            </c:when>
            <c:otherwise>
                window.top.reloadUpper("personalSettings.view");
                window.top.reloadPlayQueue();
                window.top.reloadRight();
            </c:otherwise>
        </c:choose>
    </script>
</c:if>

</body></html>
