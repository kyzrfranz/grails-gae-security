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
package org.grails.plugins.springsecurity.service.acl

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import javax.persistence.Query
import org.apache.log4j.Level
import org.apache.log4j.Logger

import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

import org.springframework.orm.jpa.JpaTemplate
import org.springframework.security.acls.domain.AccessControlEntryImpl
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.jdbc.JdbcAclService;
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AlreadyExistsException
import org.springframework.security.acls.model.AuditableAccessControlEntry
import org.springframework.security.acls.model.ChildrenExistException
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.grails.appengine.AppEngineEntityManagerFactory


/**
 * GORM implementation of {@link org.springframework.security.acls.model.AclService} and {@link MutableAclService}.
 * Ported from <code>JdbcAclService</code> and <code>JdbcMutableAclService</code>.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclService implements MutableAclService {

	private final Logger log = Logger.getLogger(getClass())

	// individual methods are @Transactional since NotFoundException
	// is a runtime exception and will cause an unwanted transaction rollback
	static transactional = false

	/** Dependency injection for aclLookupStrategy. */
	def aclLookupStrategy

	/** Dependency injection for aclCache. */
	def aclCache

	/** Dependency injection for messageSource. */
	def messageSource

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#createAcl(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	@Transactional
	MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
		Assert.notNull objectIdentity, 'Object Identity required'

		// Check this object identity hasn't already been persisted
		if (retrieveObjectIdentity(objectIdentity)) {
			throw new AlreadyExistsException("Object identity '$objectIdentity' already exists")
		}

		// Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
		PrincipalSid sid = new PrincipalSid(SCH.context.authentication)
		
		// Create the acl_object_identity row
		createObjectIdentity objectIdentity, sid

		def retval =  readAclById(objectIdentity)
		
		retval
	}

	protected void createObjectIdentity(ObjectIdentity object, Sid owner) {
		AclSid ownerSid = createOrRetrieveSid(owner, true)
		AclClass aclClass = createOrRetrieveClass(object.type, true)
		save new AclObjectIdentity(
				aclClassName: aclClass.className,
				objectId: object.identifier,
				ownerId: ownerSid.id,
				entriesInheriting: true)
	}

	protected AclSid createOrRetrieveSid(Sid sid, boolean allowCreate) {
		Assert.notNull sid, 'Sid required'
//		println "RETRIEVE SID"
		
		String sidName
		boolean principal
		if (sid instanceof PrincipalSid) {
			sidName = sid.principal
			principal = true
		}
		else if (sid instanceof GrantedAuthoritySid) {
			sidName = sid.grantedAuthority
			principal = false
		}
		else {
			throw new IllegalArgumentException('Unsupported implementation of Sid')
		}

		//AclSid aclSid = AclSid.findBySidAndPrincipal(sidName, principal)
		def jpa = getJpaTemplate()
		def aclSid = jpa.find("SELECT FROM AclSid WHERE sid = '"+sidName+"' AND principal = '"+principal+"'")
		if (!aclSid && allowCreate) {
			aclSid = save(new AclSid(sid: sidName, principal: principal))
		}
		
//		println "SID RETURNED: "+aclSid
		return aclSid
	}

	protected AclClass createOrRetrieveClass(String className, boolean allowCreate) {
//		println "CREATE OR RETRIEVE CLASS"
		AclClass aclClass = AclClass.findByClassName(className)
		if (!aclClass && allowCreate) {
			aclClass = save(new AclClass(className: className))
		}
//		println "CREATE OR RETRIEVE CLASS RETURNS: "+aclClass
		
		return aclClass
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#deleteAcl(
	 * 	org.springframework.security.acls.model.ObjectIdentity, boolean)
	 */
	@Transactional
	void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) throws ChildrenExistException {
//		println "DELETE ACL "+objectIdentity
		
		Assert.notNull objectIdentity, 'Object Identity required'
		Assert.notNull objectIdentity.identifier, "Object Identity doesn't provide an identifier"

		if (deleteChildren) {
			for (child in findChildren(objectIdentity)) {
				deleteAcl child, true
			}
		}

		AclObjectIdentity oid = retrieveObjectIdentity(objectIdentity)

		// Delete this ACL's ACEs in the acl_entry table
		deleteEntries oid

		// Delete this ACL's acl_object_identity row
		oid.delete()

		// Clear the cache
		aclCache.evictFromCache objectIdentity
	}

	protected void deleteEntries(AclObjectIdentity oid) {
		println "DELETING ENTRIES FOR "+oid
//		AclEntry.withTransaction{
//			AclEntry.executeUpdate(
//				"DELETE FROM AclEntry ae " +
//				"WHERE ae.aclOiId = "+ oid.id.getId(), [])
//		}
		println "TO DELETE "+oid
		EntityManager em = getEM()
		EntityTransaction entr
		try{
			
			//get sid to delete
			def sids = getJpaTemplate().find("SELECT sidId FROM AclEntry ae WHERE ae.aclOiId="+oid.id.getId())
			println "SIDS TO DELETE: "+commify(sids);
			
			def aces = getJpaTemplate().find("SELECT id FROM AclEntry ae WHERE ae.aclOiId="+oid.id.getId())
			println "ACES TO DELETE: "+commify(aces);
			
			
			aces.each{
				//aclEntry
				println "DELETING ACE "+it
				entr=em.getTransaction();
				entr.begin();
				Query query=em.createQuery("DELETE FROM AclEntry ae WHERE ae.id= "+it);
				//query.setParameter(1, oid.id.getId());
				println "DELETE FROM AclEntry ae WHERE ae.id= "+it
				int deleteRecord=query.executeUpdate();
				entr.commit();
				System.out.println(deleteRecord+" are deleted.");
			}
			
			//aclObjectIdentity
			entr=em.getTransaction();
			entr.begin();
			Query query=em.createQuery("DELETE FROM AclObjectIdentity oi WHERE oi.objectId= "+oid.id.getId());
			//query.setParameter(1, oid.id.getId());
			println "DELETE FROM AclObjectIdentity oi WHERE oi.objectId= "+oid.id.getId()
			int deleteRecord=query.executeUpdate();
			entr.commit();
			System.out.println(deleteRecord+" are deleted.");
			
			
			def delSids = sids
			println "COMMIFIED "+delSids 
			delSids.each{
				//aclSid
				entr=em.getTransaction();
				entr.begin();
				query=em.createQuery("DELETE FROM AclSid s WHERE s.id="+it);
				//query.setParameter(1, oid.id.getId());
				println "DELETE FROM AclSid oi WHERE oi.id="+it
				deleteRecord=query.executeUpdate();
				entr.commit();
				System.out.println(deleteRecord+" are deleted.");
			}
			
		}
		catch(Exception ex){
			ex.printStackTrace()
			entr.rollback();
		}
		finally{
			em.close();
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#updateAcl(
	 * 	org.springframework.security.acls.model.MutableAcl)
	 */
	@Transactional
	MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
//		println "UPDATE ACL"
		Assert.notNull acl.id, "Object Identity doesn't provide an identifier"

		// Delete this ACL's ACEs in the acl_entry table
		deleteEntries retrieveObjectIdentity(acl.objectIdentity)

		
		// Create this ACL's ACEs in the acl_entry table
		createEntries acl

		// Change the mutable columns in acl_object_identity
		updateObjectIdentity acl

		// Clear the cache, including children
		clearCacheIncludingChildren acl.objectIdentity

		def retval = readAclById(acl.objectIdentity)
//		println "UPDATE ACL RETURNS: "+retval
		retval
	}

	protected void createEntries(MutableAcl acl) {
		int i = 0
//		println "CREATE ENTRIES"
		def jpa = getJpaTemplate()
		
		for (AuditableAccessControlEntry entry in acl.entries) {
			Assert.isInstanceOf AccessControlEntryImpl, entry, 'Unknown ACE class'
			def sid =  createOrRetrieveSid(entry.sid, true)
			def aclOi = jpa.find(AclObjectIdentity.class, acl.id)
			
			save new AclEntry(
					aclOiId: aclOi.id.getId(),
					aceOrder: i++,
					sidId: sid.id,
					mask: entry.permission.mask,
					granting: entry.isGranting(),
					auditSuccess: entry.isAuditSuccess(),
					auditFailure: entry.isAuditFailure())
		}
	}

	protected void updateObjectIdentity(MutableAcl acl) {
//		println "UPDATE OBJECT IDENTITY"
		Assert.notNull acl.owner, "Owner is required in this implementation"
		def jpa = getJpaTemplate()
		
		AclObjectIdentity aclObjectIdentity = jpa.find(AclObjectIdentity.class, acl.getId())

		AclObjectIdentity parent
		if (acl.parentAcl) {
			def oii = acl.parentAcl.objectIdentity
			Assert.isInstanceOf ObjectIdentityImpl, oii,
				'Implementation only supports ObjectIdentityImpl'
			parent = retrieveObjectIdentity(oii)
		}
		
		
		if(parent){
			println "PARENT FOUND: "+parent
			aclObjectIdentity.parentId = parent.id
			aclObjectIdentity.parentClassName = parent.aclClassName
		}
		def owner = createOrRetrieveSid(acl.owner, true)
		aclObjectIdentity.ownerId = owner.id
		aclObjectIdentity.entriesInheriting = acl.isEntriesInheriting()
	}

	protected void clearCacheIncludingChildren(ObjectIdentity objectIdentity) {
//		println "CLEAR CACHE INCLUDING CHILDREN"
		Assert.notNull objectIdentity, 'ObjectIdentity required'
		for (child in findChildren(objectIdentity)) {
			clearCacheIncludingChildren child
		}
		aclCache.evictFromCache(objectIdentity)
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#findChildren(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	List<ObjectIdentity> findChildren(ObjectIdentity parent) {
//		println "FIND CHILDREN"
		
		def jpa = getJpaTemplate()
		
		def children = jpa.find(
				"SELECT FROM AclObjectIdentity " +
				"WHERE parentId =  " + parent.identifier,
				"  AND parentClassName = "+parent.type.toString(),
				[])

		if (!children) {
			return null
		}

		def results = []
		for (AclObjectIdentity aclObjectIdentity in children) {
			results << new ObjectIdentityImpl(
					lookupClass(aclObjectIdentity.aclClass.className), aclObjectIdentity.objectId)
		}
		results
		
//		println "FIND CHILDREN RETURNS: "+results
	}

	protected Class<?> lookupClass(String className) {
		// workaround for Class.forName() not working in tests
		return Class.forName(className, true, Thread.currentThread().contextClassLoader)
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclById(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	Acl readAclById(ObjectIdentity object) throws NotFoundException {
		readAclById object, null
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclById(
	 * 	org.springframework.security.acls.model.ObjectIdentity, java.util.List)
	 */
	Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
//		println "READ ACL BY ID"
		Map<ObjectIdentity, Acl> map = readAclsById([object], sids)
		
		Assert.isTrue map.containsKey(object),
				"There should have been an Acl entry for ObjectIdentity $object"
//		println "READ ACL BY ID RETURNS: "+map[object]
		map[object]
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclsById(java.util.List)
	 */
	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
		readAclsById objects, null
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclsById(
	 * 	java.util.List, java.util.List)
	 */
	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
//		println "READ ACLS BY ID"
		Map<ObjectIdentity, Acl> result = aclLookupStrategy.readAclsById(objects, sids)
		// Check every requested object identity was found (throw NotFoundException if needed)
		for (ObjectIdentity object in objects) {
			if (!result.containsKey(object)) {
				throw new NotFoundException(
						"Unable to find ACL information for object identity '$object'")
			}
		}
//		println "READ ACLS BY ID RETURNS: "+result
		return result
	}

	protected AclObjectIdentity retrieveObjectIdentity(ObjectIdentity oid) {
//		println "RETRIEVING ACL OI"
		
		def jpa = getJpaTemplate()
		
		def aclois = jpa.find("SELECT FROM AclObjectIdentity  "+
							  "WHERE objectId = "+oid.identifier+" AND aclClassName = '"+oid.type+"'",
							  [objectId: oid.identifier])
							  
//		println "RETRIEVING ACL OI RETURNS: "+aclois
		
		
		aclois[0]
		
		/*return AclObjectIdentity.executeQuery(
				"FROM AclObjectIdentity " +
				"WHERE aclClass.className = :className " +
				"  AND objectId = :objectId",
				[className: oid.type,
				 objectId: oid.identifier])[0]*/
	}

	protected save(bean){
		/*bean.validate()
		if (bean.hasErrors()) {
			if (log.isEnabledFor(Level.WARN)) {
				def message = new StringBuilder(
						"problem creating ${bean.getClass().simpleName}: $bean")
				def locale = Locale.getDefault()
				for (fieldErrors in bean.errors) {
					for (error in fieldErrors.allErrors) {
						message.append('\n\t')
						message.append(messageSource.getMessage(error, locale))
					}
				}
				log.warn message
			}
		}
		else {*/
//		println "SAVING: "+bean
		def em = getEM()
		def tx = null;
		//em.transaction.begin()
		
		//em.transaction.commit()
		
		try {
			tx = em.getTransaction();
			tx.begin();
			em.persist( bean )
			tx.commit();
		}
		catch (RuntimeException e) {
			if ( tx != null && tx.isActive() ) tx.rollback();
			println "!#####! TRANSACTION ROLLED BACK: \n\n\n"+e.getStackTrace()+"\n\n\n########################################"
			//throw e; // or display error message
		}
		finally {
			em.close();
		}
		
//		println "SAVED: "+bean
		
		//}
		
		bean
	}
	
	def commify(items) {
        if (!items) return items
        def sepchar = items.find{ it =~ /,/ } ? '; ' : ', '
        
        def ret = items.join(sepchar) + sepchar
        
		ret.substring(0, (ret.size() - 2))
	}
	
	private EntityManager getEM(){
		def emf = AppEngineEntityManagerFactory.get()
		EntityTransaction tx = null
		
		def em = emf.createEntityManager()
		
		em
	}
	
	private JpaTemplate getJpaTemplate(){
		def em = getEM()
		def jpa = new JpaTemplate(em)
		jpa
	}
}
