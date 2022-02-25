<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html><head>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<script src="<c:url value='/script/utils.js'/>"></script>
<script src="<c:url value='/dwr/interface/nowPlayingService.js'/>"></script>
<script src="<c:url value='/dwr/interface/playQueueService.js'/>"></script>
<script src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
<script src="<c:url value='/dwr/engine.js'/>"></script>
<script src="<c:url value='/dwr/util.js'/>"></script>
<script src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
<script src="<c:url value='/script/playQueueCast.js'/>"></script>
<script src="<c:url value='/script/jpsonic/truncate.js'/>"></script>
<script src="<c:url value='/script/jpsonic/dialogs.js'/>"></script>
</head>

<body class="playQueue">

<script>

// These variables store the media player state, received from DWR in the
// playQueueCallback function below.

// List of songs (of type PlayQueueInfo.Entry)
var songs = null;

// Stream URL of the media being played
var currentStreamUrl = null;

// Current song being played
var currentSong = null;

// Is autorepeat enabled?
var repeatEnabled = false;

// Is the "shuffle radio" playing? (More > Shuffle Radio)
var shuffleRadioEnabled = false;

// Is the "internet radio" playing?
var internetRadioEnabled = false;

// Initialize the Cast player (ChromeCast support)
var CastPlayer = new CastPlayer();

$(document).ready(function(){

    dwr.engine.setErrorHandler(null);
    startTimer();
    
    <c:if test="${model.player.web}">createMediaElementPlayer();</c:if>

    $("#playQueueBody").sortable({
        stop: function(event, ui) {
            var indexes = [];
            $("#playQueueBody").children().each(function() {
                var id = $(this).attr("id").replace("pattern", "");
                if (id.length > 0) {
                    indexes.push(parseInt(id) - 1);
                }
            });
            onRearrange(indexes);
        },
        cursor: "move",
        axis: "y",
        containment: "parent",
        helper: function(e, tr) {
            var originals = tr.children();
            var trclone = tr.clone();
            trclone.children().each(function(index) {
                // Set cloned cell sizes to match the original sizes
                $(this).width(originals.eq(index).width());
                $(this).css("maxWidth", originals.eq(index).width());
                $(this).css("border-top", "1px solid black");
                $(this).css("border-bottom", "1px solid black");
            });
            return trclone;
        }
    });

    /** Toggle between <a> and <span> in order to disable play queue action buttons */
    $.fn.toggleLink = function(newState) {
        $(this).each(function(ix, elt) {

            var node, currentState;
            if (elt.tagName.toLowerCase() === "a") currentState = true;
            else if (elt.tagName.toLowerCase() === "span") currentState = false;
            else return true;
            if (typeof newState === 'undefined') newState = !currentState;
            if (newState === currentState) return true;

            if (newState) node = document.createElement("a");
            else node = document.createElement("span");

            node.innerHTML = elt.innerHTML;
            if (elt.hasAttribute("id")) node.setAttribute("id", elt.getAttribute("id"));
            if (elt.hasAttribute("style")) node.setAttribute("style", elt.getAttribute("style"));
            if (elt.hasAttribute("class")) node.setAttribute("class", elt.getAttribute("class"));

            if (newState) {
                if (elt.hasAttribute("data-href")) node.setAttribute("href", elt.getAttribute("data-href"));
                node.classList.remove("disabled");
                node.removeAttribute("aria-disabled");
            } else {
                if (elt.hasAttribute("href")) node.setAttribute("data-href", elt.getAttribute("href"));
                node.classList.add("disabled");
                node.setAttribute("aria-disabled", "true");
            }

            elt.parentNode.replaceChild(node, elt);
            return true;
        });
    };

    getPlayQueue();

    // Initialization required when switching players
    window.top.setQueueOpened(document.getElementById("isQueueOpened").checked);
    window.top.setQueueExpand(document.getElementById("isQueueExpand").checked);

    initTruncate(".queue-container", ".tabular.songs", 2, ["album", "artist", "song"]);
    
    top.refShowPlaylist4Playqueue = function() {
        playlistService.getWritablePlaylists(function playlistCallback(playlists) {
            top.$("#dialog-select-playlist-list").empty();
            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                top.$("#dialog-select-playlist-list").append("<li><a href='#' onclick='refAppendPlaylist4Playqueue(" + playlist.id + ")'>" + escapeHtml(playlist.name) + "</a></li>");
            }
            top.$("#dialog-select-playlist").dialog("open");
        });
    }

    top.refAppendPlaylist4Playqueue = function(playlistId) {
        top.$("#dialog-select-playlist").dialog("close");
        var mediaFileIds = new Array();
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + (i + 1)).is(":checked")) {
                mediaFileIds.push(songs[i].id);
            }
        }
        playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){
            top.upper.document.getElementById("main").src = "playlist.view?id=" + playlistId;
        });
    }
    
    const ps = new PrefferedSize(480, 360);
    top.$("#dialog-select-playlist").dialog({
        autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : ps.width,
        height  : ps.height,
        buttons: {
            "<fmt:message key="common.cancel"/>": {
                text: "<fmt:message key="common.cancel"/>",
                id: 'dspCancelButton',
                click: function() {top.$("#dialog-select-playlist").dialog("close");}
            }
        },
        open: function() {top.$("#dspCancelButton").focus();}
    });
    <c:if test="${model.playqueueQuickOpen}">
        document.getElementById("playerView").addEventListener('dblclick', function (e) {
            onTogglePlayQueue();
        });
    </c:if>
});

