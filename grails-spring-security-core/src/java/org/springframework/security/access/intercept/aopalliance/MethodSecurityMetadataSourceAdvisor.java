/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.access.intercept.aopalliance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.util.Assert;

/**
 * Advisor driven by a {@link MethodSecurityMetadataSource}, used to exclude a {@link MethodSecurityInterceptor} from
 * public (non-secure) methods.
 * <p>
 * Because the AOP framework caches advice calculations, this is normally faster than just letting the
 * <code>MethodSecurityInterceptor</code> run and find out itself that it has no work to do.
 * <p>
 * This class also allows the use of Spring's {@code DefaultAdvisorAutoProxyCreator}, which makes
 * configuration easier than setup a <code>ProxyFactoryBean</code> for each object requiring security. Note that
 * autoproxying is not supported for BeanFactory implementations, as post-processing is automatic only for application
 * contexts.
 * <p>
 * Based on Spring's TransactionAttributeSourceAdvisor.
 *
 * @author Ben Alex
 * @author Luke Taylor
 */
public class MethodSecurityMetadataSourceAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {
    //~ Instance fields ================================================================================================

    private transient MethodSecurityMetadataSource attributeSource;
    private transient MethodSecurityInterceptor interceptor;
    private final Pointcut pointcut = new MethodSecurityMetadataSourcePointcut();
    private BeanFactory beanFactory;
    private String adviceBeanName;
    private String metadataSourceBeanName;
    private transient volatile Object adviceMonitor = new Object();

    //~ Constructors ===================================================================================================

    /**
     * @deprecated use the decoupled approach instead
     */
    MethodSecurityMetadataSourceAdvisor(MethodSecurityInterceptor advice) {
        Assert.notNull(advice.getSecurityMetadataSource(), "Cannot construct a MethodSecurityMetadataSourceAdvisor using a " +
                "MethodSecurityInterceptor that has no SecurityMetadataSource configured");

        this.interceptor = advice;
        this.attributeSource = advice.getSecurityMetadataSource();
    }

    /**
     * Alternative constructor for situations where we want the advisor decoupled from the advice. Instead the advice
     * bean name should be set. This prevents eager instantiation of the interceptor
     * (and hence the AuthenticationManager). See SEC-773, for example. The metadataSourceBeanName is used rather than
     * a direct reference to support serialization via a bean factory lookup.
     *
     * @param adviceBeanName name of the MethodSecurityInterceptor bean
     * @param attributeSource the SecurityMetadataSource (should be the same as the one used on the interceptor)
     * @param attributeSourceBeanName the bean name of the attributeSource (required for serialization)
     */
    public MethodSecurityMetadataSourceAdvisor(String adviceBeanName, MethodSecurityMetadataSource attributeSource,
            String attributeSourceBeanName) {
        Assert.notNull(adviceBeanName, "The adviceBeanName cannot be null");
        Assert.notNull(attributeSource, "The attributeSource cannot be null");
        Assert.notNull(attributeSourceBeanName, "The attributeSourceBeanName cannot be null");

        this.adviceBeanName = adviceBeanName;
        this.attributeSource = attributeSource;
        this.metadataSourceBeanName = attributeSourceBeanName;
    }

    //~ Methods ========================================================================================================

    public Pointcut getPointcut() {
        return pointcut;
    }

    public Advice getAdvice() {
        synchronized (this.adviceMonitor) {
            if (interceptor == null) {
                Assert.notNull(adviceBeanName, "'adviceBeanName' must be set for use with bean factory lookup.");
                Assert.state(beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");
                interceptor = beanFactory.getBean(this.adviceBeanName, MethodSecurityInterceptor.class);
            }
            return interceptor;
        }
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        adviceMonitor = new Object();
        attributeSource = beanFactory.getBean(metadataSourceBeanName, MethodSecurityMetadataSource.class);
    }

    //~ Inner Classes ==================================================================================================

    class MethodSecurityMetadataSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {
        @SuppressWarnings("unchecked")
        public boolean matches(Method m, Class targetClass) {
            return attributeSource.getAttributes(m, targetClass) != null;
        }
    }

    /**
     * Represents a <code>MethodInvocation</code>.
     * <p>
     * Required as <code>MethodSecurityMetadataSource</code> only supports lookup of configuration attributes for
     * <code>MethodInvocation</code>s.
     */
    class InternalMethodInvocation implements MethodInvocation {
        private Method method;
        private Class<?> targetClass;

        public InternalMethodInvocation(Method method, Class<?> targetClass) {
            this.method = method;
            this.targetClass = targetClass;
        }

        protected InternalMethodInvocation() {
            throw new UnsupportedOperationException();
        }

        public Object[] getArguments() {
            throw new UnsupportedOperationException();
        }

        public Method getMethod() {
            return this.method;
        }

        public AccessibleObject getStaticPart() {
            throw new UnsupportedOperationException();
        }

        public Object getThis() {
            return this.targetClass;
        }

        public Object proceed() throws Throwable {
            throw new UnsupportedOperationException();
        }
    }
}
