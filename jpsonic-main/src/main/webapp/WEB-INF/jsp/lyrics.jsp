<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <title><fmt:message key="lyrics.title"/></title>
    <script src="<c:url value='/dwr/interface/lyricsService.js'/>"></script>
    <script src="<c:url value='/dwr/engine.js'/>"></script>
    <script src="<c:url value='/dwr/util.js'/>"></script>

    <script>

        dwr.engine.setErrorHandler(null);

        function init() {
            getLyrics('${model.artist}', '${model.song}');
        }

        function getLyrics(artist, song) {
            $("wait").style.display = "inline";
            $("lyrics").style.display = "none";
            $("noLyricsFound").style.display = "none";
            $("tryLater").style.display = "none";
            lyricsService.getLyrics(artist, song, getLyricsCallback);
        }

        function getLyricsCallback(lyricsInfo) {
            dwr.util.setValue("lyricsHeader", lyricsInfo.artist + " - " + lyricsInfo.title);
            var lyrics;
            if (lyricsInfo.lyrics != null) {
                lyrics = lyricsInfo.lyrics.replace(/\n/g, "<br>");
            }
            dwr.util.setValue("lyricsText", lyrics, { escapeHtml:false });
            $("wait").style.display = "none";
            if (lyricsInfo.tryLater) {
                $("tryLater").style.display = "inline";
            } else if (lyrics != null) {
                $("lyrics").style.display = "inline";
            } else {
                $("noLyricsFound").style.display = "inline";
            }
        }

        function search() {
            getLyrics(dwr.util.getValue('artist'), dwr.util.getValue('song'));
        }
    </script>

</head>
<body class="mainframe" onload="init();">

<form action="#" onsubmit="search();return false;">
    <dl>
        <dt><fmt:message key="lyrics.artist"/></dt>
        <dd>
        	<input id="artist" type="text" size="40" value="${model.artist}" tabindex="1"/>
        	<input type="submit" value="<fmt:message key='lyrics.search'/>"  tabindex="3"/>
        </dd>
        <dt><fmt:message key="lyrics.song"/></dt>
        <dd>
        	<input id="song" type="text" size="40" value="${model.song}" tabindex="2"/>
        	<input type="button" value="<fmt:message key='common.close'/>" onclick="self.close()" tabindex="4"/>
        </dd>
    </dl>
</form>
<hr/>
<h2 id="wait"><fmt:message key="lyrics.wait"/></h2>
<h2 id="noLyricsFound"><fmt:message key="lyrics.nolyricsfound"/></h2>
<p id="tryLater"><strong><fmt:message key="lyrics.trylater"/></strong></p>

<div id="lyrics" style="display:none;">
    <h2 id="lyricsHeader" style="text-align:center;margin-bottom:1em"></h2>

    <div id="lyricsText"></div>

    <p>
        <fmt:message key="lyrics.courtesy"/>
    </p>
</div>

<hr/>
<p>
    <a href="javascript:self.close()">[<fmt:message key="common.close"/>]</a>
</p>

</body>
</html>
