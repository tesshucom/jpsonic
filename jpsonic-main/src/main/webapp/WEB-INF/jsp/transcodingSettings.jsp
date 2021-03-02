<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp"%>
<%@ include file="jquery.jsp"%>
</head>
<body class="mainframe settings transcodingSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="transcoding" />
    <c:param name="toast" value="${settings_toast}" />
    <c:param name="useRadio" value="${model.useRadio}"/>
    <c:param name="useSonos" value="${model.useSonos}"/>
    <c:param name="existsShare" value="${model.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="transcodingSettings.view"/>
    <c:param name="showOutlineHelp" value="${model.showOutlineHelp}"/>
</c:import>

<c:if test="${model.showOutlineHelp}">
    <div class="outlineHelp">
        <fmt:message key="transcodingsettings.info">
            <fmt:param value="${model.transcodeDirectory}" />
            <fmt:param value="${model.brand}" />
        </fmt:message>
  <p>
      <a href="https://airsonic.github.io/docs/transcode/" target="_blank" rel="noopener noreferrer"><fmt:message key="transcodingsettings.recommended" /></a>
  </p>
    </div>
</c:if>

<form method="post" action="transcodingSettings.view">
    <sec:csrfInput />

    <c:if test="${not empty model.transcodings}">
        <details open>
            <summary><fmt:message key="transcodingsettings.registered"/></summary>

            <table class="tabular transcoding">
                <thead>
                    <tr>
                        <th><fmt:message key="transcodingsettings.name" /></th>
                        <th><fmt:message key="sharesettings.details" /></th>
                        <th><fmt:message key="common.delete" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.transcodings}" var="transcoding">
                        <tr>
                            <td><input type="text" name="name[${transcoding.id}]" value="${fn:escapeXml(transcoding.name)}"/></td>
                            <td>
                                <dl>
                                    <dt><fmt:message key="transcodingsettings.sourceformat" /> / <fmt:message key="transcodingsettings.targetformat" /></dt>
                                    <dd><input type="text" name="sourceFormats[${transcoding.id}]" value="${transcoding.sourceFormats}" /> / <input type="text" name="targetFormat[${transcoding.id}]" value="${transcoding.targetFormat}" /></dd>
                                    <dt><fmt:message key="transcodingsettings.step1" /></dt>
                                    <dd><input type="text" name="step1[${transcoding.id}]" value="${transcoding.step1}" /></dd>
                                    <dt><fmt:message key="transcodingsettings.step2" /></dt>
                                    <dd><input type="text" name="step2[${transcoding.id}]" value="${transcoding.step2}" /></dd>
                                </dl>
                            </td>
                            <td><input type="checkbox" name="delete[${transcoding.id}]" /></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </details>
    </c:if>

    <details ${model.isOpenDetailSetting or empty model.transcodings ? "open" : ""}>
        <summary><fmt:message key="transcodingsettings.add"/></summary>

        <table class="tabular transcoding">
            <thead>
                <tr>
                    <th><fmt:message key="transcodingsettings.name" /></th>
                    <th><fmt:message key="sharesettings.details" /></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><input type="text" name="name" value="${fn:escapeXml(newTranscoding.name)}" /></td>
                    <td>
                        <dl>
                            <dt><fmt:message key="transcodingsettings.sourceformat" /> / <fmt:message key="transcodingsettings.targetformat" /></dt>
                            <dd><input type="text" name="sourceFormats" value="${newTranscoding.sourceFormats}" /> / <input name="targetFormat" value="${newTranscoding.targetFormat}" /></dd>
                            <dt><fmt:message key="transcodingsettings.step1" /></dt>
                            <dd><input type="text" name="step1" value="${newTranscoding.step1}" /></dd>
                            <dt><fmt:message key="transcodingsettings.step2" /></dt>
                            <dd><input type="text" name="step2" value="${newTranscoding.step2}" /></dd>
                        </dl>
                        <input type="checkbox" id="defaultActive" name="defaultActive" checked />
                        <label for="defaultActive"><fmt:message key="transcodingsettings.defaultactive" /></label>
                    </td>
                </tr>
            </tbody>
        </table>
    </details>

    <details ${model.isOpenDetailSetting ? "open" : ""}>
        <summary><fmt:message key="advancedsettings.hlscommand" /></summary>
        <div class="hls">
            <input type="text" name="hlsCommand" value="${model.hlsCommand}" />
            <c:import url="helpToolTip.jsp">
                <c:param name="topic" value="hlscommand" />
            </c:import>
        </div>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form>

<c:if test="${not empty error}">
    <p><strong><fmt:message key="${error}" /></strong></p>
</c:if>

</body>
</html>
