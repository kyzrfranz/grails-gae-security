package com.aclgae
import javax.persistence.*;

@Entity
class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id
	
	String username
	String password
	boolean enabled
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired
	Set<Role> authorities
	
	static constraints = {
		username blank: false, unique: true
		password blank: false
	}

	static mapping = {
		password column: '`password`'
	}

	Set<Role> getAuthorities(){
		def roleslist = []
		
//			UserRole.list().each{
//				UserRole.withTransaction{
//					if(it.userId == this.id){
//						roleslist.add(Role.get(it.roleId.getId()))
//					}
//				}
//			}
		
			roleslist = UserRole.findRolesByUserId(this.id)
			println "ROLESSS:"+roleslist
			
			roleslist = roleslist.collect{ roleId ->
				UserRole.withTransaction{
					Role.get(roleId)  
				}
			}as Set
		println "ROLESLIST:" + roleslist
		roleslist
	}
	
	
}
