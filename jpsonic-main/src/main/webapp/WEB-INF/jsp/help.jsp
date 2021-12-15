<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
</head>
<body class="mainframe help">

<c:import url="helpHeader.jsp">
    <c:param name="cat" value="help"/>
    <c:param name="isAdmin" value="${model.admin}"/>
    <c:param name="showStatus" value="${model.showStatus}"/>
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
    <p><strong><fmt:message key="help.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message></strong></p>
</c:if>

<details open>
    <summary class="jpsonic"><fmt:message key="help.appInfo"/></summary>
    <dl>
        <dt><fmt:message key="help.version.title"/></dt>
        <dd>Jpsonic ${versionString} &ndash; ${buildDateString}</dd>
        <dt><fmt:message key="help.buildnumber.title"/></dt>
        <dd>${model.buildNumber}</dd>
        <dt><fmt:message key="help.server.title"/></dt>
        <dd>${model.serverInfo} (<sub:formatBytes bytes="${model.usedMemory}"/> / <sub:formatBytes bytes="${model.totalMemory}"/>)</dd>
        <dt><fmt:message key="help.license.title"/></dt>
        <dd style="display: flex;align-items: center;">
            <div style="text-align: left;padding-right:10px;">
                <fmt:message key="help.license.text"><fmt:param value="${model.brand}"/></fmt:message><br>(C) 2009 Sindre Mehus, 2016 Airsonic Authors, 2018 tesshucom
            </div>
            <a href="http://www.gnu.org/copyleft/gpl.html" target="_blank"><img alt="GPL 3.0" src="<c:url value='/icons/gpl.png'/>"></a>
        </dd>
        <dt><fmt:message key="help.homepage.title"/></dt>
        <dd><a target="_blank" href="https://tesshu.com/" rel="noopener nofererrer">Jpsonic website</a></dd>
    </dl>
</details>

<details open>
    <summary class="jpsonic"><fmt:message key="help.thanks.title"/></summary>
    <dl>
        <dt><fmt:message key="help.analyzer.title"/></dt>
        <dd><fmt:message key="help.analyzer.text"/></dd>
        <dt><fmt:message key="help.jpfont.title"/></dt>
        <dd><fmt:message key="help.jpfont.text"/></dd>
        <dt><fmt:message key="help.iconfont.title"/></dt>
        <dd><fmt:message key="help.iconfont.text"/></dd>
        <dt><fmt:message key="help.avatar.title"/></dt>
        <dd><fmt:message key="help.avatar.text"/></dd>
    </dl>
</details>

<c:if test="${model.user.adminRole or model.showServerLog}">
    <details>
        <summary class="legacy"><fmt:message key="help.log"/> (${model.lastModified} ${model.logFile})</summary>
        <div class="actions">
            <ul class="controls">
                <li><a href="help.view?" title="<fmt:message key='common.refresh'/>" class="control refresh"><fmt:message key='common.refresh'/></a></li>
            </ul>
        </div>
        <table class="tabular log">
            <tbody>
                <c:forEach items="${model.logEntries}" var="entry">
                    <tr>
                        <td>${fn:escapeXml(entry)}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </details>
</c:if>

</body></html>
