/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */

/*
 *
 * Copyright 2005-2006 Sun Microsystems, Inc. All Rights Reserved.
 */


package javax.annotation;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * The Resource annotation marks a resource that is needed
 * by the application.  This annotation may be applied to an
 * application component class, or to fields or methods of the
 * component class.  When the annotation is applied to a
 * field or method, the container will inject an instance
 * of the requested resource into the application component
 * when the component is initialized.  If the annotation is
 * applied to the component class, the annotation declares a
 * resource that the application will look up at runtime. <p>
 *
 * Even though this annotation is not marked Inherited, deployment
 * tools are required to examine all superclasses of any component
 * class to discover all uses of this annotation in all superclasses.
 * All such annotation instances specify resources that are needed
 * by the application component.  Note that this annotation may
 * appear on private fields and methods of superclasses; the container
 * is required to perform injection in these cases as well.
 *
 * @since Common Annotations 1.0
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
public @interface Resource {
    /**
     * The JNDI name of the resource.  For field annotations,
     * the default is the field name.  For method annotations,
     * the default is the JavaBeans property name corresponding
     * to the method.  For class annotations, there is no default
     * and this must be specified.
     */
    String name() default "";

    /**
     * The Java type of the resource.  For field annotations,
     * the default is the type of the field.  For method annotations,
     * the default is the type of the JavaBeans property.
     * For class annotations, there is no default and this must be
     * specified.
     */
    Class type() default java.lang.Object.class;

    /**
     * The two possible authentication types for a resource.
     */
    enum AuthenticationType {
	    CONTAINER,
	    APPLICATION
    }

    /**
     * The authentication type to use for this resource.
     * This may be specified for resources representing a
     * connection factory of any supported type, and must
     * not be specified for resources of other types.
     */
    AuthenticationType authenticationType() default AuthenticationType.CONTAINER;

    /**
     * Indicates whether this resource can be shared between
     * this component and other components.
     * This may be specified for resources representing a
     * connection factory of any supported type, and must
     * not be specified for resources of other types.
     */
    boolean shareable() default true;

    /**
     * A product specific name that this resource should be mapped to.
     * The name of this resource, as defined by the <code>name</code>
     * element or defaulted, is a name that is local to the application
     * component using the resource.  (It's a name in the JNDI
     * <code>java:comp/env</code> namespace.)  Many application servers
     * provide a way to map these local names to names of resources
     * known to the application server.  This mapped name is often a
     * <i>global</i> JNDI name, but may be a name of any form. <p>
     *
     * Application servers are not required to support any particular
     * form or type of mapped name, nor the ability to use mapped names.
     * The mapped name is product-dependent and often installation-dependent.
     * No use of a mapped name is portable.
     */
    String mappedName() default "";

    /**
     * Description of this resource.  The description is expected
     * to be in the default language of the system on which the
     * application is deployed.  The description can be presented
     * to the Deployer to help in choosing the correct resource.
     */
    String description() default "";
}