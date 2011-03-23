package com.aclgae

import com.google.appengine.api.datastore.Key
import javax.persistence.*;

import org.apache.commons.lang.builder.HashCodeBuilder

@Entity
class UserRole implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id
	
	long userId
	Key roleId

	boolean equals(other) {
		if (!(other instanceof UserRole)) {
			return false
		}

		other.userId == userId &&
			other.roleId == roleId
	}

	int hashCode() {
		def builder = new HashCodeBuilder()
		if (userId) builder.append(userId)
		if (roleId) builder.append(roleId)
		builder.toHashCode()
	}
	
	
	static UserRole create(User user, Role role, boolean flush = false) {
		new UserRole(userId: user.id, roleId: role.id).save(flush: flush, insert: true)
	}
	
	
	static Set<Long> findRolesByUserId(long userId){
		def roleslist = []
		UserRole.withTransaction{
			UserRole.list().each{
				if(it.userId == userId){
					roleslist.add(it.roleId.getId())
				}
			}
		}
		roleslist
	}
	
	
	/*static UserRole create(User user, Role role, boolean flush = false) {
		new UserRole(user: user, role: role).save(flush: flush, insert: true)
	}

	static boolean remove(User user, Role role, boolean flush = false) {
		UserRole instance = UserRole.findByUserAndRole(user, role)
		instance ? instance.delete(flush: flush) : false
	}

	static void removeAll(User user) {
		executeUpdate 'DELETE FROM UserRole WHERE user=:user', [user: user]
	}

	static void removeAll(Role role) {
		executeUpdate 'DELETE FROM UserRole WHERE role=:role', [role: role]
	}*/

	static mapping = {
		id composite: ['roleId', 'userId']
		version false
	}
}
