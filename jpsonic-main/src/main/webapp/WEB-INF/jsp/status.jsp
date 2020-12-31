<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<meta http-equiv="CACHE-CONTROL" content="NO-CACHE">
<meta http-equiv="REFRESH" content="20;URL=status.view">
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
</head>
<body class="mainframe status">

<c:import url="helpHeader.jsp">
	<c:param name="cat" value="status"/>
    <c:param name="isAdmin" value="${model.admin}"/>
    <c:param name="showStatus" value="${model.showStatus}"/>
</c:import>

<div>
    <ul class="controls">
        <a href="status.view?" title="<fmt:message key='common.refresh'/>" class="control refresh"><fmt:message key="common.refresh"/></a>
    </ul>
</div>

<c:if test="${not empty model.transferStatuses}">
    <table class="tabular current">
        <thead>
            <tr>
                <th><fmt:message key="status.details" /></th>
                <th><fmt:message key="status.bitrate" /></th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${model.transferStatuses}" var="status">

                <c:choose>
                    <c:when test="${empty status.playerType}">
                        <fmt:message key="common.unknown" var="type" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="type" value="(${status.playerType})" />
                    </c:otherwise>
                </c:choose>

                <c:choose>
                    <c:when test="${status.stream}">
                        <fmt:message key="status.stream" var="transferType" />
                    </c:when>
                    <c:when test="${status.download}">
                        <fmt:message key="status.download" var="transferType" />
                    </c:when>
                    <c:when test="${status.upload}">
                        <fmt:message key="status.upload" var="transferType" />
                    </c:when>
                </c:choose>

                <c:choose>
                    <c:when test="${empty status.username}">
                        <fmt:message key="common.unknown" var="user" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="user" value="${status.username}" />
                    </c:otherwise>
                </c:choose>

                <c:choose>
                    <c:when test="${empty status.path}">
                        <fmt:message key="common.unknown" var="current" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="current" value="${status.path}" />
                    </c:otherwise>
                </c:choose>

                <sub:url value="/statusChart.view" var="chartUrl">
                    <c:if test="${status.stream}">
                        <sub:param name="type" value="stream" />
                    </c:if>
                    <c:if test="${status.download}">
                        <sub:param name="type" value="download" />
                    </c:if>
                    <c:if test="${status.upload}">
                        <sub:param name="type" value="upload" />
                    </c:if>
                    <sub:param name="index" value="${status.index}" />
                </sub:url>

                <tr>
                    <td>
                        <dl>
                            <dt><fmt:message key="status.user" /></dt><dd>${user}</dd>
                            <dt><fmt:message key="status.type" /></dt><dd>${transferType}</dd>
                            <dt><fmt:message key="status.player" /></dt><dd title="${status.player} ... ${type}">${status.player}<br>${type}</dd>
                            <dt><fmt:message key="status.current" /></dt><dd title="${current}">${current}</dd>
                            <dt><fmt:message key="status.transmitted" /></dt><dd>${status.bytes}</dd>
                        </dl>
                    </td>
                    <td>
                        <img width="${model.chartWidth}" height="${model.chartHeight}" src="${chartUrl}" alt="">
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</c:if>

<table class="tabular statics">
    <thead>
        <tr>
            <th><fmt:message key="home.chart.total"/></th>
            <th><fmt:message key="home.chart.stream"/></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><img src="<c:url value='/userChart.view'><c:param name='type' value='total'/></c:url>" alt=""></td>
            <td><img src="<c:url value='/userChart.view'><c:param name='type' value='stream'/></c:url>" alt=""></td>
        </tr>
    </tbody>
</table>

<table class="tabular statics">
    <thead>
        <tr>
            <th><fmt:message key="home.chart.download"/></th>
            <th><fmt:message key="home.chart.upload"/></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><img src="<c:url value='/userChart.view'><c:param name='type' value='download'/></c:url>" alt=""></td>
            <td><img src="<c:url value='/userChart.view'><c:param name='type' value='upload'/></c:url>" alt=""></td>
        </tr>
    </tbody>
</table>

</body></html>
