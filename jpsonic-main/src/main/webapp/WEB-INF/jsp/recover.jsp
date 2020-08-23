<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
</head>
<body class="mainframe recover">

<section>
    <h1 class="key"><fmt:message key="recover.title"/></h1>
</section>

<form action="recover.view" method="POST">
    <sec:csrfInput />

        <dl class="single">
            <dt><fmt:message key="recover.text"/></dt>
			<dd>
	            <c:if test="${empty model.sentTo}">
	                <input type="text" id="usernameOrEmail" autofocus name="usernameOrEmail" style="width:18em;margin-right: 1em">
	                <input name="submit" type="submit" value="<fmt:message key='recover.send'/>">
	            </c:if>
	            <c:if test="${not empty model.recaptchaSiteKey and empty model.sentTo}">
					<script src="https://www.google.com/recaptcha/api.js" async defer></script>
					<div class="g-recaptcha" data-sitekey="${model.recaptchaSiteKey}"></div>
	            </c:if>
			</dd>
		</dl>

        <c:if test="${not empty model.sentTo}">
            <p><fmt:message key="recover.success"><fmt:param value="${model.sentTo}"/></fmt:message></p>
        </c:if>
        <c:if test="${not empty model.error}">
            <strong><fmt:message key="${model.error}"/></strong>
        </c:if>

    <div class="submits">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>


</form>
</body>
</html>
