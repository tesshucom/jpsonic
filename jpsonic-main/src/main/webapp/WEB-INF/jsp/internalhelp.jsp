<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>
<body class="mainframe">

<c:import url="helpHeader.jsp">
	<c:param name="cat" value="internalhelp"/>
</c:import>

<details>
    <summary class="
        <c:choose>
            <c:when test='${model.dbIsLegacy}'>
                <c:choose>
                    <c:when test='${model.dbLogSizeBytes < 268435456}'>
                        statusOK
                    </c:when>
                    <c:otherwise>
                        statusNG
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                statusOK
            </c:otherwise>
        </c:choose>
    ">
        <fmt:message key="internalhelp.productsinfo"/>
    </summary>
    <c:choose>
        <c:when test='${model.dbIsLegacy}'>
            <ul>
                <li>
                    <c:choose>
                        <c:when test="${model.dbLogSizeBytes < 268435456}">
                            <fmt:message key="internalhelp.dblogsize.ok"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="internalhelp.dblogsize.warn"/>
                        </c:otherwise>
                    </c:choose>
                </li>
            </ul>
        </c:when>
    </c:choose>
    <dl>
        <dt><fmt:message key="internalhelp.luceneversion"/></dt>
        <dd>${model.indexLuceneVersion}</dd>
        <dt><fmt:message key="internalhelp.dbdrivername"/></dt>
        <dd><td class="ruleTableCell">${model.dbDriverName}</td></dd>
        <dt><fmt:message key="internalhelp.dbdriverversion"/></dt>
        <dd>${model.dbDriverVersion}</dd>
        <dt><fmt:message key="internalhelp.dbserverversion"/></dt>
        <dd>${model.dbServerVersion}</dd>
        <c:if test="${model.dbIsLegacy}">
            <dt><fmt:message key="internalhelp.dbdirectorysize"/></dt>
            <dd>${model.dbDirectorySize}</dd>
            <dt><fmt:message key="internalhelp.dblogsize"/></dt>
            <dd>${model.dbLogSize}</dd>
        </c:if>
    </dl>
</details>

<details>
    <summary class="
        <c:choose>
            <c:when test='${model.dbMediaFileAlbumNonPresentCount + model.dbMediaFileDirectoryNonPresentCount + model.dbMediaFileMusicNonPresentCount + model.dbMediaFilePodcastNonPresentCount == 0}'>
                statusOK
            </c:when>
            <c:otherwise>
                statusNG
            </c:otherwise>
        </c:choose>
    ">
        <fmt:message key="internalhelp.unnecessarydata"/>
    </summary>
    <ul>
        <li>
            <c:choose>
                    <c:when test="${model.dbMediaFileAlbumNonPresentCount + model.dbMediaFileDirectoryNonPresentCount + model.dbMediaFileMusicNonPresentCount + model.dbMediaFilePodcastNonPresentCount == 0}">
                    <fmt:message key="internalhelp.dbnonpresent.ok"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.dbnonpresent.warn"/>
                </c:otherwise>
            </c:choose>
        </li>
    </ul>
    <dl>
        <dt><fmt:message key="internalhelp.musicdeletedcount"/></dt>
        <dd>${model.dbMediaFileMusicNonPresentCount}</dd>
        <dt><fmt:message key="internalhelp.podcastdeletedcount"/></dt>
        <dd>${model.dbMediaFilePodcastNonPresentCount}</dd>
        <dt><fmt:message key="internalhelp.directorydeletedcount"/></dt>
        <dd>${model.dbMediaFileDirectoryNonPresentCount}</dd>
        <dt><fmt:message key="internalhelp.albumdeletedcount"/></dt>
        <dd>${model.dbMediaFileAlbumNonPresentCount}</dd>
        <dt><fmt:message key="internalhelp.musiccount"/></dt>
        <dd>${model.dbMediaFileMusicPresentCount}</dd>
        <dt><fmt:message key="internalhelp.podcastcount"/></dt>
        <dd>${model.dbMediaFilePodcastPresentCount}</dd>
        <dt><fmt:message key="internalhelp.directorycount"/></dt>
        <dd>${model.dbMediaFileDirectoryPresentCount}</dd>
        <dt><fmt:message key="internalhelp.albumcount"/></dt>
        <dd>${model.dbMediaFileAlbumPresentCount}</dd>
        <dt><fmt:message key="internalhelp.distinctalbumcount"/></dt>
        <dd>${model.dbMediaFileDistinctAlbumCount}</dd>
        <dt><fmt:message key="internalhelp.distinctartistcount"/></dt>
        <dd>${model.dbMediaFileDistinctArtistCount}</dd>
        <dt><fmt:message key="internalhelp.distinctalbumartistcount"/></dt>
        <dd>${model.dbMediaFileDistinctAlbumArtistCount}</dd>
    </dl>
