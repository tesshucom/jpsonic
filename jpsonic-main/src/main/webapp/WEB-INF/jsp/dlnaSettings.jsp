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
    <script src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe settings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value='${settings_toast}'/>
    <c:param name="useRadio" value="${model.useRadio}"/>
    <c:param name="useSonos" value="${model.useSonos}"/>
</c:import>

<form method="post" action="dlnaSettings.view">
    <sec:csrfInput />

    <c:set var="isOpen" value='${model.isOpenDetailSetting ? "open" : ""}' />
    <details open>
        <summary class="legacy"><fmt:message key="dlnasettings.basic"/></summary>
        <dl>
        	<dt></dt>
            <dd>
            	<input type="checkbox" name="dlnaEnabled" id="dlnaEnabled" ${model.dlnaEnabled? "checked": ""}/>
                <label for="dlnaEnabled"><fmt:message key="dlnasettings.enabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaenable"/></c:import>
            </dd>
        	<dt><fmt:message key="dlnasettings.servername"/></dt>
            <dd>
                <input name="dlnaServerName" id="dlnaServerName" size="40" value="<c:out value='${model.dlnaServerName}' escapeXml='true'/>"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaservername"/></c:import>
            </dd>
        	<dt><label for="dlnaBaseLANURL" ><fmt:message key="dlnasettings.baselanurl"/></label></dt>
            <dd>
                <input type="text" size="50" name="dlnaBaseLANURL" id="dlnaBaseLANURL" value="<c:out value='${model.dlnaBaseLANURL}' />" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnalanurl"/></c:import>
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
	                    <input type="checkbox" name="dlnaIndexVisible" id="dlnaIndexVisible" ${model.dlnaIndexVisible? "checked": ""}/>
	                    <label for="dlnaIndexVisible"><fmt:message key="dlna.title.index"/></label>
	                </td>
	                <td rowspan="2">
	                    <fmt:message key="dlna.title.index"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaIndexId3Visible" id="dlnaIndexId3Visible" ${model.dlnaIndexId3Visible? "checked": ""}/>
	                    <label for="dlnaIndexId3Visible"><fmt:message key="dlna.title.index"/> [ID3]</label>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaFolderVisible" id="dlnaFolderVisible" ${model.dlnaFolderVisible? "checked": ""}/>
	                    <label for="dlnaFolderVisible"><fmt:message key="dlna.title.folders"/></label>
	                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnafolder"/></c:import>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.folders"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaArtistVisible" id="dlnaArtistVisible" ${model.dlnaArtistVisible? "checked": ""}/>
	                    <label for="dlnaArtistVisible"><fmt:message key="dlna.title.artists"/> [ID3]</label>
	                </td>
	                <td rowspan="2">
	                    <fmt:message key="dlna.title.artists"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaArtistByFolderVisible" id="dlnaArtistByFolderVisible" ${model.dlnaArtistByFolderVisible? "checked": ""}/>
	                    <label for="dlnaArtistByFolderVisible"><fmt:message key="dlna.title.artistsByFolder"/> [ID3]</label>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaAlbumVisible" id="dlnaAlbumVisible" ${model.dlnaAlbumVisible? "checked": ""}/>
	                    <label for="dlnaAlbumVisible"><fmt:message key="dlna.title.albums"/> [ID3]</label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.albums"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaPlaylistVisible" id="dlnaPlaylistVisible" ${model.dlnaPlaylistVisible? "checked": ""}/>
	                    <label for="dlnaPlaylistVisible"><fmt:message key="dlna.title.playlists"/></label>
	                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaplaylist"/></c:import>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.playlists"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaAlbumByGenreVisible" id="dlnaAlbumByGenreVisible" ${model.dlnaAlbumByGenreVisible? "checked": ""}/>
	                    <label for="dlnaAlbumByGenreVisible"><fmt:message key="dlna.title.albumbygenres"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.albumbygenres"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaSongByGenreVisible" id="dlnaSongByGenreVisible" ${model.dlnaSongByGenreVisible? "checked": ""}/>
	                    <label for="dlnaSongByGenreVisible"><fmt:message key="dlna.title.songbygenres"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.songbygenres"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRecentAlbumVisible" id="dlnaRecentAlbumVisible" ${model.dlnaRecentAlbumVisible? "checked": ""}/>
	                    <label for="dlnaRecentAlbumVisible"><fmt:message key="dlna.title.recentAlbums"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.recentAlbums"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRecentAlbumId3Visible" id="dlnaRecentAlbumId3Visible" ${model.dlnaRecentAlbumId3Visible? "checked": ""}/>
	                    <label for="dlnaRecentAlbumId3Visible"><fmt:message key="dlna.title.recentAlbumsId3"/> [ID3]</label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.recentAlbumsId3"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRandomSongVisible" id="dlnaRandomSongVisible" ${model.dlnaRandomSongVisible? "checked": ""}/>
	                    <label for="dlnaRandomSongVisible"><fmt:message key="dlna.title.randomSong"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.randomSong"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRandomAlbumVisible" id="dlnaRandomAlbumVisible" ${model.dlnaRandomAlbumVisible? "checked": ""}/>
	                    <label for="dlnaRandomAlbumVisible"><fmt:message key="dlna.title.randomAlbum"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.randomAlbum"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRandomSongByArtistVisible" id="dlnaRandomSongByArtistVisible" ${model.dlnaRandomSongByArtistVisible? "checked": ""}/>
	                    <label for="dlnaRandomSongByArtistVisible"><fmt:message key="dlna.title.randomSongByArtist"/></label>
	                </td>
	                <td rowspan="2" class="labeled">
	                    <fmt:message key="dlna.title.randomSongByArtist"/>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaRandomSongByFolderArtistVisible" id="dlnaRandomSongByFolderArtistVisible" ${model.dlnaRandomSongByFolderArtistVisible? "checked": ""}/>
	                    <label for="dlnaRandomSongByFolderArtistVisible"><fmt:message key="dlna.title.randomSongByFolderArtist"/></label>
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="checkbox" name="dlnaPodcastVisible" id="dlnaPodcastVisible" ${model.dlnaPodcastVisible? "checked": ""}/>
	                    <label for="dlnaPodcastVisible"><fmt:message key="dlna.title.podcast"/></label>
	                </td>
	                <td>
	                    <fmt:message key="dlna.title.podcast"/>
	                </td>
	            </tr>
			</table>
	    </table>

    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="dlnasettings.viewopt"/> / <fmt:message key="dlnasettings.accesscontrol"/></summary>
        <dl>
        	<dt></dt>
            <dd>
            	<input type="checkbox" name="dlnaGenreCountVisible" id="dlnaGenreCountVisible" ${model.dlnaGenreCountVisible? "checked": ""}/>
                <label for="dlnaGenreCountVisible"><fmt:message key="dlnasettings.genreCountVisible"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnagenrecountvisible"/></c:import>
            </dd>
        	<dt><label for="dlnaRandomMax"><fmt:message key="dlnasettings.randommax"/></label></dt>
            <dd>
                <input type="text" size="24" maxlength="4" name="dlnaRandomMax" id="dlnaRandomMax" value="<c:out value='${model.dlnaRandomMax}' />"/>
            </dd>
        	<dt></dt>
            <dd>
                <input type="checkbox" name="dlnaGuestPublish" id="dlnaGuestPublish" ${model.dlnaGuestPublish? "checked": ""}/>
                <label for=dlnaGuestPublish><fmt:message key="dlnasettings.guestpublish"/></label>
            </dd>
        </dl>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form>

</body></html>
