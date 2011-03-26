package org.codehaus.groovy.grails.plugins.springsecurity

import java.util.Collection;
import java.util.List;
import org.grails.appengine.AppEngineEntityManagerFactory
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction
import javax.persistence.PersistenceContext;
import javax.persistence.Persistence;
import org.apache.log4j.Logger;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.ExtendedEntityManagerCreator;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException

class JPAUserDetailsService implements GrailsUserDetailsService {
		private Logger _log = Logger.getLogger(getClass())
	
		/**
		 * Some Spring Security classes (e.g. RoleHierarchyVoter) expect at least one role, so
		 * we give a user with no granted roles this one which gets past that restriction but
		 * doesn't grant anything.
		 */
		static final List NO_ROLES = [new GrantedAuthorityImpl(SpringSecurityUtils.NO_ROLE)]
	
		/** Dependency injection for the application. */
		def jpaTemplate
		def grailsApplication	
		/**
		 * {@inheritDoc}
		 * @see org.codehaus.groovy.grails.plugins.springsecurity.GrailsUserDetailsService#loadUserByUsername(
		 * 	java.lang.String, boolean)
		 */
		UserDetails loadUserByUsername(String username, boolean loadRoles) throws UsernameNotFoundException {
			def conf = SpringSecurityUtils.securityConfig
			
			String userClassname = conf.userLookup.userDomainClassName
			
			def emf = AppEngineEntityManagerFactory.get()
			EntityTransaction tx = null;
			
			def em = emf.createEntityManager();
			def jpa = new JpaTemplate(em)
			
			//TODO reactivate transactions
			//tx = em.getTransaction()
			//tx.begin()
				def user = jpa.find("select r from "+userClassname+" r where r.username = ?1", username);
				if (user.size() == 0) {
					log.warn "User not found: $username"
					throw new UsernameNotFoundException('User not found', username)
				}
				else{
					
					user = user[0]
					//def roles = [new GrantedAuthorityImpl("ROLE_USER")];
					Collection<GrantedAuthority> authorities = loadAuthorities(user, username, loadRoles)
						
					def authUser = createUserDetails(user, authorities)
					println "PASSWORD FOUND"+authUser.password
					println authUser
					authUser
				}
				
			//tx.commit()
			
			
		}
	
		/**
		 * {@inheritDoc}
		 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(
		 * 	java.lang.String)
		 */
		UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			loadUserByUsername username, true
		}
	
		protected Collection<GrantedAuthority> loadAuthorities(user, String username, boolean loadRoles) {
			if (!loadRoles) {
				return []
			}
			def conf = SpringSecurityUtils.securityConfig
	
			String authoritiesPropertyName = conf.userLookup.authoritiesPropertyName
			String authorityPropertyName = conf.authority.nameField
			def authorities = null
			Collection<?> userAuthorities = user.authorities//user."$authoritiesPropertyName"
			println "AUTHORITIES:"+ userAuthorities
			if(userAuthorities.size() > 0){
				authorities = userAuthorities.collect { new GrantedAuthorityImpl(it.authority) }
			}
			authorities ?: NO_ROLES
		}
	
		protected UserDetails createUserDetails(user, Collection<GrantedAuthority> authorities) {
	
			def conf = SpringSecurityUtils.securityConfig
	
			String usernamePropertyName = conf.userLookup.usernamePropertyName
			String passwordPropertyName = conf.userLookup.passwordPropertyName
			String enabledPropertyName = conf.userLookup.enabledPropertyName
			String accountExpiredPropertyName = conf.userLookup.accountExpiredPropertyName
			String accountLockedPropertyName = conf.userLookup.accountLockedPropertyName
			String passwordExpiredPropertyName = conf.userLookup.passwordExpiredPropertyName
	
			String username = user."$usernamePropertyName"
			String password = user."$passwordPropertyName"
			
			
			println "password: "+user.password.toString()
			boolean enabled = enabledPropertyName ? user."$enabledPropertyName" : true
			boolean accountExpired = false;//accountExpiredPropertyName ? user."$accountExpiredPropertyName" : false
			boolean accountLocked = false;//accountLockedPropertyName ? user."$accountLockedPropertyName" : false
			boolean passwordExpired = false;//passwordExpiredPropertyName ? user."$passwordExpiredPropertyName" : false
	
			new GrailsUser(username, password, enabled, !accountExpired, !passwordExpired,
					!accountLocked, authorities, user.id)
		}
	
		protected Logger getLog() { _log }
}

