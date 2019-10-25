/**
 * Copyright (c) 2013-2018, RapidMiner GmbH, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 */
package base.operators.license.annotation;

import base.operators.license.LicenseManager;
import base.operators.license.product.Product;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation which allows to restrict access to product features (e.g. usage of RapidMiner
 * operators) for specific product license levels. In case the product license level is not
 * fulfilled the feature (e.g. the RapidMiner operator) is restricted and cannot be used. To check
 * whether the product license fulfills the specification the current license precedence is compared
 * to the precedence defined by this annotation. </br>
 * {@link Object}s can be checked for any annotations of this package via
 * {@link LicenseManager#isAllowedByAnnotations(Object)} or
 * {@link LicenseManager#checkAnnotationViolations(Object, boolean)}. Currently, only one fulfilled
 * license annotation is enough to allow the usage of a product feature.
 *
 * @since 2.0.0
 *
 * @author Nils Woehler
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LicenseLevel {

	/**
	 * The comparison method that should be used for comparing allowed license precedences.
	 */
	public enum ComparisonMethod {
		/**
		 * The precedence of the license must be greater than or equal to (>=) the annotated value.
		 */
		GE,
		/**
		 * The precedence of the product must be equal to (==) to the annotated value.
		 */
		EQ
	}

	/**
	 * Specifies a regular expression for the product ID. The {@link LicenseManager} will check the
	 * license of the first registered product matching this regex. In most cases it is recommended
	 * to enter the exact product ID that should be matched. </br>
	 * In case the license for RapidMiner core product (e.g. RapidMiner Studio, RapidMiner Server,
	 * etc.) should be matched use the regex defined by {@link Product#RM_REGEX}.
	 */
	String productId();

	/**
	 * Specifies by default the minimum license precedence necessary to allow access to the
	 * operator. Can be changed to exact license precedence matching by changing the comparison
	 * method to {@link ComparisonMethod#EQ}.
	 */
	int precedence();

	/**
	 * Specifies the comparison method to compare license precedences. By default it is
	 * {@link ComparisonMethod#GE}.
	 */
	ComparisonMethod comparison() default ComparisonMethod.GE;

	/**
	 * The I18N base key which should be used in case the {@link LicenseConstraint} annotation has
	 * been violated. <br/>
	 * If a value has been specified the full I18N keys will look like this: <br/>
	 * <br/>
	 * {@code gui.dialog.license.license_level_violation.$I18N_KEY.title}<br/>
	 * {@code gui.dialog.license.license_level_violation.$I18N_KEY.icon}<br/>
	 * {@code gui.dialog.license.license_level_violation.$I18N_KEY.message}<br/>
	 * <br/>
	 * In case no value has been specified the full I18N key will use the value specified in
	 * {@link #productId()}: <br/>
	 * <br/>
	 * {@code gui.dialog.license.constraint_violation.$CONSTRAINT_ID.title}<br/>
	 * {@code gui.dialog.license.constraint_violation.$CONSTRAINT_ID.icon}<br/>
	 * {@code gui.dialog.license.constraint_violation.$CONSTRAINT_ID.message}<br/>
	 */
	String i18nKey() default "";

}
