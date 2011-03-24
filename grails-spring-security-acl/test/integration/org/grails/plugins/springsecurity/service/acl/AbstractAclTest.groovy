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

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.context.SecurityContextHolder as SCH

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
abstract class AbstractAclTest extends GroovyTestCase {

	protected static final String USER = 'username'
	protected static final String ADMIN = 'admin'

	def aclUtilService

	protected long report1Id
	protected long report2Id

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
		report1Id = new Report(name: 'r1').save().id
		report2Id = new Report(name: 'r2').save().id
		assertEquals 2, Report.count()
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		SCH.clearContext()
	}

	protected void loginAsAdmin() {
		login createAdminAuth()
	}

	protected void loginAsUser() {
		login createUserAuth()
	}

	protected void login(Authentication authentication) {
		SCH.context.authentication = authentication
	}

	protected Authentication createAuth(String username, String role) {
		new UsernamePasswordAuthenticationToken(
				username, 'password', [new GrantedAuthorityImpl(role)])
	}

	protected createAdminAuth() {
		createAuth ADMIN, 'ROLE_ADMIN'
	}

	protected createUserAuth() {
		createAuth USER, 'ROLE_USER'
	}
}
