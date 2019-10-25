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
package base.operators.operator.generator;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.io.AbstractExampleSource;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.tools.Ontology;
import base.operators.tools.RandomGenerator;
import base.operators.tools.math.container.Range;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Generates a random example set for testing purposes with more than one label.
 *
 * @author Ingo Mierswa
 */
public class MultipleLabelGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	/**
	 * The parameter name for &quot;Defines if multiple labels for regression tasks should be
	 * generated.&quot;
	 */
	public static final String PARAMETER_REGRESSION = "regression";

	/** The parameter name for &quot;The minimum value for the attributes.&quot; */
	public static final String PARAMETER_ATTRIBUTES_LOWER_BOUND = "attributes_lower_bound";

	/** The parameter name for &quot;The maximum value for the attributes.&quot; */
	public static final String PARAMETER_ATTRIBUTES_UPPER_BOUND = "attributes_upper_bound";

	private static final int NUMBER_OF_ATTRIBUTES = 5;
	private static final int NUMBER_OF_LABELS = 3;

	private static final String POSITIVE_LABEL = "positive";
	private static final String NEGATIVE_LABEL = "negative";

	public MultipleLabelGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);
		double lower = getParameterAsDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND);
		double upper = getParameterAsDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND);

		ExampleSetMetaData emd = new ExampleSetMetaData();
		for (int i = 1; i <= NUMBER_OF_ATTRIBUTES; i++) {
			emd.addAttribute(new AttributeMetaData("att" + i, null, Ontology.REAL, new Range(lower, upper)));
		}
		int labelType;
		Consumer<AttributeMetaData> amdFinisher;
		if (getParameterAsBoolean(PARAMETER_REGRESSION)) {
			labelType = Ontology.REAL;
			amdFinisher = amd -> amd.setValueRange(new Range(3 * lower, 3 * upper), SetRelation.EQUAL);
		} else {
			labelType = Ontology.NOMINAL;
			Set<String> values = new TreeSet<>();
			values.add(POSITIVE_LABEL);
			values.add(NEGATIVE_LABEL);
			amdFinisher = amd -> amd.setValueSet(new TreeSet<>(values), SetRelation.EQUAL);
		}
		for (int i = 1; i <= NUMBER_OF_LABELS; i++) {
			String name = Attributes.LABEL_NAME + i;
			AttributeMetaData amd = new AttributeMetaData(name, labelType, name);
			amdFinisher.accept(amd);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);
		double lower = getParameterAsDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND);
		double upper = getParameterAsDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND);
		boolean regression = getParameterAsBoolean(PARAMETER_REGRESSION);

		// create table
		List<Attribute> attributes = new ArrayList<>();
		for (int m = 1; m <= NUMBER_OF_ATTRIBUTES; m++) {
			attributes.add(AttributeFactory.createAttribute("att" + m, Ontology.REAL));
		}

		// generate labels
		int type = Ontology.NOMINAL;
		if (regression) {
			type = Ontology.REAL;
		}
		List<Attribute> labels = new ArrayList<>();
		for (int i = 1; i <= NUMBER_OF_LABELS; i++) {
			Attribute label = AttributeFactory.createAttribute(Attributes.LABEL_NAME + i, type);
			if (!regression) {
				label.getMapping().mapString(POSITIVE_LABEL);
				label.getMapping().mapString(NEGATIVE_LABEL);
			}
			labels.add(label);
		}
		int positiveIndex = 0;
		int negativeIndex = 1;

		List<Attribute> all_attributes = new ArrayList<>();
		all_attributes.addAll(attributes);
		all_attributes.addAll(labels);

		ExampleSetBuilder builder = ExampleSets.from(all_attributes).withExpectedSize(numberOfExamples);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// init operator progress
		getProgress().setTotal(numberOfExamples);

		for (int n = 0; n < numberOfExamples; n++) {
			double[] example = new double[NUMBER_OF_ATTRIBUTES + NUMBER_OF_LABELS];
			for (int i = 0; i < NUMBER_OF_ATTRIBUTES; i++) {
				example[i] = random.nextDoubleInRange(lower, upper);
			}
			example[NUMBER_OF_ATTRIBUTES] = example[0] + example[1] + example[2];
			example[NUMBER_OF_ATTRIBUTES + 1] = 2 * example[0] + example[3];
			example[NUMBER_OF_ATTRIBUTES + 2] = example[3] * example[3];
			if (!regression) {
				for (int i = 0; i < NUMBER_OF_LABELS; i++) {
					example[NUMBER_OF_ATTRIBUTES + i] = example[NUMBER_OF_ATTRIBUTES + i] > 0 ? positiveIndex : negativeIndex;
				}
			}
			builder.addRow(example);
			getProgress().step();
		}

		// create example set and return it
		ExampleSet result = builder.withRoles(labels.stream().collect(Collectors
				.toMap(l->l, Attribute::getName, (a, b) -> a, LinkedHashMap::new))).build();

		getProgress().complete();

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_EXAMPLES, "The number of generated examples.", 1,
				Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REGRESSION,
				"Defines if multiple labels for regression tasks should be generated.", false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND, "The minimum value for the attributes.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -10));
		types.add(new ParameterTypeDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND, "The maximum value for the attributes.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 10));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
