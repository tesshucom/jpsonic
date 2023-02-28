<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.AdvancedSettingsCommand"--%>

<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ page import="com.tesshu.jpsonic.domain.IndexScheme" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script>

function resetBandwidth() {
    $('[name="downloadLimit"]').val(0);
    $('[name="uploadLimit"]').val(0);
    $('[name="bufferSize"]').prop("selectedIndex", 1);
}

function onUseScanLogStateChanged() {
    if($("#usescanlog").prop("checked")){
        $("#scanlogretention").prop('disabled', false);
        $("#usescanevents").prop({'disabled': false});
        $("#measurememory").prop({'disabled': false});
    } else {
        $("#scanlogretention").prop('disabled', true);
        $("#scanlogretention").val(-1);
        $("#usescanevents").prop({'disabled': true, 'checked': false});
        $("#measurememory").prop({'disabled': true, 'checked': false});
    }
}

document.addEventListener('DOMContentLoaded', function () {

    onUseScanLogStateChanged();
    $("#usescanlog").on('change', function(e){
        onUseScanLogStateChanged();
    });

    $("#radio1-1").on('change', function(e){
        $("#readGreekInJapanese").prop({'disabled': false, 'checked': true});
        $("#forceInternalValueInsteadOfTags").prop({'disabled': true, 'checked': false});
    });
    $("#radio1-2").on('change', function(e){
        $("#readGreekInJapanese").prop({'disabled': true, 'checked': false});
        $("#forceInternalValueInsteadOfTags").prop({'disabled': false, 'checked': true});
    });
    $("#radio1-3").on('change', function(e){
        $("#readGreekInJapanese").prop({'disabled': true, 'checked': false});
        $("#forceInternalValueInsteadOfTags").prop({'disabled': true, 'checked': false});
    });
}, false);

</script>
</head>

<body class="mainframe settings advancedSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="advanced"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="advancedSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<form:form method="post" action="advancedSettings.view" modelAttribute="command">

    <c:set var="isOpen" value='${command.openDetailSetting ? "open" : ""}' />

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

    <details ${isOpen}>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="helppopup.scanlogoutline"/>
            </div>
        </c:if>

        <summary class="jpsonic"><fmt:message key="advancedsettings.scanlog"/></summary>
        <dl>
            <dt></dt>
            <dd>
                <form:checkbox path="useScanLog" id="usescanlog"/>
                <label for="usescanlog"><fmt:message key="advancedsettings.usescanlog"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="usescanlog"/></c:import>
            </dd>
            <dt><fmt:message key="advancedsettings.scanlogretention"/></dt>
            <dd>
                <form:select path="scanLogRetention" id="scanlogretention">
                    <fmt:message key="advancedsettings.retentionmin" var="latest"/>
                    <fmt:message key="advancedsettings.retention" var="week">
                        <fmt:param value="7" />
                    </fmt:message>
                    <fmt:message key="advancedsettings.retention" var="month">
                        <fmt:param value="30" />
                    </fmt:message>
                    <fmt:message key="advancedsettings.retention" var="halfYear">
                        <fmt:param value="180" />
                    </fmt:message>
                    <form:option value="-1" label="${latest}"/>
                    <form:option value="7" label="${week}"/>
                    <form:option value="30" label="${month}"/>
                    <form:option value="180" label="${halfYear}"/>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="scanlogretention"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="useScanEvents" id="usescanevents"/>
                <label for="usescanevents"><fmt:message key="advancedsettings.usescanevents"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="usescanevents"/></c:import>
            </dd>
            <dt></dt>
            <dd>
                <form:checkbox path="measureMemory" id="measurememory"/>
                <label for="measurememory"><fmt:message key="advancedsettings.measurememory"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="measurememory"/></c:import>
            </dd>
        </dl>
    </details>

    <details ${isOpen}>

        <c:if test="${command.showOutlineHelp}">
            <div class="outlineHelp">
                <fmt:message key="helppopup.indexschemeoutline"/>
            </div>
        </c:if>

        <summary class="jpsonic"><strong>Danger Zone</strong></summary>
        <dl class="single">
            <dt><strong><fmt:message key="advancedsettings.indexscheme"/></strong></dt>
            <dd class="scheme">
                <ul class="playerSettings">
                    <c:forEach items="${IndexScheme.values()}" var="indexScheme" varStatus="status">
                        <li>
                            <form:radiobutton class="technologyRadio" id="radio1-${status.count}" path="indexScheme" value="${indexScheme}"
                                checked="${indexScheme eq command.indexScheme.name() ? 'checked' : ''}"/>
                            <label for="radio1-${status.count}"><fmt:message key="advancedsettings.indexscheme.${fn:toLowerCase(indexScheme)}"/></label>
                            <c:import url="helpToolTip.jsp"><c:param name="topic" value="${fn:toLowerCase(indexScheme)}" /></c:import>
                            <c:if test='${"ROMANIZED_JAPANESE" eq indexScheme}'>
                                <li class="subItem">
                                    <form:checkbox path="forceInternalValueInsteadOfTags" id="forceInternalValueInsteadOfTags" cssClass="checkbox"/>
                                    <label for="forceInternalValueInsteadOfTags"><fmt:message key="advancedsettings.forceinternalvalueinsteadoftags"/></label>
                                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="forceinternalvalueinsteadoftags"/></c:import>
                                </li>
                            </c:if>                      
                        </li>
                    </c:forEach>
                </ul>
            </dd>
        </dl>
    </details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
    </div>

</form:form>

<c:if test="${settings_reload}">
    <script>
        window.top.reloadPlayQueue();
        window.top.reloadUpper("advancedSettings.view");
    </script>
</c:if>

</body></html>