function startTimer() {
    <!-- Periodically check if the current song has changed. -->
    nowPlayingService.getNowPlayingForCurrentPlayer(nowPlayingCallback);
    setTimeout("startTimer()", 10000);
}

function nowPlayingCallback(nowPlayingInfo) {
    if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != currentStreamUrl) {
        getPlayQueue();
    <c:if test="${not model.player.web}">
        currentStreamUrl = nowPlayingInfo.streamUrl;
        currentSong = null;
        onPlaying();
    </c:if>
    }
}

/**
 * Callback function called when playback for the current song has ended.
 */
function onEnded() {
    onNext(repeatEnabled);
    onPlayingStateUpdated();
}

/**
 * Callback function called when playback for the current song has started.
 */
function onPlaying() {
    top.onChangeCurrentSong(currentSong);
    if (currentSong) {
        updateWindowTitle(currentSong);
        <c:if test="${model.notify}">
            showNotification(currentSong);
        </c:if>
    }
    onPlayingStateUpdated();
}

/**
 * Initialize the Media Element Player, including callbacks.
 */
function createMediaElementPlayer() {
    // Manually run MediaElement.js initialization.
    //
    // Warning: Bugs will happen if MediaElement.js is not initialized when
    // we modify the media elements (e.g. adding event handlers). Running
    // MediaElement.js's automatic initialization does not guarantee that
    // (it depends on when we call createMediaElementPlayer at load time).
    $('#audioPlayer').mediaelementplayer({
        success: function(media, domElement, player) {

            // Once playback reaches the end, go to the next song, if any.
            $('#audioPlayer').on("ended", onEnded);
            
            // Whenever playback starts, show a notification for the current playing song.
            $('#audioPlayer').on("playing", onPlaying);

            media.addEventListener('pause', onPlayingStateUpdated);
        }
    });
}

function getPlayQueue() {
    playQueueService.getPlayQueue(playQueueCallback);
}

function onClear() {
    var ok = true;
<c:if test="${model.partyMode}">
    ok = confirm("<fmt:message key="playlist.confirmclear"/>");
</c:if>
    if (ok) {
        playQueueService.clear(playQueueCallback);
    }
}

/**
 * Start/resume playing from the current playlist
 */
function onStart() {
    if (CastPlayer.castSession) {
        CastPlayer.playCast();
    } else if ($('#audioPlayer').get(0)) {
        if ($('#audioPlayer').get(0).src) {
            $('#audioPlayer').get(0).play();  // Resume playing if the player was paused
        }
        else {
            loadPlayer(0);  // Start the first track if the player was not yet loaded
        }
    } else {
        playQueueService.start(playQueueCallback);
    }
}

/**
 * Pause playing
 */
window.onStop = function() {
    if (CastPlayer.castSession) {
        CastPlayer.pauseCast();
    } else if ($('#audioPlayer').get(0)) {
        $('#audioPlayer').get(0).pause();
    } else {
        playQueueService.stop(playQueueCallback);
    }
}

/**
 * Toggle play/pause
 *
 * FIXME: Only works for the Web player for now
 */
window.onToggleStartStop = function() {
    if (CastPlayer.castSession) {
        var playing = CastPlayer.mediaSession && CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
        if (playing) onStop();
        else onStart();
    } else if ($('#audioPlayer').get(0)) {
        var playing = $("#audioPlayer").get(0).paused != null && !$("#audioPlayer").get(0).paused;
        if (playing) onStop();
        else onStart();
    } else {
        playQueueService.toggleStartStop(playQueueCallback);
    }
}

function onGain(gain) {
    playQueueService.setGain(gain);
}
function onCastVolumeChanged() {
    var value = parseInt($("#castVolume").slider("option", "value"));
    CastPlayer.setCastVolume(value / 100, false);
}

/**
 * Increase or decrease volume by a certain amount
 *
 * @param gain amount to add or remove from the current volume
 */
window.onGainAdd = function(gain) {
    if (CastPlayer.castSession) {
        var volume = parseInt($("#castVolume").slider("option", "value")) + gain;
        if (volume > 100) volume = 100;
        if (volume < 0) volume = 0;
        CastPlayer.setCastVolume(volume / 100, false);
        $("#castVolume").slider("option", "value", volume); // Need to update UI
    } else if ($('#audioPlayer').get(0)) {
        var volume = parseFloat($('#audioPlayer').get(0).volume)*100 + gain;
        if (volume > 100) volume = 100;
        if (volume < 0) volume = 0;
        $('#audioPlayer').get(0).volume = volume / 100;
    }
}

