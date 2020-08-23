<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/starService.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
<script src="<c:url value='/dwr/interface/multiService.js'/>"></script>
<script src="<c:url value='/script/jquery.fancyzoom.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script src="<c:url value='/script/jpsonic/coverartContainer.js'/>"></script>
<script>

$(document).ready(function(){
    $("a.fancy").fancyZoom({
        minBorder: 30
    });

    $("#dialog-select-playlist").dialog({resizable: true, height: 350, autoOpen: false,
        buttons: {
            "<fmt:message key="common.cancel"/>": function() {
                $(this).dialog("close");
            }
        }});
    checkTruncate();
    function onResize(c,t){onresize=function(){clearTimeout(t);t=setTimeout(c,100)};return c};
    onResize(function() {checkTruncate();})();
});

function checkTruncate() {
    $('.tabular.songs tr td.truncate').each(function(index , e) {
        $(e).removeClass('truncate');
        $(e).children('span').removeAttr('title');
    });
    const threshold = $('.mainframe').width() / 4;
    function writeTruncate($clazz){
        $('.tabular.songs tr td.' + $clazz).each(function(index , e) {
            if(threshold < $(e).width()){
                $(e).addClass('truncate');
                $(e).children('span').attr('title', $(e).text());
            }
        });
    }
    if($('.mainframe').width() < $('.tabular.songs').width() + 10){
        writeTruncate('song');
        writeTruncate('album');
        writeTruncate('artist');
    }
}

function getSelectedIndexes() {
    var result = "";
    for (var i = 0; i < ${fn:length(model.files)}; i++) {
        var checkbox = $("#songIndex" + i);
        if (checkbox != null  && checkbox.is(":checked")) {
            result += "i=" + i + "&";
        }
    }
    return result;
}

function selectAll(b) {
    for (var i = 0; i < ${fn:length(model.files)}; i++) {
        var checkbox = $("#songIndex" + i);
        if (checkbox != null) {
            checkbox.prop("checked", b);
        }
    }
}

<sub:url value="createShare.view" var="shareUrl">
    <sub:param name="id" value="${model.dir.id}"/>
</sub:url>
<sub:url value="download.view" var="downloadUrl">
    <sub:param name="id" value="${model.dir.id}"/>
</sub:url>

function toggleSelect() {
    selectAll(!(0 < getSelectedIndexes().length));
}

function shareSelected() {
    var selectedIndexes = getSelectedIndexes();
    if(0 == selectedIndexes.length) {
        return;
    }
    location.href = "${shareUrl}&" + selectedIndexes;
}

function downloadSelected() {
    var selected = getSelectedIndexes();
    if(0 == selected.length) {
        return;
    }
    location.href = "${downloadUrl}&" + selected;
}

function toggleStar(mediaFileId, imageId) {
    if ("control star-fill" == $(imageId).attr('class')) {
        $(imageId).removeClass('star-fill');
        $(imageId).addClass('star');
        starService.unstar(mediaFileId);
    } else if ("control star" == $(imageId).attr('class')) {
        $(imageId).removeClass('star');
        $(imageId).addClass('star-fill');
        starService.star(mediaFileId);
    }
}

function playAll() {
    top.playQueue.onPlay(${model.dir.id});
}

function playRandom() {
    top.playQueue.onPlayRandom(${model.dir.id}, 40);
}

function addAll() {
    top.playQueue.onAdd(${model.dir.id});
}

function playSimilar() {
    top.playQueue.onPlaySimilar(${model.dir.id}, 50);
}

function onAppendPlaylist() {
    playlistService.getWritablePlaylists(playlistCallback);
}
function playlistCallback(playlists) {
    $("#dialog-select-playlist-list").empty();
    for (var i = 0; i < playlists.length; i++) {
        var playlist = playlists[i];
        $("<p><a href='#' onclick='appendPlaylist(" + playlist.id + ")'>" + escapeHtml(playlist.name)
                + "</a>").appendTo("#dialog-select-playlist-list");
    }
    $("#dialog-select-playlist").dialog("open");
}
function appendPlaylist(playlistId) {
    $("#dialog-select-playlist").dialog("close");

    var mediaFileIds = new Array();
    for (var i = 0; i < ${fn:length(model.files)}; i++) {
        var checkbox = $("#songIndex" + i);
        if (checkbox && checkbox.is(":checked")) {
            mediaFileIds.push($("#songId" + i).html());
        }
    }
    playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){
        $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.appendtoplaylist'/>");
    });
}
function showAllAlbums() {
    window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
}

