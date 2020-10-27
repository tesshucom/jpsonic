<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>

<body class="mainframe">

<section>
    <h1 class="error"><fmt:message key="accessDenied.title"/></h1>
</section>

<p>
    <fmt:message key="accessDenied.text"/>
</p>

<div><a href="javascript:history.go(-1)"><fmt:message key="common.back"/></a></div>

</body>
</html>