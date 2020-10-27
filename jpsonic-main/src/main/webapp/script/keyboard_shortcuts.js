function isKeyboardShortcutsEnabled() {
  if (window === window.parent.top) {
    var config = JSON.parse(document.getElementById('preferencesConfig').textContent)
    return config['keyboardShortcutsEnabled'];
  } else {
    return window.parent.top.isKeyboardShortcutsEnabled();
  }
}

if (isKeyboardShortcutsEnabled()) {

  Mousetrap.bind('shift+space', function() { top.onToggleStartStop(); return false; });
  Mousetrap.bind('shift+left',  function() { top.onPrevious(); });
  Mousetrap.bind('shift+right', function() { top.onNext(); });
  Mousetrap.bind('*',     function() { top.onStarCurrent(); });
  Mousetrap.bind('plus',  function() { top.onGainAdd(+5); });
  Mousetrap.bind('-',     function() { top.onGainAdd(-5); });
  Mousetrap.bind('q',     function() { top.onTogglePlayQueue(); });

  Mousetrap.bind('/',     function() { top.onQueryFocus(); });
  Mousetrap.bind('m',     function() { top.onToggleDrawer(); });

  Mousetrap.bind('g h',   function() { top.onChangeMainLocation('home.view?'); });
  Mousetrap.bind('g p',   function() { top.onChangeMainLocation('playlists.view?'); });
  Mousetrap.bind('g o',   function() { top.onChangeMainLocation('podcastChannels.view?'); });
  Mousetrap.bind('g s',   function() { top.onChangeMainLocation('settings.view?'); });
  Mousetrap.bind('g t',   function() { top.onChangeMainLocation('starred.view?'); });
  Mousetrap.bind('g r',   function() { top.onChangeMainLocation('more.view?'); });
  Mousetrap.bind('g a',   function() { top.onChangeMainLocation('help.view?'); });

  Mousetrap.bind('?',     function() { top.onShowKeyboardShortcuts(); });

  Mousetrap.bind('up up down down left right left right b a enter', function() {
    $('#showGrid4Debug').css('visibility', 'visible')
  });

}
