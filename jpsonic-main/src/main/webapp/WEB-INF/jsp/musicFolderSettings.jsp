<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.MusicFolderSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script>
        function init() {
            $('#newMusicFolderName').attr("placeholder", "<fmt:message key="musicfoldersettings.name"/>");
            $('#newMusicFolderPath').attr("placeholder", "<fmt:message key="musicfoldersettings.path"/>");
            <c:if test="${settings_reload}">
              window.top.reloadUpper("musicFolderSettings.view");
              window.top.reloadPlayQueue();
              window.top.reloadRight();
            </c:if>
        }
    </script>
</head>
<body class="mainframe settings musicFolderSettings" onload="init()">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="musicFolder"/>
    <c:param name="toast" value="${command.showToast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="useSonos" value="${command.useSonos}"/>
</c:import>

<form:form modelAttribute="command" action="musicFolderSettings.view" method="post">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
    <details open>
        <summary><fmt:message key="musicfoldersettings.specify"/></summary>
        <table class="tabular musicfolder">
            <caption><fmt:message key="musicfoldersettings.registered"/></caption>
            <thead>
                <tr>
                    <th><fmt:message key="musicfoldersettings.name"/></th>
                    <th><fmt:message key="musicfoldersettings.path"/></th>
                    <th><fmt:message key="musicfoldersettings.enabled"/></th>
                    <th><fmt:message key="common.delete"/></th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${command.musicFolders}" var="folder" varStatus="loopStatus">
                    <tr>
                        <td><form:input path="musicFolders[${loopStatus.count-1}].name"/></td>
                        <td><form:input path="musicFolders[${loopStatus.count-1}].path"/></td>
                        <td><form:checkbox path="musicFolders[${loopStatus.count-1}].enabled"/></td>
                        <td><form:checkbox path="musicFolders[${loopStatus.count-1}].delete"/></td>
                        <td><c:if test="${not folder.existing}"><strong><fmt:message key="musicfoldersettings.notfound"/></strong></c:if></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <table class="tabular musicfolder">
            <caption><fmt:message key="musicfoldersettings.add"/></caption>
            <thead>
                <tr>
                    <th><fmt:message key="musicfoldersettings.name"/></th>
                    <th><fmt:message key="musicfoldersettings.path"/></th>
                    <th><fmt:message key="musicfoldersettings.enabled"/></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><form:input id="newMusicFolderName" path="newMusicFolder.name"/></td>
                    <td><form:input id="newMusicFolderPath" path="newMusicFolder.path"/></td>
                    <td><form:checkbox path="newMusicFolder.enabled" cssClass="checkbox"/></td>
                </tr>
            </tbody>
        </table>
    </details>

    <details open>
        <summary><fmt:message key="musicfoldersettings.execscan"/></summary>
        <c:if test="${command.scanning}">
            <strong><fmt:message key="musicfoldersettings.nowscanning"/></strong>
        </c:if>
        <dl>
            <dt><fmt:message key='musicfoldersettings.scannow'/><c:import url="helpToolTip.jsp"><c:param name="topic" value="scanMediaFolders"/></c:import></dt>
            <dd>
                <div>
                    <c:choose>
                        <c:when test='${command.scanning}'>
                            <input type="button" onClick="location.href='musicFolderSettings.view?scanNow'" value="<fmt:message key='musicfoldersettings.doscan'/>" disabled/>
                        </c:when>
                        <c:otherwise>
                            <input type="button" onClick="location.href='musicFolderSettings.view?scanNow'" value="<fmt:message key='musicfoldersettings.doscan'/>"/>
                        </c:otherwise>
                    </c:choose>
                </div>
            </dd>
            <dt><fmt:message key="musicfoldersettings.scan"/></dt>
            <dd>
                <form:select path="interval">
                    <fmt:message key="musicfoldersettings.interval.never" var="never"/>
                    <fmt:message key="musicfoldersettings.interval.one" var="one"/>
                    <form:option value="-1" label="${never}"/>
                    <form:option value="1" label="${one}"/>
    
                    <c:forTokens items="2 3 7 14 30 60" delims=" " var="interval">
                        <fmt:message key="musicfoldersettings.interval.many" var="many"><fmt:param value="${interval}"/></fmt:message>
                        <form:option value="${interval}" label="${many}"/>
                    </c:forTokens>
                </form:select>
                <form:select path="hour">
                    <c:forEach begin="0" end="23" var="hour">
                        <fmt:message key="musicfoldersettings.hour" var="hourLabel"><fmt:param value="${hour}"/></fmt:message>
                        <form:option value="${hour}" label="${hourLabel}"/>
                    </c:forEach>
                </form:select>
            </dd>
            <dt><fmt:message key='musicfoldersettings.expunge'/><c:import url="helpToolTip.jsp"><c:param name="topic" value="expunge"/></c:import></dt>
            <dd>
                <div>
                    <c:choose>
                        <c:when test='${command.scanning}'>
                            <input type="button" onClick="location.href='musicFolderSettings.view?expunge'" value="<fmt:message key='musicfoldersettings.doexpunge'/>" disabled/>
                        </c:when>
                        <c:otherwise>
                            <input type="button" onClick="location.href='musicFolderSettings.view?expunge'" value="<fmt:message key='musicfoldersettings.doexpunge'/>"/>
                        </c:otherwise>
                    </c:choose>
                </div>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="musicfoldersettings.exclusion"/></summary>
        <dl>
            <dt><fmt:message key="musicfoldersettings.excludepattern"/></dt>
            <dd>
                <form:input path="excludePatternString"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="excludepattern"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="ignoreSymLinks" id="ignoreSymLinks"/>
                <form:label path="ignoreSymLinks"><fmt:message key="musicfoldersettings.ignoresymlinks"/></form:label>
            </dd>
    </details>

    <details ${isOpen}>
        <summary class="jpsonic"><fmt:message key="musicfoldersettings.other"/></summary>
        <dl>
            <dt><fmt:message key="musicfoldersettings.excludepattern"/></dt>
            <dd>
                <form:checkbox path="fastCache" cssClass="checkbox" id="fastCache"/>
                <form:label path="fastCache"><fmt:message key="musicfoldersettings.fastcache"/></form:label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="fastcache"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="indexEnglishPrior" id="indexEnglishPrior" disabled="true"/>
                <label for="indexEnglishPrior"><fmt:message key="generalsettings.indexEnglishPrior"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="indexEnglishPrior"/></c:import>
            </dd>
    </details>

    <div class="submits">
        <c:choose>
            <c:when test='${command.scanning}'>
                <input type="submit" value="<fmt:message key='common.save'/>" disabled/>
            </c:when>
            <c:otherwise>
                <input type="submit" value="<fmt:message key='common.save'/>"/>
            </c:otherwise>
        </c:choose>
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

</body></html>
