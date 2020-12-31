function refShowPlaylist4Album() {};
function refAppendPlaylist4Album(playlistId) {};
function refShowPlaylist4Playqueue() {};
function refAppendPlaylist4Playqueue(playlistId) {};

function createDialogVoiceInput(lang, closeText) {
    const ps = new PrefferedSize(480, 180);
    let sr;
    top.$("#dialog-voice-input").dialog({
        autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : ps.width,
        height  : ps.height,
        open: function(e, u) {
            top.$("#voice-input-result").empty();
            SpeechRecognition = webkitSpeechRecognition || SpeechRecognition;
            sr = new SpeechRecognition();
            sr.lang = lang;
            sr.interimResults = true;
            sr.continuous = true;
            sr.onresult = function(e) {
                const results = e.results;
                for (var i = e.resultIndex; i < results.length; i++) {
                    if (results[i].isFinal) {
                        sr.stop();
                    } else {
                        top.$("#voice-input-result").text(results[i][0].transcript);
                    }
                  }
                }
                function onEnd(e) {
                    sr.stop();
                    top.$("#dialog-voice-input").dialog("close");
                    if(top.$("#voice-input-result").text()) {
                        $("#query").val(top.$("#voice-input-result").text());
                        executeInstantSearch();
                    }
                };
                sr.onend = onEnd;
                sr.onerror = function(e) {console.log(e);onEnd(e)}
                sr.start();
        },
        close : function() {sr.stop();top.$("#dialog-voice-input").dialog("close");},
        buttons: {
            closeButton : {
                text: closeText,
                click: function() {top.$("#dialog-voice-input").dialog("close");}
            }
        }
    });
}

function lazyOpenDialogVoiceInput(lang, closeText) {
    if(!top.$("#dialog-voice-input").hasClass("ui-dialog-content")) {
        createDialogVoiceInput(lang, closeText);
    }
    top.$("#dialog-voice-input").dialog("open");
}

function createDialogNowplayinginfos(closeText) {
    const ps = new PrefferedSize(840, 480);
    top.$("#dialog-nowplayinginfos").dialog({
        autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : ps.width,
        height  : ps.height,
        stack: true,
        buttons: {
            closeButton : {
                text: closeText,
                id: 'npCancelButton',
                click: function() {top.$("#dialog-nowplayinginfos").dialog('close');}
            }
        },
        open : function() {
            top.$("#npCancelButton").focus();
            top.$("#dialog-nowplayinginfos").append('<iframe id="iframeNowPlayings" scrolling="no" frameborder="no"></iframe>');
            top.$("#iframeNowPlayings").attr({src : "nowPlayingInfos.view?", width : '98%', height : '98%' });},
        close : function() {top.$("#iframeNowPlayings").remove();}
    });
}

function lazyOpenDialogNowplayinginfos(closeText) {
    if(!top.$("#dialog-nowplayinginfos").hasClass("ui-dialog-content")) {
        createDialogNowplayinginfos(closeText);
    }
    top.$("#dialog-nowplayinginfos").dialog("open");
}

function createDialogKeyboardShortcuts(closeText) {
    const shortcutsPs = new PrefferedSize(840, 480);
    top.$("#dialog-keyboard-shortcuts").dialog({
        autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : shortcutsPs.width,
        height  : shortcutsPs.height,
        open : function() {
            top.$("#dialog-keyboard-shortcuts").append('<iframe id="iframeShortcuts" frameborder="no"></iframe>');
            top.$("#iframeShortcuts").attr({src : "keyboardShortcuts.view?", width : '98%', height : '98%' });},
        close : function() {top.$("#dialog-keyboard-shortcuts").dialog("close");top.$("#iframeShortcuts").remove();},
        buttons: {
            closeButton : {
                text: closeText,
                click: function() {top.$("#dialog-keyboard-shortcuts").dialog("close");}
            }
        }
    });
}

function lazyOpenDialogKeyboardShortcuts(closeText) {
    if(!top.$("#dialog-keyboard-shortcuts").hasClass("ui-dialog-content")) {
        createDialogKeyboardShortcuts(closeText);
    }
    top.$("#dialog-keyboard-shortcuts").dialog("open");
}

function createDialogVideoPlayer(videoUrl, closeText) {
    const shortcutsPs = new PrefferedSize(840, 640);
    top.$("#dialog-video").dialog({
        autoOpen: false,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        modal: true,
        width  : shortcutsPs.width,
        height  : shortcutsPs.height,
        open : function() {
            top.$("#dialog-video").append('<iframe id="iframeVideoPlayer" frameborder="no"></iframe>');
            top.$("#iframeVideoPlayer").attr({src : videoUrl, width : '100%', height : '98%' });},
        close : function() {top.$("#dialog-video").dialog("close");top.$("#iframeVideoPlayer").remove();},
        buttons: {
            closeButton : {
                text: closeText,
                click: function() {top.$("#dialog-video").dialog("close");}
            }
        }
    });
}

function openDialogVideoPlayer(videoUrl, closeText) {
    createDialogVideoPlayer(videoUrl, closeText);
    top.onStop();
    top.$("#dialog-video").dialog("open");
}

setDialogVideoPlayerTitle = function(title) {
    top.$(".ui-dialog-titlebar.ui-corner-all.ui-widget-header.ui-helper-clearfix").text(title);
}
