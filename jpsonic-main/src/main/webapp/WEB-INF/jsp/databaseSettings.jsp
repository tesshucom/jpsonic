<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="com.tesshu.jpsonic.command.DatabaseSettingsCommand"--%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script>
        function updateShownOptions() {
            $(".hideawayDatabaseOptions").hide();
            var value = $('select#configType').val();
            var objToShow = $("#" + value + "DatabaseOptions");
            if (objToShow.length) {
                objToShow.show();
            }
            if(value != 'LEGACY') {
                $("#nonLEGACYDatabaseOptions").show();
            }
        }

        $(document).ready(function () {
            updateShownOptions();
            $('select#configType').on('change', function () {
                updateShownOptions();
            });
        });
    </script>
</head>
<body class="mainframe settings databaseSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="database"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${command.useRadio}"/>
    <c:param name="existsShare" value="${command.shareCount ne 0}"/>
</c:import>

<c:import url="outlineHelpSelector.jsp">
    <c:param name="targetView" value="databaseSettings.view"/>
    <c:param name="showOutlineHelp" value="${command.showOutlineHelp}"/>
</c:import>

<c:if test="${command.showOutlineHelp}">
    <div class="outlineHelp">
	    <strong><fmt:message key="databasettings.restartRequired"/></strong><br>
	    <fmt:message key="databasesettings.moreinfo"/>
    </div>
</c:if>

<form:form modelAttribute="command" action="databaseSettings.view" method="post">

    <dl class="topSelectorContainer2">
        <dt><fmt:message key="databasesettings.configtype"/></dt>
        <dd>
            <form:select path="configType" id="configType">
	            <form:option value="LEGACY" label="Legacy"/>
	            <form:option value="EMBED" label="Embedded JDBC"/>
	            <form:option value="JNDI" label="JNDI"/>
	        </form:select>
	        <c:import url="helpToolTip.jsp"><c:param name="topic" value="databaseConfigType"/></c:import>
        </dd>
    </dl>

    <dl id="EMBEDDatabaseOptions" class="hideawayDatabaseOptions">
        <dt><fmt:message key="databasesettings.embeddriver"/></dt>
        <dd>
            <form:input path="embedDriver"/>
            <c:import url="helpToolTip.jsp"><c:param name="topic" value="embeddriver"/></c:import>
        </dd>
        <dt><fmt:message key="databasesettings.embedurl"/></dt>
        <dd><form:input path="embedUrl"/></dd>
        <dt><fmt:message key="databasesettings.embedusername"/></dt>
        <dd><form:input path="embedUsername"/></dd>
        <dt><fmt:message key="databasesettings.embedpassword"/></dt>
        <dd><form:input path="embedPassword"/></dd>
    </dl>

    <dl id="JNDIDatabaseOptions" class="hideawayDatabaseOptions single">
        <dt><fmt:message key="databasesettings.jndiname"/></dt>
        <dd><form:input path="JNDIName"/><c:import url="helpToolTip.jsp"><c:param name="topic" value="jndiname"/></c:import></dd>
    </dl>

    <dl id="nonLEGACYDatabaseOptions" class="hideawayDatabaseOptions">
        <dt><fmt:message key="databasesettings.mysqlvarcharmaxlength"/></dt>
        <dd>
            <form:input path="mysqlVarcharMaxlength"/>
            <c:import url="helpToolTip.jsp"><c:param name="topic" value="mysqlvarcharmaxlength"/></c:import>
        </dd>
        <dt><fmt:message key="databasesettings.usertablequote"/></dt>
        <dd>
            <form:input path="usertableQuote" htmlEscape="true"/>
            <c:import url="helpToolTip.jsp"><c:param name="topic" value="usertablequote"/></c:import>
        </dd>
    </dl>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form:form>

</body>
</html>
