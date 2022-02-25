<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.GeneralSettingsCommand"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ page import="com.tesshu.jpsonic.domain.IndexScheme" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>
function setDefaultIndexString() {
    document.getElementById("index").value = "${command.defaultIndexString}";
}
function setSimpleIndexString() {
    document.getElementById("index").value = "${command.simpleIndexString}";
}
function resetSortSettings() {
    $('[name="sortAlbumsByYear"]').prop('checked', ${command.defaultSortAlbumsByYear});
    $('[name="sortGenresByAlphabet"]').prop('checked', ${command.defaultSortGenresByAlphabet});
    $('[name="prohibitSortVarious"]').prop('checked', ${command.defaultProhibitSortVarious});
    $('[name="sortAlphanum"]').prop('checked', ${command.defaultSortAlphanum});
    $('[name="sortStrict"]').prop('checked', ${command.defaultSortStrict});
}
function resetExtension() {
    $("#musicFileTypes").val('${command.defaultMusicFileTypes}');
    $("#videoFileTypes").val('${command.defaultVideoFileTypes}');
    $("#coverArtFileTypes").val('${command.defaultCoverArtFileTypes}');
    $("#playlistFolder").val('${command.defaultPlaylistFolder}');
    $("#shortcuts").val('${command.defaultShortcuts}');
}
</script>
</head>

<body class="mainframe settings generalSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="general"/>
    <c:param name="toast" value="${settings_toast or command.showToast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="generalSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<form:form method="post" action="generalSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details open>
        <summary class="jpsonic"><fmt:message key="generalsettings.themeandlanguage"/></summary>
        <dl>
            <dt><fmt:message key="generalsettings.language"/></dt>
            <dd>
                <form:select path="localeIndex">
                    <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.theme"/></dt>
            <dd>
                <form:select path="themeIndex">
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.indexscheme"/></dt>
            <dd>
                <input type="text" value="<fmt:message key='advancedsettings.indexscheme.${fn:toLowerCase(command.indexScheme)}'/>" disabled/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="indexscheme"/></c:import>
            </dd>
        </dl>
    </details>

    <details open>
        <summary class="jpsonic"><fmt:message key="generalsettings.indexsettings"/></summary>
        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="generalsettings.indexoutline"/>
            </div>
        </c:if>
        <dl>
            <dt><fmt:message key="generalsettings.index"/></dt>
            <dd>
                <ul class="indexPreset">
                    <li><a href="javascript:setDefaultIndexString();"><fmt:message key="generalsettings.defaultindex"/></a></li>
                    <li><a href="javascript:setSimpleIndexString();"><fmt:message key="generalsettings.simpleindex"/></a></li>
                </ul>
                <form:input path="index" id="index"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="index"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.ignoredarticles"/></dt>
            <dd>
                <form:input path="ignoredArticles"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ignoredarticles"/></c:import>
            </dd>
            <c:if test='${"WITHOUT_JP_LANG_PROCESSING" eq command.indexScheme}'>
                <dt></dt>
                <dd>
                    <form:checkbox path="ignoreFullWidth" id="ignoreFullWidth"/>
                    <label for="ignoreFullWidth"><fmt:message key="generalsettings.ignorefullwidth"/></label>
                </dd>
            </c:if>
            <c:if test='${"ROMANIZED_JAPANESE" eq command.indexScheme or "WITHOUT_JP_LANG_PROCESSING" eq command.indexScheme}'>
                <dt></dt>
                <dd>
                    <form:checkbox path="deleteDiacritic" id="deleteDiacritic"/>
                    <label for="deleteDiacritic"><fmt:message key="generalsettings.deletediacritic"/></label>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="deletediacritic"/></c:import>
                </dd>
            </c:if>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.sortsettings"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetSortSettings()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="generalsettings.sortoutline"/>
            </div>
        </c:if>

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

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.searchsettings"/></summary>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="generalsettings.searchoutline"/>
            </div>
        </c:if>

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
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.infrequent"/></summary>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="generalsettings.infrequentoutline"/>
            </div>
        </c:if>

        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="showServerLog" id="showServerLog"/>
                <label for="showServerLog"><fmt:message key="generalsettings.showserverlog"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="showserverlog"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="showStatus" id="showStatus"/>
                <label for="showStatus"><fmt:message key="generalsettings.showstatus"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="showstatus"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="othersPlayingEnabled" id="othersPlayingEnabled"/>
                <label for="othersPlayingEnabled"><fmt:message key="generalsettings.othersplayingenabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="othersplayingenabled"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="showRememberMe" id="showRememberMe"/>
                <label for="showRememberMe"><fmt:message key="generalsettings.showrememberme"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="showrememberme"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="publishPodcast" id="publishPodcast"/>
                <label for="publishPodcast"><fmt:message key="generalsettings.publishpodcast"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="publishpodcast"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="useRadio" id="useRadio"/>
                <label for="useRadio"><fmt:message key="generalsettings.useradio"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="useradio"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="useExternalPlayer" id="useExternalPlayer"/>
                <label for="useExternalPlayer"><fmt:message key="generalsettings.useexternalplayer"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="useexternalplayer"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="generalsettings.extandshortcuts"/></summary>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetExtension()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <dl>
            <dt><fmt:message key="generalsettings.musicmask"/></dt>
            <dd>
                <form:input path="musicFileTypes"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="musicmask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.videomask"/></dt>
            <dd>
                <form:input path="videoFileTypes"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="videomask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.coverartmask"/></dt>
            <dd>
                <form:input path="coverArtFileTypes"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="coverartmask"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.playlistfolder"/></dt>
            <dd>
                <form:input path="playlistFolder"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="playlistfolder"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.shortcuts"/></dt>
            <dd>
                <form:input path="shortcuts"/>
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
                <form:input path="welcomeTitle"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </dd>
            <dt><fmt:message key="generalsettings.welcomesubtitle"/></dt>
            <dd>
                <form:input path="welcomeSubtitle"/>
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

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>
</form:form>

<c:if test="${settings_reload}">
    <script>
      window.top.reloadUpper("generalSettings.view");
      window.top.reloadPlayQueue();
    </script>
</c:if>

</body></html>
