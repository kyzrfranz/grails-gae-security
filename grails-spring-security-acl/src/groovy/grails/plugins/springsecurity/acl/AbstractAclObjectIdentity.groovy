/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.springsecurity.acl

import javax.persistence.*;
import com.google.appengine.api.datastore.Key
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

/**
 * Abstract base class for the AclObjectIdentity domain class. The default implementation
 * assumes a long objectId but custom implementations may wish to use other PK types.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
@MappedSuperclass
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
abstract class AbstractAclObjectIdentity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	Key id
	
	
	//AclClass aclClass
	String aclClassName 
	//AclObjectIdentity parent
	Long parentId
	//AclSid owner
	String parentClassName
	Long ownerId
	boolean entriesInheriting

	static constraints = {
		parentId nullable: true
		parentClassName nullable:true
		ownerId nullable: true
	}
}
