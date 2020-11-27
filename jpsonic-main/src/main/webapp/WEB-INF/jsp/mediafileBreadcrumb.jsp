<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS : (mainContloler.model)
--%>

<nav>
    <ul class="breadcrumb">
        <sub:url value="home.view" var="indexUrl"><sub:param name="listType" value="index"/></sub:url>
        <c:if test="${model.breadcrumbIndex}">
	        <c:choose>
	            <c:when test="${not empty model.selectedMusicFolder}">
	                <c:if test="${model.dir.folder eq model.selectedMusicFolder.path}">
	                    <li><a target="main" href="${indexUrl}" title="${fn:escapeXml(model.selectedMusicFolder.name)}">${fn:escapeXml(model.selectedMusicFolder.name)}</a></li>
	                </c:if>
	            </c:when>
	            <c:otherwise>
	                <li><a target="main" href="${indexUrl}" title="<fmt:message key='left.allfolders'/>"><fmt:message key='left.allfolders'/></a></li>
	            </c:otherwise>
	        </c:choose>
	    </c:if>
        <c:forEach items="${model.ancestors}" var="ancestor">
            <sub:url value="main.view" var="ancestorUrl">
                <sub:param name="id" value="${ancestor.id}"/>
            </sub:url>
            <li><a target="main" href="${ancestorUrl}">${fn:escapeXml(ancestor.name)}</a></li>
        </c:forEach>
    </ul>
</nav>
