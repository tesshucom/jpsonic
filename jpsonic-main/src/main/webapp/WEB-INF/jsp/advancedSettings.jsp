<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.AdvancedSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="advanced"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form method="post" action="advancedSettings.view" modelAttribute="command">

    <details>
        <summary><fmt:message key="advancedsettings.bandwidth"/></summary>
        <dl>
            <dt><fmt:message key="advancedsettings.downloadlimit"/></dt>
            <dd>
                <form:input path="downloadLimit" size="8"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="downloadlimit"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.uploadlimit"/></dt>
            <dd>
                <form:input path="uploadLimit" size="8"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="uploadlimit"/></c:import>
            </dd>
        </dl>
    </details>

    <details>
        <summary><fmt:message key="advancedsettings.email"/></summary>
        <dl>
            <dt><fmt:message key="advancedsettings.smtpFrom"/></dt>
            <dd>
                <form:input path="smtpFrom" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpFrom"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpServer"/></dt>
            <dd>
                <form:input path="smtpServer" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpServer"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpPort"/></dt>
            <dd>
                <form:input path="smtpPort" size="5"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpPort"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpEncryption"/></dt>
            <dd>
                <form:select path="smtpEncryption" cssStyle="width:8em">
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
                <form:input path="smtpUser" size="20"/>
            </dd>
            <dt><fmt:message key="advancedsettings.smtpPassword"/></dt>
            <dd>
                <form:password path="smtpPassword" size="20"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpCredentials"/></c:import>
            </dd>
        </dl>
    </details>

    <details>
        <summary><fmt:message key="advancedsettings.ldap"/></summary>
        <p class="warning"><fmt:message key="advancedsettings.ldapRequiresRestart"/></p>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="ldapEnabled" id="ldap"/>
                <label for="ldap"><fmt:message key="advancedsettings.ldapenabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldap"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapurl" /></dt>
            <dd>
                <form:input path="ldapUrl" size="70" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapurl" /></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapsearchfilter" /></dt>
            <dd>
                <form:input path="ldapSearchFilter" size="70" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapsearchfilter" /></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.ldapmanagerdn" /></dt>
            <dd><form:input path="ldapManagerDn" size="20" /></dd>
            <dt><fmt:message key="advancedsettings.ldapmanagerpassword" /></dt>
            <dd>
                <form:password path="ldapManagerPassword" size="20" />
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

    <details>
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
                <form:input path="recaptchaSiteKey" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="recaptchaSiteKey"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.recaptchaSecretKey"/></dt>
            <dd>
                <form:input path="recaptchaSecretKey" size="50"/>
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
    <script language="javascript" type="text/javascript">
        parent.frames.left.location.href="left.view?";
        parent.frames.playQueue.location.href="playQueue.view?";
    </script>
</c:if>

</body></html>
