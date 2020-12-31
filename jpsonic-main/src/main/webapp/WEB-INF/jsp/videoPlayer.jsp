<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/starService.js'/>"></script>
<script src="<c:url value='/script/cast_sender-v1.js'/>"></script>

<script>
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
var model = {
  duration: ${empty model.duration ? 0: model.duration},
  remoteStreamUrl: "<sub:escapeJavaScript string='${model.remoteStreamUrl}'/>",
  video_title: "<sub:escapeJavaScript string='${model.video.title}'/>",
  remoteCoverArtUrl: "${model.remoteCoverArtUrl}",
  streamUrl: "<sub:escapeJavaScript string='${model.streamUrl}'/>",
  video_id: "${model.video.id}",
  hide_share: ${model.user.shareRole && model.isShowShare ? 0 : 1},
  hide_download: ${model.user.downloadRole && model.isShowDownload ? 0: 1}
}

top.setDialogVideoPlayerTitle("<sub:escapeJavaScript string='${model.video.title}'/>");

$(document).ready(function(){
    const player = document.getElementById("videoPlayer");
    if (document.fullscreenEnabled) {
        const btn = document.getElementById("full-screen");
        btn.addEventListener("click", function() {
            if (player.requestFullscreen) {
                player.requestFullscreen();
            } else if (player.mozRequestFullScreen) {
                player.mozRequestFullScreen();
            } else if (player.webkitRequestFullscreen) {
                player.webkitRequestFullscreen();
            }
        });
        btn.style.display ="block";
    }
    if (!player.hasAttribute('isPip')) {
        const btn = document.getElementById("pip")
        btn.addEventListener("click", function() {
            if (player.requestPictureInPicture) {
                player.requestPictureInPicture();
            }
        });
        btn.style.display ="block";
    }
});

function hideControls(t) {
	t.controls=false;
}
function showControls(t){
	setTimeout(function(){t.controls=true;} ,500);
}

</script>
<script src="<c:url value='/script/videoPlayerCast.js'/>"></script>
</head>

<body class="mainframe videoPlayer" oncontextmenu="return false;">

    <div class="videoView">
        <div id="overlay"><div id="overlay_text">Playing on Chromecast</div></div>
        <video
        	id="videoPlayer"
        	onplaying="hideControls(this)"
        	onwaiting="showControls(this)"
        	playsinline
        	webkit-playsinline
        	${model.user.downloadRole && model.isShowDownload ? '': 'controlsList="nodownload"'}
        >
        </video>
    </div>

    <div id="media_control">

        <div class="progressBar">
            <div id="progress">0:00</div>
            <div id="progress_slider"></div>
            <div id="duration">0:00</div>
        </div>

        <span class="actions primary">
            <div id="play" class="control play" title="<fmt:message key='playqueue.start'/>"></div>
            <div id="pause" class="control pause" title="<fmt:message key='playqueue.stop'/>"></div>

            <div id="audio_on" class="control mute" title="<fmt:message key='playqueue.muteon'/>"></div>
            <div id="audio_off" class="control volume" title="<fmt:message key='playqueue.muteoff'/>"></div>
            <div class="volume">
                <div id="volume_slider"></div>
            </div>
        </span>
        <span class="actions sub">

            <c:choose>
                <c:when test="${not empty model.video.starredDate}">
                    <a id="starImage" href="javascript:toggleStar(${model.video.id}, '#starImage')" title="<fmt:message key='main.starredoff'/>" class="control star-fill"><fmt:message key="main.starredoff"/></a>
                </c:when>
                <c:otherwise>
                    <a id="starImage" href="javascript:toggleStar(${model.video.id}, '#starImage')" title="<fmt:message key='main.starredon'/>" class="control star"><fmt:message key="main.starredon"/></a>
                </c:otherwise>
            </c:choose>
            <div id="share" class="control share" title="<fmt:message key='main.more.share'/>"></div>
            <div id="download" class="control download" title="<fmt:message key='common.download'/>"></div>
            <div id="casticonactive" class="control cast-active" title="<fmt:message key='playqueue.caston'/>"></div>
            <div id="casticonidle" class="control cast-idle" title="<fmt:message key='playqueue.castoff'/>"></div>
            <div id="full-screen" class="control maximize" title="<fmt:message key='playqueue.maximize'/>"></div>
            <div id="pip" class="control pip" title="<fmt:message key='common.pip'/>"></div>
	        <select name="bitrate_menu" id="bitrate_menu">
	            <c:forEach items="${model.bitRates}" var="bitRate">
	                <c:choose>
	                    <c:when test="${bitRate eq model.defaultBitRate}">
	                        <option selected="selected" value="${bitRate}">${bitRate} Kbps</option>
	                    </c:when>
	                    <c:otherwise>
	                        <option value="${bitRate}">${bitRate} Kbps</option>
	                    </c:otherwise>
	                </c:choose>
	            </c:forEach>
	        </select>
        </span>
    </div>

    <div id="debug"></div>

    <script>
        var CastPlayer = new CastPlayer();
    </script>

</body>
</html>