function onSkip(index) {
    top.onChangeCurrentSong(null);
    <c:choose>
    <c:when test="${model.player.web}">
        loadPlayer(index);
    </c:when>
    <c:otherwise>
        currentStreamUrl = songs[index].streamUrl;
        playQueueService.skip(index, playQueueCallback);
    </c:otherwise>
    </c:choose>
}
window.onNext = function(wrap) {
    var index = parseInt(getCurrentSongIndex()) + 1;
    if (shuffleRadioEnabled && index >= songs.length) {
        playQueueService.reloadSearchCriteria(function(playQueue) {
            playQueueCallback(playQueue);
            onSkip(index);
        });
        return;
    } else if (wrap) {
        index = index % songs.length;
    }
    onSkip(index);
}
 window.onPrevious = function() {
    onSkip(parseInt(getCurrentSongIndex()) - 1);
}
function onPlay(id) {
    playQueueService.play(id, playQueueCallback);
}
function onPlayShuffle(albumListType, offset, size, genre, decade) {
    playQueueService.playShuffle(albumListType, offset, size, genre, decade, playQueueCallback);
}
function onPlayPlaylist(id, index) {
    playQueueService.playPlaylist(id, index, playQueueCallback);
}
function onPlayInternetRadio(id, index) {
    playQueueService.playInternetRadio(id, index, playQueueCallback);
}
function onPlayTopSong(id, index) {
    playQueueService.playTopSong(id, index, playQueueCallback);
}
function onPlayPodcastChannel(id) {
    playQueueService.playPodcastChannel(id, playQueueCallback);
}
function onPlayPodcastEpisode(id) {
    playQueueService.playPodcastEpisode(id, playQueueCallback);
}
function onPlayNewestPodcastEpisode(index) {
    playQueueService.playNewestPodcastEpisode(index, playQueueCallback);
}
function onPlayStarred() {
    playQueueService.playStarred(playQueueCallback);
}
function onPlayRandom(id, count) {
    playQueueService.playRandom(id, count, playQueueCallback);
}
function onPlaySimilar(id, count) {
    playQueueService.playSimilar(id, count, playQueueCallback);
}
function onAdd(id) {
    playQueueService.add(id, playQueueCallback);
}
function onAddNext(id) {
    playQueueService.addAt(id, getCurrentSongIndex() + 1, playQueueCallback);
}
function onAddPlaylist(id) {
    playQueueService.addPlaylist(id, playQueueCallback);
}
function onShuffle() {
    playQueueService.shuffle(playQueueCallback);
}
function onStar(index) {
    playQueueService.toggleStar(index, playQueueCallback);
}
window.onStarCurrent = function() {
    onStar(getCurrentSongIndex());
}
function onRemove(index) {
    playQueueService.remove(index, playQueueCallback);
}
function onRemoveSelected() {
    var indexes = new Array();
    var counter = 0;
    for (var i = 0; i < songs.length; i++) {
        var index = i + 1;
        if ($("#songIndex" + index).is(":checked")) {
            indexes[counter++] = i;
        }
    }
    playQueueService.removeMany(indexes, playQueueCallback);
}

function onRearrange(indexes) {
    playQueueService.rearrange(indexes, playQueueCallback);
}
function onToggleRepeat() {
    playQueueService.toggleRepeat(playQueueCallback);
}
function onUndo() {
    playQueueService.undo(playQueueCallback);
}
function onSortByTrack() {
    playQueueService.sortByTrack(playQueueCallback);
    document.activeElement.blur();
}
function onSortByArtist() {
    playQueueService.sortByArtist(playQueueCallback);
    document.activeElement.blur();
}
function onSortByAlbum() {
    playQueueService.sortByAlbum(playQueueCallback);
    document.activeElement.blur();
}
function onSavePlayQueue() {
    var positionMillis = $('#audioPlayer').get(0) ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
    playQueueService.savePlayQueue(getCurrentSongIndex(), positionMillis);
    $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveplayqueue"/>");
}
function onLoadPlayQueue() {
    playQueueService.loadPlayQueue(playQueueCallback);
}
function onSavePlaylist() {
    playlistService.createPlaylistForPlayQueue(function (playlistId) {
        top.upper.document.getElementById("main").src = "playlist.view?id=" + playlistId;
        $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveasplaylist"/>");
    });
}

