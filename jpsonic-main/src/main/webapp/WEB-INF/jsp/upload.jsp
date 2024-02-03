<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe upload">
    <section>
        <h1 class="upload"><fmt:message key="more.upload.title"/></h1>
    </section>

    <div class="responce">
        <c:forEach items="${model.uploadedFiles}" var="path">
            <fmt:message key="upload.success"><fmt:param value="${path}"/></fmt:message><br/>
        </c:forEach>
        <c:forEach items="${model.unzippedFiles}" var="path">
            <fmt:message key="upload.unzipped"><fmt:param value="${path}"/></fmt:message><br/>
        </c:forEach>
        <c:choose>
            <c:when test="${not empty model.exception}">
                <p><fmt:message key="upload.failed"><fmt:param value="${model.exception.message}"/></fmt:message></p>
            </c:when>
            <c:when test="${empty model.uploadedFiles}">
                <p><fmt:message key="upload.empty"/></p>
            </c:when>
        </c:choose>
    </div>

    <div class="submits">
        <input type="button" onClick="location.href='uploadEntry.view?'" value="<fmt:message key='common.back'/>" />
    </div>
</body>
</html>
