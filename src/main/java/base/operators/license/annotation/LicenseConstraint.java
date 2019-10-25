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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import base.operators.license.LicenseManager;
import base.operators.license.product.Constraint;
import base.operators.license.product.Product;


/**
 * An annotation which allows to restrict access to product features via a license
 * {@link Constraint}. If the specified constraint is fulfilled, the user is able to access the
 * product feature (e.g. the use of a RapidMiner operator). </br>
 * {@link Object}s can be checked for any annotations of this package via
 * {@link LicenseManager#isAllowedByAnnotations(Object)} or
 * {@link LicenseManager#checkAnnotationViolations(Object, boolean)}. Currently, one fulfilled
 * license annotation is sufficient to allow the usage of a product feature.
 *
 * @since 2.0.0
 *
 * @author Nils Woehler
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LicenseConstraint {

	/**
	 * Specifies a regular expression for the product ID. The {@link LicenseManager} will check the
	 * license of the first registered product matching this regex. In most cases it is recommended
	 * to enter the exact product ID that should be matched. </br>
	 * In case the license for RapidMiner core product (e.g. RapidMiner Studio, RapidMiner Server,
	 * etc.) should be matched use the regex defined by {@link Product#RM_REGEX}.
	 */
	String productId();

	/**
	 * Specifies the ID of the constraint which should be checked. Make sure that the license of the
	 * specified product contains this constraint as otherwise the access to the product feature is
	 * not possible at all.
	 */
	String constraintId();

	/**
	 * Specifies the constraint value which must be fulfilled to be able to use the product feature.
	 */
	String value();

	/**
	 * The I18N base key which should be used in case the {@link LicenseConstraint} annotation has
	 * been violated. It should not start or end with a dot.<br/>
	 * If a value has been specified (e.g. "example_key") the full I18N keys will look like this:
	 * <br/>
	 * <br/>
	 * {@code gui.dialog.license.constraint_violation.example_key.title}<br/>
	 * {@code gui.dialog.license.constraint_violation.example_key.icon}<br/>
	 * {@code gui.dialog.license.constraint_violation.example_key.message}<br/>
	 * <br/>
	 * In case no value has been specified the full I18N key will use the value specified in
	 * {@link #constraintId()}: <br/>
	 * <br/>
	 * {@code gui.dialog.license.constraint_violation.CONSTRAINT_ID.title}<br/>
	 * {@code gui.dialog.license.constraint_violation.CONSTRAINT_ID.icon}<br/>
	 * {@code gui.dialog.license.constraint_violation.CONSTRAINT_ID.message}<br/>
	 */
	String i18nKey() default "";

}
