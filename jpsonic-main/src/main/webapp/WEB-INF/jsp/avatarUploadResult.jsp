<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe">

<section>
    <h1 class="upload"><fmt:message key="avataruploadresult.title"/></h1>
</section>

<c:choose>
    <c:when test="${empty model.error}">
        <p>
            <fmt:message key="avataruploadresult.success"><fmt:param value="${fn:escapeXml(model.avatar.name)}"/></fmt:message>
            <sub:url value="avatar.view" var="avatarUrl">
                <sub:param name="username" value="${model.username}"/>
                <sub:param name="forceCustom" value="true"/>
            </sub:url>
            <img src="${avatarUrl}" alt="${model.avatar.name}" width="${model.avatar.width}" height="${model.avatar.height}"/>
        </p>
    </c:when>
    <c:otherwise>
        <p><strong><fmt:message key="avataruploadresult.failure"/></strong></p>
    </c:otherwise>
</c:choose>

<div><a href="personalSettings.view?"><fmt:message key="common.back"/></a></div>

</body>
</html>
