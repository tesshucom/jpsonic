<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page trimDirectiveWhitespaces="true" %>

<div class="controls">

    <span class="folder">
        <c:choose>
            <c:when test="${not empty model.musicFolder}">
                    ${fn:escapeXml(model.musicFolder.name)}
            </c:when>
            <c:otherwise>
                    <fmt:message key='left.allfolders'/>
            </c:otherwise>
        </c:choose>
    </span>

	<c:choose>
	    <c:when test="${model.listType ne 'random'}">
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

			<span class="pager">
		        <c:if test="${fn:length(model.albums) gt 0}">
		            <c:if test="${model.listOffset gt 0}">
		                <a href="${previousUrl}" title="Previous">&lt;&lt;</a>
		            </c:if>
		        	${model.listOffset + 1} - ${model.listOffset + fn:length(model.albums)}
		            <c:if test="${fn:length(model.albums) eq model.listSize}">
		                <a href="${nextUrl}" title="Forward">&gt;&gt;</a>
		            </c:if>
		        </c:if>
			</span>

			<span class="decade">
		        <c:if test="${model.listType eq 'decade'}">
		            <fmt:message key="home.decade.text"/>
	                <select name="decade" onchange="location='home.view?listType=${model.listType}&amp;decade=' + options[selectedIndex].value">
	                    <c:forEach items="${model.decades}" var="decade">
	                        <option
	                        ${decade eq model.decade ? "selected" : ""} value="${decade}">${decade}</option>
	                    </c:forEach>
	                </select>
		        </c:if>
		    </span>

			<span class="genre">
		        <c:if test="${model.listType eq 'genre'}">
		            <fmt:message key="home.genre.text"/>
	                <select name="genre" onchange="location='home.view?listType=${model.listType}&amp;genre=' + encodeURIComponent(options[selectedIndex].value)">
	                    <c:forEach items="${model.genres}" var="genre">
	                        <option ${genre.name eq model.genre ? "selected" : ""} value="${fn:escapeXml(genre.name)}">${fn:escapeXml(genre.name)} (${genre.albumCount})</option>
	                    </c:forEach>
	                </select>
		        </c:if>
		    </span>
	    </c:when>
	    <c:otherwise>
	    	<span class="pager"></span>
	    	<span class="decade"></span>
	    	<span class="genre"></span>
		</c:otherwise>
	</c:choose>

    <span class="refresh">
        <a href="javascript:refresh()">
            <img src="<spring:theme code='refreshImage'/>" alt="Refresh">
            <fmt:message key="common.refresh"/>
        </a>
    </span>

    <span class="shuffle">
	    <c:if test="${not empty model.albums}">
            <a href="javascript:playShuffle()">
              <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle">
              <fmt:message key="home.shuffle"/>
            </a>
	    </c:if>
    </span>
	
</div>
