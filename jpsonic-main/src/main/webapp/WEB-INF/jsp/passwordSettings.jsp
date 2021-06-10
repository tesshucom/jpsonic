<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.passwordsettingscommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe settings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="password"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="restricted" value="true"/>
</c:import>

<form:form method="post" action="passwordSettings.view" modelAttribute="command">
    <details open>
        <fmt:message key="passwordsettings.title" var="title"><fmt:param>${command.username}</fmt:param></fmt:message>
        <summary class="legacy">${fn:escapeXml(title)}</summary>
        <c:choose>
            <c:when test="${command.ldapAuthenticated}">
                <p><fmt:message key="usersettings.passwordnotsupportedforldap"/></p>
            </c:when>
            <c:otherwise>
                <form:hidden path="username"/>
                <dl>
                    <dt><fmt:message key="usersettings.newpassword"/></dt>
                    <dd><form:password path="password"/><strong><form:errors path="password"/></strong></dd>
                    <dt><fmt:message key="usersettings.confirmpassword"/></dt>
                    <dd><form:password path="confirmPassword"/></dd>
                </dl>
            </c:otherwise>
        </c:choose>
    </details>
    <c:if test="${not command.ldapAuthenticated}">
        <div class="submits">
            <input type="submit" value="<fmt:message key='common.save'/>"/>
            <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
        </div>
    </c:if>
</form:form>

</body></html>