</details>

<details>
    <summary class="
        <c:choose>
            <c:when test='${model.dbMediaFilesInNonPresentMusicFoldersCount == 0 and model.dbMediaFilesWithMusicFolderMismatchCount == 0}'>
                statusOK
            </c:when>
            <c:otherwise>
                statusNG
            </c:otherwise>
        </c:choose>
    ">
        <fmt:message key="internalhelp.databaseconsistency"/>
    </summary>
    <ul>
        <li>
            <c:choose>
                <c:when test="${model.dbMediaFilesInNonPresentMusicFoldersCount == 0}">
                    <fmt:message key="internalhelp.mediafilesinnonpresentmusicfoldercount.ok"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.mediafilesinnonpresentmusicfoldercount.warn"/>
                </c:otherwise>
            </c:choose>
        </li>
        <li>
            <c:choose>
                <c:when test="${model.dbMediaFilesWithMusicFolderMismatchCount == 0}">
                    <fmt:message key="internalhelp.mediafileswithmusicfoldermismatchcount.ok"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.mediafileswithmusicfoldermismatchcount.warn"/>
                </c:otherwise>
            </c:choose>
        </li>
    </ul>
    <dl>
        <dt><fmt:message key="internalhelp.mediafilesinnonpresentmusicfoldercount"/></dt>
        <dd>${model.dbMediaFilesInNonPresentMusicFoldersCount}</dd>
        <dt><fmt:message key="internalhelp.mediafileswithmusicfoldermismatchcount"/></dt>
        <dd>${model.dbMediaFilesWithMusicFolderMismatchCount}</dd>
    </dl>
</details>

<details>
    <c:set var="failCount" value="0" scope="page" />
    <c:forEach var="musicFolderStatistics" items="${model.fsMusicFolderStatistics}">
        <c:if test="${!musicFolderStatistics.value.readable}">
            <c:set var="failCount" value="${count + 1}" scope="page"/>
        </c:if>
    </c:forEach>
    <summary class="
        <c:choose>
            <c:when test='${failCount eq 0}'>
                        statusOK
            </c:when>
            <c:otherwise>
                        statusNG
            </c:otherwise>
        </c:choose>
    ">
        <fmt:message key="internalhelp.filesystem"/>
    </summary>
    <ul>
        <c:forEach var="musicFolderStatistics" items="${model.fsMusicFolderStatistics}">
            <li>
                <c:choose>
                    <c:when test="${musicFolderStatistics.value.readable}">
                        <fmt:message key="internalhelp.folderisreadable.ok"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message>
                    </c:when>
                    <c:otherwise>
                        <fmt:message key="internalhelp.folderisreadable.warn"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message>
                    </c:otherwise>
                </c:choose>
            </li>
        </c:forEach>
    </ul>
    <dl>
        <dt><fmt:message key="internalhelp.fsusage"/></dt>
        <dd>${model.fsHomeUsableSpace} / ${model.fsHomeTotalSpace}</dd>
        <dt><fmt:message key="internalhelp.fshomesize"/></dt>
        <dd>${model.fsHomeDirectorySize}</dd>

        <c:forEach var="musicFolderStatistics" items="${model.fsMusicFolderStatistics}">
            <dt><fmt:message key="internalhelp.folderisreadable"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></dt>
            <dd>${musicFolderStatistics.value.readable}</dd>
            <dt><fmt:message key="internalhelp.folderiswritable"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></dt>
            <dd>${musicFolderStatistics.value.writable}</dd>
            <dt><fmt:message key="internalhelp.folderfsusage"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></dt>
            <dd>${musicFolderStatistics.value.freeFilesystemSizeBytes} / ${musicFolderStatistics.value.totalFilesystemSizeBytes}</dd>
        </c:forEach>
    </dl>
