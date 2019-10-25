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
package base.operators.license;

import java.util.List;

import base.operators.license.annotation.LicenseConstraint;
import base.operators.license.annotation.LicenseConstraints;
import base.operators.license.annotation.LicenseLevel;
import base.operators.license.location.LicenseLoadingException;
import base.operators.license.location.LicenseLocation;
import base.operators.license.location.LicenseStoringException;
import base.operators.license.product.Constraint;
import base.operators.license.product.Product;
import base.operators.license.utils.Pair;
import base.operators.license.violation.LicenseConstraintViolation;
import base.operators.license.violation.LicenseViolation;


/**
 * The {@link LicenseManager} is an interface for a class that manages {@link License}s for
 * {@link Product}s.
 *
 * In order to use the {@link LicenseManager} as an extension has to register an instance of
 * {@link Product} by calling {@link #registerProduct(Product)}. This {@link Product} instance can
 * then be used to retrieve the current active license, the next upcoming license and all loaded
 * licenses for the {@link Product}.</br>
 *
 * @author Nils Woehler
 *
 * @since 3.0.0
 *
 */
public interface LicenseManager {

	/**
	 * Registers a new product to the license manager. After registering all licenses for the
	 * registered product will be loaded and the best valid license will be activated. This method
	 * should be called when initializing the extension.
	 *
	 * @param newProduct
	 *            the new product to register
	 *
	 * @throws AlreadyRegisteredException
	 *             thrown if a product with the same productId has already been registered.
	 * @throws LicenseLoadingException
	 *             thrown if loading of licenses fails
	 * @throws InvalidProductException
	 *             if the product signature does not match the product characteristics
	 */
	void registerProduct(final Product newProduct)
			throws AlreadyRegisteredException, LicenseLoadingException, InvalidProductException;

	/**
	 * Checks an object (the object class itself and all of its super classes) for license
	 * violations implied by license annotations. The returned list will contain entries only if
	 * {@link #isAllowedByAnnotations(Object)} returns <code>false</code>.
	 *
	 * @since 2.0.0
	 *
	 * @param obj
	 *            the object to check
	 * @param informListeners
	 *            if set to <code>true</code> and list is not empty all
	 *            {@link LicenseManagerListener} listeners will be informed about license violations
	 * @return the list of license violations. Will be empty if
	 *         {@link #isAllowedByAnnotations(Object)} returns <code>true</code>.
	 */
	List<LicenseViolation> checkAnnotationViolations(final Object obj, final boolean informListeners);

	/**
	 * Checks if the provided value violates the constraint defined by the current active license.
	 * This method should be called after a user has performed an action which might violate
	 * constraints (e.g. when executing a restricted operator). </br>
	 *
	 * The check is done by retrieving the constraint from the currently installed license and
	 * invoking {@link Constraint#isAllowed(String, Object)}. If the constraint is not allowed, all
	 * {@link LicenseManagerListener}s are informed and a {@link LicenseConstraintViolation} is
	 * returned. </br>
	 * Here is an example on how to use it within an operator:
	 *
	 * <pre>
	 * <code>
	 * public static void doWork() {
	 * 	// retrieve example set from input port
	 * 	ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
	 *
	 * 	// check whether example set size is allowed by the current license
	 * 	LicenseConstraintViolation&lt;Integer, Integer&gt; violation = licenseManager.checkConstraintViolation(
	 * 			PluginInitExample.PRODUCT, PluginInitExample.CONSTRAINT_1, exampleSet.size(), true);
	 * 	if (violation != null) {
	 * 		// This exception will not show the usual UserError dialog,
	 * 		// the license violation will be handled by the LicenseManager listeners instead
	 * 		throw new LicenseViolationException(this, violation);
	 * 	}
	 *
	 * 	// License does allow the ExampleSet size
	 * 	// -&gt; execute operator logic
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param product
	 *            the product for the current active license
	 * @param constraint
	 *            the constraint which should be checked
	 * @param checkedValue
	 *            the value that should be checked for constraint violation
	 * @param informListeners
	 *            if set to <code>true</code> the {@link LicenseManagerListener} will be informed
	 *            about the license constraint violation.
	 *
	 * @param <S>
	 *            the class a constraint is stored with
	 * @param <C>
	 *            the class of the checked value
	 *
	 * @return If a constraint is violated a {@link LicenseConstraintViolation} object is returned.
	 *         Returns <code>null</code> if license constraint is not violated.
	 */
	<S, C> LicenseConstraintViolation<S, C> checkConstraintViolation(final Product product,
                                                                     final Constraint<S, C> constraint, final C checkedValue, final boolean informListeners);

