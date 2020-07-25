function isKeyboardShortcutsEnabled() {
  if (window === window.parent.top) {
    var config = JSON.parse(document.getElementById('preferencesConfig').textContent)
    return config['keyboardShortcutsEnabled'];
  } else {
    return window.parent.top.isKeyboardShortcutsEnabled();
  }
}

if (isKeyboardShortcutsEnabled()) {

  let ref_main = window.parent.main;
  Mousetrap.bind('shift+space', function() { window.parent.onToggleStartStop(); return false; });
  Mousetrap.bind('shift+left',  function() { window.parent.onPrevious(); });
  Mousetrap.bind('shift+right', function() { window.parent.onNext(); });
  Mousetrap.bind('*',     function() { window.parent.onStarCurrent(); });
  Mousetrap.bind('plus',  function() { window.parent.onGainAdd(+5); });
  Mousetrap.bind('-',     function() { window.parent.onGainAdd(-5); });
  Mousetrap.bind('q',     function() { window.parent.onTogglePlayQueue(); });

  Mousetrap.bind('/',     function() { window.parent.onQueryFocus(); });
  Mousetrap.bind('m',     function() { window.parent.onToggleDrawer(); });

  Mousetrap.bind('g h',   function() { ref_main != null ? ref_main.location.href = "home.view?" : window.parent.onChangeMainLocation('home.view?'); });
  Mousetrap.bind('g p',   function() { ref_main != null ? ref_main.location.href = "playlists.view?" : window.parent.onChangeMainLocation('playlists.view?'); });
  Mousetrap.bind('g o',   function() { ref_main != null ? ref_main.location.href = "podcastChannels.view?" : window.parent.onChangeMainLocation('podcastChannels.view?'); });
  Mousetrap.bind('g s',   function() { ref_main != null ? ref_main.location.href = "settings.view?" : window.parent.onChangeMainLocation('settings.view?'); });
  Mousetrap.bind('g t',   function() { ref_main != null ? ref_main.location.href = "starred.view?" : window.parent.onChangeMainLocation('starred.view?'); });
  Mousetrap.bind('g r',   function() { ref_main != null ? ref_main.location.href = "more.view?" : window.parent.onChangeMainLocation('more.view?'); });
  Mousetrap.bind('g a',   function() { ref_main != null ? ref_main.location.href = "help.view?" : window.parent.onChangeMainLocation('help.view?'); });

  Mousetrap.bind('?',     function() { window.parent.onShowKeyboardShortcuts(); });

  Mousetrap.bind('up up down down left right left right b a enter', function() {
    $('#showGrid4Debug').css('visibility', 'visible')
  });

}
