import com.aclgae.Role
import com.aclgae.User
import com.aclgae.UserRole
import java.util.Date;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.transaction.support.TransactionCallback
import org.apache.commons.lang.StringUtils
import org.springframework.security.acls.domain.BasePermission


class BootStrap {
	
	def authenticationService
	def springSecurityService
	def aclService
	def aclUtilService
	def objectIdentityRetrievalStrategy
	def sessionFactory
	def jpaTemplate
	def transactionTemplate

    def init = { servletContext ->
		
		deleteOldUsers()
		deleteOldRoles()
		deleteOldUserRoles()
		createUsers()
		
    }
    def destroy = {
    }
	
	
	private void loginAsAdmin() {
		// have to be authenticated as an admin to create ACLs
		SCH.context.authentication = new UsernamePasswordAuthenticationToken(
				  'admin', 'admin123',
				  AuthorityUtils.createAuthorityList('ROLE_ADMIN'))
	}
	
	private void deleteOldUsers(){
	
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
			
	}


	private void deleteOldRoles(){
		def roleslist;
		Role.withTransaction{
			roleslist = Role.list()
		}
		
		roleslist.each{ role ->
			Role.withTransaction{
				//println user
				def roleInstance = Role.get(role.id)
				roleInstance.delete(flush:true)
			}
		}
	}
	
	
	private void deleteOldUserRoles(){
		def roleslist;
		UserRole.withTransaction{
			roleslist = UserRole.list()
		}
		
		roleslist.each{ role ->
			UserRole.withTransaction{
				//println user
				def roleInstance = UserRole.get(role.id)
				roleInstance.delete(flush:true)
			}
		}
	}

	private void createUsers() {
		def roleUser;
		def roleAdmin;
		
		Role.withTransaction{
			roleUser = new Role(authority: 'ROLE_USER').save(flush:true)
		}
		Role.withTransaction{
			roleAdmin = new Role(authority: 'ROLE_ADMIN').save(flush:true)
		}
		
		3.times {
		   long id = it + 1
		   def userInstance
		   def pwd = springSecurityService.encodePassword("password$id")
		   //println "setting pwd: "+pwd
		   User.withTransaction {
			   userInstance =  new User(username: "user$id", enabled: true,
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
			   //loginAsAdmin()
			   //aclUtilService.addPermission userInstance, userInstance.username, BasePermission.ADMINISTRATION
			   //aclUtilService.addPermission userInstance, "ROLE_ADMIN", BasePermission.ADMINISTRATION
		   }
		   
		   if(id == 1){
			   UserRole.create userInstance, roleAdmin
		   }
		   
		}
		
	}

	
}
