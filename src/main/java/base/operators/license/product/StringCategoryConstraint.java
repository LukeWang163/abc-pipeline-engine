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
package base.operators.license.product;

/**
 * An implementation of {@link AbstractCategoryConstraint} that uses strings as categories.
 *
 * @author Nils Woehler
 *
 */
public class StringCategoryConstraint extends AbstractCategoryConstraint<String> {

	private static final long serialVersionUID = 1L;

	/**
	 * @param constraintId
	 *            the constraint ID
	 * @param defaultCategories
	 *            the default categories which will be used if no licenses is installed
	 */
	public StringCategoryConstraint(String constraintId, String... defaultCategories) {
		super(constraintId, defaultCategories);
	}

	@Override
	protected String transformCategoryFromString(String stringCategory) {
		return stringCategory;
	}

	@Override
	protected String transformCategoryToString(String category) {
		return category;
	}

	@Override
	public String transformValueFromString(String checkedValue) {
		return checkedValue;
	}

}
