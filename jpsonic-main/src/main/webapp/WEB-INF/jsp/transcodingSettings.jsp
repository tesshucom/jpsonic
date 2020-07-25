<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
</head>
<body class="mainframe settings">

    <c:import url="settingsHeader.jsp">
        <c:param name="cat" value="transcoding" />
        <c:param name="toast" value="${settings_toast}" />
        <c:param name="useRadio" value="${model.useRadio}"/>
        <c:param name="useSonos" value="${model.useSonos}"/>
    </c:import>

    <form method="post" action="transcodingSettings.view">
        <sec:csrfInput />

        <c:set var="isOpen" value='${model.isOpenDetailSetting ? "open" : ""}' />
        <details open>
            <summary><fmt:message key="transcodingsettings.registered"/></summary>

            <table class="tabular transcoding">
                <thead>
                    <tr>
                        <th><fmt:message key="transcodingsettings.name" /></th>
                        <th><fmt:message key="sharesettings.details" /></th>
                        <th style="padding-left: 1em"><fmt:message key="common.delete" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.transcodings}" var="transcoding">
                        <tr>
                            <td><input name="name[${transcoding.id}]" size="10" value="${fn:escapeXml(transcoding.name)}" /></td>
                            <td>
                                <dl>
                                    <dt><fmt:message key="transcodingsettings.sourceformat" /> / <fmt:message key="transcodingsettings.targetformat" /></dt>
                                    <dd><input name="sourceFormats[${transcoding.id}]" size="36" value="${transcoding.sourceFormats}" /> / <input name="targetFormat[${transcoding.id}]" size="10" value="${transcoding.targetFormat}" /></dd>
                                    <dt><fmt:message key="transcodingsettings.step1" /></dt>
                                    <dd><input name="step1[${transcoding.id}]" size="60" value="${transcoding.step1}" /></dd>
                                    <dt><fmt:message key="transcodingsettings.step2" /></dt>
                                    <dd><input name="step2[${transcoding.id}]" size="22" value="${transcoding.step2}" /></dd>
                                </dl>
                            </td>
                            <td><input type="checkbox" name="delete[${transcoding.id}]" /></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </details>

        <details ${isOpen}>
            <summary><fmt:message key="transcodingsettings.add"/></summary>

            <table class="tabular transcoding">
                <tr>
                    <td><input name="name" size="10" placeholder="<fmt:message key='transcodingsettings.name'/>" value="${fn:escapeXml(newTranscoding.name)}" /></td>
                    <td><input name="sourceFormats" size="36" placeholder="<fmt:message key='transcodingsettings.sourceformat'/>" value="${newTranscoding.sourceFormats}" /></td>
                    <td><input name="targetFormat" size="10" placeholder="<fmt:message key='transcodingsettings.targetformat'/>" value="${newTranscoding.targetFormat}" /></td>
                    <td><input name="step1" size="60" placeholder="<fmt:message key='transcodingsettings.step1'/>" value="${newTranscoding.step1}" /></td>
                    <td><input name="step2" size="22" placeholder="<fmt:message key='transcodingsettings.step2'/>" value="${newTranscoding.step2}" /></td>
                </tr>
                <tr>
                    <td colspan="5">
                        <input type="checkbox" id="defaultActive" name="defaultActive" checked />
                        <label for="defaultActive"><fmt:message key="transcodingsettings.defaultactive" /></label>
                    </td>
                </tr>
            </table>
        </details>

        <details ${isOpen}>
            <summary><fmt:message key="advancedsettings.hlscommand" /></summary>
            <input type="text" name="hlsCommand" size="100" value="${model.hlsCommand}" />
            <c:import url="helpToolTip.jsp">
                <c:param name="topic" value="hlscommand" />
            </c:import>
        </details>

        <div class="submits">
            <input type="submit" value="<fmt:message key='common.save'/>">
            <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
        </div>

    </form>

    <p>
        <a href="https://airsonic.github.io/docs/transcode/" target="_blank" rel="noopener noreferrer"><fmt:message key="transcodingsettings.recommended" /></a>
    </p>

    <c:if test="${not empty error}">
        <p class="warning">
            <fmt:message key="${error}" />
        </p>
    </c:if>

    <div style="width: 80%">
        <fmt:message key="transcodingsettings.info">
            <fmt:param value="${model.transcodeDirectory}" />
            <fmt:param value="${model.brand}" />
        </fmt:message>
    </div>
</body>
</html>
