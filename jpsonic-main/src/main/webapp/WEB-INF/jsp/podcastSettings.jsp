<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PodcastSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe settings podcastSettings">

<c:import url="podcastsHeader.jsp">
    <c:param name="cat" value="settings"/>
    <%-- Legacy does not have a privilege check. Should be added. --%>
    <c:param name="restricted" value="false"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form modelAttribute="command" action="podcastSettings.view" method="post">

	<dl>
		<dt><fmt:message key="podcastsettings.update"/></dt>
		<dd>
	        <form:select path="interval">
	            <fmt:message key="podcastsettings.interval.manually" var="never"/>
	            <fmt:message key="podcastsettings.interval.hourly" var="hourly"/>
	            <fmt:message key="podcastsettings.interval.daily" var="daily"/>
	            <fmt:message key="podcastsettings.interval.weekly" var="weekly"/>
	
	            <form:option value="-1" label="${never}"/>
	            <form:option value="1" label="${hourly}"/>
	            <form:option value="24" label="${daily}"/>
	            <form:option value="168" label="${weekly}"/>
	        </form:select>
	    </dd>

	    <dt><fmt:message key="podcastsettings.keep"/></dt>
		<dd>
	        <form:select path="episodeRetentionCount">
	            <fmt:message key="podcastsettings.keep.all" var="all"/>
	            <fmt:message key="podcastsettings.keep.one" var="one"/>
	
	            <form:option value="-1" label="${all}"/>
	            <form:option value="1" label="${one}"/>
	
	            <c:forTokens items="2 3 4 5 10 20 30 50" delims=" " var="count">
	                <fmt:message key="podcastsettings.keep.many" var="many"><fmt:param value="${count}"/></fmt:message>
	                <form:option value="${count}" label="${many}"/>
	            </c:forTokens>
	
	        </form:select>
	    </dd>

	    <dt><fmt:message key="podcastsettings.download"/></dt>
	    <dd>
	        <form:select path="episodeDownloadCount">
	            <fmt:message key="podcastsettings.download.all" var="all"/>
	            <fmt:message key="podcastsettings.download.one" var="one"/>
	            <fmt:message key="podcastsettings.download.none" var="none"/>
	            <form:option value="-1" label="${all}"/>
	            <form:option value="1" label="${one}"/>
	            <c:forTokens items="2 3 4 5 10" delims=" " var="count">
	                <fmt:message key="podcastsettings.download.many" var="many"><fmt:param value="${count}"/></fmt:message>
	                <form:option value="${count}" label="${many}"/>
	            </c:forTokens>
	            <form:option value="0" label="${none}"/>
	        </form:select>
	    </dd>

	    <dt><fmt:message key="podcastsettings.folder"/></dt>
	    <dd><form:input path="folder"/></dd>

	</dl>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

</body></html>