function playQueueCallback(playQueue) {
    songs = playQueue.entries;
    repeatEnabled = playQueue.repeatEnabled;
    shuffleRadioEnabled = playQueue.shuffleRadioEnabled;
    internetRadioEnabled = playQueue.internetRadioEnabled;

    // If an internet radio has no sources, display a message to the user.
    if (internetRadioEnabled && songs.length == 0) {
        top.main.$().toastmessage("showErrorToast", "<fmt:message key="playlist.toast.radioerror"/>");
        onStop();
    }

    if ($("#start")) {
        $("#start").toggle(!playQueue.stopEnabled);
        $("#stop").toggle(playQueue.stopEnabled);
    }

    if ($("#repeatQueue")) {
        if (shuffleRadioEnabled) {
            $("#repeatQueue").attr('title', "<fmt:message key='playlist.repeat_radio'/>");
        } else if (repeatEnabled) {
            $("#repeatQueue").removeClass('control repeat');
            $("#repeatQueue").addClass('control no-repeat');
            $("#repeatQueue").attr('title', "<fmt:message key='playlist.repeat_off'/>");
        } else {
            $("#repeatQueue").removeClass('control no-repeat');
            $("#repeatQueue").addClass('control repeat');
            $("#repeatQueue").attr('title', "<fmt:message key='playlist.repeat_on'/>");
        }
    }

    // Disable some UI items if internet radio is playing
    $("select#moreActions #loadPlayQueue").prop("disabled", internetRadioEnabled);
    $("select#moreActions #savePlayQueue").prop("disabled", internetRadioEnabled);
    $("select#moreActions #savePlaylist").prop("disabled", internetRadioEnabled);
    $("select#moreActions #downloadPlaylist").prop("disabled", internetRadioEnabled);
    $("select#moreActions #sharePlaylist").prop("disabled", internetRadioEnabled);
    $("select#moreActions #sortByTrack").prop("disabled", internetRadioEnabled);
    $("select#moreActions #sortByAlbum").prop("disabled", internetRadioEnabled);
    $("select#moreActions #sortByArtist").prop("disabled", internetRadioEnabled);
    $("select#moreActions #selectAll").prop("disabled", internetRadioEnabled);
    $("select#moreActions #selectNone").prop("disabled", internetRadioEnabled);
    $("select#moreActions #removeSelected").prop("disabled", internetRadioEnabled);
    $("select#moreActions #download").prop("disabled", internetRadioEnabled);
    $("select#moreActions #appendPlaylist").prop("disabled", internetRadioEnabled);
    $("#shuffleQueue").toggleLink(!internetRadioEnabled);
    $("#repeatQueue").toggleLink(!internetRadioEnabled);
    $("#undoQueue").toggleLink(!internetRadioEnabled);

    if (songs.length == 0) {
        $("#songCountAll").text("0");
        $("#durationAll").text("0:00");
        $("#empty").show();
        $("#playQueueHeader").hide();
    } else {
        $("#songCountAll").text(songs.length);
        $("#durationAll").text(playQueue.durationAsString);
        $("#empty").hide();
        $("#playQueueHeader").show();
    }

    // On the web player, the play button is handled by MEJS and does
    // nothing if a first song hasn't been loaded.
    <c:if test="${model.player.web}">
    if (songs.length > 0 && !currentStreamUrl) preparePlayer(0);
    </c:if>

    // Delete all the rows except for the "pattern" row
    dwr.util.removeAllRows("playQueueBody", { filter:function(tr) {
        return (tr.id != "pattern");
    }});

    // Create a new set cloned from the pattern row
    for (var i = 0; i < songs.length; i++) {
        var song  = songs[i];
        var id = i + 1;
        dwr.util.cloneNode("pattern", { idSuffix:id });
        if ($("#trackNumber" + id)) {
            $("#trackNumber" + id).text(song.trackNumber);
        }

        if (!internetRadioEnabled) {
            // Show star/remove buttons in all cases...
            $("#starSong" + id).show();
            $("#removeSong" + id).show();
            $("#songIndex" + id).show();

            // Show star rating
            if (song.starred) {
                $("#starSong" + id).removeClass('star');
                $("#starSong" + id).addClass('star-fill');
                $("#starSong" + id).attr('title', '<fmt:message key="main.starredoff"/>');
            } else {
                $("#starSong" + id).removeClass('star-fill');
                $("#starSong" + id).addClass('star');
                $("#starSong" + id).attr('title', '<fmt:message key="main.starredon"/>');
            }
        } else {
            // ...except from when internet radio is playing.
            $("#starSong" + id).hide();
            $("#removeSong" + id).hide();
            $("#songIndex" + id).hide();
        }

        if ($("#playingStateReceiver" + id) && song.streamUrl == currentStreamUrl) {
            $("#playingStateReceiver" + id).show();
        }
        if ($("#title" + id)) {
            $("#title" + id).text(song.title);
            $("#title" + id).attr("title", song.title);
        }
        if ($("#titleUrl" + id)) {
            $("#titleUrl" + id).text(song.title);
            $("#titleUrl" + id).attr("title", song.title);
            $("#titleUrl" + id).click(function () {onSkip(this.id.substring(8) - 1)});
        }
        if ($("#album" + id)) {
            $("#album" + id).text(song.album);
            $("#album" + id).attr("title", song.album);
            $("#albumUrl" + id).attr("href", song.albumUrl); // Podcast css rendering is incorrect with cache
            // Open external internet radio links in new windows
            if (internetRadioEnabled) {
                $("#albumUrl" + id).attr({
                    target: "_blank",
                    rel: "noopener noreferrer",
                });
            }
        }
        if ($("#artist" + id)) {
            $("#artist" + id).text(song.artist);
            $("#artist" + id).attr("title", song.artist);
        }
        if ($("#composer" + id)) {
            $("#composer" + id).text(song.composer);
        }
        if ($("#genre" + id)) {
            $("#genre" + id).text(song.genre);
        }
        if ($("#year" + id)) {
            // If song.year is not an int, this will return NaN, which
            // conveniently returns false in all boolean operations.
            if (parseInt(song.year) > 0) {
                $("#year" + id).text(song.year);
            } else {
                $("#year" + id).text("");
            }
        }
        if ($("#bitRate" + id)) {
            $("#bitRate" + id).text(song.bitRate);
        }
        if ($("#duration" + id)) {
            $("#duration" + id).text(song.durationAsString);
        }
        if ($("#format" + id)) {
            $("#format" + id).text(song.format);
        }
        if ($("#fileSize" + id)) {
            $("#fileSize" + id).text(song.fileSize);
        }

        // Note: show() method causes page to scroll to top.
        $("#pattern" + id).css("display", "table-row");
    }

    if (playQueue.sendM3U) {
        parent.frames.main.location.href="play.m3u?";
    }

<c:if test="${model.player.web}">
    triggerPlayer(playQueue.startPlayerAt, playQueue.startPlayerAtPosition);
</c:if>

}

