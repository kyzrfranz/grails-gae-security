package org.codehaus.groovy.grails.plugins.springsecurity.acl

import javax.persistence.*;
import grails.plugins.springsecurity.acl.AbstractAclObjectIdentity

@Entity
class AclObjectIdentity extends AbstractAclObjectIdentity {

	
	Long objectId

	@Override
	String toString() {
		"AclObjectIdentity id $id, aclClass $aclClassName, " +
		"objectId $objectId, entriesInheriting $entriesInheriting"
	}

	static mapping = {
		version false
		aclClassName column: 'object_id_class'
		ownerId column: 'owner_sid'
		parentId column: 'parent_object'
		objectId column: 'object_id_identity'
	}

	static constraints = {
		objectId unique: 'aclClass'
	}
}
