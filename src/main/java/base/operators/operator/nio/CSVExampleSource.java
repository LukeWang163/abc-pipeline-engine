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
package base.operators.operator.nio;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.io.AbstractReader;
import base.operators.operator.nio.model.AbstractDataResultSetReader;
import base.operators.operator.nio.model.CSVResultSetConfiguration;
import base.operators.operator.nio.model.DataResultSetFactory;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.LineParser;
import base.operators.tools.StrictDecimalFormat;
import base.operators.tools.io.Encoding;
import base.operators.core.io.data.DataSetException;
import base.operators.core.io.data.source.DataSource;
import org.apache.commons.lang.ArrayUtils;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 *
 * <p>
 * This operator can be used to load data from .csv files. The user can specify the delimiter and
 * various other parameters.
 * </p>
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer, Marco Boeck
 */
public class CSVExampleSource extends AbstractDataResultSetReader {

	public static final String PARAMETER_STORAGE_TYPE = "storage_type";
	public static final String PARAMETER_CSV_FILE = "csv_file";
	public static final String PARAMETER_TRIM_LINES = "trim_lines";
	public static final String PARAMETER_SKIP_COMMENTS = "skip_comments";
	public static final String PARAMETER_COMMENT_CHARS = "comment_characters";
	public static final String PARAMETER_USE_QUOTES = "use_quotes";
	public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";
	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";
	public static final String PARAMETER_ESCAPE_CHARACTER = "escape_character";
	public static final String PARAMETER_STARTING_ROW = "starting_row";

	/**
	 * Values will be trimmed for guessing after this version
	 * @since 9.2.0
	 */
	public static final OperatorVersion BEFORE_VALUE_TRIMMING_GUESSING = new OperatorVersion(9, 0, 3);

	static {
		AbstractReader.registerReaderDescription(new AbstractReader.ReaderDescription("csv", CSVExampleSource.class, PARAMETER_CSV_FILE));
	}

	public CSVExampleSource(final OperatorDescription description) {
		super(description);
	}
	public CSVExampleSource(){
		super();
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new CSVResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() throws OperatorException {
		return StrictDecimalFormat.getInstance(this, true);
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	protected String getFileParameterName() {
		return PARAMETER_CSV_FILE;
	}

	@Override
	protected String getFileExtension() {
		return "csv";
	}


	/**
	 * Whether attributes should be trimmed for guessing
	 *
	 * @return {@code true} if compatibility level is above {@link #BEFORE_VALUE_TRIMMING_GUESSING}
	 * @since 9.2.0
	 */
	@Override
	public boolean trimForGuessing() {
		//return getCompatibilityLevel().isAbove(BEFORE_VALUE_TRIMMING_GUESSING);
		return true;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// Trim the date format, if the values are trimmed
		String dateFormat = getParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT);
		if (dateFormat != null && trimForGuessing()) {
			setParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT, dateFormat.trim());
		}
		return super.createExampleSet();
	}