function toggleComment() {
    $("#commentForm").toggle();
    $("#comment").toggle();
}

</script>
</head>
<body class="mainframe albumMain">

<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
    <h1 class="album">${fn:escapeXml(model.dir.name)}</h1>
</section>

<div class="actions">

    <c:if test="${not model.partyMode}">
        <ul class="controls">
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl">
                    <sub:param name="id" value="${model.parent.id}"/>
                </sub:url>
                <li><a href="${upUrl}" title="<fmt:message key='main.up'/>" class="control up"><fmt:message key="main.up"/></a></li>
            </c:if>
            <c:choose>
                <c:when test="${not empty model.dir.starredDate}">
                    <li><a id="starImage" href="javascript:toggleStar(${model.dir.id}, '#starImage')" class="control star-fill">Star ON</a></li>
                </c:when>
                <c:otherwise>
                    <li><a id="starImage" href="javascript:toggleStar(${model.dir.id}, '#starImage')" class="control star">Star OFF</a></li>
                </c:otherwise>
            </c:choose>
            <c:if test="${model.user.streamRole}">
                <li><a href="javascript:playAll()" title="<fmt:message key='main.playall'/>" class="control play"><fmt:message key="main.playall"/></a></li>
                <li><a href="javascript:playRandom()" title="<fmt:message key='main.playrandom'/>" class="control shuffle"><fmt:message key="main.playrandom"/></a></li>
                <li><a href="javascript:addAll()" title="<fmt:message key='main.addall'/>" class="control plus"><fmt:message key="main.addall"/></a></li>
            </c:if>
            <c:if test="${model.user.commentRole and model.showRate}">
                <c:import url="ratingInput.jsp">
                    <c:param name="id" value="${model.dir.id}"/>
                    <c:param name="rating" value="${model.userRating}"/>
                    <c:param name="isStreamRole" value="${model.user.streamRole}"/>
                </c:import>
            </c:if>
            <c:if test="${not empty model.artist and not empty model.album}">
                <c:if test="${model.showAlbumSearch}">
                    <sub:url value="https://www.google.com/search" var="googleUrl" encoding="UTF-8">
                        <sub:param name="q" value='"${fn:escapeXml(model.artist)}" "${fn:escapeXml(model.album)}"'/>
                    </sub:url>
                    <sub:url value="https://en.wikipedia.org/wiki/Special:Search" var="wikipediaUrl" encoding="UTF-8">
                        <sub:param name="search" value='"${fn:escapeXml(model.album)}"'/>
                        <sub:param name="go" value="Go"/>
                    </sub:url>
                    <sub:url value="https://www.allmusic.com/search/albums/%22${fn:escapeXml(model.artist)}%22+%22${fn:escapeXml(model.album)}%22" var="allmusicUrl">
                    </sub:url>
                    <sub:url value="https://www.last.fm/search" var="lastFmUrl" encoding="UTF-8">
                        <sub:param name="q" value='"${fn:escapeXml(model.artist)}" "${fn:escapeXml(model.album)}"'/>
                        <sub:param name="type" value="album"/>
                    </sub:url>
                    <sub:url value="https://www.discogs.com/search/" var="discogsUrl" encoding="UTF-8">
                        <sub:param name="q" value='"${fn:escapeXml(model.artist)}" "${fn:escapeXml(model.album)}"'/>
                        <sub:param name="type" value="release"/>
                    </sub:url>
                    <li>
                    	<a href="#" class="control outernal-link" title="Input rating">Input rating</a>
                    	<input type="checkbox" id="isStreamRole2" value="1" autofocus="false" ${model.user.streamRole ? "checked" : ""} />
                    	<input type="checkbox" id="isRateVisible" value="1" autofocus="false" ${model.user.commentRole and model.showRate ? "checked" : ""} />
                        <ul>
                            <li><a target="_blank" href="${googleUrl}" class="control outernal-link">Google</a></li>
                            <li><a target="_blank" rel="noopener noreferrer" href="${wikipediaUrl}" class="control outernal-link">Wikipedia</a></li>
                            <li><a target="_blank" rel="noopener noreferrer" href="${allmusicUrl}" class="control outernal-link">allmusic</a></li>
                            <li><a target="_blank" rel="noopener noreferrer" href="${lastFmUrl}" class="control outernal-link">Last.fm</a></li>
                            <li><a target="_blank" rel="noopener noreferrer" href="${discogsUrl}" class="control outernal-link">Discogs</a></li>
                            <c:if test="${not empty model.musicBrainzReleaseId}">
                                <sub:url value="https://musicbrainz.org/release/${model.musicBrainzReleaseId}" var="musicBrainzUrl" encoding="UTF-8"></sub:url>
                                <li><a target="_blank" rel="noopener noreferrer" href="${musicBrainzUrl}" class="control outernal-link">MusicBrainz</a></li>
                            </c:if>
                        </ul>
                    </li>
                </c:if>
            </c:if>

            <c:if test="${model.user.downloadRole and model.showDownload}">
                <li><a href="${downloadUrl}" title="<fmt:message key='main.downloadall'/>" class="control download"><fmt:message key="main.downloadall"/></a></li>
            </c:if>
            <c:if test="${model.user.coverArtRole and model.showTag}">
                <sub:url value="editTags.view" var="editTagsUrl">
                    <sub:param name="id" value="${model.dir.id}"/>
                </sub:url>
                <li><a href="${editTagsUrl}" title="<fmt:message key='main.tags'/>" class="control tag"><fmt:message key="main.tags"/></a></li>
            </c:if>
            <c:if test="${model.user.coverArtRole and model.showChangeCoverArt}">
                <c:url value="/changeCoverArt.view" var="changeCoverArtUrl">
                    <c:param name="id" value="${model.dir.id}"/>
                </c:url>
                <li><a class="control image" href="${changeCoverArtUrl}"><fmt:message key="coverart.change"/></a></li>
            </c:if>
            <c:if test="${model.user.commentRole and model.showComment}">
                <li><a href="javascript:toggleComment()" title="<fmt:message key='main.comment'/>" class="control comment"><fmt:message key="main.comment"/></a></li>
            </c:if>
            <c:if test="${model.user.shareRole and model.showShare}">
                <li><a href="${shareUrl}" title="<fmt:message key='main.sharealbum'/>" class="control share"><fmt:message key="main.sharealbum"/></a></li>
            </c:if>

        </ul>
    </c:if>

    <c:if test="${model.showLastPlay}">
        <div><fmt:message key="main.playcount"><fmt:param value="${model.dir.playCount}"/></fmt:message></div>
        <c:if test="${not empty model.dir.lastPlayed}">
            <div>
                <fmt:message key="main.lastplayed">
                    <fmt:param><fmt:formatDate type="date" dateStyle="long" value="${model.dir.lastPlayed}"/></fmt:param>
                </fmt:message>
            </div>
        </c:if>
    </c:if>