	/**
	 * Checks if the provided value violates the constraint defined by the current active license
	 * and allows to specify a custom i18n error message key. This method should be called after a
	 * user has performed an action which might violate constraints (e.g. when executing a
	 * restricted operator). </br>
	 *
	 * The check is done by retrieving the constraint from the currently installed license and
	 * invoking {@link Constraint#isAllowed(String, Object)}. If the constraint is not allowed, all
	 * {@link LicenseManagerListener}s are informed and a {@link LicenseConstraintViolation} is
	 * returned. </br>
	 * Here is an example on how to use it within an operator:
	 *
	 * <pre>
	 * <code>
	 * public static void doWork() {
	 * 	// retrieve example set from input port
	 * 	ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
	 *
	 * 	// check whether example set size is allowed by the current license
	 * 	LicenseConstraintViolation&lt;Integer, Integer&gt; violation = licenseManager.checkConstraintViolation(
	 * 			PluginInitExample.PRODUCT, PluginInitExample.CONSTRAINT_1, "i18n_error_key", exampleSet.size(), true);
	 * 	if (violation != null) {
	 * 		// This exception will not show the usual UserError dialog,
	 * 		// the license violation will be handled by the LicenseManager listeners instead
	 * 		throw new LicenseViolationException(this, violation);
	 * 	}
	 *
	 * 	// License does allow the ExampleSet size
	 * 	// -&gt; execute operator logic
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param product
	 *            the product for the current active license
	 * @param constraint
	 *            the constraint which should be checked
	 * @param checkedValue
	 *            the value that should be checked for constraint violation
	 * @param i18nKey
	 *            the internationalization key of the error message
	 * @param informListeners
	 *            if set to <code>true</code> the {@link LicenseManagerListener} will be informed
	 *            about the license constraint violation.
	 *
	 * @param <S>
	 *            the class a constraint is stored with
	 * @param <C>
	 *            the class of the checked value
	 *
	 * @return If a constraint is violated a {@link LicenseConstraintViolation} object is returned.
	 *         Returns <code>null</code> if license constraint is not violated.
	 * @since 3.1.0
	 */
	<S, C> LicenseConstraintViolation<S, C> checkConstraintViolation(final Product product,
                                                                     final Constraint<S, C> constraint, final C checkedValue, String i18nKey, final boolean informListeners);

	/**
	 * First evaluates whether the license is still valid and then returns a copy of the current
	 * active license for the specified product. Cannot be <code>null</code>. In case no valid
	 * license is installed the starter license for the product is returned.
	 *
	 * @param product
	 *            the product to get the active license for
	 * @return the current active license. Cannot be <code>null</code>.
	 **/
	License getActiveLicense(final Product product);

	/**
	 * Returns the active licenses of all registered products. See
	 * {@link #getActiveLicense(Product)} for details.
	 *
	 * @return the list of active licenses
	 * @since 3.1.0
	 */
	List<License> getAllActiveLicenses();

