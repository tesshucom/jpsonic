
function checkTruncate(tabularWidthBase, tabular, controlLength, cols) {
    $(tabular + ' tr td.truncate').each((index , e) => {
        $(e).removeClass('truncate');
        $(e).children('span').removeAttr('title');
    });
    function writeTruncate($clazz, isForce){
        if ($(tabularWidthBase).width() < $(tabular).width() + 60) {
            const threshold = ($(tabularWidthBase).width() - controlLength * 30) / 3;
            $(tabular + ' tr td.' + $clazz).each((index , e) => {
                if(isForce || (threshold < $(e).width())){
                    $(e).addClass('truncate');
                    $(e).children('span').attr('title', $(e).text());
                }
            });
        }
    }
    $(cols).each((index, col) => writeTruncate(col, false));
    if ($(tabularWidthBase).width() < $(tabular).width() + 60) {
        $(cols).each((index, col) => {writeTruncate(col, true)});
    }
}
function initTruncate(tabularWidthBase, tabular, controlLength, cols) {
    checkTruncate(tabularWidthBase, tabular, controlLength, cols);
    function onResize(c, t){onresize=function(){clearTimeout(t);t=setTimeout(c, 500)};return c};
    onResize(function() {checkTruncate(tabularWidthBase, tabular, controlLength, cols)})();
}
