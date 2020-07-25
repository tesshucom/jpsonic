<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <link rel="alternate" type="application/rss+xml" title="Jpsonic Podcast" href="podcast.view?suffix=.rss">
</head>
<script>

function onToggleStartStop() {};
function onPrevious() {};
function onNext() {};
function onStarCurrent() {};
function onGainAdd(val) {};
function onGainAdd(val) {};
function onTogglePlayQueue() {};
function onQueryFocus() {};
function onToggleDrawer() {};
function onChangeMainLocation(location) {};
function onShowKeyboardShortcuts() {};

$(document).ready(function(){
	$('#main').css('width','calc(100vw - ${model.showLeft ? 240 : 0}px - ${model.showRight ? 240 : 0}px)');
	$('iframe[name*="right"]').css('width','${model.showRight ? 240 : 0}px');
	$('#playQueue').css('width','calc(100vw - ${model.showLeft ? "240" : "0"}px - ${model.showRight ? 240 : 0}px)');

	onToggleStartStop = () => {document.getElementById('playQueue').contentWindow.onToggleStartStop();};
	onPrevious = () => {document.getElementById('playQueue').contentWindow.onPrevious();};
	onNext = () => {document.getElementById('playQueue').contentWindow.onNext();};
	onStarCurrent = () => {document.getElementById('playQueue').contentWindow.onStarCurrent();};
	onGainAdd = (val) => {document.getElementById('playQueue').contentWindow.onGainAdd(val);};
	onGainAdd = (val) => {document.getElementById('playQueue').contentWindow.onGainAdd(val);};
	onTogglePlayQueue = () => {document.getElementById('playQueue').contentWindow.onTogglePlayQueue();};
	onQueryFocus = () => {document.getElementById('upper').contentWindow.onQueryFocus();};
	onToggleDrawer = () => {document.getElementById('upper').contentWindow.onToggleDrawer();};
	onChangeMainLocation = (location) => {document.getElementById('upper').contentWindow.onChangeMainLocation(location);};
	onShowKeyboardShortcuts = () => {document.getElementById('upper').contentWindow.onShowKeyboardShortcuts();};

});
</script>
<body style="margin:0;height:100vh;overflow:hidden">

	<iframe name="upper" src="top.view?" scrolling="no" frameborder="no" id="upper"></iframe>
	<iframe name="right" src="right.view?" frameborder="no"></iframe>
	<iframe name="playQueue" id="playQueue" src="playQueue.view?" frameborder="no"></iframe>

</body>
</html>
