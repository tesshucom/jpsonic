<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.SearchCommand"--%>

<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
<script src="<c:url value='/script/jpsonic/truncate.js'/>"></script>
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
$(document).ready(function(){
    initTruncate("#albumsDetails", ".tabular.albums", 3, ["album", "artist"]);
    initTruncate("#songsDetails", ".tabular.songs", 3, ["album", "artist", "song"]);
});

</script>
</head>
<body class="mainframe search">

<section>
    <h1 class="search"><fmt:message key="search.title"/></h1>
</section>

<c:if test="${command.indexBeingCreated}">
    <p><strong><fmt:message key="search.index"/></strong></p>
</c:if>


<c:set var="artistClass" value="artist" />
<c:if test="${command.simpleDisplay}">
	<c:set var="artistClass" value="artist prime-end" />
</c:if>
<c:set var="suppl" value="${command.simpleDisplay ? 'supplement' : ''}" />

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
                        <td><a href="${mainUrl}">${fn:escapeXml(match.name)}</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

<c:if test="${not empty command.albums}">
    <details id="albumsDetails" open>
        <summary><fmt:message key="search.hits.albums"/></summary>

        <c:if test="${fn:length(command.albums) gt 5}">
            <ul class="controls">
                <li id="moreAlbums"><a href="javascript:showAllAlbums()" title="<fmt:message key='search.hits.more'/>" class="control all"><fmt:message key="search.hits.more"/></a></li>
            </ul>
        </c:if>

        <table class="tabular albums">
            <thead>
                <tr>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th class="album"><fmt:message key="common.fields.album" /></th>
                    <th class="${artistClass}"><fmt:message key="common.fields.albumartist" /></th>
                    <c:if test="${command.composerVisible}"><th class="${suppl} composer"></th></c:if>
                    <c:if test="${command.genreVisible}"><th class="${suppl} genre"><fmt:message key="common.fields.genre" /></th></c:if>
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
                    <td class="album"><span><a href="${mainUrl}">${fn:escapeXml(match.albumName)}</a></span></td>
                    <td class="${artistClass}"><span>${fn:escapeXml(match.artist)}</span></td>
                    <c:if test="${command.composerVisible}"><td class="${suppl} composer"></td></c:if>
                    <c:if test="${command.genreVisible}"><td class="${suppl} genre">${fn:escapeXml(match.genre)}</td></c:if>
                </tr>
            </c:forEach>
        </table>
    </details>
</c:if>


<c:if test="${not empty command.songs}">

<details id="songsDetails" open>
    <summary><fmt:message key="search.hits.songs"/></summary>

    <c:if test="${fn:length(command.songs) gt 15}">
        <ul class="controls">
            <li id="moreAlbums"><a href="javascript:showMoreSongs()" title="<fmt:message key='search.hits.more'/>" class="control all"><fmt:message key="search.hits.more"/></a></li>
        </ul>
    </c:if>

    <table class="tabular songs">
        <thead>
            <tr>
                <th></th>
                <th></th>
                <th></th>
                <th class="song"><fmt:message key="common.fields.songtitle" /></th>
                <th class="album"><fmt:message key="common.fields.album" /></th>
                <th class="${artistClass}"><fmt:message key="common.fields.artist" /></th>
                <c:if test="${command.composerVisible}"><th class="${suppl} composer"><fmt:message key="common.fields.composer" /></th></c:if>
                <c:if test="${command.genreVisible}"><th class="${suppl} genre"><fmt:message key="common.fields.genre" /></th></c:if>
            </tr>
        </thead>
		<tbody>
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
                    <td class="song"><span>${fn:escapeXml(match.title)}</span></td>
                    <td class="album"><span><a href="${mainUrl}">${fn:escapeXml(match.albumName)}</a></span></td>
                    <td class="${artistClass}"><span>${fn:escapeXml(match.artist)}</span></td>
                    <c:if test="${command.composerVisible}"><td class="${suppl} composer">${fn:escapeXml(match.composer)}</td></c:if>
                    <c:if test="${command.genreVisible}"><td class="${suppl} genre">${fn:escapeXml(match.genre)}</td></c:if>
	            </tr>
	        </c:forEach>
	    </tbody>
    </table>
</c:if>

</body></html>