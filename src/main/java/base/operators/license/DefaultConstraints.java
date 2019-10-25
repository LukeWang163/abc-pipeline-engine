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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import base.operators.license.product.Constraint;


/**
 * A container class that contains a map from constraintId to constraintValue.
 *
 * @author Nils Woehler
 *
 */
public class DefaultConstraints implements Constraints {

	@JsonProperty
	private final Map<String, String> constraints;

	/**
	 * Constructs an empty {@link DefaultConstraints} instance.
	 */
	public DefaultConstraints() {
		this.constraints = new HashMap<>();
	}

	/** Copy Constructor */
	private DefaultConstraints(Map<String, String> restrictions) {
		this.constraints = new HashMap<>(restrictions);
	}

	/**
	 * Adds a constraint value for the specified constraint to the map. If the map already contains
	 * a value for the constraint this value will be overwritten.
	 *
	 * @param constraint
	 *            the constraint to add
	 *
	 * @param constraintValue
	 *            the value of the constraint
	 * @param <S>
	 *            the class a constrained is stored with
	 * @param <C>
	 *            the class of the checked value
	 *
	 * @return the current {@link DefaultConstraints} instance to be able to chain the calls
	 */
	public <S, C> DefaultConstraints addConstraint(Constraint<S, C> constraint, S constraintValue) {
		String value = null;
		if (constraintValue != null) {
			value = constraint.transformToString(constraintValue);
		}
		constraints.put(constraint.getKey(), value);
		return this;
	}

	@Override
	public String getConstraintValue(Constraint<?, ?> constraint) throws ConstraintNotRestrictedException {
		if (!constraints.containsKey(constraint.getKey())) {
			throw new ConstraintNotRestrictedException(constraint);
		}
		return constraints.get(constraint.getKey());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Constraints [");
		if (this.constraints != null) {
			for (String key : constraints.keySet()) {
				builder.append("(key=");
				builder.append(key);
				builder.append(",value=");
				builder.append(constraints.get(key));
				builder.append(")");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public DefaultConstraints copy() {
		return new DefaultConstraints(constraints);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.constraints == null ? 0 : this.constraints.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DefaultConstraints other = (DefaultConstraints) obj;
		if (this.constraints == null) {
			if (other.constraints != null) {
				return false;
			}
		} else if (!this.constraints.equals(other.constraints)) {
			return false;
		}
		return true;
	}
}
