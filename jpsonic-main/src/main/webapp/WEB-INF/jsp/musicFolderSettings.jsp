<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.MusicFolderSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript">
        function init() {
            $("#newMusicFolderName").attr("placeholder", "<fmt:message key="musicfoldersettings.name"/>");
            $("#newMusicFolderPath").attr("placeholder", "<fmt:message key="musicfoldersettings.path"/>");

            <c:if test="${settings_reload}">
            parent.frames.upper.location.href="top.view?";
            parent.frames.left.location.href="left.view?";
            parent.frames.right.location.href="right.view?";
            </c:if>
        }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">


<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="musicFolder"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form modelAttribute="command" action="musicFolderSettings.view" method="post">

    <details open>
        <summary class="legacy"><fmt:message key="musicfoldersettings.specify"/></summary>

            <table class="indent">
                <tr>
                    <th><fmt:message key="musicfoldersettings.name"/></th>
                    <th><fmt:message key="musicfoldersettings.path"/></th>
                    <th style="padding-left:1em"><fmt:message key="musicfoldersettings.enabled"/></th>
                    <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
                    <th></th>
                </tr>
                <c:forEach items="${command.musicFolders}" var="folder" varStatus="loopStatus">
                    <tr>
                        <td><form:input path="musicFolders[${loopStatus.count-1}].name" size="20"/></td>
                        <td><form:input path="musicFolders[${loopStatus.count-1}].path" size="40"/></td>
                        <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].enabled" cssClass="checkbox"/></td>
                        <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].delete" cssClass="checkbox"/></td>
                        <td><c:if test="${not folder.existing}"><span class="warning"><fmt:message key="musicfoldersettings.notfound"/></span></c:if></td>
                    </tr>
                </c:forEach>
                <c:if test="${not empty command.musicFolders}">
                    <tr>
                        <th colspan="4" align="left" style="padding-top:1em"><fmt:message key="musicfoldersettings.add"/></th>
                    </tr>
                </c:if>
                <tr>
                    <td><form:input id="newMusicFolderName" path="newMusicFolder.name" size="20"/></td>
                    <td><form:input id="newMusicFolderPath" path="newMusicFolder.path" size="40"/></td>
                    <td align="center" style="padding-left:1em"><form:checkbox path="newMusicFolder.enabled" cssClass="checkbox"/></td>
                    <td></td>
                </tr>
            </table>

    </details>


    <details class="legacy" open>
        <summary><fmt:message key="musicfoldersettings.execscan"/></summary>
        <c:if test="${command.scanning}">
            <p class="warning"><fmt:message key="musicfoldersettings.nowscanning"/></p>
        </c:if>
        <dl>
            <dt><fmt:message key='musicfoldersettings.scannow'/><c:import url="helpToolTip.jsp"><c:param name="topic" value="scanMediaFolders"/></c:import></dt>
            <dd>
                <div>
                    <input type="button" onClick="location.href='musicFolderSettings.view?scanNow'" value="<fmt:message key='musicfoldersettings.doscan'/>" 
                        <c:if test="${command.scanning}">
                            disabled
                        </c:if>
                    />
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
                    <input type="button" onClick="location.href='musicFolderSettings.view?expunge'" value="<fmt:message key='musicfoldersettings.doexpunge'/>" 
                        <c:if test="${command.scanning}">
                            disabled
                        </c:if>
                    />
                </div>
            </dd>
        </dl>
    </details>

    <details>
        <summary class="legacy"><fmt:message key="musicfoldersettings.exclusion"/></summary>
        <dl>
            <dt><fmt:message key="musicfoldersettings.excludepattern"/></dt>
            <dd>
                <form:input path="excludePatternString" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="excludepattern"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="ignoreSymLinks" id="ignoreSymLinks"/>
                <form:label path="ignoreSymLinks"><fmt:message key="musicfoldersettings.ignoresymlinks"/></form:label>
            </dd>
    </details>

    <details>
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
        <input type="submit" value="<fmt:message key='common.save'/>" 
            <c:if test="${command.scanning}">
                disabled
            </c:if>
        >
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

</body></html>
