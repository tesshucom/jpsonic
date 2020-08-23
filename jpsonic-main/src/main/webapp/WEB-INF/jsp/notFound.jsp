<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>

<body class="mainframe">

<section>
    <h1 class="error"><fmt:message key="notFound.title"/></h1>
</section>

<fmt:message key="notFound.text"/>

<div><a href="javascript:top.location.reload(true)"><fmt:message key="notFound.reload"/></a></div>
<div><a href="musicFolderSettings.view"><fmt:message key="notFound.scan"/></a></div>

</body>
</html>