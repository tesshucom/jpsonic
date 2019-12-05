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
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="dlnaSettings.view">
    <sec:csrfInput />

    <table style="white-space:nowrap" class="indent">

        <tr>
            <td><fmt:message key="dlnasettings.basic"/></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaEnabled" id="dlnaEnabled" ${model.dlnaEnabled? "checked": ""}/>
                <label for="dlnaEnabled"><fmt:message key="dlnasettings.enabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaenable"/></c:import>
            </td>
        </tr>

        <tr>
            <td></td>
            <td></td>
            <td><fmt:message key="dlnasettings.servername"/></td>
            <td>
                <input name="dlnaServerName" id="dlnaServerName" size="40" value="<c:out value='${model.dlnaServerName}' escapeXml='true'/>"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaservername"/></c:import>
            </td>
        </tr>

        <tr>
            <td></td>
            <td></td>
            <td><label for="dlnaBaseLANURL" ><fmt:message key="dlnasettings.baselanurl"/></label></td>
            <td>
                <input type="text" size="50" name="dlnaBaseLANURL" id="dlnaBaseLANURL" value="<c:out value='${model.dlnaBaseLANURL}' />" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnalanurl"/></c:import>
            </td>
        </tr>

		<tr><td colspan="3">&nbsp;</td></tr>

        <tr>
            <td><fmt:message key="dlnasettings.view"/></td>
            <td><img src="<spring:theme code="domestic"/>" class="domestic" alt=""></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaIndexVisible" id="dlnaIndexVisible" ${model.dlnaIndexVisible? "checked": ""}/>
                <label for="dlnaIndexVisible"><fmt:message key="dlna.title.index"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaFolderVisible" id="dlnaFolderVisible" ${model.dlnaFolderVisible? "checked": ""}/>
                <label for="dlnaFolderVisible"><fmt:message key="dlna.title.folders"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaArtistVisible" id="dlnaArtistVisible" ${model.dlnaArtistVisible? "checked": ""}/>
                <label for="dlnaArtistVisible"><fmt:message key="dlna.title.artists"/>(ID3)</label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaAlbumVisible" id="dlnaAlbumVisible" ${model.dlnaAlbumVisible? "checked": ""}/>
                <label for="dlnaAlbumVisible"><fmt:message key="dlna.title.albums"/>(ID3)</label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaPlaylistVisible" id="dlnaPlaylistVisible" ${model.dlnaPlaylistVisible? "checked": ""}/>
                <label for="dlnaPlaylistVisible"><fmt:message key="dlna.title.playlists"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaGenreVisible" id="dlnaGenreVisible" ${model.dlnaGenreVisible? "checked": ""}/>
                <label for="dlnaGenreVisible"><fmt:message key="dlna.title.genres"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name=dlnaGenreCountVisible id="dlnaGenreCountVisible" style="margin-left:2em" ${model.dlnaGenreCountVisible? "checked": ""}/>
                <label for="dlnaGenreCountVisible"><fmt:message key="dlnasettings.genreCountVisible"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><img src="<spring:theme code="domestic"/>" class="domestic" alt=""></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaRecentAlbumVisible" id="dlnaRecentAlbumVisible" ${model.dlnaRecentAlbumVisible? "checked": ""}/>
                <label for="dlnaRecentAlbumVisible"><fmt:message key="dlna.title.recentAlbums"/></label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaRecentAlbumId3Visible" id="dlnaRecentAlbumId3Visible" ${model.dlnaRecentAlbumId3Visible? "checked": ""}/>
                <label for="dlnaRecentAlbumId3Visible"><fmt:message key="dlna.title.recentAlbumsId3"/>(ID3)</label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><img src="<spring:theme code="domestic"/>" class="domestic" alt=""></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaPodcastVisible" id="dlnaPodcastVisible" ${model.dlnaPodcastVisible? "checked": ""}/>
                <label for="dlnaPodcastVisible"><fmt:message key="dlna.title.podcast"/></label>
            </td>
        </tr>

		<tr><td colspan="3">&nbsp;</td></tr>

        <tr>
            <td><fmt:message key="dlnasettings.search"/></td>
            <td><img src="<spring:theme code="domestic"/>" class="domestic" alt=""></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaFileStructureSearch" id="dlnaFileStructureSearch" ${model.dlnaFileStructureSearch? "checked": ""}/>
                <label for="dlnaFileStructureSearch"><fmt:message key="dlnasettings.filestructuresearch"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaFileStructureSearch"/></c:import>
            </td>
        </tr>

        <tr>
            <td colspan="4" style="padding-top:1.5em">
                <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
                <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
            </td>
        </tr>
        
    </table>

</form>

</body></html>
