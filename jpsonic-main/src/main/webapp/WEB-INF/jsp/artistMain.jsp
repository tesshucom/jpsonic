<!DOCTYPE html>
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
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/interface/starService.js'/>"></script>
<script src="<c:url value='/dwr/interface/multiService.js'/>"></script>
<script src="<c:url value='/script/jquery.fancyzoom.js'/>"></script>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/script/jpsonic/onSceneChanged.js'/>"></script>
<script src="<c:url value='/script/jpsonic/coverartContainer.js'/>"></script>

<script>

var topSongs;

$(document).ready(function(){
  $("a.fancy").fancyZoom({
    minBorder: 30
  });
  <c:if test="${model.showArtistInfo or model.showTopSongs or model.showSimilar}">
    loadArtistInfo();
  </c:if>
});

function loadArtistInfo() {
  multiService.getArtistInfo(${model.dir.id}, 8, 50, function (artistInfo) {

    <c:if test="${model.showArtistInfo}">
      if (artistInfo.artistBio && artistInfo.artistBio.biography) {
        $(function() {
          $.when($("#artistBio").append(artistInfo.artistBio.biography))
            .done(function() {$("#artistDetails").removeClass("loading")});
        });
      }
    </c:if>

    <c:if test="${model.showTopSongs}">
      this.topSongs = artistInfo.topSongs;
      if (topSongs.length > 0) {
        $("#topSongs").show();

        // Delete all the rows except for the "pattern" row
        dwr.util.removeAllRows("topSongsBody", { filter:function(tr) {
          return (tr.id != "pattern");
        }});
  
        // Create a new set cloned from the pattern row
        for (var i = 0; i < topSongs.length; i++) {
          var song  = topSongs[i];
          var id = i + 1;
          dwr.util.cloneNode("pattern", { idSuffix:id });
          if (song.starred) {
              $("#starSong" + id).removeClass('star');
              $("#starSong" + id).addClass('star-fill');
          } else {
              $("#starSong" + id).removeClass('star-fill');
              $("#starSong" + id).addClass('star');
          }
          $("#rank" + id).text(i + 1);
          $("#title" + id).text(song.title);
          $("#title" + id).attr("title", song.title);
          $("#album" + id).text(song.album);
          $("#album" + id).attr("title", song.album);
          $("#albumUrl" + id).attr("href", "main.view?id=" + song.id);
          $("#artist" + id).text(song.artist);
          $("#artist" + id).attr("title", song.artist);
          $("#songDuration" + id).text(song.durationAsString);
  
          // Note: show() method causes page to scroll to top.
          $("#pattern" + id).css("display", "table-row");
        }
      }
    </c:if>

    <c:if test="${model.showSimilar}">
      if (artistInfo.similarArtists.length > 0) {
      let html = "<ul class=\"anchorList\">";
      artistInfo.similarArtists.forEach(function(v, i, a){
        html += "<li><a href='main.view?id=" + artistInfo.similarArtists[i].mediaFileId + "' target='main'>" + escapeHtml(artistInfo.similarArtists[i].artistName) + "</a></li>";
      });
      html += "</ul>";
      $("#similarArtists").append(html);
      $("#similar").show();
      $("#similarArtistsRadio").show();
      }
    </c:if>
  });
}

function toggleStarTopSong(index, imageId) {
  toggleStar(topSongs[index].id, imageId);
}

function toggleStar(mediaFileId, imageId) {
  if ("control star-fill" == $(imageId).attr('class')) {
    $(imageId).removeClass('star-fill');
    $(imageId).addClass('star');
    $(imageId).attr('title', '<fmt:message key="main.starredon"/>');
    starService.unstar(mediaFileId);
  } else if ("control star" == $(imageId).attr('class')) {
    $(imageId).removeClass('star');
    $(imageId).addClass('star-fill');
    $(imageId).attr('title', '<fmt:message key="main.starredoff"/>');
    starService.star(mediaFileId);
  }
}

