<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Airsonic.
  ~
  ~ Airsonic is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ Airsonic is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ Copyright 2013 (C) Sindre Mehus
  --%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ page import="com.tesshu.jpsonic.domain.TranscodeScheme" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>

    function checkBitrateAvailability() {
        var c = 0;
        Array.from(document.getElementsByName('activeTranscodingIds')).forEach(a => {if (a.checked) {c++}});
        $('[name="transcodeScheme"]').prop("disabled", c == 0);
    }

    function resetBasicSettings() {
        Array.from(document.getElementsByName('allowedMusicFolderIds')).forEach(a => a.checked = true);
        $('[name="transcodeScheme"]').prop("selectedIndex", 0);
        Array.from(document.getElementsByName('activeTranscodingIds')).forEach(a => a.checked = false);
        document.getElementsByName('uriWithFileExtensions')[0].checked = true;
        checkBitrateAvailability();
    }
    
    $(document).ready(function(){
        Array.from(document.getElementsByName('activeTranscodingIds')).forEach(a => a.onclick = checkBitrateAvailability);
        checkBitrateAvailability();
    });
</script>
</head>

<body class="mainframe settings dlnaSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value='${settings_toast}'/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="dlnaSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<form:form method="post" action="dlnaSettings.view" modelAttribute="command">
    <sec:csrfInput />

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details open>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetBasicSettings()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <summary class="jpsonic"><fmt:message key="dlnasettings.basic"/></summary>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp"><fmt:message key="dlnasettings.basicoutline"/></div>
        </c:if>

        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="dlnaEnabled" id="dlnaEnabled"/>
                <label for="dlnaEnabled"><fmt:message key="dlnasettings.enabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaenable"/></c:import>
            </dd>
            <dt><fmt:message key="dlnasettings.servername"/></dt>
            <dd>
                <input type="text" name="dlnaServerName" id="dlnaServerName" value="<c:out value='${command.dlnaServerName}' escapeXml='true'/>"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaservername"/></c:import>
            </dd>
            <dt><label for="dlnaBaseLANURL" ><fmt:message key="dlnasettings.baselanurl"/></label></dt>
            <dd>
                <input type="text" name="dlnaBaseLANURL" id="dlnaBaseLANURL" value="<c:out value='${command.dlnaBaseLANURL}' />" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnalanurl"/></c:import>
            </dd>
            <dt><fmt:message key="usersettings.folderaccess"/></dt>
             <dd>
                <c:forEach items="${command.allMusicFolders}" var="musicFolder">
                    <form:checkbox path="allowedMusicFolderIds" id="musicFolder${musicFolder.id}" value="${musicFolder.id}" cssClass="checkbox"/>
                    <label for="musicFolder${musicFolder.id}" style="padding-right:1.5em">${musicFolder.name}</label>
                    <%-- <label for="musicFolder${musicFolder.id}" style="padding-right:1.5em">${musicFolder.path}</label> --%>
                </c:forEach>
            </dd>
            <c:if test="${not empty command.allTranscodings}">
                <dt><fmt:message key="playersettings.transcodings"/></dt>
                <dd>
                    <c:forEach items="${command.allTranscodings}" var="transcoding" varStatus="loopStatus">
                        <form:checkbox path="activeTranscodingIds" id="transcoding${transcoding.id}" value="${transcoding.id}" cssClass="checkbox"/>
                        <label for="transcoding${transcoding.id}">${transcoding.name}</label>
                    </c:forEach>
                </dd>
            </c:if>
            <dt><fmt:message key="playersettings.maxbitrate"/></dt>
            <dd>
                <form:select path="transcodeScheme">
                    <c:forEach items="${TranscodeScheme.values()}" var="scheme">
                        <form:option value="${scheme}" label="${scheme.toString()}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="transcode"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="uriWithFileExtensions" id="uriWithFileExtensions"/>
                <label for=uriWithFileExtensions><fmt:message key="dlnasettings.uriwithfileextensions"/></label>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="dlnasettings.view"/></summary>

        <table class="tabular dlna-display">
            <thead>
                <tr>
                    <th><fmt:message key="dlnasettings.functionName"/></th>
                    <th><fmt:message key="dlnasettings.nameOnClient"/></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>
                        <form:checkbox path="dlnaIndexVisible" id="dlnaIndexVisible"/>
                        <label for="dlnaIndexVisible"><fmt:message key="dlna.title.index"/></label>
                    </td>
                    <td rowspan="2">
                        <fmt:message key="dlna.title.index"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaIndexId3Visible" id="dlnaIndexId3Visible"/>
                        <label for="dlnaIndexId3Visible"><fmt:message key="dlna.title.index"/> [ID3]</label>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaFolderVisible" id="dlnaFolderVisible"/>
                        <label for="dlnaFolderVisible"><fmt:message key="dlna.title.folders"/></label>
                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnafolder"/></c:import>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.folders"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaArtistVisible" id="dlnaArtistVisible"/>
                        <label for="dlnaArtistVisible"><fmt:message key="dlna.title.artists"/> [ID3]</label>
                    </td>
                    <td rowspan="2">
                        <fmt:message key="dlna.title.artists"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaArtistByFolderVisible" id="dlnaArtistByFolderVisible"/>
                        <label for="dlnaArtistByFolderVisible"><fmt:message key="dlna.title.artistsByFolder"/> [ID3]</label>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaAlbumVisible" id="dlnaAlbumVisible"/>
                        <label for="dlnaAlbumVisible"><fmt:message key="dlna.title.albums"/> [ID3]</label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.albums"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaPlaylistVisible" id="dlnaPlaylistVisible"/>
                        <label for="dlnaPlaylistVisible"><fmt:message key="dlna.title.playlists"/></label>
                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaplaylist"/></c:import>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.playlists"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaAlbumByGenreVisible" id="dlnaAlbumByGenreVisible"/>
                        <label for="dlnaAlbumByGenreVisible"><fmt:message key="dlna.title.albumbygenres"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.albumbygenres"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaSongByGenreVisible" id="dlnaSongByGenreVisible"/>
                        <label for="dlnaSongByGenreVisible"><fmt:message key="dlna.title.songbygenres"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.songbygenres"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRecentAlbumVisible" id="dlnaRecentAlbumVisible"/>
                        <label for="dlnaRecentAlbumVisible"><fmt:message key="dlna.title.recentAlbums"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.recentAlbums"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRecentAlbumId3Visible" id="dlnaRecentAlbumId3Visible"/>
                        <label for="dlnaRecentAlbumId3Visible"><fmt:message key="dlna.title.recentAlbumsId3"/> [ID3]</label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.recentAlbumsId3"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRandomSongVisible" id="dlnaRandomSongVisible"/>
                        <label for="dlnaRandomSongVisible"><fmt:message key="dlna.title.randomSong"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.randomSong"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRandomAlbumVisible" id="dlnaRandomAlbumVisible"/>
                        <label for="dlnaRandomAlbumVisible"><fmt:message key="dlna.title.randomAlbum"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.randomAlbum"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRandomSongByArtistVisible" id="dlnaRandomSongByArtistVisible"/>
                        <label for="dlnaRandomSongByArtistVisible"><fmt:message key="dlna.title.randomSongByArtist"/></label>
                    </td>
                    <td rowspan="2" class="labeled">
                        <fmt:message key="dlna.title.randomSongByArtist"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaRandomSongByFolderArtistVisible" id="dlnaRandomSongByFolderArtistVisible"/>
                        <label for="dlnaRandomSongByFolderArtistVisible"><fmt:message key="dlna.title.randomSongByFolderArtist"/></label>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:checkbox path="dlnaPodcastVisible" id="dlnaPodcastVisible"/>
                        <label for="dlnaPodcastVisible"><fmt:message key="dlna.title.podcast"/></label>
                    </td>
                    <td>
                        <fmt:message key="dlna.title.podcast"/>
                    </td>
                </tr>
            </tbody>
        </table>

    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="dlnasettings.viewopt"/> / <fmt:message key="dlnasettings.accesscontrol"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="dlnaGenreCountVisible" id="dlnaGenreCountVisible"/>
                <label for="dlnaGenreCountVisible"><fmt:message key="dlnasettings.genreCountVisible"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnagenrecountvisible"/></c:import>
            </dd>
            <dt><label for="dlnaRandomMax"><fmt:message key="dlnasettings.randommax"/></label></dt>
            <dd>
                <form:input path="dlnaRandomMax" id="dlnaRandomMax" maxlength="4"/>
            </dd>

            <dt></dt>
            <dd>
                <form:checkbox path="dlnaGuestPublish" id="dlnaGuestPublish"/>
                <label for=dlnaGuestPublish><fmt:message key="dlnasettings.guestpublish"/></label>
            </dd>
        </dl>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

</body></html>
