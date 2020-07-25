<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%--@elvariable id="command" type="org.airsonic.player.command.GeneralSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe settings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="general"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="useSonos" value="${command.useSonos}"/>
</c:import>

<form:form method="post" action="generalSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.themeandlanguage"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.language"/></dt>
            <dd>
                <form:select path="localeIndex" cssStyle="width:15em">
                    <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.theme"/></dt>
            <dd>
                <form:select path="themeIndex" cssStyle="width:15em">
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.indexsettings"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.index"/></dt>
            <dd>
                <form:input path="index" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="index"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.ignoredarticles"/></dt>
            <dd>
                <form:input path="ignoredArticles" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ignoredarticles"/></c:import>
            </dd>
        </dl>
    </details>

    <details open>
        <summary class="jpsonic"><fmt:message key="generalsettings.sortsettings"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="sortAlbumsByYear" id="sortAlbumsByYear"/>
                <label for="sortAlbumsByYear"><fmt:message key="generalsettings.sortalbumsbyyear"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="sortalbumsbyyear"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="sortGenresByAlphabet" id="sortGenresByAlphabet"/>
                <label for="sortGenresByAlphabet"><fmt:message key="generalsettings.sortgenresbyalphabet"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="sortgenresbyalphabet"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="prohibitSortVarious" id="prohibitSortVarious"/>
                <label for="prohibitSortVarious"><fmt:message key="generalsettings.prohibitsortvarious"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="prohibitsortvarious"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="sortAlphanum" id="sortAlphanum"/>
                <label for="sortAlphanum"><fmt:message key="generalsettings.sortalphanum"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="sortalphanum"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="sortStrict" id="sortStrict"/>
                <label for="sortStrict"><fmt:message key="generalsettings.sortstrict"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="sortstrict"/></c:import>
            </dd>
        </dl>
    </details>

    <details open>
        <summary class="jpsonic"><fmt:message key="generalsettings.searchsettings"/></summary>
        <dl>
            <dt></dt>
            <dd><form:checkbox path="searchComposer" id="searchComposer"/>
                <label for="searchComposer"><fmt:message key="generalsettings.searchcomposer"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="searchcomposer"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="outputSearchQuery" id="outputSearchQuery"/>
                <label for="outputSearchQuery"><fmt:message key="generalsettings.outputsearchquery"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="outputsearchquery"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                    <form:checkbox path="searchMethodLegacy" id="searchMethodLegacy"/>
                    <label for="searchMethodLegacy"><fmt:message key="generalsettings.searchmethodlegacy"/></label>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="searchmethod"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="generalsettings.extandshortcuts"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.musicmask"/></dt>
            <dd>
                    <form:input path="musicFileTypes" size="70"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="musicmask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.videomask"/></dt>
            <dd>
                    <form:input path="videoFileTypes" size="70"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="videomask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.coverartmask"/></dt>
            <dd>
                    <form:input path="coverArtFileTypes" size="70"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="coverartmask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.playlistfolder"/></dt>
            <dd>
                <form:input path="playlistFolder" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="playlistfolder"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.shortcuts"/></dt>
            <dd>
                    <form:input path="shortcuts" size="70"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="shortcuts"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="generalsettings.welcommessage"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.gettingstarted"/></dt>
            <dd>
            <form:checkbox path="gettingStartedEnabled" id="gettingStartedEnabled"/>
                <label for="gettingStartedEnabled"><fmt:message key="generalsettings.showgettingstarted"/></label>
            </dd>
            <dt><fmt:message key="generalsettings.welcometitle"/></dt>
            <dd>
                <form:input path="welcomeTitle" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.welcomesubtitle"/></dt>
            <dd>
                <form:input path="welcomeSubtitle" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.welcomemessage"/></dt>
            <dd>
                <form:textarea path="welcomeMessage" rows="5" cols="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.loginmessage"/></dt>
            <dd>
                <form:textarea path="loginMessage" rows="5" cols="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="loginmessage"/></c:import>
            </dd>
        </dl>
    </details>


    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.infrequent"/></summary>
        <dl>
        	<dt></dt>
        	<dd>
				<form:checkbox path="useRadio" id="useRadio"/>
                <label for="useRadio"><fmt:message key="generalsettings.useradio"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="useradio"/></c:import>
            </dd>
        	<dt></dt>
        	<dd>
				<form:checkbox path="useSonos" id="useSonos"/>
                <label for="useSonos"><fmt:message key="generalsettings.usesonos"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="usesonos"/></c:import>
            </dd>
        </dl>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
      window.top.right.location.reload();
      window.top.playQueue.location.reload();
      window.top.upper.location.reload();
    </script>
</c:if>

</body></html>