	/**
	 * Checks the current active license of the specified product for the value of the provided
	 * constraint.
	 *
	 * @param product
	 *            the product that should be used to get the current active license
	 * @param constraint
	 *            the constraint object of the product
	 * @param <S>
	 *            the class a constraint is stored with
	 * @param <C>
	 *            the class of the checked value
	 * @return the value of the provided constraint. The constraint value is stored in the current
	 *         active license of the product. Returns <code>null</code> if usage of the feature is
	 *         forbidden.
	 *
	 * @throws ConstraintNotRestrictedException
	 *             is thrown if the current active license does not contain a value at all for the
	 *             specified constraint. This means the license allows the usage of the feature
	 *             without any Constraints.
	 */
	<S, C> S getConstraintValue(final Product product, final Constraint<S, C> constraint)
			throws ConstraintNotRestrictedException;

	/**
	 * @param product
	 *            the product for which the licenses should be fetched
	 *
	 * @return an unmodifiable list of copies of the licenses for the specified {@link Product}. Use
	 *         this function to check all licenses installed for the specified {@link Product}.
	 *
	 */
	List<License> getLicenses(final Product product);

	/**
	 * @param product
	 *            the product to get the upcoming license for
	 *
	 * @return a copy of the upcoming license that will be used after the current active license has
	 *         expired. Cannot be <code>null</code> but might return the starter product license
	 *         with 0 precedence and {@link #STARTER_LICENSE_EDITION_KEY} as product edition.
	 */
	License getUpcomingLicense(final Product product);

	/**
	 * @param license
	 *            the license to get the upcoming license for
	 *
	 * @return a copy of the upcoming license that will be used after the current active license has
	 *         expired. Cannot be <code>null</code> but might return the starter product license
	 *         with 0 precedence and {@link #STARTER_LICENSE_EDITION_KEY} as product edition.
	 * @throws UnknownProductException
	 *             in case the {@link Product} inferred from the provided license is not registered.
	 *             This can happen only in case no {@link Product} was provided when calling the
	 *             method.
	 */
	License getUpcomingLicense(final License license) throws UnknownProductException;

	/**
	 * Checks whether the specified constraint value is allowed by the current license. </br>
	 * </br>
	 * Therefore a constraint violation check is performed without notifying the
	 * {@link LicenseManagerListener}s if the constraint is violated. This method should be used in
	 * case a feature should be disabled before the user is able to interact with it. </br>
	 * Here is an example on how to use it to disable a button:
	 *
	 * <pre>
	 * <code>
	 * // create a button
	 * final JButton button = new JButton(&quot;example-feature-button&quot;);
	 *
	 * // check whether it should be enabled by the current active license
	 * button.setEnabled(licenseManager.isAllowed(PluginInitExample.PRODUCT, PluginInitExample.CONSTRAINT_3,
	 * 		&quot;example-category&quot;));
	 *
	 * // register a license manager listener to enable the button if the license changes
	 * licenseManager.registerLicenseManagerListener(new LicenseManagerListener() {
	 *
	 * 	&#064;Override
	 * 	public &lt;S, C&gt; void handleLicenseEvent(LicenseEvent&lt;S, C&gt; event) {
	 *
	 * 		// In case the active license has changed for the registered product
	 * 		if (event.getType() == LicenseEventType.ACTIVE_LICENSE_CHANGED
	 * 				&amp;&amp; PluginInitExample.PRODUCT_ID.equals(event.getNewLicense().getProductId())) {
	 *
	 * 			// check whether the new license allows the usage of the button
	 * 			final boolean allowed = licenseManager.isAllowed(PluginInitExample.PRODUCT,
	 * 					PluginInitExample.CONSTRAINT_3, &quot;example-category&quot;);
	 *
	 * 			// change the button state
	 * 			SwingUtilities.invokeLater(new Runnable() {
	 *
	 * 				&#064;Override
	 * 				public void run() {
	 * 					button.setEnabled(allowed);
	 * 				}
	 * 			});
	 * 		}
	 * 	}
	 * });
	 * </code>
	 * </pre>
	 *
	 * @param product
	 *            the product that should be checked
	 * @param constraint
	 *            the constraint object
	 * @param checkedValue
	 *            the value that should be checked for constraint violation
	 * @param <S>
	 *            the class a constraint is stored with
	 * @param <C>
	 *            the class of the checked value
	 * @return <code>true</code> if provided value is allowed by the constraint, <code>false</code>
	 *         otherwise
	 */
	<S, C> boolean isAllowed(final Product product, final Constraint<S, C> constraint, final C checkedValue);

