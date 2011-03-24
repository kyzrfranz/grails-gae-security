package com.aclgae

import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional
import com.aclgae.User

class UserService {

	static transactional = false
	
	def aclPermissionFactory
	def aclService
	def aclUtilService
	def springSecurityService

    @PreAuthorize("hasPermission(#user, admin)")
	void addPermission(User user, String username, Permission permission) {
	   aclUtilService.addPermission user, username, permission
	}
	
	
	@PreAuthorize("hasPermission(#user, admin)")
	void deletePermission(User user, String username, Permission permission) {
		aclUtilService.deletePermission user, username, permission
	}
	
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	User create(User tmpUser) {
		def currentUser = User.get(springSecurityService.principal.id)
		
		def roleUser = Role.findWhere(authority:'ROLE_USER')
		
		def user = new User(username: tmpUser.username, enabled: true,
			password: springSecurityService.encodePassword(tmpUser.password))
			
		println "USER IS:"+ user	
		
		User.withTransaction{
			user.save(flush:true)
		}
		
		println "NEW USER IS:"+ user
		
		assert user != null
		
		UserRole.create user, roleUser
		
		addPermission user, springSecurityService.authentication.name, BasePermission.ADMINISTRATION
		println "adding admin rights to "+user+" for "+springSecurityService.authentication.name
		//addPermission user, 'ROLE_USER', BasePermission.READ
		
		user
	}
	
	@PreAuthorize("hasPermission(#id, 'com.aclgae.User', admin) ")
	public User get(long id){
		User.get(id)	
	}
}
