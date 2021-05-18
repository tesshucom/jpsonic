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
<link type="text/css" rel="stylesheet" href="<c:url value='/script/mediaelement/mediaelementplayer.min.css'/>">
<style>
<%= (String) request.getAttribute("viewhint.fontFace") %>
html {
  font-family: <%= (String) request.getAttribute("viewhint.fontFamily") %>;
  font-size: <%= (Integer) request.getAttribute("viewhint.fontSize") %>px;
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
