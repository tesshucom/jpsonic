<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe">

<section>
    <h1 class="share"><fmt:message key="share.title"/></h1>
</section>

<fmt:message key="share.warning"/>
<p>
    <span class="icon facebook"></span>&nbsp;<a href="https://www.facebook.com/sharer.php?u=${model.playUrl}" target="_blank" rel="noopener noreferrer"><fmt:message key="share.facebook"/></a>
</p>

<p>
    <span class="icon twitter"></span>&nbsp;<a href="https://twitter.com/?status=Listening to ${model.playUrl}" target="_blank" rel="noopener noreferrer"><fmt:message key="share.twitter"/></a>
</p>
<p>
    <fmt:message key="share.link">
        <fmt:param>${model.playUrl}</fmt:param>
    </fmt:message>
</p>

<div>
    <c:if test="${not empty model.dir}">
        <sub:url value="main.view" var="backUrl"><sub:param name="path" value="${model.dir.path}"/></sub:url>
        <div><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
    </c:if>
    <c:if test="${model.user.settingsRole}">
        <div><a href="shareSettings.view"><fmt:message key="share.manage"/></a></div>
    </c:if>
</div>
</body>
</html>
