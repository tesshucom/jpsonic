<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PersonalSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="personal"/>
    <c:param name="restricted" value="${not command.user.adminRole}"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<fmt:message key="common.default" var="defaultTitle"/>

<form:form method="post" action="personalSettings.view" modelAttribute="command">

    <details>
        <summary class="jpsonic"><fmt:message key="generalsettings.themeandlanguage"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.language"/></dt>
            <dd>
                <form:select path="localeIndex" cssStyle="width:15em">
                    <form:option value="-1" label="${defaultTitle}"/>
                       <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                           <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                       </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </dd>
            <dt><fmt:message key="personalsettings.theme"/></dt>
            <dd>
                <form:select path="themeIndex" cssStyle="width:15em">
                    <form:option value="-1" label="${defaultTitle}"/>
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import></dd>
            <dt><fmt:message key="personalsettings.albumlist"/></dt>
            <dd>
                <form:select path="albumListId" cssStyle="width:15em">
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

    <details open>
        <summary class="jpsonic"><fmt:message key="personalsettings.display"/></summary>
    
        <table class="indent">
            <tr>
                <th></th>
                <th style="padding:0 0.5em 0.5em 0;text-align:left;"><fmt:message key="personalsettings.display"/></th>
                <th style="padding:0 0.5em 0.5em 0.5em;text-align:center;"><fmt:message key="personalsettings.browse"/></th>
                <th style="padding:0 0 0.5em 0.5em;text-align:center;"><fmt:message key="personalsettings.playlist"/></th>
                <th style="padding:0 0 0.5em 0.5em">
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="visibility"/></c:import>
                </th>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.tracknumber"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.trackNumberVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.trackNumberVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.artist"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.artistVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.artistVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.album"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.albumVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.albumVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td><img src="<spring:theme code='domestic'/>" class="domestic" alt=""></td>
                <td><fmt:message key="personalsettings.composer"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.composerVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.composerVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td><img src="<spring:theme code='domestic'/>" class="domestic" alt=""></td>
                <td><fmt:message key="personalsettings.genre"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.genreVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.genreVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.year"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.yearVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.yearVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.bitrate"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.bitRateVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.bitRateVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.duration"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.durationVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.durationVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.format"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.formatVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.formatVisible" cssClass="checkbox"/></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:message key="personalsettings.filesize"/></td>
                <td style="text-align:center"><form:checkbox path="mainVisibility.fileSizeVisible" cssClass="checkbox"/></td>
                <td style="text-align:center"><form:checkbox path="playlistVisibility.fileSizeVisible" cssClass="checkbox"/></td>
            </tr>
        </table>

    </details>

    <details>
        <summary class="legacy"><fmt:message key="personalsettings.playinganddisplay"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="showNowPlayingEnabled" id="nowPlaying" cssClass="checkbox"/>
                <label for="nowPlaying"><fmt:message key="personalsettings.shownowplaying"/>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="showArtistInfoEnabled" id="artistInfo" cssClass="checkbox"/>
                <label for="artistInfo"><fmt:message key="personalsettings.showartistinfo"/>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="nowPlayingAllowed" id="nowPlayingAllowed" cssClass="checkbox"/>
                <label for="nowPlayingAllowed"><fmt:message key="personalsettings.nowplayingallowed"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="autoHidePlayQueue" id="autoHidePlayQueue" cssClass="checkbox"/>
                <label for="autoHidePlayQueue"><fmt:message key="personalsettings.autohideplayqueue"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="partyModeEnabled" id="partyModeEnabled" cssClass="checkbox"/>
                <label for="partyModeEnabled"><fmt:message key="personalsettings.partymode"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="partymode"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="queueFollowingSongs" id="queueFollowingSongs" cssClass="checkbox"/>
                <label for="queueFollowingSongs"><fmt:message key="personalsettings.queuefollowingsongs"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="songNotificationEnabled" id="song" cssClass="checkbox"/>
                <label for="song"><fmt:message key="personalsettings.songnotification"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="keyboardShortcutsEnabled" id="keyboardShortcutsEnabled" cssClass="checkbox"/>
                <label for="keyboardShortcutsEnabled"><fmt:message key="personalsettings.keyboardshortcutsenabled"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.paginationsize"/></dt>
            <dd><form:input path="paginationSize" size="24"/></label></dd>
        </dl>
    </details>


    <details>
        <summary class="legacy"><fmt:message key="personalsettings.musicsns"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="listenBrainzEnabled" id="listenBrainz" cssClass="checkbox"/>
                <label for="listenBrainz"><fmt:message key="personalsettings.listenbrainzenabled"/></label>
            </dd>
            <dt><fmt:message key="personalsettings.listenbrainztoken"/></dt>
            <dd><form:input path="listenBrainzToken" size="36"/></dd>
        </dl>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="lastFmEnabled" id="lastFm" cssClass="checkbox"/>
                <label for="lastFm"><fmt:message key="personalsettings.lastfmenabled"/>
            </dd>
            <dt><fmt:message key="personalsettings.lastfmusername"/></dt>
            <dd><form:input path="lastFmUsername" size="24"/></dd>
            <dt><fmt:message key="personalsettings.lastfmpassword"/></dt>
            <dd><form:password path="lastFmPassword" size="24"/></dd>
        </dl>
    </details>

    <details>
        <summary class="legacy"><fmt:message key="personalsettings.updatenotification"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="finalVersionNotificationEnabled" id="final" cssClass="checkbox"/>
                <label for="final"><fmt:message key="personalsettings.finalversionnotification"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="betaVersionNotificationEnabled" id="beta" cssClass="checkbox"/>
                <label for="beta"><fmt:message key="personalsettings.betaversionnotification"/></label>
            </dd>
        </dl>
    </details>



    <details>
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

<details>
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
        parent.location.href="index.view?";
    </script>
</c:if>

</body></html>
