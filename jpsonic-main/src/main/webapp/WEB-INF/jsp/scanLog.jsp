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

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="scanlog.view"/>
    <c:param name="showOutlineHelp" value="${model.showOutlineHelp}"/>
</c:import>


<c:set var="showHelp" value='${model.showOutlineHelp}' />
<c:if test="${showHelp}">
    <div class="outlineHelp">
        <fmt:message key="scanlog.scanlogoutline"/>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty model.scanLogs}">
        <p><fmt:message key="scanlog.noscanlogs"/></p>
    </c:when>
    <c:otherwise>
        <dl class="topSelectorContainer2">
            <dt><fmt:message key="scanlog.starttime"/></dt>
            <dd>
                <select name="scanlogs" onchange="refreshScanLogs()">
                    <c:forEach items="${model.scanLogs}" var="scanLog">
                        <option ${model.startDate eq scanLog.startDate ? "selected" : ""} value="${scanLog.startDate}">
                            ${scanLog.startDateStr} [ ${scanLog.status} ]
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
                <input type="checkbox" name="showScannedCount" ${model.showScannedCount ? 'checked' : ''}  onchange="refreshScanLogs()"/>
                <fmt:message key="scanlog.showscannedcount"/>
            </dd>
        </dl>
    </c:otherwise>
</c:choose>

<c:if test="${not empty model.scanLogs}">
    <table class="tabular scanevents">
        <caption>Duration All : ${model.scanEventsDuration}</caption>
        <thead>
            <tr>
                <th>Type</th>
                <th>End time</th>
                <th>Duration</th>
                <th>Max</th>
                <th>Total</th>
                <th>Used</th>
                <th>Comment</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${model.scanEvents}" var="scanEvent" varStatus="loopStatus">
                <tr>
                    <td>${scanEvent.type}
                        <c:if test="${showHelp}">
                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="scanlog_${fn:toLowerCase(scanEvent.type)}"/></c:import>
                        </c:if>           
                    </td>
                    <td>${scanEvent.executedStr}</td>
                    <td>${scanEvent.duration}</td>
                    <td>${scanEvent.maxMemory}</td>
                    <td>${scanEvent.totalMemory}</td>
                    <td>${scanEvent.usedMemory}</td>
                    <td>${scanEvent.comment}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</c:if>

</body>
</html>
