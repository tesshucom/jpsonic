class PrefferedSize {
    constructor(prefferedWidth, prefferedHeight) {
        this.maxWidth = Math.round(Math.min(
                  document.body.scrollWidth, document.documentElement.scrollWidth,
                  document.body.offsetWidth, document.documentElement.offsetWidth,
                  document.body.clientWidth, document.documentElement.clientWidth) * 0.8 / 10) * 10;
        this.width = Math.min(prefferedWidth, this.maxWidth);
        this.maxHeight = Math.round(Math.min(
                  document.body.scrollHeight, document.documentElement.scrollHeight,
                  document.body.offsetHeight, document.documentElement.offsetHeight,
                  document.body.clientHeight, document.documentElement.clientHeight) * 0.8 / 10) * 10;
        this.height = Math.min(prefferedHeight, this.maxHeight);
    }
}

function isDialogOpened() {
    return top.$("#dialog-delete").is(":visible") ||
        top.$("#dialog-select-playlist").is(":visible") ||
        top.$("#dialog-keyboard-shortcuts").is(":visible") ||
        top.$("#dialog-edit").is(":visible") ||
        top.$("#dialog-nowplayinginfos").is(":visible") ||
        top.$("#dialog-voice-input").is(":visible") ||
        top.$("#dialog-video").is(":visible");
}
