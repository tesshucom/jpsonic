<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Airsonic.
  ~
  ~ Airsonic is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ Airsonic is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ Copyright 2013 (C) Sindre Mehus
  --%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="dlnaSettings.view">
    <sec:csrfInput />

    <table style="white-space:nowrap" class="indent">

        <tr>
            <td><fmt:message key="dlnasettings.basic"/></td>
            <td></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaEnabled" id="dlnaEnabled" ${model.dlnaEnabled? "checked": ""}/>
                <label for="dlnaEnabled"><fmt:message key="dlnasettings.enabled"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaenable"/></c:import>
            </td>
        </tr>

        <tr>
            <td></td>
            <td></td>
            <td><fmt:message key="dlnasettings.servername"/></td>
            <td>
                <input name="dlnaServerName" id="dlnaServerName" size="40" value="<c:out value='${model.dlnaServerName}' escapeXml='true'/>"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaservername"/></c:import>
            </td>
        </tr>

        <tr>
            <td></td>
            <td></td>
            <td><label for="dlnaBaseLANURL" ><fmt:message key="dlnasettings.baselanurl"/></label></td>
            <td>
                <input type="text" size="50" name="dlnaBaseLANURL" id="dlnaBaseLANURL" value="<c:out value='${model.dlnaBaseLANURL}' />" />
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnalanurl"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="dlnasettings.search"/></td>
            <td><img src="<spring:theme code="domestic"/>" class="domestic" alt=""></td>
            <td colspan="2">
                <input type="checkbox" name="dlnaFileStructureSearch" id="dlnaFileStructureSearch" ${model.dlnaFileStructureSearch? "checked": ""}/>
                <label for="dlnaFileStructureSearch"><fmt:message key="dlnasettings.filestructuresearch"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="dlnaFileStructureSearch"/></c:import>
            </td>
        </tr>

        <tr>
            <td colspan="4" style="padding-top:1.5em">
                <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
                <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
            </td>
        </tr>
        
    </table>

</form>

</body></html>
