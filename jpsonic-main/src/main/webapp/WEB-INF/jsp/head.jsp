<%@ include file="include.jsp" %>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:set var="styleSheet"><spring:theme code="styleSheet"/></c:set>
<link rel="apple-touch-icon" sizes="180x180" href="<c:url value='/icons/apple-touch-icon.png'/>"/>
<link rel="icon" type="image/png" sizes="32x32" href="<c:url value='/icons/favicon-32x32.png'/>"/>
<link rel="icon" type="image/png" sizes="16x16" href="<c:url value='/icons/favicon-16x16.png'/>"/>
<link rel="manifest" href="<c:url value='/icons/site.webmanifest'/>"/>
<link rel="mask-icon" href="<c:url value='/icons/safari-pinned-tab.svg'/>" color="#2f7bd9"/>
<meta name="msapplication-TileColor" content="#ffffff">
<meta name="theme-color" content="#ffffff">
<meta name="description" content="Jpsonic: A free, web-based media streamer, providing ubiquitous access to your music.">
<meta name="viewport" content="width=device-width, initial-scale=1">
<%-- Included before airsonic stylesheet to allow overriding --%>

<%
    //TODO #696
	String fontFace = "";
	String fontFamily = "";
	String fontSizeAttr = "font-size: 14px";
	String fsn =(String)request.getAttribute("filter.fontSchemeName");
    if(fsn == null || fsn.equals("DEFAULT")) {
        fontFamily = "-apple-system, blinkMacSystemFont, \"Helvetica Neue\", \"Segoe UI\", \"Noto Sans JP\", YuGothicM, YuGothic, Meiryo, sans-serif";
    } else if(fsn.equals("JP_EMBED")) {
        fontFace = "@font-face {font-family: \"Kazesawa-Regular\";src: url(\"../jpsonic/fonts/kazesawa/Kazesawa-Regular.woff\") format(\"woff\"), url(\"../jpsonic/fonts/kazesawa/Kazesawa-Regular.ttf\") format(\"truetype\");}";
        fontFamily = "\"Kazesawa-Regular\", -apple-system, blinkMacSystemFont, \"Helvetica Neue\", \"Segoe UI\", \"Noto Sans JP\", YuGothicM, YuGothic, Meiryo, sans-serif";
        fontSizeAttr = "font-size: 15px";
    }
%>
<link type="text/css" rel="stylesheet" href="<c:url value='/script/mediaelement/mediaelementplayer.min.css'/>">
<style>
<%=fontFace%>
html {
  font-family:<%=fontFamily%>;
  <%=fontSizeAttr%>;
}
</style>
<link rel="stylesheet" href="<c:url value='/${styleSheet}'/>" type="text/css">
<title>Jpsonic</title>

<script id="preferencesConfig" type="application/x-configuration">
  {
    "keyboardShortcutsEnabled": ${model.keyboardShortcutsEnabled ? 'true' : 'false'}
  }
</script>
<script defer src="<c:url value='/script/mousetrap.min.js'/>"></script>
<script defer src="<c:url value='/script/keyboard_shortcuts.js'/>"></script>
