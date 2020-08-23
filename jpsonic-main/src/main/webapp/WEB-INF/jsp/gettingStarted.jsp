<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script>
        function hideGettingStarted() {
            alert("<fmt:message key='gettingStarted.hidealert'/>");
            location.href = "gettingStarted.view?hide";
        }
    </script>
</head>
<body class="mainframe gettingStarted">

<section>
    <h1 class="home"><fmt:message key="gettingStarted.title"/></h1>
</section>

<fmt:message key="gettingStarted.text"/>

<dl>
	<dt><a href="userSettings.view?userIndex=0"><fmt:message key="gettingStarted.step1.title"/></a></dt>
	<dd><fmt:message key="gettingStarted.step1.text"/></dd>
	<dt><a href="musicFolderSettings.view"><fmt:message key="gettingStarted.step2.title"/></a></dt>
	<dd><fmt:message key="gettingStarted.step2.text"/></dd>
	<dt><fmt:message key="gettingStarted.step3.title"/></dt>
	<dd><fmt:message key="gettingStarted.step3.text"/></dd>
</dl>

<c:if test="${model.runningAsRoot}">
    <h2><strong><fmt:message key="gettingStarted.root"/></strong></h2>
</c:if>

<p><a href="javascript:hideGettingStarted()"><fmt:message key="gettingStarted.hide"/></a></p>

</body></html>
