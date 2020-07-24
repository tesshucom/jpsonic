<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <link rel="alternate" type="application/rss+xml" title="Jpsonic Podcast" href="podcast.view?suffix=.rss">
</head>
<body>

<iframe name="upper" src="top.view?" class="bgcolor2" frameborder="0" class="bgcolor2" style="
	position:absolute;
	z-index:0;
	width:calc(100vw - 40px);
	height: calc(100vh - 40px);" scrolling="no" frameborder="no"></iframe>

<iframe name="main" src="nowPlaying.view?" class="bgcolor1" style="
	position:absolute;
	z-index:1;
	top:100px;
	left:230px;
	width:calc(100vw - ${model.showRight ? 235 : 0}px - 230px);
	height: calc(100vh - 130px);" frameborder="no"></iframe>

<iframe name="right" src="right.view?" class="bgcolor1" style="
  	position:relative;
	z-index:2;
	top:100px;
	right:0;
	width:${model.showRight ? 235 : 0}px;
	height: calc(100vh - 130px);;" frameborder="no"></iframe>

<iframe id="playQueue" name="playQueue" src="playQueue.view?" class="noscrollbar" style="
 	position:fixed;
	z-index:3;
 	left:calc(${model.showSideBar ? "230" : "0"}px + 16px);
 	bottom:0;
	width: calc(100vw - ${model.showSideBar ? "230" : "0"}px - 20px);
	height: 40px;" frameborder="no"></iframe>

</body>

</html>