	/**
	 * Checks whether usage of the provided object is allowed/forbidden via license class
	 * annotations. The provided object class and its super classes is checked for annotations of
	 * type
	 * <ul>
	 * <li>{@link LicenseLevel}</li>
	 * <li>{@link LicenseConstraint}</li>
	 * <li>{@link LicenseConstraints}</li>
	 * </ul>
	 * If either of these annotations was found the license manager checks whether any annotation
	 * specification is fulfilled. In case only one annotation specification is fulfilled, the
	 * object usage is allowed.
	 *
	 * @since 2.0.0
	 *
	 * @param obj
	 *            the object to be checked for license annotations
	 *
	 * @return In case any license annotations allows the object usage the method will return
	 *         <code>true</code>. Same in case no license annotation was found. In case the object
	 *         usage is not allowed by any license annotation <code>false</code> is returned.
	 */
	boolean isAllowedByAnnotations(final Object obj);

	/**
	 * Validates the provided license text for the specified product. Returns the a pair of
	 * {@link License} and {@link Product} in case the provided license text is valid.
	 *
	 * @since 3.1.0
	 *
	 * @param product
	 *            the {@link Product} to validate the license for. Can be <code>null</code> if
	 *            product is unknown. The product will than be inferred from the license content.
	 * @param licenseText
	 *            the base64 encoded license text
	 * @return a {@link Pair} which contains both, the {@link Product} the license is valid for and
	 *         the validated {@link License}
	 *
	 * @throws LicenseValidationException
	 *             thrown if validation of license text fails
	 * @throws UnknownProductException
	 *             in case the {@link Product} inferred from the provided license text is not
	 *             registered. This can happen only in case no {@link Product} was provided when
	 *             calling the method.
	 */
	Pair<Product, License> validateLicense(Product product, String licenseText)
			throws UnknownProductException, LicenseValidationException;

	/**
	 * Validates the provided license text and stores it to a {@link LicenseLocation}. After storing
	 * the best valid license for the {@link Product} which corresponds to the provided license text
	 * will be activated.
	 *
	 * @since 3.1.0
	 *
	 * @param licenseText
	 *            the base64 encoded license text
	 *
	 * @throws LicenseValidationException
	 *             thrown if validation the provided license fails. The license was not stored if
	 *             this exception is thrown.
	 *
	 * @throws UnknownProductException
	 *             thrown if product for provided license is not registered. The license was not
	 *             stored if this exception is thrown.
	 *
	 * @throws LicenseStoringException
	 *             thrown if storing the license fails (e.g. because of I/O errors) or no
	 *             {@link LicenseLocation} specified
	 *
	 * @return the {@link License} which was just stored
	 */
	License storeNewLicense(final String licenseText)
			throws LicenseStoringException, UnknownProductException, LicenseValidationException;

	/**
	 * Changes the license location.
	 *
	 * @param location
	 *            the new license location.
	 * @since 3.1.0
	 */
	void setLicenseLocation(final LicenseLocation location);

	/**
	 * Registers a listener to the license manager.
	 *
	 * @param l
	 *            the listener to register
	 */
	void registerLicenseManagerListener(final LicenseManagerListener l);

	/**
	 * Removes the listener from the license manager.
	 *
	 * @param l
	 *            the listener to remove
	 */
	void removeLicenseManagerListener(final LicenseManagerListener l);

}
