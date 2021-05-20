<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>


<html>
<head>
<%!
long totalMemory = Runtime.getRuntime().totalMemory();
long freeMemory = Runtime.getRuntime().freeMemory();
long usedMemory = totalMemory - freeMemory;
%>
<%@ include file="head.jsp" %>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>

<body class="mainframe error">

<section>
    <h1 class="error">Error</h1>
</section>

<p>
    Jpsonic encountered an internal error. You can report this error in the <a href="https://github.com/tesshucom/jpsonic/issues" target="_blank" rel="noopener nofererrer">jpsonic#Issues</a>.<br>
    Please include the information below.
</p>

<dl>
    <dt>Status</dt>
    <dd><c:out value="${status}" /></dd>
    <dt>Error</td>
    <dd><c:out value="${error}" /></dd>
    <dt>Message</td>
    <dd><c:out value="${message}" /></dd>
    <dt>Path</td>
    <dd><c:out value="${path}" /></dd>
    <dt>Time</td>
    <dd><c:out value="${timestamp}" /></dd>
    <dt>Exception</td>
    <dd><c:out value="${exception}" /></dd>
    <dt>Java version</td>
    <dd><%=System.getProperty("java.vendor") + ' ' + System.getProperty("java.version")%></dd>
    <dt>Operating system</td>
    <dd><%=System.getProperty("os.name") + ' ' + System.getProperty("os.version")%></dd>
    <dt>Server</td>
    <dd><%=application.getServerInfo()%></dd>
    <dt>Memory</td>
    <dd>Used <%=usedMemory/1024L/1024L%> of <%=totalMemory/1024L/1024L%> MB</dd>
    <c:if test="${not empty trace}">
        <dt>Stack trace</dt>
        <dd class="error-code">
        	<pre>
            	${fn:escapeXml(trace)}
			</pre>
        </dd>
    </c:if>
</dl>

</body>
</html>
