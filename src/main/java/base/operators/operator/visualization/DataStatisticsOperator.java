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
package base.operators.operator.visualization;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.GenerateNewMDRule;

import java.util.Iterator;


/**
 * This operators calculates some very simple statistics about the given example set. These are the
 * ranges of the attributes and the average or mode values for numerical or nominal attributes
 * respectively. These informations are automatically calculated and displayed by the graphical user
 * interface of RapidMiner. Since they cannot be displayed with the command line version of
 * RapidMiner this operator can be used as a workaround in cases where the graphical user interface
 * cannot be used.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class DataStatisticsOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort statisticsOutput = getOutputPorts().createPort("statistics");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/** Creates a new data statistics operator. */
	public DataStatisticsOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(new GenerateNewMDRule(statisticsOutput, DataStatistics.class));
	}

	/** Creates and delivers the simple statistics object. */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet eSet = exampleSetInput.getData(ExampleSet.class);
		eSet.recalculateAllAttributeStatistics();
		DataStatistics statistics = new DataStatistics();
		Iterator<Attribute> i = eSet.getAttributes().allAttributes();
		while (i.hasNext()) {
			statistics.addInfo(eSet, i.next());
		}
		exampleSetOutput.deliver(eSet);
		statisticsOutput.deliver(statistics);
	}
}
