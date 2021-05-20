// CSS changes by the agent (#892)
$(function () {
    if (!navigator.userAgent.match(/iPhone|Android.+Mobile/)) {
        $(".mainframe").addClass("notMobile");
        document.styleSheets[0].insertRule('::-webkit-scrollbar-track {margin-bottom: 0 !important} ', 0);
    }
});
