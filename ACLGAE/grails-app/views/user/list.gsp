<%@ page import="com.aclgae.User" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>User List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${resource(dir:'/')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New User</g:link></span>
            <span class="menuButton"><g:link class="deleteAll" action="deleteAll">Delete All</g:link></span>
            <span class="menuButton"><a href="${resource(dir:'/logout')}">Log Out</a></span>
        </div>
        <div class="body">
            <h1>User List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <g:sortableColumn property="accountExpired" title="Account Expired" />
                        
                   	        <g:sortableColumn property="accountLocked" title="Account Locked" />
                        
                   	        <g:sortableColumn property="enabled" title="Enabled" />
                        
                   	        <g:sortableColumn property="password" title="Password" />
                        
                   	        <g:sortableColumn property="passwordExpired" title="Password Expired" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${userInstanceList}" status="i" var="userInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${userInstance.id}">${fieldValue(bean:userInstance, field:'id')}</g:link></td>
                        
                            <td>${fieldValue(bean:userInstance, field:'accountExpired')}</td>
                        
                            <td>${fieldValue(bean:userInstance, field:'accountLocked')}</td>
                        
                            <td>${fieldValue(bean:userInstance, field:'enabled')}</td>
                        
                            <td>${fieldValue(bean:userInstance, field:'password')}</td>
                        
                            <td>${fieldValue(bean:userInstance, field:'passwordExpired')}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${userInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
