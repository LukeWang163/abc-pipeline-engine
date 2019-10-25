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
package base.operators.operator.preprocessing.filter.attributes;

import base.operators.example.Attribute;
import base.operators.example.set.ConditionCreationException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.MetaDataInfo;
import base.operators.parameter.ParameterHandler;


/**
 * This filter actually does nothing and removes no attribute. It is needed to create a behavior
 * consistent with earlier versions and to allow to bundle the filtering with an
 * AttributeSubsetPreprocessing operator.
 * 
 * @author Sebastian Land
 */
public class TransparentAttributeFilter extends AbstractAttributeFilterCondition {

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler)
			throws ConditionCreationException {
		return MetaDataInfo.NO;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		return ScanResult.KEEP;
	}

}