function triggerPlayer(index, positionMillis) {
    if (index != -1) {
        if (songs.length > index) {
            loadPlayer(index);
            if (positionMillis != 0) {
                $('#audioPlayer').get(0).currentTime = positionMillis / 1000;
            }
        }
    }
    if (songs.length == 0) {
        $('#audioPlayer').get(0).stop();
    }
    onPlayingStateUpdated();
}

/**
 * Return the current Media Element Player instance
 */
function getMediaElementPlayer() {
    return $('#audioPlayer').get(0);
}

/**
 * Prepare playback for a song on the Cast player.
 *
 * @param song         song object
 * @param position     position in the song, in seconds
 */
function prepareCastPlayer(song, position) {
    // The Cast player does not support preloading a song, so we don't do
    // anything here and return directly.
}

/**
 * Prepare playback for a song on the Media Element player.
 *
 * @param song         song object
 * @param position     position in the song, in seconds
 */
function prepareMediaElementPlayer(song, position) {
     onPlayingStateUpdated();
    // The Media Element player supports setting the current song URL,
    // which allows the user to start playback by interacting with the
    // controls, without going through the Airsonic code.
    getMediaElementPlayer().src = song.streamUrl;
}

/**
 * Start playing a song on the Cast player.
 *
 * @param song         song object
 * @param position     position in the song, in seconds
 */
function loadCastPlayer(song, position, prepareOnly) {
    // Start playback immediately
    CastPlayer.loadCastMedia(song, position);
    // The Cast player does not handle events, so we call the 'onPlaying' function manually.
    onPlaying();
}

/**
 * Start playing a song on the Media Element player.
 *
 * @param song         song object
 * @param position     position in the song, in seconds
 */
function loadMediaElementPlayer(song, position) {
    // Retrieve the media element player
    var player = getMediaElementPlayer();
    // The player should have been prepared by `preparePlayer`, but if
    // it hasn't we set it here so that the function behaves correctly.
    if (player.src != song.streamUrl) player.src = song.streamUrl;
    // Inform MEJS that we need to load the media source. The
    // 'canplay' event will be fired once playback is possible.
    player.load();
    // The 'skip' function takes a 'position' argument. We don't
    // usually send it, and in this case it's better to do nothing.
    // Otherwise, the 'canplay' event will also be fired after
    // setting 'currentTime'.
    if (position && position > 0) {
        player.currentTime = position;
    }
    // Start playback immediately. The 'onPlaying' function will be called
    // when the player notifies us that playback has started.
    player.play();
}

/**
 * Prepare for playing a song by its index on the active player.
 *
 * The song is not played immediately, but set as the current song.
 *
 * The active player is then prepared for playback: in Media Element
 * Player's case, for example, this lets the player start playback using
 * its own controls without relying on Airsonic code.
 *
 * @param index        song index in the play queue
 * @param position     position in the song, in seconds
 */
function preparePlayer(index, position) {

    // Check that the song index is valid for the current play queue
    if (index < 0 || index >= songs.length) {
        return;
    }

    // Set the current song, index and URL as global variables
    currentSong = songs[index];
    currentStreamUrl = currentSong.streamUrl;

    // Run player-specific preparation code
    if (CastPlayer.castSession) {
        prepareCastPlayer(currentSong, position);
    } else {
        prepareMediaElementPlayer(currentSong, position);
    }

    return currentSong;
}

window.initCurrentSongView = function() {
    if(0 < $('#audioPlayer').get(0).currentTime) {
        top.onChangeCurrentSong(currentSong);
    }
}

/**
 * Start playing a song by its index on the active player.
 *
 * @param index        song index in the play queue
 * @param position     position in the song, in seconds
 */
function loadPlayer(index, position) {
    // Prepare playback on the active player
    var song = preparePlayer(index, position);
    if (!song) return;
    // Run player-specific code to start playback
    if (CastPlayer.castSession) {
        loadCastPlayer(song, position);
    } else {
        loadMediaElementPlayer(song, position);
    }
}

function updateWindowTitle(song) {
    top.document.title = song.title + " - " + song.artist + " - Jpsonic";
}

function showNotification(song) {
    if (!("Notification" in window)) {
        return;
    }
    if (Notification.permission === "granted") {
        createNotification(song);
    }
    else if (Notification.permission !== 'denied') {
        Notification.requestPermission(function (permission) {
            Notification.permission = permission;
            if (permission === "granted") {
                createNotification(song);
            }
        });
    }
}

function createNotification(song) {
    var n = new Notification(song.title, {
        tag: "jpsonic",
        body: song.artist + " - " + song.album,
        icon: "coverArt.view?id=" + song.id + "&size=${model.coverArtSize}"
    });
    n.onshow = function() {
        setTimeout(function() {n.close()}, 5000);
    }
}

