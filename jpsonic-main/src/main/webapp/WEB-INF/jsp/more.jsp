<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<style type="text/css">
    #randomPlayQueue td { padding: 0 5px; }
</style>
<script src="<c:url value='/dwr/interface/transferService.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>

<script>

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


// Load previously used shuffle parameters
function loadShuffleForm() {
    if (!isLocalStorageEnabled()) return;
    var form = document.getElementById("randomPlayQueue");
    try {
        var data = JSON.parse(localStorage.getItem("randomPlayQueue"));
    } catch(e) { return; }
    if (data == null ) { return; }
    var elements = form.getElementsByTagName("input");
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].type == "hidden") continue;
        if (elements[i].type == "submit") continue;
        if (data[elements[i].name]) elements[i].value = data[elements[i].name];
    }
    elements = form.getElementsByTagName("select");
    for (var i = 0; i < elements.length; i++) {
        var element = elements[i];
        var value = data[element.name];
        if (value) {
            for (var j = 0; j < element.options.length; j++) {
                if (element.options[j].value == value) {
                    element.value = value;
                    break;
                }
            }
        }
    }
}

// Save shuffle parameters
function saveShuffleForm() {
    if (!isLocalStorageEnabled()) return;
    var form = document.getElementById("randomPlayQueue");
    var data = {};
    var elements = [];
    elements = form.getElementsByTagName("input");
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].type == "hidden") continue;
        if (elements[i].type == "submit") continue;
        data[elements[i].name] = elements[i].value;
    }
    elements = form.getElementsByTagName("select");
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].type == "hidden") continue;
        if (elements[i].type == "submit") continue;
        data[elements[i].name] = elements[i].value;
    }
    localStorage.setItem("randomPlayQueue", JSON.stringify(data));
}

$(function() {
    $("#randomPlayQueue").on("submit", saveShuffleForm);
    loadShuffleForm();
});
</script>

</head>
<body class="mainframe more">

<c:import url="playlistsHeader.jsp">
    <c:param name="cat" value="more"/>
</c:import>