function playAll() {
  top.playQueue.onPlay(${model.dir.id});
}
function playRandom() {
  top.playQueue.onPlayRandom(${model.dir.id}, 40);
}
function addAll() {
  top.playQueue.onAdd(${model.dir.id});
}
function playSimilar() {
  top.playQueue.onPlaySimilar(${model.dir.id}, 50);
}
function playAllTopSongs() {
  top.playQueue.onPlayTopSong(${model.dir.id});
}
function playTopSong(index) {
  top.playQueue.onPlayTopSong(${model.dir.id}, index);
}
function addTopSong(index) {
  top.playQueue.onAdd(topSongs[index].id);
  $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
}
function addNextTopSong(index) {
  top.playQueue.onAddNext(topSongs[index].id);
  $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
}
function showAllAlbums() {
  window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
}
function toggleComment() {
  $("#commentForm").toggle();
  $("#comment").toggle();
}
</script>

</head><body class="mainframe artistMain">

<%@ include file="mediafileBreadcrumb.jsp" %>

<section>
    <c:choose>
        <c:when test="${model.showArtistInfo}">
            <details id="artistDetails" class="loading">
                <summary><h1 class="artist">${fn:escapeXml(model.dir.name)}</h1></summary>
                    <%-- <a id="artistImageZoom" rel="zoom" href="void"><img id="artistImage"></a> --%>
                    <div id="artistBio"></div>
                    <c:if test="${model.useRadio eq true}">
                        <input id="similarArtistsRadio" type="button" value="<fmt:message key='main.startradio'/>" onclick="playSimilar()">
                    </c:if>
            </details>
        </c:when>
        <c:otherwise>
            <h1 class="artist">${fn:escapeXml(model.dir.name)}</h1>
        </c:otherwise>
    </c:choose>
</section>

<div class="actions">

    <c:if test="${not model.partyMode}">
        <ul class="controls">
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl"><sub:param name="id" value="${model.parent.id}"/></sub:url>
                <li><a href="${upUrl}" title="<fmt:message key='up'/>" class="control up"><fmt:message key="main.up"/></a></li>
            </c:if>
            <c:choose>
                <c:when test="${not empty model.dir.starredDate}">
                    <li><a href="#" id="starImage${model.dir.id}" title="<fmt:message key='main.starredoff'/>" class="control star-fill" onclick="toggleStar(${model.dir.id}, '#starImage${model.dir.id}'); return false;"><fmt:message key="main.starredoff"/></a></li>
                </c:when>
                <c:otherwise>
                    <li><a href="#" id="starImage${model.dir.id}" title="<fmt:message key='main.starredon'/>" class="control star" onclick="toggleStar(${model.dir.id}, '#starImage${model.dir.id}'); return false;"><fmt:message key="main.starredon"/></a></li>
                </c:otherwise>
            </c:choose>
            <c:if test="${model.user.streamRole}">
                <li><a href="javascript:playAll()" title="<fmt:message key='main.playall'/>" class="control play"><fmt:message key="main.playall"/></a></li>
                <li><a href="javascript:playRandom(0)" title="<fmt:message key='main.playrandom'/>" class="control shuffle"><fmt:message key="main.playrandom"/></a></li>
                <li><a href="javascript:addAll(0)" title="<fmt:message key='main.addall'/>" class="control plus"><fmt:message key="main.addall"/></a></li>
            </c:if>
            <c:if test="${model.user.commentRole and model.showComment}">
                <li><a href="javascript:toggleComment()" title="<fmt:message key='main.comment'/>" class="control comment"><fmt:message key="main.comment"/></a></li>
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

<div id="comment" class="comment-input">${model.dir.comment}</div>

<div id="commentForm">
    <form method="post" action="setMusicFileInfo.view">
        <sec:csrfInput />
        <input type="hidden" name="action" value="comment">
        <input type="hidden" name="id" value="${model.dir.id}">
        <textarea name="comment" rows="6" cols="70">${model.dir.comment}</textarea>
        <input type="submit" value="<fmt:message key='common.save'/>">
    </form>
</div>

