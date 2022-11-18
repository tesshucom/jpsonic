<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script>
        function hideGettingStarted() {
            <fmt:message key="gettingStarted.hidealert" var="msg" />
            alert("${fn:replace(msg, '\"', '\\"')}");
            location.href = "gettingStarted.view?hide";
        }
    </script>
</head>
<body class="mainframe gettingStarted">

<section>
    <h1 class="home"><fmt:message key="gettingStarted.title"/></h1>
</section>

<fmt:message key="gettingStarted.text"/>

<dl>
    <dt>
        <form:form name="localeForm" method="post" action="gettingStarted.view" modelAttribute="command">
            <fmt:message key="generalsettings.language"/>
            <form:select path="localeIndex" onChange="document.forms['localeForm'].submit();">
                <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                    <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                </c:forEach>
            </form:select>
        </form:form>
    </dt>
    <dd><fmt:message key="gettingStarted.step0.text"/></dd>
    <dt><a href="userSettings.view?userIndex=0"><fmt:message key="gettingStarted.step1.title"/></a></dt>
    <dd><fmt:message key="gettingStarted.step1.text"/></dd>
    <dt><a href="musicFolderSettings.view"><fmt:message key="gettingStarted.step2.title"/></a></dt>
    <dd><fmt:message key="gettingStarted.step2.text"/></dd>
    <dt><fmt:message key="gettingStarted.step3.title"/></dt>
    <dd><fmt:message key="gettingStarted.step3.text"/></dd>
</dl>

<c:if test="${model.runningAsRoot}">
    <h2><strong><fmt:message key="gettingStarted.root"/></strong></h2>
</c:if>

<p><a href="javascript:hideGettingStarted()"><fmt:message key="gettingStarted.hide"/></a></p>

<c:if test="${settings_reload}">
    <script>
      window.top.reloadUpper("gettingStarted.view");
      window.top.reloadPlayQueue();
    </script>
</c:if>

</body></html>
