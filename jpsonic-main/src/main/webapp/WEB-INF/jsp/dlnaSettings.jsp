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
<%@ page import="com.tesshu.jpsonic.domain.MenuItemId" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>

    function checkBitrateAvailability() {
        var c = 0;
        Array.from(document.getElementsByName('activeTranscodingIds')).forEach(a => {if (a.checked) {c++}});
        $('[name="transcodeScheme"]').prop("disabled", c == 0);
    }

    function resetBasicSettings() {
        Array.from(document.getElementsByName('allowedMusicFolderIds')).forEach(a => a.checked = true);
        $('[name="transcodeScheme"]').prop("selectedIndex", 4);
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
                    <label for="musicFolder${musicFolder.id}">${musicFolder.name}</label>
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
                <c:if test="${not command.transcodingSupported}">
                    <strong><fmt:message key="playersettings.notranscoder"/></strong>
                </c:if>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="uriWithFileExtensions" id="uriWithFileExtensions"/>
                <label for=uriWithFileExtensions><fmt:message key="dlnasettings.uriwithfileextensions"/></label>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="dlnasettings.submenu"/></summary>

        <table class="tabular sub-menus">
            <thead>
                <tr>
                    <th><fmt:message key="dlnasettings.menuname"/></th>
                    <th><fmt:message key="dlnasettings.menuenabled"/></th>
                    <th><fmt:message key="dlnasettings.nameOnClient"/></th>
                    <th><fmt:message key="dlnasettings.contentoverview"/></th>
                    <th><fmt:message key="dlnasettings.viewopt"/></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${command.subMenuItems}" var="subMenuItem" varStatus="loopStatus">
                    <c:set var="rowInfo" value="${command.subMenuItemRowInfos[subMenuItem.parent]}" />
                    <c:set var="isFirstSubMenu" value="${subMenuItem.parent eq rowInfo.firstChild().parent and subMenuItem.id eq rowInfo.firstChild().id}" />
                    <c:choose>
                        <c:when test="${loopStatus.count eq 13}">
                            <!-- 13: It will be rewritten when Menu is implemented. -->
                            <tr class="lastMenu">
                        </c:when>
                        <c:otherwise>
                            <tr>
                        </c:otherwise>
                    </c:choose>
                        <c:choose>
                            <c:when test="${isFirstSubMenu}">
                                <td rowspan="${rowInfo.count()}">${subMenuItem.parent.name()}</td>
                            </c:when>
                            <c:otherwise>
                            </c:otherwise>
                        </c:choose>
                        <td class="chk-container"><form:checkbox path="subMenuItems[${loopStatus.count-1}].enabled" /></td>
                        <td class="input-text-container">
                            <form:input path="subMenuItems[${loopStatus.count-1}].name" placeholder="${subMenuItem.defaultName}" value="${subMenuItem.name}" class="subMenuName"/>
                        </td>
                        <c:choose>
                            <c:when test="${subMenuItem.id eq MenuItemId.INDEX}">
                                <td>[Index] [Artist Folder] [Album Folder] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.MEDIA_FILE}">
                                <td>[Folder ... ] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.INDEX_ID3}">
                                <td>[Index] [Artist] [Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.ALBUM_ARTIST}">
                                <td>[Artist] [Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.ALBUM_ARTIST_BY_FOLDER}">
                                <td>[Music Folder] [Artist] [Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.ALBUM_ID3}">
                                <td>[Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.ALBUM_BY_GENRE}">
                                <td>[Genre] [Album Folder] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.SONG_BY_GENRE}">
                                <td>[Genre] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.PODCAST_DEFALT}">
                                <td>[Channel] [Episode] [Media Content(Audio)]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.PLAYLISTS_DEFALT}">
                                <td>[Playlist] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RECENTLY_ADDED_ALBUM}">
                                <td>[Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RECENTLY_TAGGED_ALBUM}">
                                <td>[Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RANDOM_ALBUM}">
                                <td>[Random Album] [Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RANDOM_SONG}">
                                <td>[Random Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RANDOM_SONG_BY_ARTIST}">
                                <td>[Artist] [Random Song]</td>
                            </c:when>
                            <c:when test="${subMenuItem.id eq MenuItemId.RANDOM_SONG_BY_FOLDER_ARTIST}">
                                <td>[Music Folder] [Artist] [Random Song]</td>
                            </c:when>
                            <c:otherwise>
                            </c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${isFirstSubMenu}">
                                <c:choose>
                                    <c:when test="${subMenuItem.parent eq MenuItemId.GENRE}">
                                        <td rowspan="${rowInfo.count()}">
                                            <form:checkbox path="dlnaGenreCountVisible" id="dlnaGenreCountVisible"/>
                                            <label for="dlnaGenreCountVisible"><fmt:message key="dlnasettings.genreCountVisible"/></label>
                                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnagenrecountvisible"/></c:import>
                                        </td>
                                    </c:when>
                                    <c:when test="${subMenuItem.parent eq MenuItemId.SHUFFLE}">
                                        <td rowspan="${rowInfo.count()}">
                                            <label for="dlnaRandomMax"><fmt:message key="dlnasettings.randommax"/></label>
                                            <form:input path="dlnaRandomMax" id="dlnaRandomMax" maxlength="4"/>
                                        </td>
                                    </c:when>
                                    <c:when test="${subMenuItem.parent eq MenuItemId.PLAYLISTS}">
                                        <td rowspan="${rowInfo.count()}">
                                            <form:checkbox path="dlnaGuestPublish" id="dlnaGuestPublish"/>
                                            <label for=dlnaGuestPublish><fmt:message key="dlnasettings.guestpublish"/></label>
                                        </td>
                                    </c:when>
                                    <c:otherwise>
                                        <td rowspan="${rowInfo.count()}"></td>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:otherwise>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
    </div>

</form:form>

</body></html>
