package com.aclgae
import com.google.appengine.api.datastore.Key
import javax.persistence.*;

@Entity
class Role {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Key id
	
	String authority

	static mapping = {
		cache true
	}

	static constraints = {
		authority blank: false, unique: true
	}
}