</details>

<details>
    <summary class="
        <c:choose>
            <c:when test='${model.fsFfprobeInfo.readable and model.fsFfprobeInfo.executable && model.fsFfmpegInfo.readable and model.fsFfmpegInfo.executable}'>
                statusOK
            </c:when>
            <c:otherwise>
                statusNG
            </c:otherwise>
        </c:choose>
    ">
        <fmt:message key="internalhelp.transcoding"/>
    </summary>
    <ul>
        <li>
            <c:choose>
                <c:when test="${model.fsFfprobeInfo.readable and model.fsFfprobeInfo.executable}">
                    <fmt:message key="internalhelp.tool.ok"><fmt:param value="ffprobe"/></fmt:message>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.tool.warn"><fmt:param value="ffprobe"/></fmt:message>
                </c:otherwise>
            </c:choose>
        </li>
        <li>
            <c:choose>
                <c:when test="${model.fsFfmpegInfo.readable and model.fsFfmpegInfo.executable}">
                    <fmt:message key="internalhelp.tool.ok"><fmt:param value="ffmpeg"/></fmt:message>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.tool.warn"><fmt:param value="ffmpeg"/></fmt:message>
                </c:otherwise>
            </c:choose>
        </li>
    </ul>
    <dl>
        <dt><fmt:message key="internalhelp.tool.path"><fmt:param value="ffprobe"/></fmt:message></dt>
        <dd>${model.fsFfprobeInfo.path}</dd>
        <dt><fmt:message key="internalhelp.tool.isreadable"><fmt:param value="ffprobe"/></fmt:message></dt>
        <dd>${model.fsFfprobeInfo.readable}</dd>
        <dt><fmt:message key="internalhelp.tool.isexecutable"><fmt:param value="ffprobe"/></fmt:message></dt>
        <dd>${model.fsFfprobeInfo.executable}</dd>
        <dt><fmt:message key="internalhelp.tool.path"><fmt:param value="ffmpeg"/></fmt:message></dt>
        <dd>${model.fsFfmpegInfo.path}</dd>
        <dt><fmt:message key="internalhelp.tool.isreadable"><fmt:param value="ffmpeg"/></fmt:message></dt>
        <dd>${model.fsFfmpegInfo.readable}</dd>
        <dt><fmt:message key="internalhelp.tool.isexecutable"><fmt:param value="ffmpeg"/></fmt:message></dt>
        <dd>${model.fsFfmpegInfo.executable}</dd>
    </dl>
</details>

