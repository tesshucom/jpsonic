function isKeyboardShortcutsEnabled() {
  if (window === window.parent.top) {
    var config = JSON.parse(document.getElementById('preferencesConfig').textContent)
    return config['keyboardShortcutsEnabled'];
  } else {
    return window.parent.top.isKeyboardShortcutsEnabled();
  }
}

if (isKeyboardShortcutsEnabled()) {

  Mousetrap.bind('shift+space', function() { if(!top.isDialogOpened()) top.onToggleStartStop(); return false; });
  Mousetrap.bind('shift+left',  function() { if(!top.isDialogOpened()) top.onPrevious(); });
  Mousetrap.bind('shift+right', function() { if(!top.isDialogOpened()) top.onNext(); });
  Mousetrap.bind('*',     function() { if(!top.isDialogOpened()) top.onStarCurrent(); });
  Mousetrap.bind('plus',  function() { if(!top.isDialogOpened()) top.onGainAdd(+5); });
  Mousetrap.bind('-',     function() { if(!top.isDialogOpened()) top.onGainAdd(-5); });
  Mousetrap.bind('q',     function() { if(!top.isDialogOpened()) top.onTogglePlayQueue(); });

  Mousetrap.bind('/',     function() { if(!top.isDialogOpened()) top.onQueryFocus(); });
  Mousetrap.bind('m',     function() { if(!top.isDialogOpened()) top.onToggleDrawer(); });

  Mousetrap.bind('g h',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('home.view?'); });
  Mousetrap.bind('g p',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('playlists.view?'); });
  Mousetrap.bind('g o',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('podcastChannels.view?'); });
  Mousetrap.bind('g s',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('settings.view?'); });
  Mousetrap.bind('g t',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('starred.view?'); });
  Mousetrap.bind('g r',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('more.view?'); });
  Mousetrap.bind('g a',   function() { if(!top.isDialogOpened()) top.onChangeMainLocation('help.view?'); });

  Mousetrap.bind('?',     function() { if(!top.isDialogOpened()) top.onShowKeyboardShortcuts(); });

  Mousetrap.bind('up up down down left right left right b a enter', function() {
    if(!top.isDialogOpened()) $('#showGrid4Debug').css('visibility', 'visible');
  });

}
