<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe settings importPlaylist">

<c:import url="playlistsHeader.jsp">
    <c:param name="cat" value="import"/>
</c:import>

<c:if test="${not empty model.playlist}">
    <p>
        <fmt:message key="importPlaylist.success"><fmt:param value="${model.playlist.name}"/></fmt:message>
        <script>
            top.upper.document.getElementById("main").src = "playlist.view?id=${model.playlist.id}";
        </script>
    </p>
</c:if>

<c:if test="${not empty model.error}">
    <p><strong><fmt:message key="importPlaylist.error"><fmt:param value="${model.error}"/></fmt:message></strong></p>
</c:if>

<form method="post" enctype="multipart/form-data" action="importPlaylist.view?${_csrf.parameterName}=${_csrf.token}">
	<dl class="single">
		<dt><fmt:message key="importPlaylist.text"/></dt>
		<dd><input type="file" id="file" name="file"/></dd>
	</dl>
    <div class="submits">
        <input type="submit" value="<fmt:message key='common.ok'/>">
    </div>
</form>

</body></html>
