<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<style type="text/css">
    #progressBar {width: 350px; height: 10px; border: 1px solid black; display:none;}
    #progressBarContent {width: 0; height: 10px; background: url("<c:url value="/icons/default_light/progress.png"/>") repeat;}
</style>
<script src="<c:url value='/dwr/interface/transferService.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<script>

function refreshProgress() {
    transferService.getUploadInfo(updateProgress);
}

function updateProgress(uploadInfo) {

    var progressBar = document.getElementById("progressBar");
    var progressBarContent = document.getElementById("progressBarContent");
    var progressText = document.getElementById("progressText");


    if (uploadInfo.bytesTotal > 0) {
        var percent = Math.ceil((uploadInfo.bytesUploaded / uploadInfo.bytesTotal) * 100);
        progressBarContent.style.width = parseInt(percent * 3.5) + 'px';
        progressText.innerHTML = percent + "<fmt:message key="more.upload.progress"/>";
        progressBar.style.display = "block";
        progressText.style.display = "block";
        window.setTimeout("refreshProgress()", 1000);
    } else {
        progressBar.style.display = "none";
        progressText.style.display = "none";
        window.setTimeout("refreshProgress()", 5000);
    }
}

// From Modernizr
// See: https://modernizr.com/
function isLocalStorageEnabled() {
    var mod = 'modernizr';
    try {
        localStorage.setItem(mod, mod);
        localStorage.removeItem(mod);
        return true;
    } catch(e) {
        return false;
    }
}

$(function() {
    ${model.user.uploadRole ? "refreshProgress();" : ""}
});

</script>
</head>
<body class="mainframe upload">

<section>
    <h1 class="upload"><fmt:message key="more.upload.title"/></h1>
</section>

<c:if test="${model.user.uploadRole}">

<form method="post" enctype="multipart/form-data" action="upload.view?${_csrf.parameterName}=${_csrf.token}">

    <dl>
        <dt><fmt:message key="more.upload.source"/></dt>
        <dd><input type="file" id="file" name="file"/></dd>
        <dt><fmt:message key="more.upload.target"/></dt>
        <dd><input type="text" id="dir" name="dir" value="${model.uploadDirectory}"/></dd>
        <dt></dt>
        <dd><input type="checkbox" checked name="unzip" id="unzip"/><label for="unzip"><fmt:message key="more.upload.unzip"/></label></dd>
    </dl>

    <p class="detail" id="progressText"/>
    
    <div id="progressBar">
        <div id="progressBarContent"></div>
    </div>

    <div class="submits">
        <input type="submit" value="<fmt:message key='more.upload.ok'/>"/>
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form>

</c:if>

</body></html>


