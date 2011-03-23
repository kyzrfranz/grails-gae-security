package org.codehaus.groovy.grails.plugins.springsecurity.acl

import javax.persistence.*;
import com.google.appengine.api.datastore.Key;


@Entity
class AclClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Key id
	String className

	@Override
	String toString() {
		"AclClass id $id, className $className"
	}

	static mapping = {
		className column: 'class'
		version false
	}

	static constraints = {
		className unique: true, blank: false, size: 1..255
	}
}
