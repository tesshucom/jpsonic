<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
<script src="<c:url value='/dwr/interface/starService.js'/>"></script>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script>

$(document).ready(function(){
	init();
    checkTruncate();
	function onResize(c,t){onresize=function(){clearTimeout(t);t=setTimeout(c,100)};return c};
	onResize(function() {checkTruncate();})();
});

function checkTruncate() {
    $('.tabular.playlist tr td.truncate').each(function(index , e) {
        $(e).removeClass('truncate');
        $(e).children('span').removeAttr('title');
    });
    if($(window).width() < $('.tabular.playlist').width() + 20){
        const threshold = $(window).width() / 4;
        function writeTruncate($clazz){
            $('.tabular.playlist tr td.' + $clazz).each(function(index , e) {
                if(threshold < $(e).width()){
                    $(e).addClass('truncate');
                    $(e).children('span').attr('title', $(e).text());
                }
            });
        }
        writeTruncate('song');
        writeTruncate('album');
        writeTruncate('artist');
    }
}

var playlist;
var songs;
function init() {
    dwr.engine.setErrorHandler(null);
    $("#dialog-edit").dialog({resizable: true, width:400, autoOpen: false,
        buttons: {
            "<fmt:message key="common.save"/>": function() {
                $(this).dialog("close");
                var name = $("#newName").val();
                var comment = $("#newComment").val();
                var shared = $("#newShared").is(":checked");
                $("#name").text(name);
                $("#comment").text(comment);
                playlistService.updatePlaylist(playlist.id, name, comment, shared, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
            },
            "<fmt:message key="common.cancel"/>": function() {
                $(this).dialog("close");
            }
        }
    });

    $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
        buttons: {
            "<fmt:message key="common.delete"/>": function() {
                $(this).dialog("close");
                playlistService.deletePlaylist(playlist.id, function (){
                    window.parent.main.location = "playlists.view";
                });
            },
            "<fmt:message key="common.cancel"/>": function() {
                $(this).dialog("close");
            } 
        }
    });

    $("#playlistBody").sortable({
        stop: function(event, ui) {
            var indexes = [];
            $("#playlistBody").children().each(function() {
                var id = $(this).attr("id").replace("pattern", "");
                if (id.length > 0) {
                    indexes.push(parseInt(id) - 1);
                }
            });
            onRearrange(indexes);
        },
        cursor: "move",
        axis: "y",
        containment: "parent",
        helper: function(e, tr) {
            var originals = tr.children();
            var trclone = tr.clone();
            trclone.children().each(function(index) {
                $(this).width($('.tabular.playlist').width());
                $(this).addClass('row-drag');
            });
            return trclone;
        }
    });

    getPlaylist();
}

function getPlaylist() {
    playlistService.getPlaylist(${model.playlist.id}, playlistCallback);
}

function playlistCallback(playlistInfo) {
    this.playlist = playlistInfo.playlist;
    this.songs = playlistInfo.entries;

    if (songs.length == 0) {
        $("#empty").show();
        $("#playlistHeader").hide();
    } else {
        $("#empty").hide();
        $("#playlistHeader").show();
    }

    $("#songCount").text(playlist.fileCount);
    $("#duration").text(playlist.durationAsString);

    if (playlist.shared) {
        $("#shared").html("<fmt:message key="playlist2.shared"/>");
    } else {
        $("#shared").html("<fmt:message key="playlist2.notshared"/>");
    }

    // Delete all the rows except for the "pattern" row
    dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
        return (tr.id != "pattern");
    }});

    // Create a new set cloned from the pattern row
    for (var i = 0; i < songs.length; i++) {
        var song  = songs[i];
        var id = i + 1;
        dwr.util.cloneNode("pattern", { idSuffix:id });
        if (song.starred) {
            $("#starSong" + id).removeClass('star');
            $("#starSong" + id).addClass('star-fill');
        } else {
            $("#starSong" + id).removeClass('star-fill');
            $("#starSong" + id).addClass('star');
        }
        if (!song.present) {
            $("#missing" + id).show();
        }
        $("#index" + id).text(id);
        $("#title" + id).text(song.title);
        $("#title" + id).attr("title", song.title);
        $("#album" + id).text(song.album);
        $("#album" + id).attr("title", song.album);
        $("#albumUrl" + id).attr("href", "main.view?id=" + song.id);
        $("#artist" + id).text(song.artist);
        $("#artist" + id).attr("title", song.artist);
        $("#composer" + id).text(song.composer);
        $("#genre" + id).text(song.genre);
        $("#songDuration" + id).text(song.durationAsString);

        // Note: show() method causes page to scroll to top.
        $("#pattern" + id).css("display", "table-row");
    }
    checkTruncate();
}