function onPlayingStateUpdated() {
    if (songs == null) {
        return;
    }
    for (var i = 0; i < songs.length; i++) {
        var song  = songs[i];
        var id = i + 1;
        var receiver = $("#playingStateReceiver" + id);
        if (receiver) {
            $(receiver).removeClass('paused');
            $(receiver).removeClass('playing');
            if (song.streamUrl == currentStreamUrl) {
                if($("#audioPlayer").get(0).paused) {
                    top.onChangeCurrentSong(null);
                    $(receiver).addClass('paused');
                } else {
                    $(receiver).addClass('playing');
                }
            }
        }
    }
}

function getCurrentSongIndex() {
    for (var i = 0; i < songs.length; i++) {
        if (songs[i].streamUrl == currentStreamUrl) {
            return i;
        }
    }
    return -1;
}

function getSelectedIndexes() {
    var result = "";
    for (var i = 0; i < songs.length; i++) {
        if ($("#songIndex" + (i + 1)).is(":checked")) {
            result += "i=" + i + "&";
        }
    }
    return result;
}

function selectAll(b) {
    for (var i = 0; i < songs.length; i++) {
        $("#songIndex" + (i + 1)).prop("checked", b);
    }
}
    
function toggleSelect() {
    selectAll(!(0 < getSelectedIndexes().length));
}

function getSelectedIndexes() {
    var result = "";
    for (var i = 0; i < songs.length; i++) {
        var checkbox = $("#songIndex" + i);
        if (checkbox != null  && checkbox.is(":checked")) {
            result += "i=" + i + "&";
        }
    }
    return result;
}

function downloadSelected() {
    var selected = getSelectedIndexes();
    if(0 == selected.length) {
        return;
    }
    location.href = "download.view?player=${model.player.id}&" + selected;
}

window.onTogglePlayQueue = function() {
    let isQueueOpened = document.getElementById("isQueueOpened");
    window.top.setQueueOpened(!isQueueOpened.checked);
    <c:if test="${model.alternativeDrawer}">
        if(!isQueueOpened.checked){
              top.onCloseDrawer();
        }
    </c:if>
    isQueueOpened.checked = !isQueueOpened.checked;
}

function toggleElasticity() {
    let isQueueExpand = document.getElementById("isQueueExpand");
    window.top.setQueueExpand(!isQueueExpand.checked);
    if(!isQueueExpand.checked){
        document.getElementById("isElementUnderQueue").checked = false;
        $("#elasticity").attr("title", "<fmt:message key='playqueue.shrink'/>");
        $(".control .elasticity").text("<fmt:message key='playqueue.shrink'/>");
    } else {
        document.getElementById("isElementUnderQueue").checked = !${model.showAlbumActions};
        $("#elasticity").attr("title", "<fmt:message key='playqueue.maximize'/>");
        $(".control .elasticity").text("<fmt:message key='playqueue.maximize'/>");
    }
    isQueueExpand.checked = !isQueueExpand.checked;
}

window.onCloseQueue = function() {
    if(document.getElementById("isQueueOpened").checked){
        onTogglePlayQueue();
    }
};

window.onTryCloseQueue = function() {
    <c:if test="${model.closePlayQueue}">
        if (document.getElementById("isQueueOpened").checked) {
            onTogglePlayQueue();
        }
    </c:if>
};

</script>

<input type="checkbox" id="isQueueOpened" value="1" autofocus="false" tabindex="-1"/>
<input type="checkbox" id="isQueueExpand" value="1" autofocus="false" tabindex="-1"/>

