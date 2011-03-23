// Place your Spring DSL code here
beans = {
	userDetailsService(org.codehaus.groovy.grails.plugins.springsecurity.JPAUserDetailsService)
	aclLookupStrategy(org.codehaus.groovy.grails.plugins.springsecurity.acl.JPAAclLookupStrategy){
		aclAuthorizationStrategy = ref('aclAuthorizationStrategy')
		aclCache = ref('aclCache')
		permissionFactory = ref('aclPermissionFactory')
		auditLogger = ref('aclAuditLogger')
	}
}
