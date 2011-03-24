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
package org.codehaus.groovy.grails.plugins.springsecurity

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.vote.AuthenticatedVoter
import org.springframework.security.access.vote.RoleVoter
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler
import org.springframework.security.web.util.AntUrlPathMatcher

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class InterceptUrlMapFilterInvocationDefinitionTests extends GroovyTestCase {

	private _fid = new InterceptUrlMapFilterInvocationDefinition()

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
		CH.config = new ConfigObject()
		_fid.urlMatcher = new AntUrlPathMatcher()
	}

	void testAfterPropertiesSet() {
		_fid.urlMatcher = null // simulate not having set it

		assertEquals 'url matcher is required', shouldFail(IllegalArgumentException) {
			_fid.afterPropertiesSet()
		}

		_fid.urlMatcher = new AntUrlPathMatcher()

		_fid.afterPropertiesSet()
	}

	void testStoreMapping() {

		assertEquals 0, _fid.configAttributeMap.size()

		_fid.storeMapping '/foo/bar', ['ROLE_ADMIN']
		assertEquals 1, _fid.configAttributeMap.size()

		_fid.storeMapping '/foo/bar', ['ROLE_USER']
		assertEquals 1, _fid.configAttributeMap.size()

		_fid.storeMapping '/other/path', ['ROLE_SUPERUSER']
		assertEquals 2, _fid.configAttributeMap.size()
	}

	void testInitialize() {
		ReflectionUtils.setConfigProperty('interceptUrlMap',
				['/foo/**': 'ROLE_ADMIN',
				 '/bar/**': ['ROLE_BAR', 'ROLE_BAZ']])

		_fid.roleVoter = new RoleVoter()
		_fid.authenticatedVoter = new AuthenticatedVoter()
		_fid.expressionHandler = new DefaultWebSecurityExpressionHandler()

		assertEquals 0, _fid.configAttributeMap.size()

		_fid.initialize()
		assertEquals 2, _fid.configAttributeMap.size()

		_fid.resetConfigs()

		_fid.initialize()
		assertEquals 0, _fid.configAttributeMap.size()
	}

	void testDetermineUrl() {

		def request = new MockHttpServletRequest()
		def response = new MockHttpServletResponse()
		def chain = new MockFilterChain()
		request.contextPath = '/context'

		request.requestURI = '/context/foo'
		assertEquals '/foo', _fid.determineUrl(new FilterInvocation(request, response, chain))

		request.requestURI = '/context/fOo/Bar?x=1&y=2'
		assertEquals '/foo/bar', _fid.determineUrl(new FilterInvocation(request, response, chain))
	}

	void testSupports() {
		assertTrue _fid.supports(FilterInvocation)
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		CH.config = null
		SpringSecurityUtils.securityConfig = null
	}
}
