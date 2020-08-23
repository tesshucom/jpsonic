<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  id: ID of file.
  video: Whether the file is a video (default false).
  playEnabled: Whether to show play button (default true).
  addEnabled: Whether to show add next/last buttons (default true).
  downloadEnabled: Whether to show download button (default false).
  starEnabled: Whether to show star/unstar controls (default false).
  starred: Whether the file is currently starred.
  asTable: Whether to put the images in td tags.
  onPlay: Overrides the javascript used for the play action.
--%>

<c:if test="${param.starEnabled}">
    <c:if test="${param.asTable}"><td></c:if>
    <c:choose>
        <c:when test="${param.starred}">
            <div id="starImage${param.id}" class="control star-fill" onclick="toggleStar(${param.id}, '#starImage${param.id}'); return false;">Star ON</div>
        </c:when>
        <c:otherwise>
            <div id="starImage${param.id}" class="control star" onclick="toggleStar(${param.id}, '#starImage${param.id}'); return false;">Star OFF</div>
        </c:otherwise>
    </c:choose>
    <c:if test="${param.asTable}"></td></c:if>
</c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${empty param.playEnabled or param.playEnabled}">
    <c:choose>
        <c:when test="${param.video}">
            <sub:url value="/videoPlayer.view" var="videoUrl">
                <sub:param name="id" value="${param.id}"/>
            </sub:url>
            <a target="main" href="${videoUrl}" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/></a>
        </c:when>
        <c:when test="${not empty param.onPlay}">
            <div onclick="${param.onPlay}; return false;" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/><fmt:message key='common.play'/></div>
        </c:when>
        <c:otherwise>
            <div onclick="top.playQueue.onPlay(${param.id}); return false;" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/></div>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${(empty param.addEnabled or param.addEnabled) and not param.video}">
    <div id="add${param.id}" onclick="top.playQueue.onAdd(${param.id}); $().toastmessage('showSuccessToast', '<fmt:message key='main.addlast.toast'/>'); return false;" title="<fmt:message key='main.addlast'/>" class="control plus"><fmt:message key='main.addlast'/></div>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${(empty param.addEnabled or param.addEnabled) and not param.video}">
    <div id="add${param.id}" onclick="top.playQueue.onAddNext(${param.id}); $().toastmessage('showSuccessToast', '<fmt:message key='main.addnext.toast'/>'); return false;" title="<fmt:message key='main.addnext'/>" class="control next"><fmt:message key='main.addnext'/></div>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.downloadEnabled}">
	<c:if test="${param.asTable}"><td></c:if>
    <sub:url value="/download.view" var="downloadUrl">
        <sub:param name="id" value="${param.id}"/>
    </sub:url>
    <a href="${downloadUrl}" title="<fmt:message key='common.download'/>" class="control download"><fmt:message key='common.download'/></a>
	<c:if test="${param.asTable}"></td></c:if>
</c:if>