<details>
    <summary class="
            <c:choose>
                <c:when test='${model.localeDefaultCharsetSupportsUtf8 && model.localeFileEncodingSupportsUtf8}'>
                    statusOK
                </c:when>
                <c:otherwise>
                    statusNG
                </c:otherwise>
            </c:choose>
    ">
        <fmt:message key="internalhelp.locale"/>
    </summary>
    <ul>
        <li>
            <c:choose>
                <c:when test="${model.localeDefaultCharsetSupportsUtf8}">
                    <fmt:message key="internalhelp.defaultcharset.ok"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.defaultcharset.warn"/>
                </c:otherwise>
            </c:choose>
        </li>
        <li>
            <c:choose>
                <c:when test="${model.localeFileEncodingSupportsUtf8}">
                    <fmt:message key="internalhelp.file.encoding.ok"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="internalhelp.file.encoding.warn"/>
                </c:otherwise>
            </c:choose>
        </li>
        <c:if test="${not empty model.localeLang and not model.localeLangSupportsUtf8}">
            <li>The LANG environment variable is defined but appears to disable UTF-8 support. International characters may be partially supported.</li>
        </c:if>
        <c:if test="${not empty model.localeLcAll and not model.localeLcAllSupportsUtf8}">
            <li>The LC_ALL environment variable is defined but appears to disable UTF-8 support. International characters may be partially supported.</li>
        </c:if>
    </ul>
    <dl>
        <dt><fmt:message key="internalhelp.defaultcharset"/></dt>
        <dd>${model.localeDefaultCharset}</dd>
        <dt><fmt:message key="internalhelp.defaultlocale"/></dt>
        <dd>${model.localeDefault}</dd>
        <dt><fmt:message key="internalhelp.user.language"/></dt>
        <dd>${model.localeUserLanguage}</dd>
        <dt><fmt:message key="internalhelp.user.country"/></dt>
        <dd>${model.localeUserCountry}</dd>
        <dt><fmt:message key="internalhelp.file.encoding"/></dt>
        <dd>${model.localeFileEncoding}</dd>
        <dt><fmt:message key="internalhelp.sun.jnu.encoding"/></dt>
        <dd>${model.localeSunJnuEncoding}</dd>
        <dt><fmt:message key="internalhelp.sun.io.unicode.encoding"/></dt>
        <dd>${model.localeSunIoUnicodeEncoding}</dd>

        <dt><fmt:message key="internalhelp.langvar"/></dt>
        <dd>${model.localeLang}</dd>
        <dt><fmt:message key="internalhelp.lcallvar"/></dt>
        <dd>${model.localeLcAll}</dd>
    </dl>
</details>

<details>
    <summary class="statusOK">
        <fmt:message key="internalhelp.statistics"/>
    </summary>
    <dl>
        <dt><fmt:message key="internalhelp.albumcount"/></dt>
        <dd>${model.statAlbumCount}</dd>
        <dt><fmt:message key="internalhelp.artistcount"/></dt>
        <dd>${model.statArtistCount}</dd>
        <dt><fmt:message key="internalhelp.songcount"/></dt>
        <dd>${model.statSongCount}</dd>
        <dt><fmt:message key="internalhelp.lastscandate"/></dt>
        <dd>${model.statLastScanDate}</dd>
        <dt><fmt:message key="internalhelp.totaldurationseconds"/></dt>
        <dd>${model.statTotalDurationSeconds}</dd>
        <dt><fmt:message key="internalhelp.totalsizebytes"/></dt>
        <dd>${model.statTotalLengthBytes}</dd>
    </dl>
</details>

<details>
    <summary class="statusOK">
        <fmt:message key="internalhelp.indexdetails"/>
    </summary>
    <dl>
        <c:forEach var="stat" items="${model.indexStatistics}">
                <dt><fmt:message key="internalhelp.indexdeletedcount"><fmt:param value="${stat.value.name}"/></fmt:message></dt>
                <dd>${stat.value.deletedCount}</dd>
        </c:forEach>
        <c:forEach var="stat" items="${model.indexStatistics}">
                <dt><fmt:message key="internalhelp.indexcount"><fmt:param value="${stat.value.name}"/></fmt:message></dt>
                <dd>${stat.value.count}</dd>
        </c:forEach>
    </dl>
</details>

<details>
    <summary class="statusOK">
        <fmt:message key="internalhelp.databasedetails"/>
    </summary>
    <dl>
        <c:forEach var="tableCount" items="${model.dbTableCount}">
            <dt><fmt:message key="internalhelp.tablecount"><fmt:param value="${tableCount.key}"/></fmt:message></dt>
            <dd>${tableCount.value}</dd>
        </c:forEach>
    </dl>
</details>

<div class="submits">
    <input type="button" onClick="location.href='internalhelp.view?'" value="<fmt:message key='common.refresh'/>" />
</div>

</body>
</html>
