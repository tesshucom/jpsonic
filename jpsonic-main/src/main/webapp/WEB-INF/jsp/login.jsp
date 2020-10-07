<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script>
        if (window != window.top) {
            top.location.href = location.href;
        }
    </script>

</head>
<body style="min-width:550px" class="mainframe">

    <form action="<c:url value='/login'/>" method="POST">
        <sec:csrfInput />

        <div id="loginframe">

            <img src="./icons/logo.png" alt="">

            <div class="loginmessagetop">${model.loginMessage}</div>

            <input required type="text" autofocus id="j_username" name="j_username" tabindex="1" placeholder="<fmt:message key='login.username'/>">

            <input required type="password" autocomplete="off"  name="j_password" tabindex="2" placeholder="<fmt:message key='login.password'/>">

            <input name="submit" type="submit" value="<fmt:message key='login.login'/>" tabindex="4"></td>

            <div class="details">
                <div id="loginremember">
                    <label for="remember"><fmt:message key="login.remember"/></label>
                    <input type="checkbox" name="remember-me" id="remember" tabindex="3">
                </div>

                <a href="recover.view"><fmt:message key="login.recover"/></a>
            </div>

            <c:if test="${model.logout}">
                <div class="loginmessagebottom">
                    <fmt:message key="login.logout"/>
                </div>
            </c:if>

            <c:if test="${model.error}">
                <div class="loginmessagebottom"><strong><fmt:message key="login.error"/></strong></div>
            </c:if>

            <c:if test="${model.insecure}">
                <div class="loginmessagebottom"><strong><fmt:message key="login.insecure"><fmt:param value="${model.brand}"/></fmt:message></strong></div>
            </c:if>

        </div>
    </form>

</body>
</html>
