<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<script src="<c:url value='/dwr/interface/tagService.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script>
var index = 0;
var fileCount = ${fn:length(model.songs)};
function setArtist() {
    var artist = dwr.util.getValue("artistAll");
    for (var i = 0; i < fileCount; i++) {
        dwr.util.setValue("artist" + i, artist);
    }
}
function setAlbum() {
    var album = dwr.util.getValue("albumAll");
    for (var i = 0; i < fileCount; i++) {
        dwr.util.setValue("album" + i, album);
    }
}
function setYear() {
    var year = dwr.util.getValue("yearAll");
    for (var i = 0; i < fileCount; i++) {
        dwr.util.setValue("year" + i, year);
    }
}
function setGenre() {
    var genre = dwr.util.getValue("genreAll");
    for (var i = 0; i < fileCount; i++) {
        dwr.util.setValue("genre" + i, genre);
    }
}
function suggestTitle() {
    for (var i = 0; i < fileCount; i++) {
        var title = dwr.util.getValue("suggestedTitle" + i);
        dwr.util.setValue("title" + i, title);
    }
}
function resetTitle() {
    for (var i = 0; i < fileCount; i++) {
        var title = dwr.util.getValue("originalTitle" + i);
        dwr.util.setValue("title" + i, title);
    }
}
function suggestTrack() {
    for (var i = 0; i < fileCount; i++) {
        var track = dwr.util.getValue("suggestedTrack" + i);
        dwr.util.setValue("track" + i, track);
    }
}
function resetTrack() {
    for (var i = 0; i < fileCount; i++) {
        var track = dwr.util.getValue("originalTrack" + i);
        dwr.util.setValue("track" + i, track);
    }
}
function updateTags() {
    document.getElementById("save").disabled = true;
    index = 0;
    dwr.util.setValue("errors", "");
    for (var i = 0; i < fileCount; i++) {
        dwr.util.setValue("status" + i, "");
    }
    updateNextTag();
}
function updateNextTag() {
    var id = dwr.util.getValue("id" + index);
    var artist = dwr.util.getValue("artist" + index);
    var track = dwr.util.getValue("track" + index);
    var album = dwr.util.getValue("album" + index);
    var title = dwr.util.getValue("title" + index);
    var year = dwr.util.getValue("year" + index);
    var genre = dwr.util.getValue("genre" + index);
    dwr.util.setValue("status" + index, "<fmt:message key="edittags.working"/>");
    tagService.setTags(id, track, artist, album, title, year, genre, setTagsCallback);
}
function setTagsCallback(result) {
    var message;
    if (result == "SKIPPED") {
        message = "<fmt:message key="edittags.skipped"/>";
    } else if (result == "UPDATED") {
        message = "<b><fmt:message key="edittags.updated"/></b>";
    } else {
        message = "<strong><div><fmt:message key="edittags.error"/></div></strong>";
        var errors = dwr.util.getValue("errors");
        errors += "<br>" + result + "<br>";
        dwr.util.setValue("errors", errors, { escapeHtml:false });
    }
    dwr.util.setValue("status" + index, message, { escapeHtml:false });
    index++;
    if (index < fileCount) {
        updateNextTag();
    } else {
        document.getElementById("save").disabled = false;
    }
}
</script>
</head>

<body class="mainframe editTags">

<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
	<h1 class="image"><fmt:message key="edittags.title"/></h1>
</section>

