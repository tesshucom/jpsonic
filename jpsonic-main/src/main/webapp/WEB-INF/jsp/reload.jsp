<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp"%>
</head>
<body>
<script>
	<c:forEach items="${model.reloadFrames}" var="reloadFrame">
		<c:if test="${reloadFrame.frame eq 'playQueue'}">window.top.reloadPlayQueue();</c:if>
		<c:if test="${reloadFrame.frame eq 'upper'}">window.top.reloadUpper("more.view");</c:if>
	</c:forEach>
</script>
</body>
</html>
