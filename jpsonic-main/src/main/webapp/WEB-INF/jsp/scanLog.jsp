<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>

<script>

function refreshScanLogs() {
    location='scanlog.view?startDate=' + $('[name="scanlogs"] option:selected').val()
             + "&showScannedCount=" + $('[name="showScannedCount"]:checked').prop('checked');
}

</script>
</head>
<body class="mainframe settings scanlog">

<c:import url="helpHeader.jsp">
    <c:param name="cat" value="scanlog"/>
    <c:param name="isAdmin" value="${model.admin}"/>
    <c:param name="showStatus" value="${model.showStatus}"/>
</c:import>

<c:choose>
    <c:when test="${empty model.scanLogs}">
        <p><fmt:message key="playersettings.noplayers"/></p>
    </c:when>
    <c:otherwise>
        <dl class="topSelectorContainer2">
            <dt>Scan start time</dt>
            <dd>
                <select name="scanlogs" onchange="refreshScanLogs()">
                    <c:forEach items="${model.scanLogs}" var="scanLog">
                        <option ${model.startDate eq scanLog.startDate ? "selected" : ""} value="${scanLog.startDate}">
                            <fmt:parseDate value="${scanLog.startDate}" type="both" pattern="yyyy-MM-dd'T'HH:mm:ss.SSS" var="parsedDate" />
                            <fmt:formatDate value="${parsedDate}" pattern="yyyy-MM-dd HH:mm:ss.SSS" /> [ ${scanLog.status} ]
                        </option> 
                    </c:forEach>
                </select>
                <c:if test="${model.scanning && (empty model.startDate or model.startDate eq model.scanLogs[0].startDate)}">
                    <fmt:message key="common.refresh" var="refresh" />
                    <a href="javaScript:refreshScanLogs()" class="control reflesh" alt="${refresh}">${refresh}</a>
                </c:if>
            </dd>
	        <dt></dt>
	        <dd>
	            <input type="checkbox" name="showScannedCount" ${model.showScannedCount ? 'checked' : ''}  onchange="refreshScanLogs()"/> Show Scanned Count
	        </dd>
        </dl>
    </c:otherwise>
</c:choose>

<table class="tabular scanevents">
    <caption>Duration All : ${model.scanEventsDuration}</caption>
    <thead>
        <tr>
            <th>End time</th>
            <th>Duration</th>
            <th>Type</th>
            <th>Max</th>
            <th>Total</th>
            <th>Used</th>
            <th>Comment</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach items="${model.scanEvents}" var="scanEvent" varStatus="loopStatus">
            <tr>
                <td>
                    <fmt:parseDate value="${scanEvent.executed}" type="both" pattern="yyyy-MM-dd'T'HH:mm:ss.SSS" var="parsedDate" />
                    <fmt:formatDate value="${parsedDate}" pattern="yyyy-MM-dd HH:mm:ss.SSS" />
                </td>
                <td>${scanEvent.duration}</td>
                <td>${scanEvent.type}</td>
                <td>${scanEvent.maxMemory}</td>
                <td>${scanEvent.totalMemory}</td>
                <td>${scanEvent.usedMemory}</td>
                <td>${scanEvent.comment}</td>
            </tr>
        </c:forEach>
    </tbody>
</table>

</body>
</html>
