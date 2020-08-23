<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%--
(depends home.model, isFootPager)
--%>
<div class="controls">

    <c:if test="${model.listType ne 'random'}">
        <sub:url value="home.view" var="previousUrl">
            <sub:param name="listType" value="${model.listType}"/>
            <sub:param name="listOffset" value="${model.listOffset - model.listSize}"/>
            <sub:param name="genre" value="${model.genre}"/>
            <sub:param name="decade" value="${model.decade}"/>
        </sub:url>
        <sub:url value="home.view" var="nextUrl">
            <sub:param name="listType" value="${model.listType}"/>
            <sub:param name="listOffset" value="${model.listOffset + model.listSize}"/>
            <sub:param name="genre" value="${model.genre}"/>
            <sub:param name="decade" value="${model.decade}"/>
        </sub:url>

        <c:if test="${!isFootPager}">
            <c:if test="${model.listType eq 'decade'}">
                <span class="titledSelector decade">
                    <fmt:message key="home.decade.text"/>
                    <select name="decade" onchange="location='home.view?listType=${model.listType}&amp;decade=' + options[selectedIndex].value">
                        <c:forEach items="${model.decades}" var="decade">
                            <option
                            ${decade eq model.decade ? "selected" : ""} value="${decade}">${decade}</option>
                        </c:forEach>
                    </select>
                </span>
            </c:if>
            <c:if test="${model.listType eq 'genre'}">
                <span class="titledSelector genre">
                    <fmt:message key="home.genre.text"/>
                    <select name="genre" onchange="location='home.view?listType=${model.listType}&amp;genre=' + encodeURIComponent(options[selectedIndex].value)">
                        <c:forEach items="${model.genres}" var="genre">
                            <option ${genre.name eq model.genre ? "selected" : ""} value="${fn:escapeXml(genre.name)}">${fn:escapeXml(genre.name)} (${genre.albumCount})</option>
                        </c:forEach>
                    </select>
                </span>
            </c:if>
        </c:if>

        <c:if test="${fn:length(model.albums) gt 0}">
            <span class="pager">
                <c:choose>
                    <c:when test="${fn:length(model.albums) lt model.listOffset + 1}">
                        <a href="${previousUrl}" title="Previous" class="control previous">Previous</a>
                    </c:when>
                    <c:otherwise>
                        <span title="Previous" class="control previous disabled">Previous</span>
                    </c:otherwise>
                </c:choose>
                <span class="pages">
                    <span>${model.listOffset + 1}</span>
                    <span> - </span>
                    <span>${model.listOffset + fn:length(model.albums)}</span>
                </span>
                <c:choose>
                    <c:when test="${fn:length(model.albums) eq model.listSize}">
                        <a href="${nextUrl}" title="Forward" class="control forward">Forward</a>
                    </c:when>
                    <c:otherwise>
                        <span title="Forward" class="control forward disabled">Forward</span>
                    </c:otherwise>
                </c:choose>
            </span>
        </c:if>
    </c:if>

</div>
