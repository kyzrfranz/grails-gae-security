package com.aclgae

import grails.plugins.springsecurity.Secured
import org.springframework.security.acls.domain.BasePermission


class UserController {
    
    def index = { redirect(action:list,params:params) }
	def springSecurityService
	def userService
	
	
    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
		def users = userService.list( params )
        [ userInstanceList: users, userInstanceTotal: 10 ]
    }

    def show = {
        def userInstance = User.get( params.id )

        if(!userInstance) {
            flash.message = "User not found with id ${params.id}"
            redirect(action:list)
        }
        else { return [ userInstance : userInstance ] }
    }

    def delete = {
		def userInstance = User.get( params.id )
		User.withTransaction {
	        if(userInstance) {
	            try {
	                userInstance.delete()
	                flash.message = "User ${params.id} deleted"
	                redirect(action:list)
	            }
	            catch(org.springframework.dao.DataIntegrityViolationException e) {
	                flash.message = "User ${params.id} could not be deleted"
	                redirect(action:show,id:params.id)
	            }
	        }
	        else {
	            flash.message = "User not found with id ${params.id}"
	            redirect(action:list)
	        }			
		}
    }
	
	def deleteAll = {
		def userlist;
		User.withTransaction{ 
			userlist = User.list()
		} 
		
		userlist.each{ user ->
			User.withTransaction{
				//println user
				def userInstance = User.get(user.id)
				userInstance.delete(flush:true)
			}
		}
		redirect(action:list)
	}

    def edit = {
        def userInstance = userService.get(params.id.toLong()) //User.get( params.id )

        if(!userInstance) {
            flash.message = "User not found with id ${params.id}"
            redirect(action:list)
        }
        else {
            return [ userInstance : userInstance ]
        }
    }
	
	def addReading = {
		def userInstance = userService.get(params.id.toLong())
		userService.addPermission userInstance, params.username, BasePermission.READ
	}
	
	def removeReading = {
		def userInstance = userService.get(params.id.toLong())
		userService.deletePermission userInstance, params.username, BasePermission.READ
	}

    def update = {
		User.withTransaction {
	        def userInstance = User.get( params.id )
	        if(userInstance) {
	            if(params.version) {
	                def version = params.version.toLong()
	                if(userInstance.version > version) {
	                    
	                    userInstance.errors.rejectValue("version", "user.optimistic.locking.failure", "Another user has updated this User while you were editing.")
	                    render(view:'edit',model:[userInstance:userInstance])
	                    return
	                }
	            }
	            userInstance.properties = params
				userInstance.password = springSecurityService.encodePassword(params.password)
	            if(!userInstance.hasErrors() && userInstance.save()) {
	                flash.message = "User ${params.id} updated"
	                redirect(action:show,id:userInstance.id)
	            }
	            else {
	                render(view:'edit',model:[userInstance:userInstance])
	            }
	        }
	        else {
	            flash.message = "User not found with id ${params.id}"
	            redirect(action:list)
	        }			
		}
    }

    def create = {
        def userInstance = new User()
        userInstance.properties = params
        return ['userInstance':userInstance]
    }

    def save = {
        def userInstance = new User(params)
		userInstance = userService.create(userInstance)
			
			
		redirect(action:list)
			//redirect(action:show,id:userInstance.id)
	        /*if(userInstance.save(flush:true)) {
	            flash.message = "User ${userInstance.id} created"
	            redirect(action:show,id:userInstance.id)
	        }
	        else {
	            render(view:'create',model:[userInstance:userInstance])
	        }*/
		
    }
}
