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
package org.codehaus.groovy.grails.plugins.springsecurity.acl

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GormAclLookupStrategyTests extends GroovyTestCase {

	def aclLookupStrategy
	def ehcacheAclCache
	def sessionFactory

	private aclClass
	private sid
	private aclObjectIdentity
	private final Sid principalSid = new PrincipalSid('ben')
	private final ObjectIdentity topParentOid = new ObjectIdentityImpl(Report, 100L)
	private final ObjectIdentity middleParentOid = new ObjectIdentityImpl(Report, 101L)
	private final ObjectIdentity childOid = new ObjectIdentityImpl(Report, 102L)

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()

		aclLookupStrategy.batchSize = 50

		sid = new AclSid(sid: 'ben', principal: true).save()

		aclClass = new AclClass(className: Report.name).save()

		aclObjectIdentity = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 100L,
				owner: sid,
				entriesInheriting: true).save()

		new AclEntry(
				aclObjectIdentity: aclObjectIdentity,
				sid: sid,
				mask: 1,
				granting: true).save()

		new AclEntry(
				aclObjectIdentity: aclObjectIdentity,
				aceOrder: 1,
				sid: sid,
				mask: 2).save()

		def aclObjectIdentity2 = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 101L,
				parent: aclObjectIdentity,
				owner: sid,
				entriesInheriting: true).save()

		new AclEntry(
				aclObjectIdentity: aclObjectIdentity2,
				sid: sid,
				mask: 8,
				granting: true).save()

		def aclObjectIdentity3 = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 102L,
				parent: aclObjectIdentity2,
				owner: sid,
				entriesInheriting: true).save()

		new AclEntry(
				aclObjectIdentity: aclObjectIdentity3,
				sid: sid,
				mask: 8).save()

		flushAndClear()
	}

	void testAclsRetrievalWithDefaultBatchSize() {
		// Deliberately use an integer for the child, to reproduce bug report in SEC-819
		ObjectIdentity childOid = new ObjectIdentityImpl(Report, 102L)
		Map<ObjectIdentity, Acl> map = aclLookupStrategy.readAclsById(
				[topParentOid, middleParentOid, childOid], null)
		checkEntries topParentOid, middleParentOid, childOid, map
	}

	void testAclsRetrievalFromCacheOnly() {
		// Objects were put in cache
		aclLookupStrategy.readAclsById([topParentOid, middleParentOid, childOid], null)

		// Let's empty the database to force acls retrieval from cache
		AclEntry.list()*.delete()
		AclObjectIdentity.list()*.delete()
		AclClass.list()*.delete()
		AclSid.list()*.delete()
		flushAndClear()

		Map<ObjectIdentity, Acl> map = aclLookupStrategy.readAclsById(
				[topParentOid, middleParentOid, childOid], null)
		checkEntries topParentOid, middleParentOid, childOid, map
	}

	void testAclsRetrievalWithCustomBatchSize() {
		// Set a batch size to allow multiple database queries in order to retrieve all acls
		aclLookupStrategy.batchSize = 1
		Map<ObjectIdentity, Acl> map = aclLookupStrategy.readAclsById(
				[topParentOid, middleParentOid, childOid], null)
		checkEntries topParentOid, middleParentOid, childOid, map
	}

	void testAllParentsAreRetrievedWhenChildIsLoaded() {
		new AclObjectIdentity(
			aclClass: aclClass,
			objectId: 103L,
			parent: aclObjectIdentity,
			owner: sid,
			entriesInheriting: true).save()
		flushAndClear()

		ObjectIdentity middleParent2Oid = new ObjectIdentityImpl(Report, 103L)

		// Retrieve the child
		Map map = aclLookupStrategy.readAclsById([childOid], null)

		// Check that the child and all its parents were retrieved
		assertNotNull map[childOid]
		assertEquals childOid, map[childOid].objectIdentity
		assertNotNull map[middleParentOid]
		assertEquals middleParentOid, map[middleParentOid].objectIdentity
		assertNotNull map.get(topParentOid)
		assertEquals topParentOid, map[topParentOid].objectIdentity

		// The second parent shouldn't have been retrieved
		assertNull map[middleParent2Oid]
	}

	/**
	 * Test created from SEC-590.
	 */
	void testReadAllObjectIdentitiesWhenLastElementIsAlreadyCached() {
		def aclObjectIdentity4 = new AclObjectIdentity(
			aclClass: aclClass,
			objectId: 104L,
			owner: sid,
			entriesInheriting: true).save()

		def aclObjectIdentity5 = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 105L,
				parent: aclObjectIdentity4,
				owner: sid,
				entriesInheriting: true).save()

		def aclObjectIdentity6 = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 106L,
				parent: aclObjectIdentity4,
				owner: sid,
				entriesInheriting: true).save()

		def aclObjectIdentity7 = new AclObjectIdentity(
				aclClass: aclClass,
				objectId: 107L,
				parent: aclObjectIdentity5,
				owner: sid,
				entriesInheriting: true).save()

		new AclEntry(
				aclObjectIdentity: aclObjectIdentity4,
				sid: sid,
				mask: 1,
				granting: true).save()

		flushAndClear()

		ObjectIdentity grandParentOid = new ObjectIdentityImpl(Report, 104L)
		ObjectIdentity parent1Oid = new ObjectIdentityImpl(Report, 105L)
		ObjectIdentity parent2Oid = new ObjectIdentityImpl(Report, 106L)
		ObjectIdentity childOid = new ObjectIdentityImpl(Report, 107L)

		// First lookup only child, thus populating the cache with grandParent, parent1 and child
		List<Permission> checkPermission = [BasePermission.READ]
		List<Sid> sids = [principalSid]
		List<ObjectIdentity> childOids = [childOid]

		aclLookupStrategy.batchSize = 6
		Map foundAcls = aclLookupStrategy.readAclsById(childOids, sids)

		Acl foundChildAcl = foundAcls[childOid]
		assertNotNull foundChildAcl
		assertTrue foundChildAcl.isGranted(checkPermission, sids, false)

		// Search for object identities has to be done in the following order: last element have to be one which
		// is already in cache and the element before it must not be stored in cache
		List<ObjectIdentity> allOids = [grandParentOid, parent1Oid, parent2Oid, childOid]
		foundAcls = aclLookupStrategy.readAclsById(allOids, sids)

		Acl foundParent2Acl = foundAcls[parent2Oid]
		assertNotNull foundParent2Acl
		assertTrue foundParent2Acl.isGranted(checkPermission, sids, false)
	}

	private void checkEntries(ObjectIdentity topParentOid, ObjectIdentity middleParentOid,
			ObjectIdentity childOid, Map<ObjectIdentity, Acl> map) {

		assertEquals 3, map.size()

		MutableAcl topParent = map[topParentOid]
		MutableAcl middleParent = map[middleParentOid]
		MutableAcl child = map[childOid]

		// Check the retrieved versions has IDs
		assertNotNull topParent.id
		assertNotNull middleParent.id
		assertNotNull child.id

		// Check their parents were correctly retrieved
		assertNull topParent.parentAcl
		assertEquals topParentOid, middleParent.parentAcl.objectIdentity
		assertEquals middleParentOid, child.parentAcl.objectIdentity

		// Check their ACEs were correctly retrieved
		assertEquals 2, topParent.entries.size()
		assertEquals 1, middleParent.entries.size()
		assertEquals 1, child.entries.size()

		// Check object identities were correctly retrieved
		assertEquals topParentOid, topParent.objectIdentity
		assertEquals middleParentOid, middleParent.objectIdentity
		assertEquals childOid, child.objectIdentity

		// Check each entry
		assertTrue topParent.isEntriesInheriting()
		assertEquals topParent.owner, principalSid
		assertEquals topParent.entries[0].permission, BasePermission.READ
		assertEquals topParent.entries[0].sid, principalSid
		assertFalse topParent.entries[0].isAuditFailure()
		assertFalse topParent.entries[0].isAuditSuccess()
		assertTrue topParent.entries[0].isGranting()

		assertEquals topParent.entries[1].permission, BasePermission.WRITE
		assertEquals topParent.entries[1].sid, principalSid
		assertFalse topParent.entries[1].isAuditFailure()
		assertFalse topParent.entries[1].isAuditSuccess()
		assertFalse topParent.entries[1].isGranting()

		assertTrue middleParent.isEntriesInheriting()
		assertEquals middleParent.owner, principalSid
		assertEquals middleParent.entries[0].permission, BasePermission.DELETE
		assertEquals middleParent.entries[0].sid, principalSid
		assertFalse middleParent.entries[0].isAuditFailure()
		assertFalse middleParent.entries[0].isAuditSuccess()
		assertTrue middleParent.entries[0].isGranting()

		assertTrue child.isEntriesInheriting()
		assertEquals child.owner, principalSid
		assertEquals child.entries[0].permission, BasePermission.DELETE
		assertEquals child.entries[0].sid, principalSid
		assertFalse child.entries[0].isAuditFailure()
		assertFalse child.entries[0].isAuditSuccess()
		assertFalse child.entries[0].isGranting()
	}

	private void flushAndClear() {
		sessionFactory.currentSession.flush()
		sessionFactory.currentSession.clear()
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		ehcacheAclCache.removeAll()
	}
}
