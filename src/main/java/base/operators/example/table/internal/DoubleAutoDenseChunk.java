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
package base.operators.example.table.internal;

import java.util.Arrays;

import base.operators.example.utils.ExampleSetBuilder;


/**
 * Dense {@link DoubleAutoColumn.DoubleAutoChunk} for double value data in a {@link DoubleAutoColumn}.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class DoubleAutoDenseChunk extends DoubleAutoColumn.DoubleAutoChunk {

	private static final long serialVersionUID = 1L;

	private boolean undecided = true;
	private int ensuredSize;

	private double[] data = AutoColumnUtils.EMPTY_DOUBLE_ARRAY;

	DoubleAutoDenseChunk(DoubleAutoColumn.DoubleAutoChunk[] chunks, int id, int size, ExampleSetBuilder.DataManagement management) {
		super(id, chunks, management);
		ensure(size);
	}

	@Override
	double get(int row) {
		return data[row];
	}

	@Override
	void set(int row, double value) {
		data[row] = value;
	}

	@Override
	void ensure(int size) {
		ensuredSize = size;
		int newSize = size;
		if (undecided) {
			newSize = Math.min(size, AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE);
			// several ensures can happen while still undecided
			if (newSize == data.length) {
				return;
			}
		}
		data = Arrays.copyOf(data, newSize);
	}

	@Override
	void setLast(int row, double value) {
		data[row] = value;
		if (undecided && row == AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1) {
			undecided = false;
			checkSparse();
		}
	}

	/**
	 * Finds the most frequent value in the values set until now. If this value if frequent enough,
	 * it changes to a sparse representation.
	 */
	private void checkSparse() {
		AutoColumnUtils.DensityResult result = AutoColumnUtils.checkDensity(data);
		double thresholdDensity = management == ExampleSetBuilder.DataManagement.AUTO ? AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY
				: AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY;

		if (result.density < thresholdDensity) {
			double defaultValue = result.mostFrequentValue;
			DoubleAutoColumn.DoubleAutoChunk sparse = new DoubleAutoSparseChunk(chunks, id, defaultValue, management);
			sparse.ensure(ensuredSize);
			boolean isNaN = Double.isNaN(defaultValue);
			for (int i = 0; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE; i++) {
				double value = data[i];
				// only set non-default values
				if (isNaN ? !Double.isNaN(value) : value != defaultValue) {
					sparse.set(i, value);
				}
			}
			chunks[id] = sparse;
		} else {
			ensure(ensuredSize);
		}
	}

	@Override
	void complete() {
		if (data.length < ensuredSize) {
			data = Arrays.copyOf(data, ensuredSize);
		}
		undecided = false;
	}

}