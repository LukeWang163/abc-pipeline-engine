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
package base.operators.operator.learner.local;

import base.operators.parameter.ParameterHandler;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.Tools;
import base.operators.tools.container.Tupel;
import base.operators.tools.math.container.GeometricDataCollection;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * This neighborhood includes a fraction of the complete training data set. So the number of nearest
 * neighbors depends on the size of the training set but at least 1.
 * 
 * @author Sebastian Land
 * 
 */
public class RelativeNeighborhood implements Neighborhood {

	private static final long serialVersionUID = -3244742069757655400L;

	public static final String PARAMETER_RELATIVE_SIZE = "relative_size";

	private double relativeSize;

	@Override
	public <T extends Serializable> Collection<Tupel<Double, T>> getNeighbourhood(GeometricDataCollection<T> samples,
                                                                                  double[] probePoint) {
		return samples.getNearestValueDistances(Math.max((int) (samples.size() * relativeSize), 1), probePoint);
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeDouble(
				PARAMETER_RELATIVE_SIZE,
				"Specifies the size of the neighborhood relative to the total number of examples. A value of 0.04 would include 4% of the data points into the neighborhood.",
				0, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public void init(ParameterHandler handler) throws UndefinedParameterError {
		relativeSize = handler.getParameterAsDouble(PARAMETER_RELATIVE_SIZE);
	}

	@Override
	public String toString() {
		return "Relative neighborhood with a fraction of " + Tools.formatNumber(relativeSize, 3)
				+ " of the complete data set";
	}
}
