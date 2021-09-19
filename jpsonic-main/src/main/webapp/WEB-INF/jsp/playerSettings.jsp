<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.PlayerSettingsCommand"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ page import="com.tesshu.jpsonic.domain.PlayerTechnology" %>
<%@ page import="com.tesshu.jpsonic.domain.TranscodeScheme" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>
    <c:choose>
        <c:when test="${not empty playerId}">
            window.top.reloadUpper('playerSettings.view', '${playerId}');
            window.top.reloadPlayQueue();
        </c:when>
        <c:when test="${settings_reload}">
            window.top.reloadUpper("playerSettings.view");
            window.top.reloadPlayQueue();
        </c:when>
    </c:choose>
</script>
</head>
<body class="mainframe settings playerSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="player"/>
    <c:param name="toast" value="${command.showToast}"/>
    <c:param name="restricted" value="${not command.admin}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:if test="${command.admin}">
    <c:import url="outlineHelpSelector.jsp">
        <c:param name="targetView" value="playerSettings.view"/>
        <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
    </c:import>
</c:if>

<fmt:message key="common.unknown" var="unknown"/>

<c:choose>
    <c:when test="${empty command.players}">
        <p><fmt:message key="playersettings.noplayers"/></p>
    </c:when>
    <c:otherwise>

        <div class="titledSelector player">
            <fmt:message key="playersettings.title"/>
            <select name="player" onchange="location='playerSettings.view?id=' + options[selectedIndex].value;">
                <c:forEach items="${command.players}" var="player">
                    <option ${player.id eq command.playerId ? "selected" : ""}
                            value="${player.id}">${fn:escapeXml(player.description)}</option>
                </c:forEach>
            </select>
        </div>

        <form:form modelAttribute="command" method="post" action="playerSettings.view">
            <form:hidden path="playerId"/>
            <details open>
                <summary class="jpsonic"><fmt:message key="playersettings.settings"/></summary>
                <c:if test="${command.admin}">
                    <c:if test="${command.showOutlineHelp}">
                        <div class="outlineHelp">
                            <fmt:message key="playersettings.outline"/>
                        </div>
                    </c:if>
                </c:if>
                <dl>
                    <dt><fmt:message key="playersettings.type"/></dt>
                    <dd>
                        <c:choose>
                            <c:when test="${empty command.type}">${unknown}</c:when>
                            <c:otherwise>${command.type}</c:otherwise>
                        </c:choose>
                    </dd>
                    <dt><fmt:message key="playersettings.name"/></dt>
                    <dd><form:input path="name" value="${command.name}"/><c:import url="helpToolTip.jsp"><c:param name="topic" value="playername"/></c:import></dd>
                    <dt><fmt:message key="playersettings.devices"/></dt>
                    <dd>
                        <ul class="playerSettings">
                            <c:forEach items="${PlayerTechnology.values()}" var="scheme">
                                <c:set var="schemeName">
                                    <fmt:message key="playersettings.technology.${fn:toLowerCase(scheme)}"/>
                                </c:set>
                                <c:if test="${not (command.guest or command.anonymous) or technologyHolder.name eq 'WEB'}">
                                    <li>
                                        <form:radiobutton class="technologyRadio" id="radio-${schemeName}" path="playerTechnology" value="${scheme}"/>
                                        <label for="radio-${schemeName}">${schemeName}</label>
                                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="playersettings.technology.${fn:toLowerCase(scheme)}"/></c:import>
                                    </li>
                                </c:if>
                            </c:forEach>
                        </ul>
                    </dd>

                    <c:if test="${not command.anonymous or (command.anonymous and not command.sameSegment)}">
                        <dt><fmt:message key="playersettings.maxbitrate"/></dt>
                        <dd>
                            <form:select path="transcodeScheme">
                                <c:forEach items="${TranscodeScheme.values()}" var="scheme">
                                    <form:option value="${scheme}" label="${scheme.toString()}"/>
                                </c:forEach>
                            </form:select>
                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="transcode"/></c:import>
                            <c:if test="${not command.transcodingSupported}">
                                <strong><fmt:message key="playersettings.notranscoder"/></strong>
                            </c:if>
                        </dd>
                        <c:if test="${not empty command.allTranscodings}">
                            <dt><fmt:message key="playersettings.transcodings"/></dt>
                            <dd>
                                <c:forEach items="${command.allTranscodings}" var="transcoding" varStatus="loopStatus">
                                    <form:checkbox path="activeTranscodingIds" id="transcoding${transcoding.id}" value="${transcoding.id}" cssClass="checkbox"/>
                                    <label for="transcoding${transcoding.id}">${transcoding.name}</label>
                                </c:forEach>
                            </dd>
                        </c:if>
                    </c:if>

                    <dt></dt>
                    <dd>
                        <form:checkbox path="dynamicIp" id="dynamicIp" cssClass="checkbox"/>
                        <label for="dynamicIp"><fmt:message key="playersettings.dynamicip"/></label>
                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="dynamicip"/></c:import>
                    </dd>
                    <c:if test="${not (command.guest or command.anonymous)}">
                        <dt></dt>
                        <dd>
                            <form:checkbox path="autoControlEnabled" id="autoControlEnabled" cssClass="checkbox"/>
                            <label for="autoControlEnabled"><fmt:message key="playersettings.autocontrol"/></label>
                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="autocontrol"/></c:import>
                        </dd>
                        <dt></dt>
                        <dd>
                            <form:checkbox path="m3uBomEnabled" id="m3uBomEnabled" cssClass="checkbox"/>
                            <label for="m3uBomEnabled"><fmt:message key="playersettings.m3ubom"/></label>
                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="m3ubom"/></c:import>
                        </dd>
                    </c:if>
                    <dt><fmt:message key="playersettings.lastseen"/></dt>
                    <dd><fmt:formatDate value="${command.lastSeen}" type="both" dateStyle="long" timeStyle="medium"/></dd>
                </dl>
            </details>
    
            <c:url value="playerSettings.view" var="deleteUrl">
                <c:param name="delete" value="${command.playerId}"/>
            </c:url>
            <c:url value="playerSettings.view" var="cloneUrl">
                <c:param name="clone" value="${command.playerId}"/>
            </c:url>

            <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />
            <details ${isOpen}>
                <summary><fmt:message key="playersettings.deleteandclone"/></summary>
                <dl>
                    <dt><fmt:message key="playersettings.forget"/></dt>
                    <dd><div><input type="button" onClick="location.href='${deleteUrl}';window.top.playQueue.location.reload()" value="<fmt:message key='playersettings.forgetplayer'/>"/></div></dd>       
                    <dt><fmt:message key="playersettings.clone"/></dt>
                    <dd><div><input type="button" onClick="location.href='${cloneUrl}';window.top.playQueue.location.reload()" value="<fmt:message key='playersettings.cloneplayer'/>"/></div></dd>
                </dl>
            </details>

            <div class="submits">
                <input type="submit" value="<fmt:message key='common.save'/>">
                <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
            </div>

        </form:form>
    </c:otherwise>
</c:choose>

</body></html>
