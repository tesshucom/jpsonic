<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>
<body class="mainframe bgcolor1">

<c:import url="helpHeader.jsp">
    <c:param name="cat" value="help"/>
    <c:param name="restricted" value="${not model.admin}"/>
</c:import>

<c:choose>
    <c:when test="${empty model.buildDate}">
        <fmt:message key="common.unknown" var="buildDateString"/>
    </c:when>
    <c:otherwise>
        <fmt:formatDate value="${model.buildDate}" dateStyle="long" var="buildDateString"/>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${empty model.localVersion}">
        <fmt:message key="common.unknown" var="versionString"/>
    </c:when>
    <c:otherwise>
        <c:set var="versionString" value="${model.localVersion}"/>
    </c:otherwise>
</c:choose>

<c:if test="${model.newVersionAvailable}">
    <p class="warning"><fmt:message key="help.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message></p>
</c:if>

<details open>
    <summary class="jpsonic"><fmt:message key="help.appInfo"/></summary>
    <dl>
        <dt><fmt:message key="help.version.title"/></dt>
        <dd>Jpsonic ${versionString} (based on Airsonic 11.0.0-SNAPSHOT) &ndash; ${buildDateString}</dd>
        <dt><fmt:message key="help.server.title"/></dt>
        <dd>${model.serverInfo} (<sub:formatBytes bytes="${model.usedMemory}"/> / <sub:formatBytes bytes="${model.totalMemory}"/>)</dd>
        <dt><fmt:message key="help.license.title"/></dt>
        <dd>
            <a href="http://www.gnu.org/copyleft/gpl.html" target="_blank"><img style="float:right;margin-left: 10px" alt="GPL 3.0" src="<c:url value='/icons/default_light/gpl.png'/>">
            <fmt:message key="help.license.text"><fmt:param value="${model.brand}"/></fmt:message>
        </dd>
        <dt><fmt:message key="help.homepage.title"/></dt>
        <dd><a target="_blank" href="https://airsonic.github.io/" rel="noopener nofererrer">Airsonic website</a> / <a target="_blank" href="https://tesshu.com/" rel="noopener nofererrer">Jpsonic website</a></dd>
        <dt><fmt:message key="help.forum.title"/></dd>
        <dd><a target="_blank" href="https://www.reddit.com/r/airsonic" rel="noopener nofererrer">Airsonic on Reddit</a></dd>
        <dt><fmt:message key="help.contact.title"/></dt>
        <dd><fmt:message key="help.contact.text"><fmt:param value="Airsonic"/></fmt:message></dd>
    </dl>
</details>        

<c:if test="${model.user.adminRole}">
    <details>
        <summary class="legacy"><fmt:message key="help.log"/></summary>
        <p>
            <fmt:message key="help.logfile"><fmt:param value="${model.logFile}"/></fmt:message>
        </p>
        <table cellpadding="2" class="log indent">
            <c:forEach items="${model.logEntries}" var="entry">
                <tr>
                    <td>${fn:escapeXml(entry)}</td>
                </tr>
            </c:forEach>
        </table>
    </details>
    <div class="submits">
        <input type="button" onClick="location.href='help.view?'" value="<fmt:message key='common.refresh'/>" />
    </div>
</c:if>

</body></html>
