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
package base.operators.operator.preprocessing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.tools.Ontology;
import org.apache.commons.lang.ArrayUtils;

import base.operators.example.Statistics;
import base.operators.example.table.NominalMapping;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.preprocessing.RemoveUnusedNominalValuesModel.MappingTranslation;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator will remove each unused (=not occurring) nominal value from the mapping.
 *
 * @author Sebastian Land
 */
public class RemoveUnusedNominalValuesOperator extends PreprocessingOperator {

	private static final String PARAMETER_SORT_MAPPING_ALPHABETICALLY = "sort_alphabetically";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public RemoveUnusedNominalValuesOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		amd.setValueSetRelation(SetRelation.SUBSET);
		return Collections.singleton(amd);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		boolean sortMappings = getParameterAsBoolean(PARAMETER_SORT_MAPPING_ALPHABETICALLY);

		Map<String, MappingTranslation> translations = new HashMap<String, MappingTranslation>();

		exampleSet.recalculateAllAttributeStatistics();
		for (Attribute attribute : exampleSet.getAttributes()) {

			if (attribute.isNominal()) {
				MappingTranslation translation = new MappingTranslation((NominalMapping) attribute.getMapping().clone());
				for (String value : attribute.getMapping().getValues()) {
					double count = exampleSet.getStatistics(attribute, Statistics.COUNT, value);
					if (count > 0) {
						translation.newMapping.mapString(value);
					}
				}
				if (translation.newMapping.size() < attribute.getMapping().size()) {
					if (sortMappings) {
						translation.newMapping.sortMappings();
					}
					translations.put(attribute.getName(), translation);
				}
			}
		}
		return new RemoveUnusedNominalValuesModel(exampleSet, translations);
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return RemoveUnusedNominalValuesModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_SORT_MAPPING_ALPHABETICALLY,
				"If checked, the resulting mapping will be sorted alphabetically.", true, false));

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return false;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}