</div>

<div id="comment" class="comment-input">${model.dir.comment}</div>

<div id="commentForm">
    <form method="post" action="setMusicFileInfo.view">
        <sec:csrfInput />
        <input type="hidden" name="action" value="comment">
        <input type="hidden" name="id" value="${model.dir.id}">
        <textarea name="comment" rows="6" cols="70">${model.dir.comment}</textarea>
        <input type="submit" value="<fmt:message key='common.save'/>">
    </form>
</div>

<c:set var="songClass" value="song" />
<c:set var="albumClass" value="album" />
<c:set var="artistClass" value="artist" />
<c:choose>
    <c:when test="${!model.visibility.albumVisible and !model.visibility.artistVisible}">
        <c:set var="songClass" value="song prime-end" />
    </c:when>
    <c:when test="${!model.visibility.artistVisible}">
        <c:set var="albumClass" value="album prime-end" />
    </c:when>
    <c:otherwise>
  <c:set var="artistClass" value="artist prime-end" />
    </c:otherwise>
</c:choose>

<div class="tabular-and-thumb">
    <div>

        <input type="checkbox" class="jps-input-without-track-no" value="1" autofocus="false" ${model.visibility.trackNumberVisible ? '' : 'checked'}/>
        <input type="checkbox" class="jps-show-album-actions" value="1" autofocus="false" ${model.showAlbumActions ? 'checked' : ''}/>

        <table class="tabular songs">

            <thead>
                <tr>
                    <th></th><%-- star --%>
                    <th></th><%-- play --%>
                    <th></th><%-- add --%>
                    <th></th><%-- next --%>
                    <c:if test="${model.showAlbumActions}">
                        <th></th><%-- check --%>
                    </c:if>
                    <th class="track"></th>
                    <th class="${songClass}"><fmt:message key="common.fields.songtitle" /></th>
                    <c:if test="${model.visibility.albumVisible}"><th class="${albumClass}"><fmt:message key="common.fields.album" /></th></c:if>
                    <c:if test="${model.visibility.artistVisible}"><th class="${artistClass}"><fmt:message key="common.fields.artist" /></th></c:if>
                    <c:if test="${model.visibility.composerVisible}"><th class="supplement composer"><fmt:message key="common.fields.composer" /></th></c:if>
                    <c:if test="${model.visibility.genreVisible}"><th class="supplement genre"><fmt:message key="common.fields.genre" /></th></c:if>
                    <c:if test="${model.visibility.yearVisible}"><th class="supplement"></th></c:if>
                    <c:if test="${model.visibility.formatVisible}"><th class="supplement"></th></c:if>
                    <c:if test="${model.visibility.fileSizeVisible}"><th class="supplement"></th></c:if>
                    <c:if test="${model.visibility.durationVisible}"><th class="supplement"></th></c:if>
                    <c:if test="${model.visibility.bitRateVisible}"><th class="supplement"></th></c:if>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${model.files}" var="song" varStatus="loopStatus">
                <tr>
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${song.id}"/>
                        <c:param name="video" value="${song.video and model.player.web}"/>
                        <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                        <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyMode or not song.directory)}"/>
                        <c:param name="starEnabled" value="true"/>
                        <c:param name="starred" value="${not empty song.starredDate}"/>
                        <c:param name="asTable" value="true"/>
                    </c:import>
                    <c:if test="${model.showAlbumActions}">
                        <td class="action"><input type="checkbox" id="songIndex${loopStatus.count - 1}"><span id="songId${loopStatus.count - 1}">${song.id}</span></td>
                    </c:if>
                    <td class="track"><c:if test="${model.visibility.trackNumberVisible}">${song.trackNumber}</c:if></td>
                    <td class="${songClass}"><span>${fn:escapeXml(song.title)}</span></td>
                    <c:if test="${model.visibility.albumVisible}"><td class="${albumClass}"><span>${fn:escapeXml(song.albumName)}</span></td></c:if>
                    <c:if test="${model.visibility.artistVisible}"><td class="${artistClass}"><span>${fn:escapeXml(song.artist)}</td></span></c:if>
                    <c:if test="${model.visibility.composerVisible}"><td class="supplement composer">${fn:escapeXml(song.composer)}</td></c:if>
                    <c:if test="${model.visibility.genreVisible}"><td class="supplement genre">${fn:escapeXml(song.genre)}</td></c:if>
                    <c:if test="${model.visibility.yearVisible}"><td class="supplement year"><span>${song.year}</td></c:if>
                    <c:if test="${model.visibility.formatVisible}"><td class="supplement format">${fn:toLowerCase(song.format)}</td></c:if>
                    <c:if test="${model.visibility.fileSizeVisible}"><td class="supplement size"><span><sub:formatBytes bytes="${song.fileSize}"/></span></td></c:if>
                    <c:if test="${model.visibility.durationVisible}"><td class="supplement duration">${song.durationString}</td></c:if>
                    <c:if test="${model.visibility.bitRateVisible}">
                        <td class="supplement bitrate">
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

        <c:if test="${model.showAlbumActions}">
            <div class="actions">
                <ul class="controls">
                    <li><a href="javascript:toggleSelect()" title="<fmt:message key='playlist.more.selectall'/> / <fmt:message key='playlist.more.selectnone'/>" class="control select-all"><fmt:message key='playlist.more.selectall'/> / <fmt:message key='playlist.more.selectnone'/></a></li>
                    <c:if test="${model.user.downloadRole and model.showDownload}">
                        <li><a href="javascript:downloadSelected()" title="<fmt:message key='common.download'/>" class="control download"><fmt:message key='common.download'/></a></li>
                    </c:if>
                    <c:if test="${model.user.shareRole and model.showShare}">
                        <li><a href="javascript:shareSelected()" title="<fmt:message key='main.more.share'/>" class="control share"><fmt:message key='main.more.share'/></a></li>
                    </c:if>
                    <li><a href="javascript:onAppendPlaylist()" title="<fmt:message key='playlist.append'/>" class="control export"><fmt:message key='playlist.append'/></a></li>
                </ul>
            </div>
        </c:if>
    </div>

    <div class="albumThumb">
        <c:import url="coverArt.jsp">
           <c:param name="albumId" value="${model.dir.id}"/>
           <c:param name="coverArtSize" value="${model.coverArtSizeLarge}"/>
           <c:param name="showZoom" value="true"/>
       </c:import>
    </div>

