package org.jhat.spring.mvc.extend.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.ValueConstants;

/**
 * @project spring-mvc-extend
 * @author jhat
 * @email cpf624@126.com
 * @date Mar 19, 20138:40:19 PM
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestAttribute {

	/**
	 * The name of the request parameter to bind to.
	 */
	String value() default "";

	/**
	 * Whether the parameter is required.
	 * <p>Default is <code>true</code>, leading to an exception thrown in case
	 * of the parameter missing in the request. Switch this to <code>false</code>
	 * if you prefer a <code>null</value> in case of the parameter missing.
	 * <p>Alternatively, provide a {@link #defaultValue() defaultValue},
	 * which implicitly sets this flag to <code>false</code>.
	 */
	boolean required() default true;

	/**
	 * The default value to use as a fallback. Supplying a default value implicitly
	 * sets {@link #required()} to false.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;
	
}
