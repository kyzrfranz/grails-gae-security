package org.codehaus.groovy.grails.plugins.springsecurity.acl

import javax.persistence.*;

import com.google.appengine.api.datastore.Key;

@Entity
class AclEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	AclObjectIdentity aclObjectIdentity
	
	int aceOrder
	AclSid sid
	int mask
	boolean granting
	boolean auditSuccess
	boolean auditFailure

	@Override
	String toString() {
		"AclEntry id $id, aceOrder $aceOrder, mask $mask, granting $granting, " +
		"aclObjectIdentity $aclObjectIdentity"
	}

	static mapping = {
		version false
		sid column: 'sid'
		aclObjectIdentity column: 'acl_object_identity'
	}

	static constraints = {
		aceOrder unique: 'aclObjectIdentity'
	}
}
