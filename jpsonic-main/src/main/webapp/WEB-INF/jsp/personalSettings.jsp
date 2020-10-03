<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PersonalSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe settings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="personal"/>
    <c:param name="restricted" value="${not command.user.adminRole}"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="useSonos" value="${command.useSonos}"/>
</c:import>

<fmt:message key="common.default" var="defaultTitle"/>

<form:form method="post" action="personalSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.themeandlanguage"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.language"/></dt>
            <dd>
                <form:select path="localeIndex">
                    <form:option value="-1" label="${defaultTitle}"/>
                       <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                           <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                       </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.theme"/></dt>
            <dd>
                <form:select path="themeIndex">
                    <form:option value="-1" label="${defaultTitle}"/>
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import></dd>
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
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="personalsettings.display"/></summary>
    
        <table class="tabular personalsettings-display">
            <thead>
	            <tr>
	                <th><fmt:message key="personalsettings.display"/><c:import url="helpToolTip.jsp"><c:param name="topic" value="visibility"/></c:import></th>
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

    <details open>
        <summary class="jpsonic"><fmt:message key="personalsettings.playinganddisplay"/></summary>
        <dl>
            <dt><fmt:message key="personalsettings.pageall"/></dt>
            <dd>
                <form:checkbox path="keyboardShortcutsEnabled" id="keyboardShortcutsEnabled" />
                <label for="keyboardShortcutsEnabled"><fmt:message key="personalsettings.keyboardshortcutsenabled"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="closeDrawer" id="closeDrawer" />
                <label for="closeDrawer"><fmt:message key="personalsettings.closedrawer"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="closedrawer"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="showIndex" id="showIndex" />
                <label for="showIndex"><fmt:message key="personalsettings.showindex"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="showindex"/></c:import>
            </dd>

            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="showNowPlayingEnabled" id="nowPlaying" />
                <label for="nowPlaying"><fmt:message key="personalsettings.shownowplaying"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="shownowplaying"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="nowPlayingAllowed" id="nowPlayingAllowed" />
                <label for="nowPlayingAllowed"><fmt:message key="personalsettings.nowplayingallowed"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.frames"/></dt>
            <dd>
                <form:checkbox path="autoHidePlayQueue" id="autoHidePlayQueue" />
                <label for="autoHidePlayQueue"><fmt:message key="personalsettings.autohideplayqueue"/></label>
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
            <dt><fmt:message key="personalsettings.pages"/></dt>
            <dd>
                <form:checkbox path="assignAccesskeyToNumber" id="assignAccesskeyToNumber" />
                <label for="assignAccesskeyToNumber"><fmt:message key="personalsettings.numberaccesskey"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="numberaccesskey"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/></dt>
            <dd>
                <form:checkbox path="showArtistInfoEnabled" id="artistInfo" />
                <label for="artistInfo"><fmt:message key="personalsettings.showartistinfo"/>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/></dt>
            <dd>
                <form:checkbox path="partyModeEnabled" id="partyModeEnabled" />
                <label for="partyModeEnabled"><fmt:message key="personalsettings.partymode"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="partymode"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/></dt>
            <dd>
                <form:checkbox path="queueFollowingSongs" id="queueFollowingSongs" />
                <label for="queueFollowingSongs"><fmt:message key="personalsettings.queuefollowingsongs"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.pages"/></dt>
            <dd><form:input path="paginationSize" size="4"/>
                <fmt:message key="personalsettings.paginationsize"/>
	            <c:import url="helpToolTip.jsp"><c:param name="topic" value="paginationsize"/></c:import>
            </dd>
            
            <dt><fmt:message key="personalsettings.browser"/></dt>
            <dd>
                <form:checkbox path="songNotificationEnabled" id="song" />
                <label for="song"><fmt:message key="personalsettings.songnotification"/></label>
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
            <dd><form:input path="listenBrainzToken" size="36"/></dd>
        </dl>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="lastFmEnabled" id="lastFm" />
                <label for="lastFm"><fmt:message key="personalsettings.lastfmenabled"/>
            </dd>
            <dt><fmt:message key="personalsettings.lastfmusername"/></dt>
            <dd><form:input path="lastFmUsername" size="24"/></dd>
            <dt><fmt:message key="personalsettings.lastfmpassword"/></dt>
            <dd><form:password path="lastFmPassword" size="24"/></dd>
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
                        <img src="${avatarUrl}" alt="${fn:escapeXml(command.customAvatar.name)}" width="${command.customAvatar.width}" height="${command.customAvatar.height}" style="padding-right:2em"/>
                    </c:if>
                </label>
            </dd>
            <dt></dt>
            <dd>
                <c:forEach items="${command.avatars}" var="avatar">
                    <c:url value="avatar.view" var="avatarUrl">
                        <c:param name="id" value="${avatar.id}"/>
                    </c:url>
                    <span style="white-space:nowrap;">
                        <form:radiobutton id="avatar-${avatar.id}" path="avatarId" value="${avatar.id}"/>
                        <label for="avatar-${avatar.id}"><img src="${avatarUrl}" alt="${fn:escapeXml(avatar.name)}" width="${avatar.width}" height="${avatar.height}" style="padding-right:2em;padding-bottom:1em"/></label>
                    </span>
                </c:forEach>
                <p class="detail" style="text-align:right">
                    <fmt:message key="personalsettings.avatar.courtesy"/>
                </p>
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
    <dl>
        <dt></dt>
        <dd>
            <form method="post" enctype="multipart/form-data" action="avatarUpload.view?${_csrf.parameterName}=${_csrf.token}">
                <input type="file" id="file" name="file" size="40" mode="deleteText"/>
                <input type="submit" value="<fmt:message key='personalsettings.avatar.upload'/>" />
            </form>
        </dd>
    </dl>
</details>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
      window.top.location.reload();
    </script>
</c:if>

</body></html>