</div>

<c:if test="${model.showSibling and not empty model.siblingAlbums}">

    <h2><fmt:message key="albummain.siblingartists"/></h2>
    <div class="actions">
        <c:import url="viewSelector.jsp">
            <c:param name="targetView" value="main.view"/>
        </c:import>
        <c:if test="${model.thereIsMore}">
            <ul class="controls">
                <li><a href="javascript:showAllAlbums()" title="<fmt:message key='main.showall'/>" class="control all"><fmt:message key='main.showall'/></a></li>
            </ul>
        </c:if>
    </div>
    <nav>
        <c:choose>
            <c:when test="${model.viewAsList}">
                <table class="tabular sibling">
                    <thead>
                        <tr>
                            <th></th><%-- play --%>
                            <th></th><%-- add --%>
                            <th></th><%-- next --%>
                            <th class="${albumClass}"><fmt:message key="common.fields.album" /></th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${model.subDirs}" var="child" varStatus="loopStatus">
                            <sub:url value="main.view" var="albumUrl">
                                <sub:param name="id" value="${child.id}"/>
                            </sub:url>
                            <tr>
                                <c:import url="playButtons.jsp">
                                    <c:param name="id" value="${child.id}"/>
                                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="asTable" value="true"/>
                                </c:import>
                                <td class="truncate"><a href="${albumUrl}" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</a></td>
                                <td></td>
                            </tr>
                        </c:forEach>
                        <c:forEach items="${model.siblingAlbums}" var="album" varStatus="loopStatus">
                            <tr>
                                <c:import url="playButtons.jsp">
                                    <c:param name="id" value="${album.id}"/>
                                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                    <c:param name="asTable" value="true"/>
                                </c:import>
                                <td class="${albumClass}"><a href="main.view?id=${album.id}" title="${fn:escapeXml(album.name)}">${fn:escapeXml(album.name)}</a></td>
                                <td class="year">${album.year}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <div class="coverart-container">
                    <c:forEach items="${model.siblingAlbums}" var="album" varStatus="loopStatus">
                        <div class="albumThumb">
                            <c:import url="coverArt.jsp">
                                <c:param name="albumId" value="${album.id}"/>
                                <c:param name="caption1" value="${fn:escapeXml(album.name)}"/>
                                <c:param name="caption2" value="${album.year}"/>
                                <c:param name="captionCount" value="2"/>
                                <c:param name="coverArtSize" value="${model.coverArtSizeMedium}"/>
                                <c:param name="showLink" value="true"/>
                                <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                                <c:param name="hideOverflow" value="true"/>
                            </c:import>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </nav>
</c:if>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

</body>
</html>
