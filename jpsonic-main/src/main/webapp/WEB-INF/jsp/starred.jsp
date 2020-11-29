<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/starService.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
<script src="<c:url value='/script/jpsonic/coverartContainer.js'/>"></script>
<script src="<c:url value='/script/jpsonic/truncate.js'/>"></script>
<script>

$(document).ready(function(){
    document.getElementById('albumsContainer').addEventListener("toggle", function(event) {
        adjustCoverartContainer();
    });
    document.getElementById('songsContainer').addEventListener("toggle", function(event) {
        checkTruncate("#songsContainer", ".tabular.songs", 4, ["album", "artist", "song"]);
    });
    initTruncate("#songsContainer", ".tabular.songs", 4, ["album", "artist", "song"]);
});

function toggleStar(mediaFileId, imageId) {
    if ("control star-fill" == $(imageId).attr('class')) {
        $(imageId).removeClass('star-fill');
        $(imageId).addClass('star');
        $(imageId).attr('title', '<fmt:message key="main.starredon"/>');
        starService.unstar(mediaFileId);
    } else if ("control star" == $(imageId).attr('class')) {
        $(imageId).removeClass('star');
        $(imageId).addClass('star-fill');
        $(imageId).attr('title', '<fmt:message key="main.starredoff"/>');
        starService.star(mediaFileId);
    }
}

function onSavePlaylist() {
    playlistService.createPlaylistForStarredSongs(function (playlistId) {
        window.parent.main.location.href = "playlist.view?id=" + playlistId;
        $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveasplaylist"/>");
    });
}

function onPlayAll() {
    top.playQueue.onPlayStarred();
}

</script>
</head>
<body class="mainframe starred">

<section>
    <h1 class="star"><fmt:message key="starred.title"/></h1>
</section>

<c:if test="${empty model.artists and empty model.albums and empty model.songs}">
    <p><strong><fmt:message key="starred.empty"/></strong></p>
</c:if>

<c:set var="isOpen" value='${model.openDetailStar ? "open" : ""}' />

<c:if test="${not empty model.artists}">
    <details ${isOpen}>
        <summary><fmt:message key="search.hits.artists"/> (${fn:length(model.artists)})</summary>
        <table class="tabular artists">
            <thead>
                <tr>
                    <th></th><%-- star --%>
                    <th></th><%-- play --%>
                    <th></th><%-- add --%>
                    <th></th><%-- next --%>
                    <th class="${artistClass}"><fmt:message key="common.fields.artist" /></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${model.artists}" var="artist">
                    <c:url value="/main.view" var="mainUrl">
                        <c:param name="id" value="${artist.id}"/>
                    </c:url>
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${artist.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not artist.directory)}"/>
                            <c:param name="starEnabled" value="true"/>
                            <c:param name="starred" value="${not empty artist.starredDate}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td><a href="${mainUrl}">${fn:escapeXml(artist.name)}</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

<c:if test="${not empty model.albums}">
    <details ${isOpen} id="albumsContainer">
        <summary><fmt:message key="search.hits.albums"/> (${fn:length(model.albums)})</summary>
        <c:import url="viewAsListSelector.jsp">
            <c:param name="targetView" value="starred.view"/>
            <c:param name="viewAsList" value="${model.viewAsList}"/>
            <c:param name="directoryId" value="${model.dir.id}"/>
        </c:import>

        <c:choose>
            <c:when test="${model.viewAsList}">
                <table class="tabular albums">
                    <thead>
                        <tr>
                            <th></th><%-- star --%>
                            <th></th><%-- play --%>
                            <th></th><%-- add --%>
                            <th></th><%-- next --%>
                            <th><fmt:message key="common.fields.album" /></th>
                            <th></th><%-- year --%>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${model.albums}" var="albums">
                            <tr>
                                <c:import url="playButtons.jsp">
                                    <c:param name="id" value="${albums.id}"/>
                                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="starEnabled" value="true"/>
                                    <c:param name="starred" value="${not empty albums.starredDate}"/>
                                    <c:param name="asTable" value="true"/>
                                </c:import>
                                <td><a href="main.view?id=${albums.id}" title="${fn:escapeXml(albums.name)}">${fn:escapeXml(albums.name)}</a></td>
                                <td class="year">${albums.year}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <div class="coverart-container">
                    <c:forEach items="${model.albums}" var="album" varStatus="loopStatus">
                        <c:set var="albumTitle">
                            <c:choose>
                                <c:when test="${empty album.name}">
                                    <fmt:message key="common.unknown"/>
                                </c:when>
                                <c:otherwise>
                                    ${fn:escapeXml(album.name)}
                                </c:otherwise>
                            </c:choose>
                        </c:set>
                        <div class="albumThumb">
                            <c:import url="coverArt.jsp">
                                <c:param name="albumId" value="${album.id}"/>
                                <c:param name="caption1" value="${albumTitle}"/>
                                <c:param name="caption2" value="${fn:escapeXml(album.artist)}"/>
                                <c:param name="captionCount" value="2"/>
                                <c:param name="coverArtSize" value="${model.coverArtSize}"/>
                                <c:param name="showLink" value="true"/>
                                <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                                <c:param name="hideOverflow" value="true"/>
                            </c:import>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </details>
</c:if>

