<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="command" type="org.airsonic.player.command.SearchCommand"--%>

<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script>
	function showMoreArtists() {
		$('.artistRow').show();
		$('#moreArtists').hide();
	}
	function showMoreAlbums() {
		$('.albumRow').show();
		$('#moreAlbums').hide();
	}
	function showMoreSongs() {
		$('.songRow').show();
		$('#moreSongs').hide();
	}
</script>
</head>
<body class="mainframe search">

<section>
    <h1 class="search"><fmt:message key="search.title"/></h1>
</section>

<c:if test="${command.indexBeingCreated}">
    <p><strong><fmt:message key="search.index"/></strong></p>
</c:if>

<c:if test="${not command.indexBeingCreated and empty command.artists and empty command.albums and empty command.songs}">
    <p><strong><fmt:message key="search.hits.none"/></strong></p>
</c:if>

<c:if test="${not empty command.artists}">
    <details open>
        <summary><fmt:message key="search.hits.artists"/></summary>
    
        <c:if test="${fn:length(command.artists) gt 5}">
            <ul class="controls">
                <li id="moreAlbums"><a href="javascript:showMoreArtists()" title="<fmt:message key='search.hits.more'/>" class="control all"><fmt:message key="search.hits.more"/></a></li>
            </ul>
        </c:if>
    
        <table class="tabular artists">
            <thead>
                <tr>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th><fmt:message key="common.fields.filebaseartist" /></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${command.artists}" var="match" varStatus="loopStatus">
                    <sub:url value="/main.view" var="mainUrl">
                        <sub:param name="path" value="${match.path}"/>
                    </sub:url>
                    <tr class="artistRow" ${loopStatus.count > 5 ? "style='display:none'" : ""}>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${match.id}"/>
                            <c:param name="playEnabled" value="${command.user.streamRole and not command.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${command.user.streamRole and (not command.partyModeEnabled or not match.directory)}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td class="truncate"><a href="${mainUrl}">${fn:escapeXml(match.name)}</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

<c:if test="${not empty command.albums}">
    <details open>
        <summary><fmt:message key="search.hits.albums"/></summary>

        <c:if test="${fn:length(command.albums) gt 5}">
            <ul class="controls">
                <li id="moreAlbums"><a href="javascript:showAllAlbums()" title="<fmt:message key='search.hits.more'/>" class="control all"><fmt:message key="search.hits.more"/></a></li>
            </ul>
        </c:if>

        <table class="tabular albums">
            <thead>
                <tr>
                    <th class="truncate"></th>
                    <th class="truncate"></th>
                    <th class="truncate"></th>
                    <th class="truncate searchAlbumAlbum"><fmt:message key="common.fields.album" /></th>
                    <th class="truncate 
                    <c:choose>
                        <c:when test='${command.composerVisible && command.genreVisible}'>searchAlbumAlbumartist1</c:when>
                        <c:when test='${command.genreVisible}'>searchAlbumAlbumartist2</c:when>
                        <c:otherwise></c:otherwise>
                    </c:choose>
                    "><fmt:message key="common.fields.albumartist" /></th>
                    <c:if test="${command.genreVisible}">
                        <th class="truncate"><fmt:message key="common.fields.genre" /></th>
                    </c:if>
                </tr>
            </thead>
    
            <c:forEach items="${command.albums}" var="match" varStatus="loopStatus">
                <sub:url value="/main.view" var="mainUrl">
                    <sub:param name="path" value="${match.path}"/>
                </sub:url>
                <tr class="albumRow" ${loopStatus.count > 5 ? "style='display:none'" : ""}>
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${match.id}"/>
                        <c:param name="playEnabled" value="${command.user.streamRole and not command.partyModeEnabled}"/>
                        <c:param name="addEnabled" value="${command.user.streamRole and (not command.partyModeEnabled or not match.directory)}"/>
                        <c:param name="asTable" value="true"/>
                    </c:import>
                    <td class="truncate"><a href="${mainUrl}">${fn:escapeXml(match.albumName)}</a></td>
                    <td class="truncate"><span class="detail">${fn:escapeXml(match.artist)}</span></td>
                    <c:if test="${command.genreVisible}">
                        <td class="truncate"><span class="detail">${fn:escapeXml(match.genre)}</span></td>
                    </c:if>
                </tr>
            </c:forEach>
        </table>
    </details>
</c:if>


<c:if test="${not empty command.songs}">

<details open>
    <summary><fmt:message key="search.hits.songs"/></summary>

    <c:if test="${fn:length(command.songs) gt 15}">
        <ul class="controls">
            <li id="moreAlbums"><a href="javascript:showMoreSongs()" title="<fmt:message key='search.hits.more'/>" class="control all"><fmt:message key="search.hits.more"/></a></li>
        </ul>
    </c:if>

    <table class="tabular songs">
        <thead>
            <tr>
                <th class="truncate"></th>
                <th class="truncate"></th>
                <th class="truncate"></th>
                <th class="truncate searchSongTitle"><fmt:message key="common.fields.songtitle" /></th>
                <th class="truncate searchSongAlbum"><fmt:message key="common.fields.album" /></th>
                <th class="truncate searchSongArtist"><fmt:message key="common.fields.artist" /></th>
                <c:if test="${command.composerVisible}">
                    <th class="truncate 
                    <c:if test='${command.genreVisible}'>searchSongArtist</c:if>
                    "><fmt:message key="common.fields.composer" /></th>
                </c:if>
                <c:if test="${command.genreVisible}">
                    <th class="truncate"><fmt:message key="common.fields.genre" /></th>
                </c:if>
            </tr>
        </thead>

        <c:forEach items="${command.songs}" var="match" varStatus="loopStatus">
            <sub:url value="/main.view" var="mainUrl">
            <sub:param name="path" value="${match.parentPath}"/>
            </sub:url>
            <tr class="songRow" ${loopStatus.count > 15 ? "style='display:none'" : ""}>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${match.id}"/>
                    <c:param name="playEnabled" value="${command.user.streamRole and not command.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${command.user.streamRole and (not command.partyModeEnabled or not match.directory)}"/>
                    <c:param name="video" value="${match.video and command.player.web}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>
                <td class="truncate"><span class="songTitle">${fn:escapeXml(match.title)}</span></td>
                <td class="truncate"><a href="${mainUrl}"><span class="detail">${fn:escapeXml(match.albumName)}</span></a></td>
                <td class="truncate"><span class="detail">${fn:escapeXml(match.artist)}</span></td>
                <c:if test="${command.composerVisible}">
                    <td class="truncate"><span class="detail">${fn:escapeXml(match.composer)}</span></td>
                </c:if>
                <c:if test="${command.genreVisible}">
                    <td class="truncate"><span class="detail">${fn:escapeXml(match.genre)}</span></td>
                </c:if>
            </tr>
        </c:forEach>
    </table>
</c:if>

</body></html>