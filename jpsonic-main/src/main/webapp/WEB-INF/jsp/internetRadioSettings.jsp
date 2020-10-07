<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe settings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="internetRadio"/>
    <c:param name="toast" value="${model.showToast}"/>
    <c:param name="useRadio" value="${model.useRadio}"/>
    <c:param name="useSonos" value="${model.useSonos}"/>
</c:import>

<form method="post" action="internetRadioSettings.view">
	<sec:csrfInput />

	<c:if test="${not empty model.internetRadios}">
	    <details open>
	        <summary><fmt:message key="transcodingsettings.registered"/></summary>
			<table class="tabular internet-radio">
				<thead>
				    <tr>
				        <th><fmt:message key="internetradiosettings.name"/></th>
				        <th><fmt:message key="sharesettings.details"/></th>
				        <th><fmt:message key="internetradiosettings.enabled"/></th>
				        <th><fmt:message key="common.delete"/></th>
				    </tr>
				</thead>
				<tbody>
				    <c:forEach items="${model.internetRadios}" var="radio">
				        <tr>
				            <td><input type="text" name="name[${radio.id}]" size="20" value="${radio.name}"/></td>
				            <td>
				            	<dl>
				        			<dt><fmt:message key="internetradiosettings.streamurl"/></dt>
						            <dd><input type="text" name="streamUrl[${radio.id}]" size="40" value="${radio.streamUrl}"/></dd>
							        <dt><fmt:message key="internetradiosettings.homepageurl"/></dt>
						            <dd><input type="text" name="homepageUrl[${radio.id}]" size="40" value="${radio.homepageUrl}"/></dd>
						        </dl>
					        </td>
				            <td><input type="checkbox" ${radio.enabled ? "checked" : ""} name="enabled[${radio.id}]" /></td>
				            <td><input type="checkbox" name="delete[${radio.id}]" /></td>
				        </tr>
				    </c:forEach>
				</tbody>
			</table>
		</details>
	</c:if>

    <details ${model.isOpenDetailSetting or empty model.internetRadios ? "open" : ""}>
		<summary><fmt:message key="internetradiosettings.add"/></summary>

		<table class="tabular internet-radio">
			<thead>
			    <tr>
			        <th><fmt:message key="internetradiosettings.name"/></th>
			        <th><fmt:message key="sharesettings.details"/></th>
			        <th><fmt:message key="internetradiosettings.enabled"/></th>
			    </tr>
			</thead>
			<tbody>
			    <tr>
			        <td><input type="text" name="name" size="20" placeholder="<fmt:message key='internetradiosettings.name'/>"/></td>
			        <td>
						<dl>
					        <dt><fmt:message key="internetradiosettings.streamurl"/></dt>
							<dd><input type="text" name="streamUrl" size="40" placeholder="<fmt:message key='internetradiosettings.streamurl'/>"/></dd>
							<dt><fmt:message key="internetradiosettings.homepageurl"/></dt>
				        	<dd><input type="text" name="homepageUrl" size="40" placeholder="<fmt:message key='internetradiosettings.homepageurl'/>"/></dd>
				        </dl>
					</td>
			        <td><input name="enabled" checked type="checkbox"/></td>
			    </tr>
			</tbody>
		</table>
	</details>

    <div class="submits">
        <input type="submit" value="<fmt:message key='common.save'/>">
        <input type="button" onClick="location.href='nowPlaying.view'" value="<fmt:message key='common.cancel'/>"/>
    </div>

</form>

<c:if test="${not empty error}">
    <p><strong><fmt:message key="${error}"/></strong></p>
</c:if>

<c:if test="${settings_reload}">
    <script>
      window.top.reloadUpper("internetRadioSettings.view");
    </script>
</c:if>

</body></html>
