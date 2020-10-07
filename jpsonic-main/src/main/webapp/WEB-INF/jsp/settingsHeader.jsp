<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>
<script src="<c:url value='/script/jpsonic/tryCloseDrawer.js'/>"></script>
<c:if test="${param.toast}">
    <script>
        $(document).ready(function () {
            $().toastmessage("showSuccessToast", "<fmt:message key="common.settingssaved"/>");
        });
    </script>
</c:if>

<c:set var="categories" value="${param.restricted ? 'personal password player share' : 'musicFolder general advanced personal user player share dlna sonos transcoding internetRadio database'}"/>

<section>
	<h1 class="settings"><fmt:message key="settingsheader.title"/></h1>
	<ul class="sibling-pages">
	    <c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
	        <c:choose>
	            <c:when test="${loopStatus.count > 1}"></li><li></c:when>
	            <c:otherwise><li></c:otherwise>
	        </c:choose>
	        <c:url var="url" value="${cat}Settings.view?"/>

	        <c:if test="${('internetRadio' != cat and 'sonos' != cat) or ('internetRadio' == cat and param.useRadio eq true) or ('sonos' == cat and param.useSonos eq true)}">
		        <c:choose>
		            <c:when test="${param.cat eq cat}">
		                <span class="selected"><fmt:message key="settingsheader.${cat}"/></span>
		            </c:when>
		            <c:otherwise>
		                <a href="${url}"><fmt:message key="settingsheader.${cat}"/></a>
		            </c:otherwise>
		        </c:choose>
			</c:if>
	    </c:forTokens>
	</ul>
</section>