<c:if test="${not empty model.songs}">
    <details ${isOpen} id="songsContainer">
        <summary><fmt:message key="search.hits.songs"/> (${fn:length(model.songs)})</summary>
        <ul class="controls">
            <li><a href="javascript:onPlayAll()" title="<fmt:message key='main.playall'/>" class="control play"><fmt:message key="main.playall"/></a></li>
            <li><a href="javascript:onSavePlaylist()" title="<fmt:message key='playlist.save'/>" class="control saveas"><fmt:message key="playlist.save"/></a></li>
        </ul>
        <table class="tabular songs">

            <c:set var="titleClass" value="song" />
            <c:set var="albumClass" value="album" />
            <c:set var="artistClass" value="artist" />
            <c:if test="${model.simpleDisplay}">
                <c:choose>
                    <c:when test="${!model.visibility.albumVisible and !model.visibility.artistVisible}">
                        <c:set var="titleClass" value="song prime-end" />
                    </c:when>
                    <c:when test="${!model.visibility.artistVisible}">
                        <c:set var="albumClass" value="album prime-end" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="artistClass" value="artist prime-end" />
                    </c:otherwise>
                </c:choose>
            </c:if>
            <c:set var="suppl" value="${model.simpleDisplay ? 'supplement' : ''}" />

            <thead>
                <tr>
                    <th></th><%-- star --%>
                    <th></th><%-- play --%>
                    <th></th><%-- add --%>
                    <th></th><%-- next --%>
                    <th class="${titleClass}"><fmt:message key="common.fields.songtitle" /></th>
                    <th class="${albumClass}"><fmt:message key="common.fields.album" /></th>
                    <th class="${artistClass}"><fmt:message key="common.fields.artist" /></th>
                    <c:if test="${model.visibility.composerVisible}"><th class="${suppl} composer"><fmt:message key="common.fields.composer" /></th></c:if>
                    <c:if test="${model.visibility.genreVisible}"><th class="${suppl} genre"><fmt:message key="common.fields.genre" /></th></c:if>
                    <c:if test="${model.visibility.yearVisible}"><th class="${suppl} year"></th></c:if>
                    <c:if test="${model.visibility.formatVisible}"><th class="${suppl} format"></th></c:if>
                    <c:if test="${model.visibility.fileSizeVisible}"><th class="${suppl} size"></th></c:if>
                    <c:if test="${model.visibility.durationVisible}"><th class="${suppl} duration"></th></c:if>
                    <c:if test="${model.visibility.bitRateVisible}"><th class="${suppl} bitrate"></th></c:if>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${model.songs}" var="song">
                    <sub:url value="/main.view" var="mainUrl">
                        <sub:param name="path" value="${song.parentPath}"/>
                    </sub:url>
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${song.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="starEnabled" value="true"/>
                            <c:param name="starred" value="${not empty song.starredDate}"/>
                            <c:param name="video" value="false"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td class="${titleClass}"><span>${fn:escapeXml(song.title)}</span></td>
                        <td class="${albumClass}"><span><a href="${mainUrl}">${fn:escapeXml(song.albumName)}</a></span></td>
                        <td class="${artistClass}"><span>${fn:escapeXml(song.artist)}</span></td>
                        <c:if test="${model.visibility.composerVisible}"><td class="${suppl} composer">${fn:escapeXml(song.composer)}</td></c:if>
                        <c:if test="${model.visibility.genreVisible}"><td class="${suppl} genre">${fn:escapeXml(song.genre)}</td></c:if>
                        <c:if test="${model.visibility.yearVisible}"><td class="${suppl} year">${song.year}</td></c:if>
                        <c:if test="${model.visibility.formatVisible}"><td class="${suppl} format">${fn:toLowerCase(song.format)}</td></c:if>
                        <c:if test="${model.visibility.fileSizeVisible}"><td class="${suppl} size"><span><sub:formatBytes bytes="${song.fileSize}"/></span></td></c:if>
                        <c:if test="${model.visibility.durationVisible}"><td class="${suppl} duration">${song.durationString}</td></c:if>
                        <c:if test="${model.visibility.bitRateVisible}">
                            <td class="${suppl} bitrate">
                                <c:if test="${not empty song.bitRate}">
                                    ${song.bitRate} Kbps ${song.variableBitRate ? "vbr" : ""}
                                </c:if>
                                <c:if test="${song.video and not empty song.width and not empty song.height}">
                                    (${song.width}x${song.height})
                                </c:if>
                            </td>
                        </c:if>

                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

<c:if test="${not empty model.videos}">
    <details ${isOpen}>
        <summary><fmt:message key="search.hits.videos"/> (${fn:length(model.videos)})</summary>
        <table class="tabular movie">
            <thead>
                <tr>
                    <th></th><%-- star --%>
                    <th></th><%-- play --%>
                    <th></th><%-- empty: Since it is implemented so that it can be displayed in the same table as song --%>
                    <th></th><%-- empty: Since it is implemented so that it can be displayed in the same table as song --%>
                    <th class="${titleClass}"><fmt:message key="common.fields.songtitle" /></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${model.videos}" var="video">
                    <c:url value="/videoPlayer.view" var="videoUrl">
                        <c:param name="id" value="${video.id}"/>
                    </c:url>
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${video.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="starEnabled" value="true"/>
                            <c:param name="starred" value="${not empty video.starredDate}"/>
                            <c:param name="video" value="${model.player.web}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td><a href="javascript:top.onOpenDialogVideoPlayer('${videoUrl}')">${fn:escapeXml(video.name)}</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

</body></html>
