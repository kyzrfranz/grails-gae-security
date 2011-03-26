package com.aclgae

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional
import com.aclgae.User

class UserService {

	static transactional = false
	
	def aclPermissionFactory
	def authenticationService
	def springSecurityService
	def aclService
	def aclUtilService
	def objectIdentityRetrievalStrategy
	def sessionFactory
	def jpaTemplate
	def transactionTemplate

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
		def userInstance
		
		User.withTransaction{
			def pwd = springSecurityService.encodePassword(tmpUser.password)
			userInstance = new User(username: tmpUser.username, enabled: true,
			password: pwd).save(flush:true)
			
			   if(userInstance) {
				   try {
					   //println "creating user "+id
					   userInstance.save()
				   }
				   catch(org.springframework.dao.DataIntegrityViolationException e) {
				   }
			   }
		}
		
		if(userInstance) {
			//println "adding permission to user "+roleUser+" for "+userInstance.id
			UserRole.create userInstance, roleUser
		}
		
		assert userInstance != null
		
		addPermission userInstance, userInstance.username, BasePermission.ADMINISTRATION
		addPermission userInstance, "ROLE_ADMIN", BasePermission.ADMINISTRATION
		
		println "adding admin rights to "+userInstance.username+" for "+userInstance.username
		
		userInstance
	}
	
	@PreAuthorize("hasPermission(#id, 'com.aclgae.User', admin)")
	public User get(long id){
		User.get(id)	
	}
	
	public User getByUsername(String username){
		User.findByUsername(username)
	}
	
	@PostFilter("hasPermission(filterObject, admin) or hasPermission(filterObject, read)")
	public List<User> list(Map params){
		def retUsers = []
		def users = User.list()
		
		users.each{retUsers << it}
		
		retUsers
	}
}
