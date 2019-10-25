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


/**
 * An annotation which allows to restrict access to product features via multiple
 * {@link LicenseConstraint} annotations. If <b>all</b> specified constraints are fulfilled, the
 * user is able to access the product feature (e.g. the use of a RapidMiner operator). </br>
 * {@link Object}s can be checked for any annotations of this package via
 * {@link LicenseManager#isAllowedByAnnotations(Object)} or
 * {@link LicenseManager#checkAnnotationViolations(Object, boolean)}.
 *
 * @since 3.2.0
 *
 * @author Nils Woehler
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LicenseConstraints {

	/**
	 * The license constraints which need to be fulfilled to allow access to the checked product
	 * feature.
	 */
	LicenseConstraint[] value();

}
