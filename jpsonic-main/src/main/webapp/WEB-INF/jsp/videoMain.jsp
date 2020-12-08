<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--
  ~ This file is part of Airsonic.
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
  ~  Copyright 2014 (C) Sindre Mehus
  --%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script src="<c:url value='/script/utils.js'/>"></script>

    <script>
        var image;
        var id;
        var duration;
        var timer;
        var offset;
        var step;
        var size = 120;

        function startPreview(img, id, duration) {
            stopPreview();
            image = $(img);
            step = Math.max(5, Math.round(duration / 50));
            offset = step;
            this.id = id;
            this.duration = duration;
            updatePreview();
            timer = window.setInterval(updatePreview, 1000);
        }

        function updatePreview() {
            image.attr("src", "coverArt.view?id=" + id + "&size=" + size + "&offset=" + offset);
            offset += step;
            if (offset > duration) {
                stopPreview();
            }
        }

        function stopPreview() {
            if (timer != null) {
                window.clearInterval(timer);
                timer = null;
            }
            if (image != null) {
                image.attr("src", "coverArt.view?id=" + id + "&size=" + size);
            }
        }
        function showAllAlbums() {
            window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
        }
    </script>

</head>
<body class="mainframe videoMain">

<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
    <h1 class="movie">${fn:escapeXml(model.dir.name)}</h1>
</section>

<div class="actions">

    <c:if test="${not model.partyMode}">
        <ul class="controls">
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl">
                    <sub:param name="id" value="${model.parent.id}"/>
                </sub:url>
                <li><a title="<fmt:message key='main.up'/>" href="${upUrl}" class="control up"><fmt:message key="main.up"/></a></li>
            </c:if>
        </ul>
    </c:if>

    <c:import url="viewAsListSelector.jsp">
        <c:param name="targetView" value="main.view"/>
        <c:param name="viewAsList" value="${model.viewAsList}"/>
        <c:param name="directoryId" value="${model.dir.id}"/>
    </c:import>
    <c:if test="${model.thereIsMore}">
        <ul class="controls">
            <li><a href="javascript:showAllAlbums()" title="<fmt:message key='main.showall'/>" class="control all"><fmt:message key='main.showall'/></a></li>
        </ul>
    </c:if>

</div>

<c:choose>
    <c:when test="${model.viewAsList}">
        <table class="tabular songs">
            <thead>
                <tr>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                </tr>
            </thead>
            <tbody>    
                <c:forEach items="${model.subDirs}" var="subDir" varStatus="loopStatus">
                    <tr>
                        <td></td>
                        <td></td>
                        <td></td>
                        <td></td>
                        <td colspan="5">
                            <a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a>
                        </td>
                    </tr>
                </c:forEach>
                <c:forEach items="${model.files}" var="child">
                    <c:url value="/videoPlayer.view" var="videoUrl">
                        <c:param name="id" value="${child.id}"/>
                    </c:url>
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${child.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                            <c:param name="downloadEnabled" value="${model.user.downloadRole and model.showDownload}"/>
                            <c:param name="video" value="${child.video}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <c:if test="${!(model.user.downloadRole and model.showDownload and not model.partyMode)}"><td></td></c:if>
                        <td class="song"><a href="javascript:top.onOpenDialogVideoPlayer('${videoUrl}')" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</a></td>
                        <td class="year">${child.year}</td>
                        <td class="format">${fn:toLowerCase(child.format)}</td>
                        <td class="size"><sub:formatBytes bytes="${child.fileSize}"/></td>
                        <td class="duration">${child.durationString}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:when>
    <c:otherwise>
        <div class="coverart-container">
            <c:forEach items="${model.files}" var="child">
                <c:url value="/videoPlayer.view" var="videoUrl">
                    <c:param name="id" value="${child.id}"/>
                </c:url>
                <c:url value="/coverArt.view" var="coverArtUrl">
                    <c:param name="id" value="${child.id}"/>
                    <c:param name="size" value="120"/>
                </c:url>
                <div class="albumThumb">
                    <div class="coverart">
                        <div>
                            <div>
                                <a href="javascript:top.onOpenDialogVideoPlayer('${videoUrl}')" title="${fn:escapeXml(child.name)}">
                                    <img src="${coverArtUrl}"
                                        onmouseover="startPreview(this, ${child.id}, ${child.durationSeconds})"
                                        onmouseout="stopPreview()"></a>
                            </div>
                        </div>
                        <div class="caption1" title="${fn:escapeXml(child.name)}">
                            <a href="javascript:top.onOpenDialogVideoPlayer('${videoUrl}')" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</a>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

</body>
</html>