<c:if test="${not empty model.subDirs}">
    <c:choose>
        <c:when test="${model.viewAsList}">
            <table class="tabular albums">
                <thead>
                    <tr>
                        <th></th><%-- star --%>
                        <th></th><%-- play --%>
                        <th></th><%-- add --%>
                        <th></th><%-- next --%>
                        <th><fmt:message key="common.fields.album" /></th>
                        <th></th><%-- year --%>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.subDirs}" var="subDir">
                        <tr>
                            <c:import url="playButtons.jsp">
                                <c:param name="id" value="${subDir.id}"/>
                                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                <c:param name="starEnabled" value="true"/>
                                <c:param name="starred" value="${not empty subDir.starredDate}"/>
                                <c:param name="asTable" value="true"/>
                            </c:import>
                            <td><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td>
                            <td class="year">${subDir.year}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:when>
    
        <c:otherwise>
            <div class="coverart-container">
                <c:set var="albumCount" value="0"/>
                <c:forEach items="${model.subDirs}" var="subDir" varStatus="loopStatus">
                    <c:if test="${subDir.album}">
                        <c:set var="albumCount" value="${albumCount + 1}"/>
                        <div class="albumThumb">
                            <c:import url="coverArt.jsp">
                                <c:param name="albumId" value="${subDir.id}"/>
                                <c:param name="caption1" value="${fn:escapeXml(subDir.name)}"/>
                                <c:param name="caption2" value="${subDir.year}"/>
                                <c:param name="captionCount" value="2"/>
                                <c:param name="coverArtSize" value="${model.coverArtSizeMedium}"/>
                                <c:param name="showLink" value="true"/>
                                <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                                <c:param name="hideOverflow" value="true"/>
                            </c:import>
                        </div>
                    </c:if>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<c:if test="${model.showTopSongs}">
    <div id="topSongs">
        <h2><fmt:message key="main.topsongs"/></h2>
        <div class="actions">
            <ul class="controls">
                <li><a href="javascript:playAllTopSongs()" title="<fmt:message key='main.playtopsongs'/>" class="control play"><fmt:message key='main.playtopsongs'/></a></li>
            </ul>
        </div>
        <table class="tabular top-songs">
            <thead>
                <tr>
                    <th></th><%-- star --%>
                    <th></th><%-- play --%>
                    <th></th><%-- add --%>
                    <th></th><%-- next --%>
                    <th></th><%-- rank --%>
                    <th><fmt:message key="common.fields.songtitle" /></th>
                    <th><fmt:message key="common.fields.album" /></th>
                    <th><fmt:message key="common.fields.artist" /></th>
                    <th></th><%-- duration --%>
                </tr>
            </thead>
            <tbody id="topSongsBody">
                <tr id="pattern">
                    <td><div id="starSong" onclick="toggleStarTopSong(this.id.substring(8) - 1, '#starSong' + this.id.substring(8))" title="<fmt:message key='main.starredon'/>" class="control star"><fmt:message key="main.starredon"/></div></td>
                    <td><div id="play" onclick="playTopSong(this.id.substring(4) - 1)" title="<fmt:message key='common.play'/>" class="control play"><fmt:message key='common.play'/></div></td>
                    <td><div id="add" onclick="addTopSong(this.id.substring(3) - 1)" title="<fmt:message key='common.add'/>" class="control plus"><fmt:message key='common.add'/></div></td>
                    <td><div id="addNext" onclick="addNextTopSong(this.id.substring(7) - 1)" title="<fmt:message key='main.addnext'/>" class="control next"><fmt:message key='main.addnext'/></div></td>
                    <td><span id="rank">Rank</span></td>
                    <td class="song"><span id="title">Title</span></td>
                    <td class="album"><a id="albumUrl" target="main"><span id="album">Album</span></a></td>
                    <td class="artist"><span id="artist">Artist</span></td>
                    <td class="duration"><span id="songDuration">Duration</span></td>
                </tr>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${model.showSimilar}">
    <div id="similar">
        <h2><fmt:message key="main.similarartists"/></h2>
        <span id="similarArtists"></span>
    </div>
</c:if>

</body>
</html>
