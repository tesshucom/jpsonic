<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<div id="dialog-delete" title="<fmt:message key='common.confirm'/>"></div>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>">
    <ul id="dialog-select-playlist-list"></ul>
</div>

<div id="dialog-keyboard-shortcuts" title="<fmt:message key='more.keyboard.title'/>"></div>

<div id="dialog-edit" title="<fmt:message key='playlist2.edit'/>">
    <form>
        <dl>
            <dt><label for="newName"><fmt:message key="playlist2.name"/></label></dt>
            <dd><input type="text" name="newName" id="newName" class="ui-widget-content"/></dd>
            <dt><label for="newComment"><fmt:message key="playlist2.comment"/></label></dt>
            <dd><input type="text" name="newComment" id="newComment" class="ui-widget-content"/></dd>
            <dt></dt>
            <dd>
                <input type="checkbox" name="newShared" id="newShared"/>
                <label for="newShared"><fmt:message key="playlist2.public"/></label>
            </dd>
        </dl>
    </form>
</div>

<div id="dialog-nowplayinginfos" title="<fmt:message key='top.othersplaying'/>"></div>

<div id="dialog-voice-input" title="<fmt:message key='top.microphone'/>">
    <div id="voice-input-result"></div>
</div>
