<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <link rel="alternate" type="application/rss+xml" title="Jpsonic Podcast" href="podcast.view?suffix=.rss">
</head>
<body>

<div style="
	display: grid;
	grid-template-columns: ${model.showSideBar ? "230" : "0"}px 1fr ${model.showRight ? 235 : 0}px;
	grid-template-rows: 80px 1fr;
	height: calc(100vh - 80px);">

    <iframe name="upper" src="top.view?" class="bgcolor2" frameborder="0" class="bgcolor2" style="
		grid-column-start: 1;
		grid-column-end: 4;
		grid-row-start: 1;
		grid-row-end: 2;
		width:100%;
		height: 80px;" scrolling="no" frameborder="no"></iframe>

	<iframe name="left" src="left.view?" class="bgcolor2" style="
		grid-column-start: 1;
		grid-column-end: 2;
		grid-row-start: 2;
		grid-row-end: 3;
		width:${model.showSideBar ? "230" : "0"}px;
		height: calc(100vh - 100px);" frameborder="no"></iframe>

    <iframe name="main" src="nowPlaying.view?" class="bgcolor1" style="
		grid-column-start: 2;
		grid-column-end: 3;
		grid-row-start: 2;
		grid-row-end: 3;
		width:100%;
		height: calc(100vh - 130px);" frameborder="no"></iframe>

    <iframe name="right" src="right.view?" class="bgcolor1" style="
		grid-column-start: 3;
		grid-column-end: 4;
		grid-row-start: 2;
		grid-row-end: 3;
		width:${model.showRight ? 235 : 0}px;
		height: calc(100vh - 130px);;" frameborder="no"></iframe>
</div>

<iframe id="playQueue" name="playQueue" src="playQueue.view?" class="noscrollbar" style="
 	position:fixed;
 	left:calc(${model.showSideBar ? "230" : "0"}px + 16px);
 	bottom:0;
	width: calc(100vw - ${model.showSideBar ? "230" : "0"}px - 20px);
	height: 40px;" frameborder="no"></iframe>

</body>

</html>
