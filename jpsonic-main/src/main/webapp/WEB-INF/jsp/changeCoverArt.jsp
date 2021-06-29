<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/dwr/interface/coverArtService.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script>
dwr.engine.setErrorHandler(function() {
    $("#wait").hide();
    dwr.util.setValue("errorDetails", "Sorry, an error occurred while searching for cover art.");
    $("#errorDetails").show();
});

function setImage(imageUrl) {
    $("#wait").show();
    $("#result").hide();
    $("#success").hide();
    $("#error").hide();
    $("#errorDetails").hide();
    $("#noImagesFound").hide();
    coverArtService.saveCoverArtImage(${model.id}, imageUrl, setImageComplete);
}

function setImageComplete(errorDetails) {
    $("#wait").hide();
    if (errorDetails != null) {
        dwr.util.setValue("errorDetails", errorDetails, { escapeHtml:false });
        $("#error").show();
        $("#errorDetails").show();
    } else {
        $("#success").show();
    }
}

function searchComplete(searchResults) {
    $("#wait").hide();

    if (searchResults.length > 0) {
        var images = $("#images");
        images.empty();

        for (var i = 0; i < searchResults.length; i++) {
            var result = searchResults[i];
            var node = $("#template").clone();

            node.find(".search-result-link").attr("href", "javascript:setImage('" + result.imageUrl + "');");
            node.find(".search-result-image").attr("src", result.imageUrl);
            node.find(".caption1").text(result.artist);
            node.find(".caption2").text(result.album);
            node.css('display','inherit');
            node.show();
            node.appendTo(images);
        }

        $("#result").show();
    } else {
        $("#noImagesFound").show();
    }
}

function search() {
    $("#wait").show();
    $("#result").hide();
    $("#success").hide();
    $("#error").hide();
    $("#errorDetails").hide();
    $("#noImagesFound").hide();

    var artist = dwr.util.getValue("artist");
    var album = dwr.util.getValue("album");
    coverArtService.searchCoverArt(artist, album, searchComplete);
}
</script>
</head>
<body class="mainframe changeCoverArt" onload="search()">

<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
	<h1 class="image"><fmt:message key="changecoverart.title"/></h1>
</section>

<div class="actions">
	<ul class="controls">
		<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.id}"/></sub:url>
		<li><a href="${backUrl}" title="<fmt:message key='main.up'/>" class="control up"><fmt:message key="main.up"/></a></li>
	</ul>
</div>

<details>
	<summary><fmt:message key='changecoverart.selectimg'/></summary>
	<form action="javascript:search()">
	    <sec:csrfInput />
		<dl>
			<dt><fmt:message key='changecoverart.artist'/></dt>
			<dd><input id="artist" name="artist" placeholder="<fmt:message key='changecoverart.artist'/>" type="text" value="${model.artist}" onclick="select()"/></dd>
			<dt><fmt:message key='changecoverart.album'/></dt>
	        <dd><input id="album" name="album" placeholder="<fmt:message key='changecoverart.album'/>" type="text" value="${model.album}" onclick="select()"/></dd>
	    </dl>
	    <div class="submits">
            <input type="submit" value="<fmt:message key='changecoverart.search'/>">
        </div>
	</form>

</details>

<p id="wait"><fmt:message key="changecoverart.wait"/></p>
<p id="noImagesFound"><fmt:message key="changecoverart.noimagesfound"/></p>
<p id="success"><fmt:message key="changecoverart.success"/></p>
<p id="error"><fmt:message key="changecoverart.error"/></p>
<strong><div id="errorDetails"></div></strong>
<div id="result">
    <div id="images" class="coverart-container"></div>
</div>

<details>
	<summary><fmt:message key='changecoverart.directdesignation'/></summary>
	<form action="javascript:setImage(dwr.util.getValue('url'))">
	    <sec:csrfInput />
	    <dl class="single">
	    	<dt><label for="url"><fmt:message key="changecoverart.address"/></label></dt>
	    	<dd><input type="text" name="url" id="url" value="http://" onclick="select()"/></dd>
	    </dl>
	    <div class="submits">
            <input type="submit" value="<fmt:message key='common.ok'/>">
        </div>
	</form>
</details>

<div id="template" class="coverart">
    <div>
        <a class="search-result-link"><img alt="Search result" class="search-result-image"></a>
        <div class="caption1"></div>
        <div class="caption2"></div>
    </div>
</div>

</body></html>

