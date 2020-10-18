<%--
  ~ This file is part of Jpsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2020 (C) tesshucom
  
  <%--
    PARAMETERS
    targetView: View name to be switched.
    showOutlineHelp:
  --%>

<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ include file="include.jsp" %>

<c:url value="${param.targetView}" var="changeViewUrl">
    <c:param name="showOutlineHelp" value="${not param.showOutlineHelp}"/>
</c:url>
<ul class="controls">
    <c:choose>
        <c:when test="${param.showOutlineHelp}">
            <li><a href="${changeViewUrl}" title="<fmt:message key='common.noverbosehelp'/>" class="control outline"><fmt:message key='common.noverbosehelp'/></a></li>
            <li><span title="<fmt:message key='common.verbosehelp'/>" class="control working-outline disabled"><fmt:message key='common.verbosehelp'/></span></li>
        </c:when>
        <c:otherwise>
            <li><span title="<fmt:message key='common.noverbosehelp'/>" class="control outline disabled"><fmt:message key='common.noverbosehelp'/></span></li>
            <li><a href="${changeViewUrl}" title="<fmt:message key='common.verbosehelp'/>" class="control working-outline"><fmt:message key='common.verbosehelp'/></a></li>
        </c:otherwise>
    </c:choose>
</ul>
