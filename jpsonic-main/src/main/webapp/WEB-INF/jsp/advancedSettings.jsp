<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.AdvancedSettingsCommand"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>

function resetLoggingControl() {
    $('[name="verboseLogStart"]').prop('checked', true);
    $('[name="verboseLogScanning"]').prop('checked', true);
    $('[name="verboseLogPlaying"]').prop('checked', false);
    $('[name="verboseLogShutdown"]').prop('checked', true);
}

function resetBandwidth() {
    $('[name="downloadLimit"]').val(0);
    $('[name="uploadLimit"]').val(0);
    $('[name="bufferSize"]').prop("selectedIndex", 1);
}

</script>
</head>

<body class="mainframe settings advancedSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="advanced"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="useSonos" value="${command.useSonos}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="advancedSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<form:form method="post" action="advancedSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />

    <details open>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetLoggingControl()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="advancedsettings.loggingcontroloutline"/>
            </div>
        </c:if>

        <summary class="jpsonic"><fmt:message key="advancedsettings.loggingcontrol"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="verboseLogStart" id="verboseLogStart"/>
                <label for="verboseLogStart"><fmt:message key="advancedsettings.verboselogstart"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="verboseLogScanning" id="verboseLogScanning"/>
                <label for="verboseLogScanning"><fmt:message key="advancedsettings.verboselogscanning"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="verboseLogPlaying" id="verboseLogPlaying"/>
                <label for="verboseLogPlaying"><fmt:message key="advancedsettings.verboselogplaying"/></label>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="verboseLogShutdown" id="verboseLogShutdown"/>
                <label for="verboseLogShutdown"><fmt:message key="advancedsettings.verboselogshutdown"/></label>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>

        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:resetBandwidth()" title="<fmt:message key='common.reset'/>" class="control reset"><fmt:message key="common.reset"/></a></li>
            </ul>
        </div>

        <summary class="jpsonic"><fmt:message key="advancedsettings.bandwidth"/></summary>
        <dl>
            <dt><fmt:message key="advancedsettings.downloadlimit"/></dt>
            <dd>
                <form:input path="downloadLimit"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="downloadlimit"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.uploadlimit"/></dt>
            <dd>
                <form:input path="uploadLimit"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="uploadlimit"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.buffersize"/></dt>
            <dd>
                <form:select path="bufferSize">
                    <c:forEach begin="1" end="16" var="base">
                        <form:option value="${base * 2048}" />
                    </c:forEach>
                </form:select>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="advancedsettings.email"/></summary>
        <dl>
            <dt><fmt:message key="advancedsettings.smtpFrom"/></dt>
            <dd>
                <form:input path="smtpFrom"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpFrom"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpServer"/></dt>
            <dd>
                <form:input path="smtpServer"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpServer"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpPort"/></dt>
            <dd>
                <form:input path="smtpPort"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpPort"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpEncryption"/></dt>
            <dd>
                <form:select path="smtpEncryption">
                    <fmt:message key="advancedsettings.smtpEncryption.none" var="none"/>
                    <fmt:message key="advancedsettings.smtpEncryption.starttls" var="starttls"/>
                    <fmt:message key="advancedsettings.smtpEncryption.ssl" var="ssl"/>
                    <form:option value="None" label="${none}"/>
                    <form:option value="STARTTLS" label="${starttls}"/>
                    <form:option value="SSL/TLS" label="${ssl}"/>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpEncryption"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpUser"/></dt>
            <dd>
                <form:input path="smtpUser"/>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpPassword"/></dt>
            <dd>
                <form:password path="smtpPassword"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpCredentials"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="advancedsettings.ldap"/></summary>
        <p><strong><fmt:message key="advancedsettings.ldapRequiresRestart"/></strong></p>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="ldapEnabled" id="ldap"/>
                <label for="ldap"><fmt:message key="advancedsettings.ldapenabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldap"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapurl" /></dt>
            <dd>
                <form:input path="ldapUrl"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapurl" /></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapsearchfilter" /></dt>
            <dd>
                <form:input path="ldapSearchFilter"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapsearchfilter" /></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapmanagerdn" /></dt>
            <dd><form:input path="ldapManagerDn"/></dd>
            <dt><fmt:message key="advancedsettings.ldapmanagerpassword" /></dt>
            <dd>
                <form:password path="ldapManagerPassword"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapmanagerdn" /></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="ldapAutoShadowing" id="ldapAutoShadowing" cssClass="checkbox" />
                <label for="ldapAutoShadowing"><fmt:message key="advancedsettings.ldapautoshadowing">
                    <fmt:param value="${command.brand}" /></fmt:message>
                </label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapautoshadowing" /></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>
        <summary><fmt:message key="advancedsettings.accountrecovery"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="captchaEnabled" id="enablecaptcha"/>
                <label for="enablecaptcha"><fmt:message key="advancedsettings.enableCaptcha"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="captcha"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.recaptchaSiteKey"/></dt>
            <dd>
                <form:input path="recaptchaSiteKey"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="recaptchaSiteKey"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.recaptchaSecretKey"/></dt>
            <dd>
                <form:input path="recaptchaSecretKey"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="recaptchaSecretKey"/></c:import>
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
        window.top.reloadPlayQueue();
        window.top.reloadUpper("advancedSettings.view");
    </script>
</c:if>

</body></html>
