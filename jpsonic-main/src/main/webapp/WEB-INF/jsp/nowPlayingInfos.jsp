<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html>
<head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/dwr/interface/nowPlayingService.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>
$(document).ready(function(){
    dwr.engine.setErrorHandler(null);
    startGetNowPlayingTimer();
});

function startGetNowPlayingTimer() {
    nowPlayingService.getNowPlaying(getNowPlayingCallback);
    setTimeout("startGetNowPlayingTimer()", 10000);
}

function getNowPlayingCallback(nowPlaying) {

    dwr.util.removeAllRows("nowPlayingAllBody", { filter:function(tr) {
        return (tr.id != "pattern");
    }});
    for (var i = 0; i < nowPlaying.length; i++) {
        var npInfo  = nowPlaying[i];
        var id = i + 1;
        dwr.util.cloneNode("pattern", { idSuffix:id });

        if (npInfo.coverArtUrl == null) {
            $("#coverArt" + id).css("display", "hidden");
        } else {
            $("#coverArt" + id).attr('src', npInfo.coverArtUrl);
        }
        if ($("#username" + id)) {
            $("#username" + id).text(npInfo.username);
        }
        if ($("#link" + id)) {
            let link = "<a title='" + npInfo.tooltip + "' target='main' href='" + npInfo.albumUrl + "'>";
            if (npInfo.artist != null) {
                link += npInfo.artist + "<br/>";
            }
            link += "<span class='songTitle'>" + npInfo.title + "</span></a><br/>";
            $("#link" + id).html(link);
        }
        if (npInfo.lyricsUrl != null) {
            $("#lyrics" + id).html("<a href='" + nowPlaying[i].lyricsUrl + "' onclick=\"return popupSize(this, 'lyrics', 640, 550)\">" + "<fmt:message key="main.lyrics"/>" + "</a>");
        }
        let minutesAgo = nowPlaying[i].minutesAgo;
        if (minutesAgo > 4) {
            $("#time" + id).html(minutesAgo + "<fmt:message key="main.minutesago"/>");
        }

        $("#pattern" + id).css("display", "table-row");
    }

}
</script>
</head>
<body class="nowPlayingInfos">

<table>
    <thead>
        <tr>
            <th></th>
            <th></th>
            <th></th>
            <th></th>
            <th></th>
        </tr>
    </thead>
    <tbody id="nowPlayingAllBody">
        <tr id="pattern">
            <td><img id="coverArt" style='padding-right:5pt;width:60px;height:60px'></td>
            <td id="username"></td>
            <td id="link"></td>
            <td id="lyrics"></td>
            <td id="time"></td>
        </tr>
    </tbody>
</table>

</body>
</html>
