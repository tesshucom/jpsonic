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
    viewAsList:
    directoryId:
  --%>

<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ include file="include.jsp" %>

<c:url value="${param.targetView}" var="changeViewUrl">
    <c:param name="id" value="${param.directoryId}"/>
    <c:param name="viewAsList" value="${not param.viewAsList}"/>
</c:url>
<ul class="controls">
    <c:choose>
        <c:when test="${param.viewAsList}">
            <li><span title="<fmt:message key='common.viewaslist'/>" class="control list disabled"><fmt:message key='common.viewaslist'/></span></li>
            <li><a href="${changeViewUrl}" title="<fmt:message key='common.viewasgrid'/>" class="control tile"><fmt:message key='common.viewasgrid'/></a></li>
        </c:when>
        <c:otherwise>
            <li><a href="${changeViewUrl}" title="<fmt:message key='common.viewaslist'/>" class="control list"><fmt:message key='common.viewaslist'/></a></li>
            <li><span title="<fmt:message key='common.viewasgrid'/>" class="control tile disabled"><fmt:message key='common.viewasgrid'/></span></li>
        </c:otherwise>
    </c:choose>
</ul>
