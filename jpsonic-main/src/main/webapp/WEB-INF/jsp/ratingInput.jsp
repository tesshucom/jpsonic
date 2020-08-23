<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
Creates HTML for displaying the rating stars.
PARAMETERS
  id: Album ID. May be null if readonly.
  rating: The rating, an integer from 0 (no rating), through 10 (lowest rating), to 50 (highest rating).
  isStreamRole: Whether the account has a stream Role
--%>

<li>
	<a href="#" class="control ratings" title="Input rating">Input rating</a>
	<input type="checkbox" id="isStreamRole" value="1" autofocus="false" ${param.isStreamRole ? "checked" : ""} />
    <ul>
        <c:forEach var="i" begin="1" end="5">
            <sub:url value="setRating.view" var="ratingUrl">
                <sub:param name="id" value="${param.id}"/>
                <sub:param name="action" value="rating"/>
                <sub:param name="rating" value="${i}"/>
            </sub:url>
            <c:set var="ratingClass" value="" />
            <c:choose>
                <c:when test="${param.rating ge i * 10}">
                    <c:set var="ratingClass" value="control star-fill" />
                </c:when>
                <c:when test="${param.rating ge i*10 - 7 and param.rating le i*10 - 3}">
                    <c:set var="ratingClass" value="control star-half" />
                </c:when>
                <c:otherwise>
                    <c:set var="ratingClass" value="control star" />
                </c:otherwise>
            </c:choose>
            <li><a href="${ratingUrl}" class="${ratingClass}" title="<fmt:message key='rating.rating'/> ${i}"><fmt:message key='rating.rating'/> ${i}</a></li>
        </c:forEach>
        <sub:url value="setRating.view" var="clearRatingUrl">
            <sub:param name="id" value="${param.id}"/>
            <sub:param name="action" value="rating"/>
            <sub:param name="rating" value="0"/>
        </sub:url>
        <li><a href="${clearRatingUrl}" title="<fmt:message key='rating.clearrating'/>" class="control cross"><fmt:message key='rating.clearrating'/></a></li>
       </ul>
</li>
