<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
</head>
<body class="mainframe help">

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
    <p><strong><fmt:message key="help.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message></strong></p>
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
            <a href="http://www.gnu.org/copyleft/gpl.html" target="_blank"><img style="float:right;margin-left: 10px" alt="GPL 3.0" src="<c:url value='/icons/gpl.png'/>">
            <fmt:message key="help.license.text"><fmt:param value="${model.brand}"/></fmt:message>
            <fmt:message key="personalsettings.avatar.courtesy"/>
            <a href="https://last.fm/" target="_blank" rel="noopener noreferrer"><img alt="Lastfm icon" src="<c:url value='/icons/lastfm.gif'/>"></a>
            <fmt:message key="changecoverart.courtesy"/></span>
        </dd>
        <dt><fmt:message key="help.homepage.title"/></dt>
        <dd><a target="_blank" href="https://airsonic.github.io/" rel="noopener nofererrer">Airsonic website</a> / <a target="_blank" href="https://tesshu.com/" rel="noopener nofererrer">Jpsonic website</a></dd>
        <dt><fmt:message key="help.forum.title"/></dd>
        <dd><a target="_blank" href="https://www.reddit.com/r/airsonic" rel="noopener nofererrer">Airsonic on Reddit</a></dd>
        <dt><fmt:message key="help.contact.title"/></dt>
        <dd><fmt:message key="help.contact.text"><fmt:param value="Airsonic"/></fmt:message></dd>
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
