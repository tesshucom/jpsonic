<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
<link rel="alternate" type="application/rss+xml" title="Jpsonic Podcast" href="podcast.view?suffix=.rss">
<script src="<c:url value='/script/jpsonic/dialogs.js'/>"></script>
<script>
<%-- deligate >> --%>
function onQueryFocus() {document.getElementById("upper").contentWindow.onQueryFocus()};
function onToggleDrawer() {document.getElementById("upper").contentWindow.onToggleDrawer()};
function onCloseDrawer() {document.getElementById("upper").contentWindow.onCloseDrawer()};
function onChangeMainLocation(location) {document.getElementById("upper").contentWindow.onChangeMainLocation(location)};
function onShowKeyboardShortcuts() {document.getElementById("upper").contentWindow.onShowKeyboardShortcuts()};
function onStartScanning() {document.getElementById("upper").contentWindow.onStartScanning();};
function callPassiveScanningStatus() {document.getElementById("upper").contentWindow.callPassiveScanningStatus();};
function onChangeCurrentSong(song) {document.getElementById("upper").contentWindow.onChangeCurrentSong(song);};
function initCurrentSongView() {document.getElementById("playQueue").contentWindow.initCurrentSongView();}
function onToggleStartStop() {document.getElementById("playQueue").contentWindow.onToggleStartStop()};
function onPrevious() {document.getElementById("playQueue").contentWindow.onPrevious()};
function onNext() {document.getElementById("playQueue").contentWindow.onNext()};
function onStarCurrent() {document.getElementById("playQueue").contentWindow.onStarCurrent()};
function onGainAdd(val) {document.getElementById("playQueue").contentWindow.onGainAdd(val)};
function onTogglePlayQueue() {document.getElementById("playQueue").contentWindow.onTogglePlayQueue();};
function onCloseQueue() {document.getElementById("playQueue").contentWindow.onCloseQueue();};
function onTryCloseDrawerBoth() {document.getElementById("upper").contentWindow.onTryCloseDrawer();document.getElementById("playQueue").contentWindow.onTryCloseQueue();};
<%-- deligate << --%>

function reloadUpper(...mainViewName) {
    const upper = document.getElementById("upper");
    const playQueue = document.getElementById("playQueue");
    if (mainViewName.length == 1) {
        document.getElementById('upper').contentWindow.location.replace("top.view?mainView=" + mainViewName[0]);
    } else {
        document.getElementById('upper').contentWindow.location.reload(true);
    }
}

function reloadPlayQueue() {
    document.getElementById('playQueue').contentWindow.location.reload(true);
}

function setDrawerOpened(isDrawerOpened) {
	document.getElementById('isDrawerOpened').checked = isDrawerOpened;
}

function setQueueOpened(isQueueOpened) {
	document.getElementById('isQueueOpened').checked = isQueueOpened;
}

function setQueueExpand(isQueueExpand) {
    document.getElementById('isQueueExpand').checked = isQueueExpand;
}

</script>
</head>
<body class="index">
	<input type="checkbox" id="isDrawerOpened" value="1" autofocus="false" checked />
	<iframe name="upper" id="upper" src="top.view?"></iframe>

	<input type="checkbox" id="isQueueOpened" value="1" autofocus="false" tabindex="-1"/>
    <input type="checkbox" id="isQueueExpand" value="1" autofocus="false" tabindex="-1"/>
	<iframe name="playQueue" id="playQueue" src="playQueue.view?"></iframe>

	<%@ include file="dialogs.jsp" %>
</body>
</html>
