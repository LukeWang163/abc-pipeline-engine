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
package base.operators.operator.preprocessing.discretization;

import base.operators.operator.Operator;
import base.operators.operator.preprocessing.PreprocessingOperator;
import base.operators.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorService;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.quickfix.OperatorInsertionQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.UndefinedParameterError;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * @author Simon Fischer
 */
public abstract class AbstractDiscretizationOperator extends PreprocessingOperator {

	public AbstractDiscretizationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		AttributeMetaData newAMD = new AttributeMetaData(amd.getName(), Ontology.NOMINAL, amd.getRole());
		return Collections.singletonList(newAMD);
	}

	private static Set<Class<? extends AbstractDiscretizationOperator>> ALL_DISCRETIZATION_OPERATORS = new HashSet<Class<? extends AbstractDiscretizationOperator>>();

	protected static void registerDiscretizationOperator(Class<? extends AbstractDiscretizationOperator> clazz) {
		ALL_DISCRETIZATION_OPERATORS.add(clazz);
	}

	public static List<QuickFix> createDiscretizationFixes(final InputPort inputPort, final String labelName) {
		List<QuickFix> fixes = new LinkedList<QuickFix>();
		for (final Class<? extends AbstractDiscretizationOperator> dclass : ALL_DISCRETIZATION_OPERATORS) {
			String name = dclass.getName();
			OperatorDescription ods[] = OperatorService.getOperatorDescriptions(dclass);
			if (ods.length > 0) {
				name = ods[0].getName();
			}
			fixes.add(new OperatorInsertionQuickFix("insert_discretization", new Object[] { name }, 10, inputPort) {

				@Override
				public Operator createOperator() throws OperatorCreationException {
					Operator op = OperatorService.createOperator(dclass);
					if (labelName != null) {
						op.setParameter(
								AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
								AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION]);
						op.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
						op.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION, labelName);
					}
					return op;
				}
			});
		}
		return fixes;
	}

	@Override
	public int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}
}
