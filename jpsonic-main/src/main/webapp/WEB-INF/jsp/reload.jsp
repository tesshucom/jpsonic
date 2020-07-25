<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html>
<head>
<%@ include file="head.jsp"%>
</head>
<body>

	<c:forEach items="${model.reloadFrames}" var="reloadFrame">
		<script language="javascript" type="text/javascript">
			if('main' != '${reloadFrame.frame}'){
			    window.top.${reloadFrame.frame}.location.reload();
			} else {
				// "${reloadFrame.view}" Update the child iframe #646
	    		parent.frames.upper.location.reload();
			}
    	</script>
	</c:forEach>

</body>
</html>
