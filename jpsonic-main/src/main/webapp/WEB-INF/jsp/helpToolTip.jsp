<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
  Shows online help as a balloon tool tip.

PARAMETERS
  topic: Refers to a key in the resource bundle containing the text to display in the tool tip.
--%>

<script>
    $(function () {
        $(document).tooltip({
            content: function () { return $(this).prop('title');}
        });
    });
</script>

<fmt:message key="common.help" var="help"/>

<div id="placeholder-${param.topic}" class="help-title">
    <div>
        <fmt:message key="helppopup.${param.topic}.title">
            <fmt:param value="Jpsonic"/>
        </fmt:message>
    </div>
</div>

<fmt:message key="helppopup.${param.topic}.title" var="title"/>
<fmt:message key="helppopup.${param.topic}.text" var="text">
    <fmt:param value="Jpsonic"/>
</fmt:message>
<c:set var="oldStr" value='\"' />
<c:set var="newStr" value='&quot;' />
<div id="looltop-${param.topic}" title='${fn:replace(text, oldStr, newStr)}' class="popup help">${fn:replace(title, oldStr, newStr)}</div>
