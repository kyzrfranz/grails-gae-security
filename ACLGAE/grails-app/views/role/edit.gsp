<%@ page import="com.aclgae.Role" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Edit Role</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${resource(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Role List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New Role</g:link></span>
        </div>
        <div class="body">
            <h1>Edit Role</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${roleInstance}">
            <div class="errors">
                <g:renderErrors bean="${roleInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form method="post" >
                <input type="hidden" name="id" value="${roleInstance?.id}" />
                <input type="hidden" name="version" value="${roleInstance?.version}" />
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="authority">Authority:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:roleInstance,field:'authority','errors')}">
                                    <g:textField name="authority" value="${roleInstance?.authority}" />
                                </td>
                            </tr> 
                        
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><g:actionSubmit class="save" value="Update" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>
