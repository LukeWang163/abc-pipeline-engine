package abc_pipeline_engine.utils;

import abc_pipeline_engine.utils.forRP.HDFSUtil;
import abc_pipeline_engine.utils.forRP.ParquetExampleSourceUtil;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.Partition;
import base.operators.example.set.SplittedExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.tools.Ontology;
import base.operators.tools.StrictDecimalFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class ExampleSetUtil extends base.operators.utils.HDFSUtil {

    /**
     * 通过ExampleSet获取HDFS的CSV的schema
     *
     * @param path
     * @return
     */
    public static List<Map<String, Object>> readCSVSchemaFromHDFS(String path) {
        List<Map<String, Object>> schemaList = new ArrayList<>();
        try {
            try (FileSystem fs = HDFSUtil.getFileSystem()) {
                InputStream inputStream = fs.open(new Path(path));

                Map<String, Object> csvDataType = guessCsvDataType(inputStream);

                String[] columnNames = (String[])csvDataType.get("Column_Names");
                int[] valueTypes = (int[])csvDataType.get("Value_Types");

                int attributeCount = columnNames.length;
                for (int i = 0; i < attributeCount; i++) {
                    Map<String, Object> map = new LinkedHashMap<String, Object>();
                    map.put("Attribute_Name", columnNames[i]);
                    map.put("Attribute_Type", rmToTableColumnTypeMap.get(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueTypes[i])));
                    map.put("Attribute_Role", "attribute");
                    schemaList.add(map);
                }
                try {
                    inputStream.close();
                } catch (IOException e){

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return schemaList;
    }

    private static Map<String, Object> guessCsvDataType(InputStream inputStream) {
        Map<String, Object> csvDataType = new HashMap<>();
        String[] columnNames = null;
        int[] valueTypes = null;
        Charset charset = StandardCharsets.UTF_8;
        int offset = 0;
        CSVFormat format = CSVFormat.DEFAULT.withHeader().withDelimiter(',');
        DateFormat dateFormat = (DateFormat) DateFormat.getDateTimeInstance().clone();
        NumberFormat numberFormat = new StrictDecimalFormat('.');
        try (CSVParser parser = CSVParser.parse(inputStream, charset, format)) {
            Map<String, Integer> header = parser.getHeaderMap();
            if (header != null) {
                columnNames = new String[header.size()];
                valueTypes = new int[header.size()];
                for (Map.Entry<String, Integer> column : header.entrySet()) {
                    columnNames[column.getValue()] = column.getKey();
                    valueTypes[column.getValue()] = Ontology.INTEGER;
                }
                int k = 0;
                int count = 0;

                for (CSVRecord record : parser) {
                    if (++count < offset) {
                        continue;
                    }
                    guessValueTypes(record, valueTypes, dateFormat, numberFormat);
                    if (++k >= 1000) {
                        break;
                    }
                }

            } else {
                Iterator<CSVRecord> iter = parser.iterator();
                if (!iter.hasNext()) {
                    throw new RuntimeException("Empty File");
                }
                int k = 0;
                int count = 0;

                for (CSVRecord record : parser) {
                    if (++count < offset) {
                        continue;
                    }
                    if (count == offset) {
                        columnNames = new String[record.size()];
                        valueTypes = new int[record.size()];
                        for (int i = 0; i < columnNames.length; i++) {
                            columnNames[i] = String.format("Column %d", i + 1);
                            valueTypes[i] = Ontology.INTEGER;
                        }
                    }
                    guessValueTypes(record, valueTypes, dateFormat, numberFormat);
                    if (++k >= 1000) {
                        break;
                    }
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        csvDataType.put("Column_Names", columnNames);
        csvDataType.put("Value_Types", valueTypes);
        return csvDataType;
    }
    /**
     * 从HDFS解析CSV到ExampleSet
     *
     * @param viewPath String
     * @return base.operators.example.ExampleSet
     */
    public static ExampleSet readCSVFromHDFSToExampleSet(String viewPath, List<Map<String, Object>> columnList) {
        ExampleSet exampleSet = null;
        try (FileSystem fs = HDFSUtil.getFileSystem()) {
            InputStream inputStream = fs.open(new Path(viewPath)).getWrappedStream();
            exampleSet = readCSVFromStreamToExampleSet(inputStream, columnList, Integer.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exampleSet;
    }

    /**
     * 从流解析CSV到ExampleSet
     *
     * @param inputStream InputStream
     * @return base.operators.example.ExampleSet
     */
    public static ExampleSet readCSVFromStreamToExampleSet(InputStream inputStream, int limit) {
        ExampleSet exampleSet = null;
        try {
            try {
                Charset charset = StandardCharsets.UTF_8;
                int offset = 0;
                CSVFormat format = CSVFormat.DEFAULT.withHeader().withDelimiter(',');
                DateFormat dateFormat = (DateFormat) DateFormat.getDateTimeInstance().clone();
                NumberFormat numberFormat = new StrictDecimalFormat('.');

                Map<String, Object> csvDataType = guessCsvDataType(inputStream);
                List<Map<String, Object>> schemaList = new ArrayList<>();
                String[] columnNames = (String[])csvDataType.get("Column_Names");
                int[] valueTypes = (int[])csvDataType.get("Value_Types");
                int attributeCount = columnNames.length;
                for (int i = 0; i < attributeCount; i++) {
                    Map<String, Object> map = new LinkedHashMap<String, Object>();
                    map.put("Attribute_Name", columnNames[i]);
                    map.put("Attribute_Type", rmToTableColumnTypeMap.get(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueTypes[i])));
                    map.put("Attribute_Role", "attribute");
                    schemaList.add(map);
                }

                inputStream.reset();

                exampleSet = readCSVFromStreamToExampleSet(inputStream, schemaList, Integer.MAX_VALUE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return exampleSet;
        } catch (Exception e) {
            e.printStackTrace();
            return exampleSet;
        }
    }

    /**
     * 从流解析CSV到ExampleSet，根据已知columnList
     *
     * @param inputStream InputStream
     * @param columnList List<Map<String, Object>>
     * @return base.operators.example.ExampleSet
     */
    public static ExampleSet readCSVFromStreamToExampleSet(InputStream inputStream, List<Map<String, Object>> columnList, int limit) {
        ExampleSet exampleSet = null;
        try {
            try {
                Charset charset = StandardCharsets.UTF_8;
                CSVFormat format = CSVFormat.DEFAULT.withHeader().withDelimiter(',');

                String[] columnNames = new String[columnList.size()];
                Attribute[] listOfAttrs = new Attribute[columnList.size()];
                NumberFormat numberFormat = new StrictDecimalFormat('.');

                for (int i = 0; i < columnList.size(); i++){
                    columnNames[i] = columnList.get(i).get("Attribute_Name").toString();
                    Attribute newAttribute = AttributeFactory.createAttribute(columnNames[i], Ontology.ATTRIBUTE_VALUE_TYPE.mapName(columnList.get(i).get("Attribute_Type").toString()));
                    newAttribute.setConstruction(columnNames[i]);
                    listOfAttrs[i] = newAttribute;
                }

                try (CSVParser parser = CSVParser.parse(inputStream, charset, format)) {
                    ExampleSetBuilder builder = ExampleSets.from(listOfAttrs);
                    int count = 0;
                    for (CSVRecord record : parser) {
                        if (++count > limit) {
                            break;
                        }
                        double[] doubleArray = new double[listOfAttrs.length];
                        for (int j = 0; j < listOfAttrs.length; j++) {
                            Attribute attribute = listOfAttrs[j];
                            String attributeType = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType());
                            switch (attributeType) {
                                case "date_time":
                                case "integer":
                                    doubleArray[j] = getLong(record.get(j), numberFormat);
                                case "numeric":
                                case "real":
                                    doubleArray[j] = getDouble(record.get(j), numberFormat);
                                    break;
                                case "nominal":
                                case "attribute_value":
                                case "text":
                                case "binominal":
                                case "polynominal":
                                case "file_path":
                                default:
                                    doubleArray[j] = attribute.getMapping().mapString(record.get(j));
                                    break;
                            }
                        }

                        builder.addRow(doubleArray);
                    }
                    exampleSet = builder.build();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return exampleSet;
        } catch (Exception e) {
            e.printStackTrace();
            return exampleSet;
        }
    }

    public static void guessValueTypes(CSVRecord record, int[] valueTypes, DateFormat dateFormat, NumberFormat numberFormat) {
        for (int c = 0; c < valueTypes.length; c++) {
            String value = record.get(c).trim();
            if ((value != null) && (!value.equals("?")) && (value.length() > 0)) {
                valueTypes[c] = guessValueType(valueTypes[c], value, dateFormat, numberFormat);
            }
        }
    }

    private static int guessValueType(int currentValueType, String value, DateFormat dateFormat,
                                      NumberFormat numberFormat) {
        if (currentValueType == Ontology.BINOMINAL || currentValueType == Ontology.POLYNOMINAL) {
            // Don't set to binominal type, it fails too often.
            return Ontology.POLYNOMINAL;
        }

        if (currentValueType == Ontology.DATE) {
            try {
                dateFormat.parse(value);
                return currentValueType;
            } catch (ParseException e) {
                return Ontology.POLYNOMINAL;
            }
        }
        if (currentValueType == Ontology.REAL) {
            if (numberFormat != null) {
                try {
                    numberFormat.parse(value);
                    return Ontology.REAL;
                } catch (ParseException e) {
                    return guessValueType(Ontology.DATE, value, dateFormat, numberFormat);
                }
            } else {
                try {
                    Double.parseDouble(value);
                    return Ontology.REAL;
                } catch (NumberFormatException e) {
                    return guessValueType(Ontology.DATE, value, dateFormat, null);
                }
            }
        }
        try {
            Integer.parseInt(value);
            return Ontology.INTEGER;
        } catch (NumberFormatException e) {
            return guessValueType(Ontology.REAL, value, dateFormat, numberFormat);
        }
    }

    private static double getDouble(String value, NumberFormat numberFormat) {
        try {
            Number parsedValue;
            parsedValue = numberFormat.parse(value);
            if (parsedValue == null) {
                return Double.NaN;
            } else {
                return parsedValue.doubleValue();
            }
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    private static double getLong(String value, NumberFormat numberFormat) {
        try {
            Number parsedValue;
            parsedValue = numberFormat.parse(value);
            if (parsedValue == null) {
                return Double.NaN;
            } else {
                return parsedValue.longValue();
            }
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    public static final Map<String, String> rmToTableColumnTypeMap = new HashMap<String, String>() {
        {
            put("polynominal", "nominal");
            put("binominal", "nominal");
            put("nominal", "nominal");
            put("attribute_value", "nominal");
            put("text", "nominal");
            put("file_path", "nominal");
            put("numeric", "numeric");
            put("real", "numeric");
            put("integer", "numeric");
            put("date_time", "date_time");
            put("date", "date_time");
            put("time", "date_time");
        }
    };

    public static List<Map<String, Object>> getViewColFromExampleSet(String viewPath) {
        List<Map<String, Object>> columnList = new LinkedList<>();
        List<Map<String, Object>> metaDataMapList = new LinkedList<>();
        try {
            FSDataInputStream inputStream = fs.open(new Path(viewPath + Path.SEPARATOR + "metadata"));
            ObjectInputStream objectReader = new ObjectInputStream(inputStream);
            Map<String, Object> viewAndMeta = (Map<String, Object>) objectReader.readObject();
            metaDataMapList = (List<Map<String, Object>>) viewAndMeta.get("metaData");
            for (Map<String, Object> metaData : metaDataMapList) {
                Map<String, Object> tmpMetaData = new LinkedHashMap<String, Object>();
                tmpMetaData.put("name", metaData.get("Attribute_Name"));
                tmpMetaData.put("type", rmToTableColumnTypeMap.get(metaData.get("Attribute_Type")));
                columnList.add(tmpMetaData);
            }
        } catch (IOException | ClassNotFoundException e){
            return null;
        }

        return columnList;
    }

    public static List<Map<String, Object>> getViewColFromHDFSCsv(String viewPath) {
        List<Map<String, Object>> columnList = new LinkedList<>();
        FileSystem fs = HDFSUtil.getFileSystem();
        InputStream inputStream = null;
        try {
            inputStream = fs.open(new Path(viewPath));
        } catch (IOException e) {
            e.printStackTrace();
            return columnList;
        }
        Map<String, Object> csvDataType = guessCsvDataType(inputStream);
        String[] columnNames = (String[])csvDataType.get("Column_Names");
        int[] valueTypes = (int[])csvDataType.get("Value_Types");
        for (int i=0; i < columnNames.length; i++ ){
            Map<String, Object> tmpMetaData = new LinkedHashMap<String, Object>();
            tmpMetaData.put("name", columnNames[i]);
            tmpMetaData.put("type", rmToTableColumnTypeMap.get(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueTypes[i])));
            columnList.add(tmpMetaData);
        }
        try {
            inputStream.close();
        } catch (IOException e){

        }
        return columnList;
    }

    /**
     * ExampleSet写CSV
     *
     * @param exampleSet ExampleSet
     * @param path       String
     */
    public static void writeToHDFSCsv(ExampleSet exampleSet, String path) {
        try (FileSystem fs = HDFSUtil.getFileSystem()) {
            OutputStream outputStream = fs.create(new Path(path));
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writeCSV(exampleSet, out, ",", true, true, DateFormat.getInstance(), String.valueOf(Double.POSITIVE_INFINITY));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCSV(final ExampleSet exampleSet, final PrintWriter out, final String colSeparator, final boolean quoteNomValues,
                                 final boolean writeAttribNames, DateFormat dateFormatter, String infinitySymbol) {
        infinitySymbol = infinitySymbol == null ? String.valueOf(Double.POSITIVE_INFINITY) : infinitySymbol;
        final String negativeInfinitySymbol = "-" + infinitySymbol;
        final boolean writeInt = true;
        final boolean formatDate = dateFormatter != null;

        // write column names
        if (writeAttribNames) {
            boolean first = true;
            for (Attribute attribute : (Iterable<Attribute>) () -> exampleSet.getAttributes().allAttributes()) {
                if (!first) {
                    out.print(colSeparator);
                }
                String name = attribute.getName();
                if (quoteNomValues) {
                    out.print('"');
                    out.print(name.replace('"', '\''));
                    out.print('"');
                } else {
                    out.print(name);
                }
                first = false;
            }
            out.println();
        }

        // write data
        for (Example example : exampleSet) {
            boolean first = true;
            for (Attribute attribute : (Iterable<Attribute>) () -> exampleSet.getAttributes().allAttributes()) {
                if (!first) {
                    out.print(colSeparator);
                }
                if (!Double.isNaN(example.getValue(attribute))) {
                    if (attribute.isNominal()) {
                        String stringValue = example.getValueAsString(attribute);
                        if (quoteNomValues) {
                            out.print('"');
                            out.print(stringValue.replace('"', '\''));
                            out.print('"');
                        } else {
                            out.print(stringValue);
                        }
                    } else {
                        Double value = example.getValue(attribute);
                        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
                            if (formatDate) {
                                Date date = new Date(value.longValue());
                                out.print(dateFormatter.format(date));
                            } else if (writeInt) {
                                out.print(value.longValue());
                            } else {
                                out.print(value);
                            }
                        } else if (value == Double.POSITIVE_INFINITY) {
                            out.print(infinitySymbol);
                        } else if (value == Double.NEGATIVE_INFINITY) {
                            out.print(negativeInfinitySymbol);
                        } else if (writeInt && attribute.getValueType() == Ontology.INTEGER) {
                            out.print(value.longValue());
                        } else {
                            out.print(value);
                        }
                    }
                }
                first = false;
            }

            out.println();

        }
    }

    public static List<Map<String, Object>> getExampleSetSchema(String viewPath) {
        List<Map<String, Object>> columnList = new LinkedList<>();
        OutputStream metaDataStream = new ByteArrayOutputStream();
        String metaDataStr;
        try {
            HDFSUtil.copyFileAsStream(viewPath + Path.SEPARATOR + "metadata", metaDataStream);
            metaDataStr = ((ByteArrayOutputStream)metaDataStream).toString("UTF-8");
            metaDataStream.close();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> metaDataMapList = mapper.readValue(metaDataStr, new TypeReference<LinkedList<LinkedHashMap<String, Object>>>() {
            });
            columnList.addAll(metaDataMapList);
        } catch (IOException | InterruptedException e) {
        }
        return columnList;
    }

    public static Map<String, Object> getViewAndMeta(String viewPath) {
        Map<String, Object> transformViewAndMeta = new HashMap<>();
        try {
            fs.getStatus();
        } catch (Exception e){
            initFileSystem();
        }
        try {
            FSDataInputStream inputStream = fs.open(new Path(viewPath + Path.SEPARATOR + "metadata"));
            ObjectInputStream objectReader = new ObjectInputStream(inputStream);
            Map<String, Object> viewAndMeta = (Map<String, Object>) objectReader.readObject();

            SplittedExampleSet viewExampleSet = (SplittedExampleSet) viewAndMeta.get("viewData");
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> dataList = new ArrayList<>();
            Attributes attributes = viewExampleSet.getAttributes();
            int rowCount = viewExampleSet.parent.size();
            int columnCount = attributes.allSize();
            int actualRowCount = viewExampleSet.size();
            data.put("rowCount", rowCount);
            data.put("columnCount", columnCount);
            for (int i = 0; i < actualRowCount; i++) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzzz");
                Iterator<Attribute> attributeIterator = attributes.allAttributes();
                while (attributeIterator.hasNext()) {
                    Attribute attribute = attributeIterator.next();
                    switch (Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType())){
                        case "integer":
                            map.put(attribute.getName(), viewExampleSet.getExample(i).getValueAsString(attribute, -1, true));
                            break;
                        case "real":
                        case "numeric":
                            map.put(attribute.getName(), String.format("%.3f", viewExampleSet.getExample(i).getValue(attribute)));
                            break;
                        case "date_time":
                        case "date":
                        case "time":
                            map.put(attribute.getName(), formatter.format(viewExampleSet.getExample(i).get(attribute.getName())));
                            break;
                        default:
                            map.put(attribute.getName(), viewExampleSet.getExample(i).getValueAsString(attribute));
                            break;
                    }
                }
                dataList.add(map);
            }

            data.put("data_list", dataList);

            transformViewAndMeta.put("metaData", viewAndMeta.get("metaData"));
            transformViewAndMeta.put("viewData", data);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return transformViewAndMeta;
    }

    /**
     * 获取CSV文件的前limitNum条数据
     *
     * @param path
     * @param limitNum
     * @return
     */
    public static Map<String, Object> getViewDataFromHDFSCsv(String path, List<Map<String, Object>> columnList, int limitNum) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();
        ExampleSet exampleSet;
        try {
            exampleSet = readCSVFromHDFSToExampleSet(path, columnList);
        } catch (Exception e){
            return data;
        }

        limitNum = (limitNum < exampleSet.size()) ? limitNum : exampleSet.size();

        Attributes attributes = exampleSet.getAttributes();
        data.put("rowCount", exampleSet.size());
        data.put("columnCount", attributes.size());
        for (int i = 0; i < limitNum; i++) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzzz");
            for (Attribute attribute: attributes) {
                switch (Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType())){
                    case "integer":
                        map.put(attribute.getName(), exampleSet.getExample(i).getValueAsString(attribute, -1, true));
                        break;
                    case "real":
                    case "numeric":
                        map.put(attribute.getName(), String.format("%.3f", exampleSet.getExample(i).getValue(attribute)));
                        break;
                    case "date_time":
                    case "date":
                    case "time":
                        map.put(attribute.getName(), formatter.format(new java.util.Date((long)exampleSet.getExample(i).get(attribute.getName()))));
                        break;
                    default:
                        map.put(attribute.getName(), exampleSet.getExample(i).getValueAsString(attribute));
                        break;
                }
            }
            dataList.add(map);
        }
        data.put("data_list", dataList);

        return data;
    }

    /**
     * 获取Parquet内容前limitNum条到List<Map<String, Object>>
     *
     * @param parquetPath String
     * @param columnList List<Map<String, Object>>
     * @param limitNum  Integer
     * @return List<Map<String, Object>>
     */
    public static Map<String, Object> readFromParquetToList(String parquetPath, List<Map<String, Object>> columnList, Integer limitNum) throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        try (ParquetFileReader reader = ParquetFileReader.open(HDFSUtil.getFileSystemConf(), new Path(parquetPath))) {
            ParquetMetadata footer = reader.getFooter();
            MessageType schema = footer.getFileMetaData().getSchema();
            int nrows = (int) Math.min(reader.getRecordCount(), limitNum);
            data.put("row_count", reader.getRecordCount());
            List<String> columnNames = new ArrayList<>();
            List<String> columnTypes = columnList.stream().map(column -> (String)column.get("Attribute_Type")).collect(Collectors.toList());
            for (ColumnDescriptor column: schema.getColumns()) {
                columnNames.add(String.join(".", column.getPath()));
            }
            int ncols = (int) Math.min(columnNames.size(), limitNum + 1);
            data.put("column_count", columnNames.size());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzzz");
            PageReadStore store;
            while ((store = reader.readNextRowGroup()) != null) {
                final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                final RecordReader<Group> recordReader = columnIO.getRecordReader(store, new GroupRecordConverter(schema));
                for (int i = 0; i < nrows; i++) {
                    Group group = recordReader.read();
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int j = 0; j < ncols; j++) {
                        int repetitionCount = group.getFieldRepetitionCount(j);
                        if (repetitionCount > 0) {
                            switch (columnTypes.get(j)) {
                                case "real":
                                case "numeric":
                                    map.put(columnNames.get(j), String.format("%.3f", group.getDouble(j, 0)));
                                    break;
                                case "date_time":
                                case "date":
                                case "time":
                                    map.put(columnNames.get(j), formatter.format(new java.util.Date(group.getLong(j, 0))));
                                    break;
                                default:
                                    map.put(columnNames.get(j), group.getValueToString(j, 0));
                                    break;
                            }

                        } else {
                            map.put(columnNames.get(j), "NaN");
                        }
                    }
                    dataList.add(map);
                }
            }
        }
        catch (Exception e){
            data.put("data_list", dataList);
        }
        data.put("data_list", dataList);
        return data;
    }

    public static void writeExampleSetToHDFS(ExampleSet exampleSet, String basePath) {
        try {
            fs.getStatus();
        } catch (Exception e) {
            initFileSystem();
        }
        FSDataOutputStream outputStream = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            Path path = new Path(basePath);
            if (fs.exists(path)) {
                fs.delete(path, true);
            }
            if (!fs.exists(path)) {
                fs.mkdirs(path);
            }
            Path objectPath = new Path(basePath + Path.SEPARATOR + "data.parquet");
            outputStream = fs.create(objectPath);
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(exampleSet);
            oos.writeObject(null);
            oos.flush();
            oos.close();

            int[] partition = new int[exampleSet.size()];
            int endIndex = Math.min(100, exampleSet.size());
            for (int i = 0; i < endIndex; i++) {
                    partition[i] = 1;
            }
            SplittedExampleSet result = new SplittedExampleSet(exampleSet, new Partition(partition, 2));

            result.selectSingleSubset(1);

            List<Map<String, Object>> exampleSetMetaData = ParquetExampleSourceUtil.formMetaData(exampleSet);
            Map<String, Object> viewAndMeta = new HashMap<>();
            viewAndMeta.put("viewData", result);
            viewAndMeta.put("metaData", exampleSetMetaData);
            Path metaPath = new Path(basePath + Path.SEPARATOR + "metadata");
            outputStream = fs.create(metaPath);
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(viewAndMeta);
            oos.writeObject(null);
            oos.flush();
            oos.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ExampleSet readExampleSetFromHDFS(String basePath) {
        try {
            fs.getStatus();
        } catch (Exception e) {
            initFileSystem();
        }
        InputStream inputStream = null;
        ObjectInputStream oi = null;
        Object object = null;
        try {
            Path path = new Path(basePath + Path.SEPARATOR + "data.parquet");
            if (fs.exists(path)) {
                return null;
            }
            inputStream = fs.open(new Path(basePath + Path.SEPARATOR + "data"));
            oi = new ObjectInputStream(inputStream);
            object = oi.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                oi.close();
                inputStream.close();
            } catch (Exception e) {
            }
        }
        return (ExampleSet) object;
    }
}
