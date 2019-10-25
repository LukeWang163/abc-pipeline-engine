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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * A category constraint checks whether a category is allowed by checking if the category is
 * contained in the list of allowed categories. Extend this class to create different category
 * constraints.
 *
 * @param <T>
 *            the category class
 *
 * @author Nils Woehler
 */
public abstract class AbstractCategoryConstraint<T> extends AbstractConstraint<List<T>, T> {

	private static final long serialVersionUID = 1L;

	private static final String SEPERATOR = " ";

	/**
	 * @param constraintId
	 *            the id of the constraint
	 * @param defaultCategories
	 *            the categories which should be allowed by the default license. Might be empty in
	 *            case no categories should be allowed with the default license.
	 */
	@SafeVarargs
	public AbstractCategoryConstraint(String constraintId, T... defaultCategories) {
		super(constraintId, convertArrayToList(defaultCategories));

		// do sanity check
		List<T> defaultValues = getDefaultValue();
		for (T category : defaultValues) {
			if (category == null || transformCategoryToString(category).contains(SEPERATOR)) {
				throw new IllegalArgumentException("Categories must not be null or contain space characters.");
			}
		}
	}

	private final static <T> List<T> convertArrayToList(T[] categories) {
		if (categories == null || categories.length == 0) {
			return Collections.emptyList();
		}
		List<T> list = new LinkedList<>();
		for (T category : categories) {
			list.add(category);
		}
		return list;
	}

	@Override
	public final List<T> transformFromString(String stringConstraint) {
		if (stringConstraint.trim().isEmpty()) {
			return Collections.emptyList();
		}
		List<T> categories = new LinkedList<>();
		for (String categoryString : stringConstraint.split(SEPERATOR)) {
			categories.add(transformCategoryFromString(categoryString));
		}
		return categories;
	}

	/**
	 * Transforms a category string returned by {@link #transformCategoryToString(Object)} back to a
	 * category.
	 *
	 * @param stringCategory
	 *            the category string returned by {@link #transformCategoryToString(Object)}.
	 *
	 * @return the category in its correct representation
	 */
	protected abstract T transformCategoryFromString(String stringCategory);

	/**
	 * Transforms a category to a string value. This value will be used by
	 * {@link #transformCategoryFromString(String)} to transform it back to the category
	 * representation. <br/>
	 * <br/>
	 * The string value must not contain space characters.
	 *
	 * @param category
	 *            the category which will be transformed to a string representation
	 *
	 * @return the string representation of the category
	 */
	protected abstract String transformCategoryToString(T category);

	@Override
	public final String transformToString(List<T> constraints) {
		StringBuilder builder = new StringBuilder();
		int index = 0;
		for (T category : constraints) {
			builder.append(transformCategoryToString(category));
			if (index < constraints.size() - 1) {
				builder.append(SEPERATOR);
			}
			++index;
		}
		return builder.toString();
	}

	@Override
	protected final boolean checkConstraint(List<T> constraintValue, T checkedValue) {
		return constraintValue.contains(checkedValue);
	}

}