<table class="tabular tags">
	<thead>
	    <tr>
	        <th><fmt:message key="edittags.file"/></th>
	        <th><fmt:message key="edittags.track"/></th>
	        <th><fmt:message key="edittags.songtitle"/></th>
	        <th><fmt:message key="edittags.artist"/></th>
	        <th><fmt:message key="edittags.album"/></th>
	        <th><fmt:message key="edittags.year"/></th>
	        <th><fmt:message key="edittags.genre"/></th>
	        <th><fmt:message key="edittags.status"/></th>
	    </tr>
	    <tr>
	        <th/>
	        <th>
	        	<a href="javascript:suggestTrack()" title="<fmt:message key='edittags.suggest'/>" class="control suggest"><fmt:message key="edittags.suggest"/></a>
	        	<a href="javascript:resetTrack()" title="<fmt:message key='edittags.reset'/>" class="control undo"><fmt:message key="edittags.reset"/></a>
	        </th>
	        <th>
	        	<a href="javascript:suggestTitle()" title="<fmt:message key='edittags.suggest'/>" class="control suggest"><fmt:message key="edittags.suggest"/></a>
	        	<a href="javascript:resetTitle()" title="<fmt:message key='edittags.reset'/>" class="control undo"><fmt:message key="edittags.reset"/></a>
	        </th>
	        <th>
	        	<input type="text" name="artistAll" size="15" onkeypress="dwr.util.onReturn(event, setArtist)" value="${fn:escapeXml(model.defaultArtist)}"/>
	        	<a href="javascript:setArtist()" title="<fmt:message key='edittags.set'/>" class="control apply-all"><fmt:message key="edittags.set"/></a>
	        <th>
	        	<input type="text" name="albumAll" size="15" onkeypress="dwr.util.onReturn(event, setAlbum)" value="${fn:escapeXml(model.defaultAlbum)}"/>
	        	<a href="javascript:setAlbum()" title="<fmt:message key='edittags.set'/>" class="control apply-all"><fmt:message key="edittags.set"/></a>
			</th>
	        <th>
	        	<input type="text" name="yearAll" size="5" onkeypress="dwr.util.onReturn(event, setYear)" value="${model.defaultYear}"/>
	        	<a href="javascript:setYear()" title="<fmt:message key='edittags.set'/>" class="control apply-all"><fmt:message key="edittags.set"/></a>
			</th>
	        <th>
	            <select name="genreAll">
	                <option value=""/>
	                <c:forEach items="${model.allGenres}" var="genre">
	                    <option ${genre eq model.defaultGenre ? "selected" : ""} value="${fn:escapeXml(genre)}">${fn:escapeXml(genre)}</option>
	                </c:forEach>
	            </select>
	        	<a href="javascript:setGenre()" title="<fmt:message key='edittags.set'/>" class="control apply-all"><fmt:message key="edittags.set"/></a>
	        </th>
	        <th/>
	    </tr>
	</thead>
	<tbody>
	    <c:forEach items="${model.songs}" var="song" varStatus="loopStatus">
	        <tr>
                <str:truncateNicely lower="25" upper="25" var="fileName">${song.fileName}</str:truncateNicely>
	            <td title="${fn:escapeXml(song.fileName)}">${fn:escapeXml(fileName)}</td>
	           
	            <input type="hidden" name="id${loopStatus.count - 1}" value="${song.id}"/>
	            <input type="hidden" name="suggestedTitle${loopStatus.count - 1}" value="${fn:escapeXml(song.suggestedTitle)}"/>
	            <input type="hidden" name="originalTitle${loopStatus.count - 1}" value="${fn:escapeXml(song.title)}"/>
	            <input type="hidden" name="suggestedTrack${loopStatus.count - 1}" value="${song.suggestedTrack}"/>
	            <input type="hidden" name="originalTrack${loopStatus.count - 1}" value="${song.track}"/>

	            <td><input type="text" size="5" name="track${loopStatus.count - 1}" value="${song.track}"/></td>
	            <td><input type="text" size="30" name="title${loopStatus.count - 1}" value="${fn:escapeXml(song.title)}"/></td>
	            <td><input type="text" size="22" name="artist${loopStatus.count - 1}" value="${fn:escapeXml(song.artist)}"/></td>
	            <td><input type="text" size="22" name="album${loopStatus.count - 1}" value="${fn:escapeXml(song.album)}"/></td>
	            <td><input type="text" size="10" name="year${loopStatus.count - 1}" value="${song.year}"/></td>
	            <td><input type="text" size="26" name="genre${loopStatus.count - 1}" value="${fn:escapeXml(song.genre)}" /></td>
	            <td><div id="status${loopStatus.count - 1}"/></td>
	        </tr>
	    </c:forEach>
	</tbody>
</table>

    <div class="submits">
        <input type="submit"  id="save" value="<fmt:message key='common.save'/>" onclick="updateTags()">
    </div>
    <strong><div id="errors"/><strong>

</body></html>