<c:if test="${model.user.streamRole}">

    <form id="randomPlayQueue" method="post" action="randomPlayQueue.view?">
        <sec:csrfInput />
        <dl>
            <dt><fmt:message key="more.random.text"/></dt>
            <dd>
                <select name="size">
                    <option value="10"><fmt:message key="more.random.songs"><fmt:param value="10"/></fmt:message></option>
                    <option value="20" selected><fmt:message key="more.random.songs"><fmt:param value="20"/></fmt:message></option>
                    <option value="30"><fmt:message key="more.random.songs"><fmt:param value="30"/></fmt:message></option>
                    <option value="40"><fmt:message key="more.random.songs"><fmt:param value="40"/></fmt:message></option>
                    <option value="50"><fmt:message key="more.random.songs"><fmt:param value="50"/></fmt:message></option>
                    <option value="100"><fmt:message key="more.random.songs"><fmt:param value="100"/></fmt:message></option>
                </select>
            </dd>
            <dt><fmt:message key="more.random.year"/></dt>
            <dd>
                <select name="year">
                    <option value="any"><fmt:message key="more.random.anyyear"/></option>

                    <c:forEach begin="0" end="${model.currentYear - 2010}" var="yearOffset">
                        <c:set var="year" value="${model.currentYear - yearOffset}"/>
                        <option value="${year} ${year}">${year}</option>
                    </c:forEach>

                    <option value="2010 2015">2010 &ndash; 2015</option>
                    <option value="2005 2010">2005 &ndash; 2010</option>
                    <option value="2000 2005">2000 &ndash; 2005</option>
                    <option value="1990 2000">1990 &ndash; 2000</option>
                    <option value="1980 1990">1980 &ndash; 1990</option>
                    <option value="1970 1980">1970 &ndash; 1980</option>
                    <option value="1960 1970">1960 &ndash; 1970</option>
                    <option value="1950 1960">1950 &ndash; 1960</option>
                    <option value="0 1949">&lt; 1950</option>
                </select>
            </dd>
            <dt><fmt:message key="more.random.genre"/></dt>
            <dd>
                <select name="genre">
                    <option value="any"><fmt:message key="more.random.anygenre"/></option>
                    <c:forEach items="${model.genres}" var="genre">
                        <option value="${fn:escapeXml(genre.name)}"><str:truncateNicely upper="20">${fn:escapeXml(genre.name)} (${genre.songCount})</str:truncateNicely></option>
                    </c:forEach>
                </select>
            </dd>
            <dt><fmt:message key="more.random.albumrating"/></dt>
            <dd>
                <select name="albumRatingComp">
                    <option value="lt">&lt;</option>
                    <option value="le">&le;</option>
                    <option value="eq">=</option>
                    <option value="ge" selected="selected">&ge;</option>
                    <option value="gt">&gt;</option>
                </select>
                <select name="albumRatingValue">
                    <option value="" selected="selected"><fmt:message key="more.random.any"/></option>
                    <option value="0">0 <fmt:message key="more.random.stars"/></option>
                    <option value="1">1 <fmt:message key="more.random.star"/></option>
                    <option value="2">2 <fmt:message key="more.random.stars"/></option>
                    <option value="3">3 <fmt:message key="more.random.stars"/></option>
                    <option value="4">4 <fmt:message key="more.random.stars"/></option>
                    <option value="5">5 <fmt:message key="more.random.stars"/></option>
                </select>
            </dd>
            <dt><fmt:message key="more.random.songrating"/></dt>
            <dd>
                <select name="songRating">
                    <option value="any" selected="selected"><fmt:message key="more.random.any"/></option>
                    <option value="starred"><fmt:message key="more.random.starred"/></option>
                    <option value="unstarred"><fmt:message key="more.random.unstarred"/></option>
                </select>
            </dd>
            <dt><fmt:message key="more.random.lastplayed"/></dt>
            <dd>
                <select name="lastPlayedComp">
                    <option value="lt" selected="selected">&lt;</option>
                    <option value="gt">&gt;</option>
                </select>
                <select name="lastPlayedValue">
                    <option value="any" selected="selected"><fmt:message key="more.random.any"/></option>
                    <option value="1day"><fmt:message key="more.random.1day"/></option>
                    <option value="1week"><fmt:message key="more.random.1week"/></option>
                    <option value="1month"><fmt:message key="more.random.1month"/></option>
                    <option value="3months"><fmt:message key="more.random.3months"/></option>
                    <option value="6months"><fmt:message key="more.random.6months"/></option>
                    <option value="1year"><fmt:message key="more.random.1year"/></option>
                </select>
            </dd>
            <dt><fmt:message key="more.random.folder"/></dt>
            <dd>
                <select name="musicFolderId">
                    <option value="-1"><fmt:message key="more.random.anyfolder"/></option>
                    <c:forEach items="${model.musicFolders}" var="musicFolder">
                        <option value="${musicFolder.id}">${musicFolder.name}</option>
                    </c:forEach>
                </select>
            </dd>
            <dt><fmt:message key="more.random.playcount"/></dt>
            <dd>
                <select name="playCountComp">
                    <option value="lt" selected="selected">&lt;</option>
                    <option value="gt">&gt;</option>
                </select>
                <input type="number" name="playCountValue"/>
            </dd>
            <dt><fmt:message key="more.random.format"/></dt>
            <dd>
                <select name="format">
                    <option value="any" selected="selected"><fmt:message key="more.random.any"/></option>
                    <option value="flac">FLAC</option>
                    <option value="mp3">MP3</option>
                </select>
            </dd>
        </dl>
        <div class="submits">
            <input type="submit" name="addToPlaylist" value="<fmt:message key='more.random.add'/>">
            <c:if test="${model.useRadio eq true}">
                  <input type="submit" name="autoRandom" value="<fmt:message key='more.random.radio'/>">
            </c:if>
        </div>
    </form>
</c:if>

</body>
</html>