	@Override
	public List<ParameterType> getParameterTypes() {
		LinkedList<ParameterType> types = new LinkedList<>();

//		ParameterType type = new ParameterTypeConfiguration(CSVExampleSourceConfigurationWizardCreator.class, this);
//		type.setExpert(false);
//		types.add(type);
		ParameterType type;
		types.add(makeFileParameterType());

		// storage type
		types.add(new ParameterTypeCategory(PARAMETER_STORAGE_TYPE, "Storage type for the data files", new String[]{"HDFS", "Local"}, 0, false));
		// Separator
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
				"Column separators for data files (regular expression)", ",", false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_TRIM_LINES,
				"Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed. This option might be problematic if TABs are used as a seperator.",
				false));
		// Quotes
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded.", true, false));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.",
				LineParser.DEFAULT_QUOTE_CHARACTER, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		type = new ParameterTypeChar(PARAMETER_ESCAPE_CHARACTER,
				"The character that is used to escape quotes and column seperators",
				LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER, true);
		types.add(type);

		// Comments
		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_COMMENTS, "Indicates if a comment character should be used.",
				false, false));
		type = new ParameterTypeString(PARAMETER_COMMENT_CHARS, "Lines beginning with these characters are ignored.", "#",
				false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SKIP_COMMENTS, false, true));
		types.add(type);

		// Starting row
		types.add(new ParameterTypeInt(PARAMETER_STARTING_ROW, "The first row where reading should start, everything before it will be skipped.",
				1, Integer.MAX_VALUE, 1, true));

		// Number formats
		types.addAll(StrictDecimalFormat.getParameterTypes(this, true));
		type = new ParameterTypeDateFormat();
		type.setDefaultValue(ParameterTypeDateFormat.DATE_FORMAT_YYYY_MM_DD);
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	public void configure(DataSource dataSource) throws DataSetException {
		// set general csv import configParameters
		Map<String, String> configParameters = dataSource.getConfiguration().getParameters();

		setParameter(PARAMETER_STORAGE_TYPE, configParameters.get(CSVResultSetConfiguration.PARAMETER_STORAGE_TYPE));
		setParameter(PARAMETER_CSV_FILE, configParameters.get(CSVResultSetConfiguration.CSV_FILE_LOCATION));
		setParameter(PARAMETER_SKIP_COMMENTS, configParameters.get(CSVResultSetConfiguration.CSV_SKIP_COMMENTS));
		setParameter(PARAMETER_COMMENT_CHARS, configParameters.get(CSVResultSetConfiguration.CSV_COMMENT_CHARACTERS));
		setParameter(PARAMETER_COLUMN_SEPARATORS, configParameters.get(CSVResultSetConfiguration.CSV_COLUMN_SEPARATORS));
		setParameter(StrictDecimalFormat.PARAMETER_DECIMAL_CHARACTER, configParameters.get(CSVResultSetConfiguration.CSV_DECIMAL_CHARACTER));
		setParameter(Encoding.PARAMETER_ENCODING, configParameters.get(CSVResultSetConfiguration.CSV_ENCODING));
		setParameter(PARAMETER_ESCAPE_CHARACTER, configParameters.get(CSVResultSetConfiguration.CSV_ESCAPE_CHARACTER));
		setParameter(PARAMETER_USE_QUOTES, configParameters.get(CSVResultSetConfiguration.CSV_USE_QUOTES));
		setParameter(PARAMETER_QUOTES_CHARACTER, configParameters.get(CSVResultSetConfiguration.CSV_QUOTE_CHARACTER));
		setParameter(PARAMETER_TRIM_LINES, configParameters.get(CSVResultSetConfiguration.CSV_TRIM_LINES));

		// the backend uses technical row values starting with 0 but the operator parameters show human readable versions thus +1
		int rowOffset = Integer.parseInt(configParameters.get(CSVResultSetConfiguration.CSV_STARTING_ROW)) + 1;
		int headerRowIndex = Integer.parseInt(configParameters.get(CSVResultSetConfiguration.CSV_HEADER_ROW));

		if (rowOffset < 0) {
			rowOffset = 0;
		}

		boolean headerRowEqualsStartingRow = headerRowIndex == rowOffset;
		// if row offset equals header row, then we need to increase offset by one
		if (headerRowEqualsStartingRow) {
			rowOffset++;
		}
		setParameter(PARAMETER_FIRST_ROW_AS_NAMES, String.valueOf(Boolean.valueOf(configParameters.get(CSVResultSetConfiguration.CSV_HAS_HEADER_ROW)) || headerRowEqualsStartingRow));
		setParameter(PARAMETER_STARTING_ROW, String.valueOf(rowOffset));

		// set meta data
		ImportWizardUtils.setMetaData(dataSource, this);

		// update compatibility level to latest version
		setCompatibilityLevel(OperatorVersion.getLatestVersion(getOperatorDescription()));
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{
						BEFORE_VALUE_TRIMMING_GUESSING
				});
	}
}
