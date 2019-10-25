/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package base.operators.example.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import base.operators.RapidMiner;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.SimpleAttributes;
import base.operators.example.set.AbstractExampleSet;
import base.operators.operator.preprocessing.MaterializeDataInMemory;
import base.operators.tools.ParameterService;
import base.operators.example.table.BinominalAttribute;
import base.operators.example.table.DateAttribute;
import base.operators.example.table.ExampleTable;
import base.operators.example.table.GrowingExampleTable;
import base.operators.example.table.MemoryExampleTable;
import base.operators.example.table.NumericalAttribute;
import base.operators.example.table.PolynominalAttribute;
import base.operators.example.table.internal.ColumnarExampleTable;


/**
 * This class consists exclusively of static methods that help to build new {@link ExampleSet}s.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public final class ExampleSets {
	
	/** Set of primitive attribute types that are known to be thread safe for read accesses. */
	private static final Set<Class<? extends Attribute>> SAFE_ATTRIBUTES = new HashSet<>(5);
	static {
		SAFE_ATTRIBUTES.add(DateAttribute.class);
		SAFE_ATTRIBUTES.add(BinominalAttribute.class);
		SAFE_ATTRIBUTES.add(PolynominalAttribute.class);
		SAFE_ATTRIBUTES.add(DateAttribute.class);
		SAFE_ATTRIBUTES.add(NumericalAttribute.class);
	}

	private ExampleSets() {}

	/**
	 * Creates a builder for an {@link ExampleSet} starting from the given attributes. If the given
	 * attributes are {@code null}, the example will have no attributes.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleSet}, can be {@code null}
	 * @return the {@link ExampleSetBuilder} to build the example set
	 */
	public static ExampleSetBuilder from(List<Attribute> attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			return new MemoryExampleSetBuilder(attributes);
		} else {
			return new ColumnarExampleSetBuilder(attributes);
		}
	}

	/**
	 * Creates a builder for an {@link ExampleSet} starting from the given attributes.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleSet}
	 * @return the {@link ExampleSetBuilder} to build the example set
	 */
	public static ExampleSetBuilder from(Attribute... attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			return new MemoryExampleSetBuilder(attributes);
		} else {
			return new ColumnarExampleSetBuilder(attributes);
		}
	}

	/**
	 * Creates an {@link ExampleTable} to which rows can be added. Only use this if it is not
	 * possible to use an {@link ExampleSetBuilder}.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleTable}
	 * @return a table that can grow
	 */
	@SuppressWarnings("deprecation")
	public static GrowingExampleTable createTableFrom(List<Attribute> attributes) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			return new MemoryExampleTable(attributes);
		} else {
			return new ColumnarExampleTable(attributes);
		}
	}

	/**
	 * Creates an {@link ExampleTable} to which rows can be added. Only use this if it is not
	 * possible to use an {@link ExampleSetBuilder}.
	 *
	 * @param attributes
	 *            the attributes for the new {@link ExampleTable}
	 * @param management
	 *            the {@link ExampleSetBuilder.DataManagement} to use for the table if supported
	 * @return a table that can grow
	 */
	@SuppressWarnings("deprecation")
	public static GrowingExampleTable createTableFrom(List<Attribute> attributes, ExampleSetBuilder.DataManagement management) {
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			return new MemoryExampleTable(attributes);
		} else {
			return new ColumnarExampleTable(attributes, management, false);
		}
	}
	
	/**
	 * Creates a copy of the input that guarantees thread-safety for read access and attribute set manipulations.
	 * If the input already provides these guarantees, a shallow copy is return, otherwise a deep copy is created.
	 * 
	 * @param set the input example set
	 * @return the thread safe copy of the given set
	 * @throws IllegalArgumentException if the input example set is {@code null}
	 */
	public static ExampleSet createThreadSafeCopy(ExampleSet set) {
		if (set == null) {
			throw new IllegalArgumentException("Example set must not be null");
		}

		// search for unsafe components
		boolean foundUnsafeComponent;
		
		// check example set implementation
		boolean threadSafeView = set instanceof AbstractExampleSet && ((AbstractExampleSet) set).isThreadSafeView();
		foundUnsafeComponent = !threadSafeView;
		
		// check example table implementation
		if (!foundUnsafeComponent) {
			ExampleTable table = set.getExampleTable();
			foundUnsafeComponent = table.getClass() != ColumnarExampleTable.class;
		}
		
		// check attribute implementation
		if (!foundUnsafeComponent) {
			Attributes attributes = set.getAttributes();
			foundUnsafeComponent = attributes.getClass() != SimpleAttributes.class;
		}
		
		// check individual attributes and attribute transformations
		if (!foundUnsafeComponent) {
			Iterator<Attribute> attributes = set.getAttributes().allAttributes();
			while (!foundUnsafeComponent && attributes.hasNext()) {
				Attribute attribute = attributes.next();
				if (!SAFE_ATTRIBUTES.contains(attribute.getClass()) || attribute.getLastTransformation() != null) {
					foundUnsafeComponent = true;
				}
			}
		}
		
		if (foundUnsafeComponent) {
			return MaterializeDataInMemory.materializeExampleSet(set);
		} else {
			return (ExampleSet) set.clone();
		}
		
	}

}
