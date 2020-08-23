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
        starService.unstar(mediaFileId);
    } else if ("control star" == $(imageId).attr('class')) {
        $(imageId).removeClass('star');
        $(imageId).addClass('star-fill');
        starService.star(mediaFileId);
    }
}
var model = {
  duration: ${empty model.duration ? 0: model.duration},
  remoteStreamUrl: "${model.remoteStreamUrl}",
  video_title: "${model.video.title}",
  remoteCoverArtUrl: "${model.remoteCoverArtUrl}",
  streamUrl: "${model.streamUrl}",
  video_id: "${model.video.id}",
  hide_share: ${model.user.shareRole ? 1: 0},
  hide_download: ${model.user.downloadRole ? 1: 0}
}
</script>
<script src="<c:url value='/script/videoPlayerCast.js'/>"></script>
</head>

<body class="mainframe videoPlayer">


<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
    <h1 class="movie">${fn:escapeXml(model.video.title)}</h1>
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
                <c:when test="${not empty model.video.starredDate}">
                    <li><a id="starImage" href="javascript:toggleStar(${model.video.id}, '#starImage')" class="control star-fill">Star ON</a></li>
                </c:when>
                <c:otherwise>
                    <li><a id="starImage" href="javascript:toggleStar(${model.video.id}, '#starImage')" class="control star">Star OFF</a></li>
                </c:otherwise>
            </c:choose>
        </ul>
    </c:if>
</div>

<div>
    <div id="overlay">
        <div id="overlay_text">Playing on Chromecast</div>
    </div>
    <video id="videoPlayer" width="640" height="360"></video>
    <div id="media_control">
        <div id="progress_slider"></div>
        <div id="play"></div>
        <div id="pause"></div>
        <div id="progress">0:00</div>
        <div id="duration">0:00</div>
        <div id="audio_on"></div>
        <div id="audio_off"></div>
        <div id="volume_slider"></div>
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
        <div id="share"></div>
        <div id="download"></div>
        <div id="casticonactive"></div>
        <div id="casticonidle"></div>
    </div>
</div>
<div id="debug"></div>

<script>
    var CastPlayer = new CastPlayer();
</script>

</body>
</html>