<%-- player --%>
<div id="playerView" class="playerView">
    <c:if test="${model.user.settingsRole and fn:length(model.players) gt 1}">
        <select name="player" onchange="location='playQueue.view?player=' + options[selectedIndex].value;">
            <c:forEach items="${model.players}" var="player">
                <option ${player.id eq model.player.id ? "selected" : ""} value="${player.id}">${player.shortDescription}</option>
            </c:forEach>
        </select>
    </c:if>
    <c:if test="${model.player.web}">
        <div title="<fmt:message key='more.keyboard.previous'/>" onclick="onPrevious()" class="control prev"><fmt:message key="more.keyboard.previous"/></div>
    </c:if>
    <c:if test="${model.player.web}">
        <span id="player">
            <audio id="audioPlayer" data-mejsoptions='{"alwaysShowControls": true, "enableKeyboard": false, "defaultAudioWidth": 400}' tabindex="-1" style="width:100%"/>
        </span>
        <span id="castPlayer">
            <span>
                <span title="<fmt:message key='playqueue.play'/>" id="castPlay" onclick="CastPlayer.playCast()"><fmt:message key="playqueue.play"/></span>
                <span title="<fmt:message key='playqueue.pause'/>" id="castPause" onclick="CastPlayer.pauseCast()"><fmt:message key="playqueue.pause"/></span>
                <span title="<fmt:message key='playqueue.muteon'/>" id="castMuteOn" onclick="CastPlayer.castMuteOn()" class="control volume"><fmt:message key="playqueue.muteon"/></span>
                <span title="<fmt:message key='playqueue.muteoff'/>" id="castMuteOff" onclick="CastPlayer.castMuteOff()"><fmt:message key="playqueue.muteoff"/></span>
            </span>
            <span>
                <div id="castVolume"></div>
                <script>
                    $("#castVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                    $("#castVolume").on("slidestop", onCastVolumeChanged);
                </script>
            </span>
        </span>
        <%--
        #622 Nodes that are not currently in use. Reimplement if necessary.
        <div title="Cast on" id="castOn" onclick="CastPlayer.launchCastApp()">
        <div title="Cast off" id="castOff" onclick="CastPlayer.stopCastApp()">
         --%>
    </c:if>
    <c:if test="${model.user.streamRole and not model.player.web}">
        <span title="<fmt:message key='playqueue.start'/>" id="start" onclick="onStart()" class="control play"><fmt:message key="playqueue.start"/></span>
        <span title="<fmt:message key='playqueue.stop'/>" id="stop" onclick="onStop()" class="control pause"><fmt:message key="playqueue.stop"/></span>
    </c:if>
    <c:if test="${model.player.web}">
        <div title="<fmt:message key='more.keyboard.next'/>" onclick="onNext(false)" class="control forward"><fmt:message key="more.keyboard.next"/></div>
    </c:if>

    <a title="<fmt:message key='playqueue.maximize'/>" href="javascript:toggleElasticity()" id="elasticity"><div class="control elasticity"><fmt:message key="playqueue.maximize"/></div></a>
    <a href="javascript:onTogglePlayQueue()">
        <div title="<fmt:message key='playqueue.show'/>" class="control expand"/><fmt:message key="playqueue.show"/></div>
        <div title="<fmt:message key='playqueue.hide'/>" class="control shrink"/><fmt:message key="playqueue.hide"/></div>
    </a>
</div>

<%-- drawer --%>
<div id="playqueue-drawer" class="jps-playqueue-drawer">

    <section>
        <h1 class="playqueue"><fmt:message key="playlist.more.playlist"/></h1>
        <dl class="overview">
            <dt><span class="icon numberofsongs"><fmt:message key='playlist2.numberofsongs'/></span></dt>
            <dd><span id="songCountAll"></span></dd>
            <dt><span class="icon duration"><fmt:message key='playlist2.duration'/></span></dt>
            <dd><span id="durationAll"></dd>
        </dl>
    </section>

    <div class="actions">
        <ul class="controls">
            <li><a title="<fmt:message key='playlist.saveplayqueue'/>" href="javascript:onSavePlayQueue()" class="control save-pq"><fmt:message key="playlist.saveplayqueue"/></a></li>
            <li><a title="<fmt:message key='playlist.loadplayqueue'/>" href="javascript:onLoadPlayQueue()" class="control load-pq"><fmt:message key="playlist.loadplayqueue"/></a></li>
            <li><a title="<fmt:message key='playlist.remove'/>" href="javascript:onClear()" class="control cross"><fmt:message key="playlist.remove"/></a></li>
            <li><a title="<fmt:message key='home.shuffle'/>" href="javascript:onShuffle()" id="shuffleQueue" class="control shuffle"><fmt:message key="home.shuffle"/></a></li>
            <li><a title="<fmt:message key='playlist.more.sort'/>" href="#" class="control sort"><fmt:message key="playlist.more.sort"/></a>
                <ul>
                    <li><a title="<fmt:message key='playlist.more.sortbyartist'/>" href="javascript:onSortByArtist()" class="control artist"><fmt:message key="playlist.more.sortbyartist"/></a></li>
                    <li><a title="<fmt:message key='playlist.more.sortbyalbum'/>" href="javascript:onSortByAlbum()" class="control album"><fmt:message key="playlist.more.sortbyalbum"/></a></li>
                    <li><a title="<fmt:message key='playlist.more.sortbytrack'/>" href="javascript:onSortByTrack()" class="control track"><fmt:message key="playlist.more.sortbytrack"/></a></li>
                </ul>
            </li>
            <li><a title="<fmt:message key='playlist.undo'/>" href="javascript:onUndo()" id="undoQueue" class="control undo"><fmt:message key="playlist.undo"/></a></li>
            <li><a title="<fmt:message key='playlist.save'/>" href="javascript:onSavePlaylist()" class="control saveas"><fmt:message key="playlist.save"/></a></li>
            <c:if test="${model.user.downloadRole and model.showDownload}">
                <li><a title="<fmt:message key='main.downloadall'/>" href="javascript:location.href = 'download.view?player=${model.player.id}';" class="control download"><fmt:message key="main.downloadall"/></a></li>
            </c:if>
            <c:if test="${model.user.shareRole and model.showShare}">
                <li><a title="<fmt:message key='main.sharealbum'/>" href="javascript:location.href = 'createShare.view?player=${model.player.id}&' + getSelectedIndexes();" target="main" class="control share"><fmt:message key="main.sharealbum"/></a></li>
            </c:if>
        </ul>
    </div>

    <input type="checkbox" id="isElementUnderQueue" value="1" autofocus="false" tabindex="-1" ${model.showAlbumActions ? '' : 'checked'}/>

    <div class="queue-container">
        <p id="empty"><strong><fmt:message key="playlist.empty"/></strong></p>
    
        <c:set var="songClass" value="song" />
        <c:set var="albumClass" value="album" />
        <c:set var="artistClass" value="artist" />
        <c:set var="suppl" value="${model.simpleDisplay ? 'supplement' : ''}" />
        <table class="tabular queue">
            <thead id="playQueueHeader">
                <tr>
                    <th></th><%-- star --%>
                    <c:if test="${model.showAlbumActions}">
                        <th class="action"></th>
                    </c:if>
                    <c:if test="${model.visibility.trackNumberVisible}"><th class="track"></th></c:if>
                    <th class="${songClass}"><fmt:message key="common.fields.songtitle" /></th>
                    <c:if test="${model.visibility.albumVisible}"><th class="${albumClass}"><fmt:message key="common.fields.album" /></th></c:if>
                    <c:if test="${model.visibility.artistVisible}"><th class="${artistClass}"><fmt:message key="common.fields.artist" /></th></c:if>
                    <c:if test="${model.visibility.composerVisible}"><th class="${suppl} composer"><fmt:message key="common.fields.composer" /></th></c:if>
                    <c:if test="${model.visibility.genreVisible}"><th class="${suppl} genre"><fmt:message key="common.fields.genre" /></th></c:if>
                    <c:if test="${model.visibility.yearVisible}"><th class="${suppl} year"></th></c:if>
                    <c:if test="${model.visibility.formatVisible}"><th class="${suppl} format"></th></c:if>
                    <c:if test="${model.visibility.fileSizeVisible}"><th class="${suppl} size"></th></c:if>
                    <c:if test="${model.visibility.durationVisible}"><th class="${suppl} duration"></th></c:if>
                    <c:if test="${model.visibility.bitRateVisible}"><th class="${suppl} bitrate"></th></c:if>
                    <th></th><%-- delete --%>
                </tr>
            </thead>
            <tbody id="playQueueBody">
                <tr id="pattern">
                    <td><div title="<fmt:message key='main.starredon'/>" id="starSong" onclick="onStar(this.id.substring(8) - 1)" class="control star"><fmt:message key="main.starredon"/></div></td>
                    <c:if test="${model.showAlbumActions}">
                        <td class="action"><input type="checkbox" class="checkbox" id="songIndex"></td>
                    </c:if>
                    <c:if test="${model.visibility.trackNumberVisible}"><td class="track"><span id="trackNumber">1</span></td></c:if>
                    <td class="${songClass}" id="playingStateReceiver">
                        <c:choose>
                            <c:when test="${model.player.externalWithPlaylist}"><span id="title">Title</span></c:when>
                            <c:otherwise><span><a id="titleUrl" href="javascript:void(0)">Title</a></span></c:otherwise>
                        </c:choose>
                    </td>
                    <c:if test="${model.visibility.albumVisible}"><td class="${albumClass}"><a id="albumUrl" target="main"><span id="album">Album</span></a></td></c:if>
                    <c:if test="${model.visibility.artistVisible}"><td class="${artistClass}"><span id="artist">Artist</span></td></c:if>
                    <c:if test="${model.visibility.composerVisible}"><td class="${suppl} composer"><span id="composer">Composer</span></td></c:if>
                    <c:if test="${model.visibility.genreVisible}"><td class="${suppl} genre"><span id="genre">Genre</span></td></c:if>
                    <c:if test="${model.visibility.yearVisible}"><td class="${suppl} year"><span id="year">Year</span></td></c:if>
                    <c:if test="${model.visibility.formatVisible}"><td class="${suppl} format"><span id="format">Format</span></td></c:if>
                    <c:if test="${model.visibility.fileSizeVisible}"><td class="${suppl} size"><span id="fileSize">Format</span></td></c:if>
                    <c:if test="${model.visibility.durationVisible}"><td class="${suppl} duration"><span id="duration">Duration</span></td></c:if>
                    <c:if test="${model.visibility.bitRateVisible}"><td class="${suppl} bitrate"><span id="bitRate">Bit Rate</span></td></c:if>
                    <td class="remove"><div title="<fmt:message key='playlist.remove'/>" id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" class="control minus"><fmt:message key='playlist.remove'/></div></td>
                </tr>
            </tbody>
        </table>
    </div>

    <c:if test="${model.showAlbumActions}">
        <div class="actions">
            <ul class="controls">
                <li><a title="<fmt:message key='playlist.more.selectall'/> / <fmt:message key='playlist.more.selectnone'/>" href="javascript:toggleSelect()" class="control select-all"><fmt:message key='playlist.more.selectall'/> / <fmt:message key='playlist.more.selectnone'/></a></li>
                <li><a title="<fmt:message key='playlist.remove'/>" href="javascript:onRemoveSelected()" class="control cross"><fmt:message key="playlist.remove"/></a></li>
                <c:if test="${model.user.downloadRole and model.showDownload}">
                    <li><a title="<fmt:message key='common.download'/>" href="javascript:downloadSelected()" class="control download"><fmt:message key='common.download'/></a></li>
                </c:if>
                <li><a title="<fmt:message key='playlist.append'/>" href="javascript:top.refShowPlaylist4Playqueue()" class="control export"><fmt:message key='playlist.append'/></a></li>
            </ul>
        </div>
    </c:if>
</div>

<script>
    window['__onGCastApiAvailable'] = function(isAvailable) {
        if (isAvailable) {
            CastPlayer.initializeCastPlayer();
        }
    };
</script>

<script src="https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"></script>

</body></html>
