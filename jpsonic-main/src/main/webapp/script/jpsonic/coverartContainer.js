function adjustCoverartContainer() {
  const c = $(".coverart-container > div.albumThumb").length
  if (c == 0) {
    return
  }
  const w = $("div.coverart-container").width()
  const div = w / $("div.coverart-container > :first").width()
  const surplus = c % parseInt(div)
  const lastMargin = surplus == 0 ? 0 : w - (w / parseInt(div)) * surplus
  $("div.coverart-container > :last").css("margin-right", lastMargin + "px")
}

$(document).ready(function () {
  adjustCoverartContainer()
  function onResize(c,t){onresize=function(){clearTimeout(t);t=setTimeout(c,100)};return c};
  onResize(function() {adjustCoverartContainer();})();
});