function onPlay(index) {
    top.playQueue.onPlayPlaylist(playlist.id, index);
}
function onPlayAll() {
    top.playQueue.onPlayPlaylist(playlist.id);
}
function onAddAll() {
    top.playQueue.onAddPlaylist(playlist.id);
}
function onAdd(index) {
    top.playQueue.onAdd(songs[index].id);
    $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
}
function onAddNext(index) {
    top.playQueue.onAddNext(songs[index].id);
    $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
}
function onStar(index) {
    playlistService.toggleStar(playlist.id, index, playlistCallback);
}
function onRemove(index) {
    playlistService.remove(playlist.id, index, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
}
function onRearrange(indexes) {
    playlistService.rearrange(playlist.id, indexes, playlistCallback);
}
function onEditPlaylist() {
    $("#dialog-edit").dialog("open");
}
function onDeletePlaylist() {
    $("#dialog-delete").dialog("open");
}

</script>
</head>
<body class="mainframe playlist">

<nav>
    <ul class="breadcrumb">
        <li><a href="playlists.view"><fmt:message key="left.playlists"/></a></li>
    </ul>
</nav>

<section>
    <h1 class="playlists"><span id="name">${fn:escapeXml(model.playlist.name)}</span></h1>

    <dl class="overview">
        <dt><span class="icon duration"><fmt:message key="playlist2.duration"/></span></dt>
        <dd><span id="songCount"></span><fmt:message key="playlist2.songs"/> &ndash; <span id="duration"></span></dd>
        <dt><span class="icon date"><fmt:message key="playlist2.created"/></span></dt>
        <dd><fmt:formatDate type="date" dateStyle="long" value="${model.playlist.created}"/></dd>
        <dt><span class="icon person"><fmt:message key="playlist2.author"/></span></dt>
        <dd>${fn:escapeXml(model.playlist.username)}</dd>
        <dt><span class="icon visibility"><fmt:message key="playlist2.visibility"/></span></dt>
        <dd><span id="shared"></span></dd>
        <dt><span class="icon comment"><fmt:message key="playlist2.comment"/></span></dt>
        <dd><div id="comment" title="${fn:escapeXml(model.playlist.comment)}">${fn:escapeXml(model.playlist.comment)}</div></dd>
    </dl>

</section>

<div class="actions">
    <ul class="controls">
        <li><a href="javascript:onPlayAll()" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key="common.play"/></a></li>
        <li><a href="javascript:onAddAll()" title="<fmt:message key='main.addall'/>" class="control plus"><fmt:message key="main.addall"/></a></li>
        <c:if test="${model.user.downloadRole and model.showDownload}">
            <c:url value="download.view" var="downloadUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
            <li><a href="${downloadUrl}" title="<fmt:message key='common.download'/>" class="control plus"><fmt:message key="common.download"/></a></li>
        </c:if>
        <c:if test="${model.user.shareRole and model.showShare}">
            <c:url value="createShare.view" var="shareUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
            <li><a href="${shareUrl}" title="<fmt:message key='share.title'/>" class="control share"><fmt:message key="share.title"/></a></li>
        </c:if>
        <c:if test="${model.editAllowed}">
            <li><a href="javascript:void(0)" onclick="onEditPlaylist();" title="<fmt:message key='common.edit'/>" class="control edit"><fmt:message key="common.edit"/></a></li>
            <li><a href="javascript:void(0)" onclick="onDeletePlaylist();" title="<fmt:message key='common.delete'/>" class="control cross"><fmt:message key="common.delete"/></a></li>
        </c:if>
        <c:url value="exportPlaylist.view" var="exportUrl"><c:param name="id" value="${model.playlist.id}"/></c:url>
        <li><a href="${exportUrl}" title="<fmt:message key='playlist2.export'/>" class="control export"><fmt:message key="playlist2.export"/></a></li>
    </ul>
</div>

<div class="tabular-and-thumb">

    <p id="empty"><strong><fmt:message key="playlist2.empty"/></strong></p>

    <c:set var="songClass" value="song" />
    <c:set var="albumClass" value="album" />
    <c:set var="artistClass" value="artist" />
    <c:if test="${not model.editAllowed}">
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
    </c:if>

    <table class="tabular playlist">
        <thead id="playlistHeader">
            <tr>
                <th></th><%-- star --%>
                <th></th><%-- play --%>
                <th></th><%-- add --%>
                <th></th><%-- next --%>
                <th></th><%-- index --%>
                <th></th><%-- missing --%>
                <th class="${songClass}"><fmt:message key="common.fields.songtitle" /></th>
                <th class="${albumClass}"><fmt:message key="common.fields.album" /></th>
                <th class="${artistClass}"><fmt:message key="common.fields.artist" /></th>
                <c:if test="${model.visibility.composerVisible}"><th class="supplement composer"><fmt:message key="common.fields.composer" /></th></c:if>
                <c:if test="${model.visibility.genreVisible}"><th class="supplement genre"><fmt:message key="common.fields.genre" /></th></c:if>
                <th class="supplement"></th><%-- duration --%>
                <c:if test="${model.editAllowed}"><th></th></c:if><%-- delete --%>
            </tr>
        </thead>
        <tbody id="playlistBody">
            <tr id="pattern">
                <td><div id="starSong" onclick="onStar(this.id.substring(8) - 1)" title="Star ON" class="control star">Star ON</div></td>
                <td><div id="play" onclick="onPlay(this.id.substring(4) - 1)" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/></div></td>
                <td><div id="add" onclick="onAdd(this.id.substring(3) - 1)" title="<fmt:message key='common.add'/>" class="control plus"><fmt:message key='common.add'/></div></td>
                <td><div id="addNext" onclick="onAddNext(this.id.substring(7) - 1)" title="<fmt:message key='main.addnext'/>" class="control next"><fmt:message key='main.addnext'/></div></td>
                <td><span id="index">1</span></td>
                <td><span id="missing" class="playlist-missing"><fmt:message key="playlist.missing"/></span></td>
                <td class="${songClass}"><span id="title">Title</span></td>
                <td class="${albumClass}"><a id="albumUrl" target="main"><span id="album">Album</span></a></td>
                <td class="${artistClass}"><span id="artist">Artist</span></td>
                <c:if test="${model.visibility.composerVisible}"><td class="supplement composer"><span id="composer">Composer</span></td></c:if>
                <c:if test="${model.visibility.genreVisible}"><td class="supplement genre"><span id="genre">Genre</span></td></c:if>
                <td class="supplement duration"><span id="songDuration">Duration</span></td>
                <c:if test="${model.editAllowed}">
                  <td class="remove"><div id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" title="<fmt:message key='playlist.remove'/>" class="control minus"><fmt:message key='playlist.remove'/></div></td>
                </c:if>
            </tr>
        </tbody>
    </table>

    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="playlistId" value="${model.playlist.id}"/>
            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
        </c:import>
    </div>

</div>

<div id="dialog-delete" title="<fmt:message key='common.confirm'/>">
    <p><span class="ui-icon ui-icon-alert"></span>
        <fmt:message key="playlist2.confirmdelete"/>
    </p>
</div>

<div id="dialog-edit" title="<fmt:message key='common.edit'/>">
    <form>
    
    	<dl>
    		<dt><label for="newName"><fmt:message key="playlist2.name"/></label></dt>
        	<dd><input type="text" name="newName" id="newName" value="${fn:escapeXml(model.playlist.name)}" class="ui-widget-content"/></dd>
        	<dt><label for="newComment"><fmt:message key="playlist2.comment"/></label></dt>
        	<dd><input type="text" name="newComment" id="newComment" value="${fn:escapeXml(model.playlist.comment)}" class="ui-widget-content"/></dd>
        	<dt></dt>
        	<dd>
        		<input type="checkbox" name="newShared" id="newShared" ${model.playlist.shared ? "checked='checked'" : ""}/>
        		<label for="newShared"><fmt:message key="playlist2.public"/></label>
        	</dd>
        </dl>
    </form>
</div>

</body></html>
