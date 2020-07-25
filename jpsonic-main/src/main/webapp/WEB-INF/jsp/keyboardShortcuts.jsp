<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ page trimDirectiveWhitespaces="true" %>
<html>
<head>
<%@ include file="head.jsp" %>
</head>
<body>

<h2><img src="<spring:theme code='keyboardImage'/>" alt=""/><fmt:message key="more.keyboard.title"/></h2>
<fmt:message key="more.keyboard.text"/>
<table class="indent music" style="width:inherit">
    <tr>
        <th colspan="2"><fmt:message key="more.keyboard.playback"/></th>
        <th colspan="2"><fmt:message key="more.keyboard.navigation"/></th>
        <th colspan="2"><fmt:message key="more.keyboard.general"/></th>
    </tr>
    <tr>
        <td class="more-shortcut">Shift + Space</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.playpause"/></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> h</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.home"/></td>
        <td class="more-shortcut">/</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.search"/></td>
    </tr>
    <tr>
        <td class="more-shortcut">Shift + &#8592;</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.previous"/></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> p</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.playlists"/></td>
        <td class="more-shortcut">m</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.drawer"/></td>
    </tr>
    <tr>
        <td class="more-shortcut">Shift + &#8594;</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.next"/></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> o</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.podcasts"/></td>
        <td class="more-shortcut">q</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.playqueue"/></td>
    </tr>
    <tr>
        <td class="more-shortcut">&ndash;</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.volumedown"/></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> s</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.settings"/></td>
        <td class="more-shortcut">?</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.shortcuts"/></td>
    </tr>
    <tr>
        <td class="more-shortcut">+</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.volumeup"/></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> t</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.starred"/></td>
        <td></td><td></td>
    </tr>
    <tr>
        <td></td><td></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> r</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.more"/></td>
        <td></td><td></td>
    </tr>
    <tr>
        <td></td><td></td>
        <td class="more-shortcut">g <fmt:message key="more.keyboard.then"/> a</td><td class="more-shortcut-descr"><fmt:message key="more.keyboard.about"/></td>
        <td></td><td></td>
    </tr>
</table>

</body>
</html>
