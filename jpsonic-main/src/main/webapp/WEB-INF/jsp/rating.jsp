<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
Creates HTML for displaying the rating stars.
PARAMETERS
  id: Album ID. May be null if readonly.
  readonly: Whether rating can be changed.
  rating: The rating, an integer from 0 (no rating), through 10 (lowest rating), to 50 (highest rating).
--%>

<div class="ratings">
	<c:forEach var="i" begin="1" end="5">

	    <sub:url value="setRating.view" var="ratingUrl">
	        <sub:param name="id" value="${param.id}"/>
	        <sub:param name="action" value="rating"/>
	        <sub:param name="rating" value="${i}"/>
	    </sub:url>

        <c:set var="starClass" value="" />
	    <c:choose>
	        <c:when test="${param.rating ge i * 10}">
                <c:set var="starClass" value="star-fill" />
	        </c:when>
	        <c:when test="${param.rating ge i*10 - 7 and param.rating le i*10 - 3}">
                <c:set var="starClass" value="star-half" />
	        </c:when>
	        <c:otherwise>
                <c:set var="starClass" value="star" />
	        </c:otherwise>
	    </c:choose>
	    <div title="<fmt:message key='rating.rating'/> ${i}" class="rating ${starClass}"><fmt:message key='rating.rating'/> ${i}</div>
	</c:forEach>

	<sub:url value="setRating.view" var="clearRatingUrl">
	    <sub:param name="id" value="${param.id}"/>
	    <sub:param name="action" value="rating"/>
	    <sub:param name="rating" value="0"/>
	</sub:url>

	<c:if test="${not param.readonly}">
	    <a href="${clearRatingUrl}" title="<fmt:message key='rating.clearrating'/>" class="rating clear"><fmt:message key='rating.clearrating'/></a>
	</c:if>
</div>
	