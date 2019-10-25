package abc_pipeline_engine.utils.forRP;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.PolynominalMapping;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.preprocessing.statistics.*;
import base.operators.tools.Ontology;
import base.operators.utils.HDFSUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.jfree.data.statistics.HistogramDataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author wangj_lc
 * read from parquet file to {@link ExampleSet} & write {@link ExampleSet} to parquet file
 */
public class ParquetExampleSourceUtil extends HDFSUtil {
    /**
     * write Table to Parquet file
     *
     * @param exampleSet ExampleSet
     * @param basePath String
     * @param toLocal Boolean
     * @throws IOException
     */
    public static void writeToParquet(final ExampleSet exampleSet, String basePath, Boolean toLocal) throws IOException {

        String pqFileName;
        Path parquetPath;
        // 获取HDFS连接的Configuration
        Configuration conf;
        OutputStream outputStream;
        if (toLocal) {
            conf = new Configuration();
            File baseDir = new File(basePath);
            if (baseDir.exists()) {
                baseDir.delete();
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            outputStream = new FileOutputStream(new File(basePath + File.separator + "metadata"));
            pqFileName = basePath + File.separator + "data.parquet";
        } else {
            try {
                fs.getStatus();
            } catch (Exception e){
                initFileSystem();
            }
            conf = fs.getConf();
            Path baseDir = new Path(basePath);
            if (fs.exists(baseDir)) {
                fs.delete(baseDir, true);
            }
            if (!fs.exists(baseDir)) {
                fs.mkdirs(baseDir);
            }
            outputStream = fs.create(new Path(basePath + Path.SEPARATOR + "metadata"));
            pqFileName = basePath + Path.SEPARATOR + "data.parquet";
        }
        // 保存metadata
        List<Map<String, Object>> metaDataMapList = writeMetaData(exampleSet, outputStream);

        if(metaDataMapList.size() == 0){
            outputStream.close();
            return;
        }

        Map<String, String> attributeNameToColumnNameMap = metaDataMapList.stream().collect(Collectors.toMap(s ->
                (String) s.get("Attribute_Name"), s -> (String) s.get("Column_Name")));

        // 定义avro schema
        Map<String, Object> tempAvscMap = new LinkedHashMap<String, Object>();
        tempAvscMap.put("type", "record");
        tempAvscMap.put("name", "data");

        List<Map<String, Object>> columnList = metaDataMapList.stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", e.get("Column_Name").toString().trim());
            String[] typeArr = {ontologyMap.get(e.get("Attribute_Type")), "null"};
            map.put("type", typeArr);
            return map;
        }).collect(Collectors.toList());

        tempAvscMap.put("fields", columnList);

        ObjectMapper mapper = new ObjectMapper();
        String avroJson = "";
        try {
            avroJson = mapper.writeValueAsString(tempAvscMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        parquetPath = new Path(pqFileName);
        // 解析成Parquet schema
        Schema schema = new Schema.Parser().parse(avroJson);
        ParquetWriter<GenericData.Record> parquetWriter = AvroParquetWriter.
                <GenericData.Record>builder(parquetPath)
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
                .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withValidation(false)
                .withDictionaryEncoding(false)
                .build();

        try {
            for (Example example : exampleSet) {
                // 每个Example对应一个Record
                GenericData.Record record = new GenericData.Record(schema);
                for (Attribute attribute : (Iterable<Attribute>) () -> exampleSet.getAttributes().allAttributes()) {
                    if (!Double.isNaN(example.getValue(attribute))) {
                        if (attribute.isNominal()) {
                            String stringValue = example.getValueAsString(attribute);
                            record.put(attributeNameToColumnNameMap.get(attribute.getName()), stringValue);
                        } else {
                            Double value = example.getValue(attribute);
                            if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
                                record.put(attributeNameToColumnNameMap.get(attribute.getName()), value.longValue());
                                // TODO: infinity values in parquet
                            } else if (value == Double.POSITIVE_INFINITY) {
                                record.put(attributeNameToColumnNameMap.get(attribute.getName()), Double.POSITIVE_INFINITY);
                            } else if (value == Double.NEGATIVE_INFINITY) {
                                record.put(attributeNameToColumnNameMap.get(attribute.getName()), Double.NEGATIVE_INFINITY);
                            } else if (attribute.getValueType() == Ontology.INTEGER) {
                                record.put(attributeNameToColumnNameMap.get(attribute.getName()), value.longValue());
                            } else {
                                record.put(attributeNameToColumnNameMap.get(attribute.getName()), value);
                            }
                        }
                    }
                }
                // 写record
                parquetWriter.write(record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (parquetWriter != null) {
                try {
                    parquetWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 定义{@link Ontology} 到Parquet数据类型的映射关系
     */
    public static final Map<String, String> ontologyMap;

    static {
        Map<String, String> tempOntologyMap = new HashMap<>();
        tempOntologyMap.put("attribute_value", "string");
        tempOntologyMap.put("nominal", "string");
        tempOntologyMap.put("numeric", "double");
        tempOntologyMap.put("integer", "long");
        tempOntologyMap.put("real", "double");
        tempOntologyMap.put("text", "string");
        tempOntologyMap.put("binominal", "string");
        tempOntologyMap.put("polynominal", "string");
        tempOntologyMap.put("file_path", "string");
        // TODO: maybe Date type or Time type
        tempOntologyMap.put("date_time", "long");
        tempOntologyMap.put("date", "long");
        tempOntologyMap.put("time", "long");
        ontologyMap = Collections.unmodifiableMap(tempOntologyMap);
    }

    /**
     * Save metadata to OutputStream
     *
     * @param exampleSet
     * @param stream
     * @return Map<String, String>
     */
    private static List<Map<String, Object>> writeMetaData(ExampleSet exampleSet, OutputStream stream) {
        ExampleSetMetaData esmd = new ExampleSetMetaData(exampleSet);
        List<Map<String, Object>> metaDataList = formMetaData(exampleSet);

        ObjectMapper mapper = new ObjectMapper();
        String mdJson = "";
        try {
            mdJson = mapper.writeValueAsString(metaDataList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            out.print(mdJson);
        }
        return metaDataList;
    }

    /**
     * form metadata
     *
     * @param exampleSet
     * @return List<Map<String, Object>>
     */
    public static List<Map<String, Object>> formMetaData(ExampleSet exampleSet) {
        int sampleSize = exampleSet.size();
        ExampleSetMetaData esmd = new ExampleSetMetaData(exampleSet);
        Attributes attributes = exampleSet.getAttributes();
        List<Map<String, Object>> metaDataList = new LinkedList<>();
        PreparationStatistics statistics = new PreparationStatistics(false);
        statistics.updateStatistics(exampleSet);
        NumericalStatisticsHandler numericalStatisticsHandler = statistics.getNumericalStatisticsHandler();
        NominalValueCountHandler nominalValueCountHandler = statistics.getNominalValueCountHandler();
        DateStatisticsHandler dateStatisticsHandler = statistics.getDateStatisticsHandler();
        DistributionHandler distributionHandler = statistics.getDistributionHandler();
        QualityMeasurementsHandler qualityMeasurementsHandler = statistics.getQualityMeasurementsHandler();
        DecimalFormat percentFormat = new DecimalFormat("0.00%");
        int columnCount = 1;
        for (AttributeMetaData amd: esmd.getAllAttributes()) {
            String attributeName = amd.getName();
            Attribute currentAttribute = attributes.get(attributeName);
            Map<String, Object> amdMap = new LinkedHashMap<>();
            amdMap.put("Column_Name", "column_" + String.valueOf(columnCount));
            amdMap.put("Attribute_Name", attributeName);
            amdMap.put("Attribute_Role", amd.getRole()!=null ? amd.getRole() : "attribute");
            amdMap.put("Attribute_Type", amd.getValueTypeName());
            amdMap.put("Attribute_Number_Missing_Value", amd.getNumberOfMissingValues().toString());
            amdMap.put("Attribute_Mean", amd.getMean().toString());
            amdMap.put("Attribute_Mode", amd.getMode()!=null ? amd.getMode() : "?");
            amdMap.put("Attribute_Value_Set_Relation", amd.getValueSetRelation()!=null ? amd.getValueSetRelation().toString(): "?");
            amdMap.put("Attribute_Value_Range", amd.getValueRange()!=null ? amd.getValueRange().toString() : "?");
            Set<String> valueSet = amd.getValueSet();
            String valueSetString;
            if (valueSet != null) {
                if (valueSet.size() != 0) {
                    valueSetString = valueSet.size() > 10 ? String.join("␝", Iterables.limit(valueSet, 10)) : String.join("␝", valueSet);
                } else {
                    valueSetString = "?";
                }
            } else {
                valueSetString = "?";
            }
            amdMap.put("Attribute_Value_Set", valueSetString);
            amdMap.put("Attribute_Annotations", amd.getAnnotations()!=null ? amd.getAnnotations().toString() : "?");
            amdMap.put("Attribute_Construction", attributes.get(attributeName).getConstruction());
            double missingNess = qualityMeasurementsHandler.getMissing(attributeName);
            missingNess = Double.isNaN(missingNess) ? 0.0 : missingNess;
            amdMap.put("Attribute_MissingNess", percentFormat.format(missingNess));
            amdMap.put("Attribute_MissingCount", String.valueOf((int) (sampleSize * missingNess)));
            double infiniteNess = qualityMeasurementsHandler.getInfinite(attributeName);
            infiniteNess = Double.isNaN(infiniteNess) ? 0.0 : infiniteNess;
            amdMap.put("Attribute_InfiniteNess", percentFormat.format(infiniteNess));
            amdMap.put("Attribute_InfiniteCount", String.valueOf((int) (sampleSize * infiniteNess)));
//            double idNess = qualityMeasurementsHandler.getIDNess(amd.getName());
//            idNess = Double.isNaN(idNess) ? 0.0 : idNess;
//            amdMap.put("Attribute_idNess", percentFormat.format(idNess).toString());
//            double stability = qualityMeasurementsHandler.getStability(amd.getName());
//            stability = Double.isNaN(stability) ? 0.0 : stability;
//            amdMap.put("Attribute_stability", percentFormat.format(stability).toString());
            double validNess = 1 - missingNess - infiniteNess;
            amdMap.put("Attribute_Valid", percentFormat.format(validNess));
            amdMap.put("Attribute_ValidCount", String.valueOf((int) (sampleSize * validNess)));

            if (currentAttribute.isNominal()) {
                amdMap.put("Attribute_GeneralType", "Nominal");
                amdMap.put("Attribute_NominalMostCommon", String.valueOf(nominalValueCountHandler.getMode(attributeName)));
                amdMap.put("Attribute_NominalUnique", String.valueOf(nominalValueCountHandler.getValueCounts(attributeName).size()));
                LinkedHashMap<String, Integer> top3Map = nominalValueCountHandler.getValueCounts(attributeName).entrySet()
                        .stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(3)
                        .collect(Collectors.toMap(x->x.getKey(), x->x.getValue(),
                                (e1, e2) -> e1, LinkedHashMap::new));
                LinkedList<String> top3Keys = new LinkedList<>(top3Map.keySet());
                LinkedList<Integer> top3Value = new LinkedList<>(top3Map.values());
                LinkedList<String> top3Ratio = new LinkedList<>();
                Iterator<Map.Entry<String, Integer>> top3ValueIterator = top3Map.entrySet().iterator();
                while (top3ValueIterator.hasNext()) {
                    int currentValue = top3ValueIterator.next().getValue();
                    top3Ratio.add(percentFormat.format((double)currentValue/sampleSize));
                }
                amdMap.put("Attribute_NominalTopKeys", top3Keys);
                amdMap.put("Attribute_NominalTopValues", top3Value);
                amdMap.put("Attribute_NominalTopRatio", top3Ratio);
            } else if (currentAttribute.isNumerical()) {
                amdMap.put("Attribute_GeneralType", "Numerical");
                amdMap.put("Attribute_NumericalMean", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.AVG)));
                amdMap.put("Attribute_NumericalStd", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.STD_DEV)));
                amdMap.put("Attribute_NumericalMin", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.MIN)));
                amdMap.put("Attribute_NumericalMax", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.MAX)));
                amdMap.put("Attribute_NumericalLowerQuartile", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.MIN)));
                amdMap.put("Attribute_NumericalHigherQuartile", String.format("%.3f", numericalStatisticsHandler.getStatistics(attributeName).get(NumericalStatisticsHandler.Type.MAX)));
                HistogramDataset histogramDataset = distributionHandler.getNumericalDistribution(attributeName);
                LinkedList<Double> bins = new LinkedList<>();
                LinkedList<Double> binCount = new LinkedList<>();
                for (int i = 0; i < histogramDataset.getItemCount(0); i++) {
                    bins.add(histogramDataset.getXValue(0, i));
                    binCount.add(histogramDataset.getYValue(0, i));
                }
                amdMap.put("Attribute_NumericalBins", bins);
                amdMap.put("Attribute_NumericalBinCount", binCount);
            } else if (currentAttribute.isDateTime()) {
                amdMap.put("Attribute_GeneralType", "DateTime");
                amdMap.put("Attribute_DateFrom", dateStatisticsHandler.getStatistics(attributeName).get(DateStatisticsHandler.Type.FROM));
                amdMap.put("Attribute_DateUntil", dateStatisticsHandler.getStatistics(attributeName).get(DateStatisticsHandler.Type.UNTIL));
                amdMap.put("Attribute_DateDuration", dateStatisticsHandler.getStatistics(attributeName).get(DateStatisticsHandler.Type.DURATION));
                amdMap.put("Attribute_DateMean", dateStatisticsHandler.getStatistics(attributeName).get(DateStatisticsHandler.Type.MEAN));
                HistogramDataset histogramDataset = distributionHandler.getNumericalDistribution(attributeName);
                LinkedList<Double> bins = new LinkedList<>();
                LinkedList<Double> binCount = new LinkedList<>();
                for (int i = 0; i < histogramDataset.getItemCount(0); i++) {
                    bins.add(histogramDataset.getXValue(0, i));
                    binCount.add(histogramDataset.getYValue(0, i));
                }
                amdMap.put("Attribute_DateBins", bins);
                amdMap.put("Attribute_DateBinCount", binCount);
            }

            metaDataList.add(amdMap);
            columnCount += 1;
        }
        return metaDataList;
    }

    /**
     * Read from parquet file to {@link ExampleSet}
     *
     * @param basePath
     * @param inferMetaData
     * @param fromLocal
     * @return
     */
    public static ExampleSet readFromParquet(String basePath, Boolean inferMetaData, Boolean fromLocal) {
        ExampleSet exampleSet = null;
        Configuration conf;
        if (fromLocal) {
            conf = new Configuration();
        } else {
            try {
                fs.getStatus();
            } catch (Exception e){
                initFileSystem();
            }
            conf = fs.getConf();
        }
        // 从metadata读取数据类型
        if (inferMetaData) {
            try (ParquetFileReader reader = ParquetFileReader.open(conf, new Path(basePath + Path.SEPARATOR + "data.parquet"))) {
                ParquetMetadata footer = reader.getFooter();
                MessageType schema = footer.getFileMetaData().getSchema();

                int size = (int) reader.getRecordCount();
                String metaDataStr;
                if (fromLocal) {
                    metaDataStr = FileUtils.readFileToString(new File(basePath + File.separator + "metadata"), StandardCharsets.UTF_8);
                } else {
                    OutputStream metaDataStream = new ByteArrayOutputStream();
                    HDFSUtil.copyFileAsStream(basePath + Path.SEPARATOR + "metadata", metaDataStream);
                    metaDataStr = ((ByteArrayOutputStream)metaDataStream).toString("UTF-8");
                    metaDataStream.close();
                }

                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, String>> metaDataMapList = mapper.readValue(metaDataStr, new TypeReference<LinkedList<LinkedHashMap<String, String>>>() {
                });
                List<Attribute> listOfAttrs = new LinkedList<>();
                Map<Attribute, String> specialAttributes = new LinkedHashMap<>();
                for (Map<String, String> metaDataMap : metaDataMapList) {
                    String columnName = metaDataMap.get("Column_Name");
                    String attributeName = metaDataMap.get("Attribute_Name");
                    String attributeType = metaDataMap.get("Attribute_Type");
                    String attributeRole = metaDataMap.get("Attribute_Role");
                    String attributeValueSet = metaDataMap.get("Attribute_Value_Set");
                    String attributeConstruction = metaDataMap.get("Attribute_Construction");
                    Attribute attribute;
                    if ("attribute_value".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.ATTRIBUTE_VALUE);
                    } else if ("nominal".equals(attributeType)) {
                        String[] attributeValueSetArray = attributeValueSet.split("␝");
                        Map<Integer, String> attributeValueSetMap =
                                IntStream.range(0, attributeValueSetArray.length)
                                        .boxed()
                                        .collect(Collectors.toMap(i -> i, i -> attributeValueSetArray[i]));
                        PolynominalMapping nominalMapping = new PolynominalMapping(attributeValueSetMap);
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.NOMINAL);
                        attribute.setMapping(nominalMapping);
                    } else if ("numeric".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.NUMERICAL);
                    } else if ("integer".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.INTEGER);
                    } else if ("real".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.REAL);
                    } else if ("text".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.STRING);
                    } else if ("binominal".equals(attributeType)) {
                        String[] attributeValueSetArray = attributeValueSet.split("␝");
                        Map<Integer, String> attributeValueSetMap =
                                IntStream.range(0, attributeValueSetArray.length)
                                        .boxed()
                                        .collect(Collectors.toMap(i -> i, i -> attributeValueSetArray[i]));
                        PolynominalMapping nominalMapping = new PolynominalMapping(attributeValueSetMap);
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.BINOMINAL);
                        attribute.setMapping(nominalMapping);
                    } else if ("polynominal".equals(attributeType)) {
                        String[] attributeValueSetArray = attributeValueSet.split("␝");
                        Map<Integer, String> attributeValueSetMap =
                                IntStream.range(0, attributeValueSetArray.length)
                                        .boxed()
                                        .collect(Collectors.toMap(i -> i, i -> attributeValueSetArray[i]));
                        PolynominalMapping nominalMapping = new PolynominalMapping(attributeValueSetMap);
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.POLYNOMINAL);
                        attribute.setMapping(nominalMapping);
                    } else if ("file_path".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.FILE_PATH);
                    } else if ("date_time".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.DATE_TIME);
                    } else if ("date".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.DATE);
                    } else if ("time".equals(attributeType)) {
                        attribute = AttributeFactory.createAttribute(attributeName, Ontology.TIME);
                    } else {
                        throw new IOException("Unknown value type: '" + attributeType + "'");
                    }
                    attribute.setConstruction(attributeConstruction);
                    listOfAttrs.add(attribute);
                    if (!"attribute".equals(attributeRole)) {
                        specialAttributes.put(attribute, attributeRole);
                    }
                }

                ExampleSetBuilder builder = ExampleSets.from(listOfAttrs).withExpectedSize(size).withRoles(specialAttributes);
                PageReadStore store;
                while ((store = reader.readNextRowGroup()) != null) {
                    final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                    final RecordReader<Group> recordReader = columnIO.getRecordReader(store, new GroupRecordConverter(schema));
                    for (int i = 0; i < size; i++) {
                        Group group = recordReader.read();
                        double[] doubleArray = new double[listOfAttrs.size()];
                        for (int j = 0; j < listOfAttrs.size(); j++) {
                            Attribute attribute = listOfAttrs.get(j);
                            String attributeType = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType());
                            int repetitionCount = group.getFieldRepetitionCount(j);
                            if (repetitionCount == 1) {
                                switch (attributeType) {
                                    case "attribute_value":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "nominal":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "numeric":
                                        doubleArray[j] = group.getDouble(j, 0);
                                        break;
                                    case "integer":
                                        doubleArray[j] = group.getLong(j, 0);
                                        break;
                                    case "real":
                                        doubleArray[j] = group.getDouble(j, 0);
                                        break;
                                    case "text":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "binominal":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "polynominal":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "file_path":
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "date_time":
                                    case "time":
                                    case "date":
                                        doubleArray[j] = group.getLong(j, 0);
                                        break;
                                    default:
                                        doubleArray[j] = attribute.getMapping().mapString(group.getString(j, 0));
                                        break;
                                }
                            } else if (repetitionCount == 0) {
                                doubleArray[j] = Double.NaN;
                            }
                        }
                        builder.addRow(doubleArray);
                    }
                }

                exampleSet = builder.build();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try (ParquetFileReader reader = ParquetFileReader.open(conf, new Path(basePath + Path.SEPARATOR + "data.parquet"))) {
                ParquetMetadata footer = reader.getFooter();
                MessageType schema = footer.getFileMetaData().getSchema();
                int size = (int) reader.getRecordCount();

                List<Attribute> listOfAttrs = new LinkedList<>();

                for (ColumnDescriptor column : schema.getColumns()) {
                    String name = String.join(".", column.getPath());
                    PrimitiveType primitiveType = column.getPrimitiveType();

                    Attribute attribute;
                    switch (primitiveType.getPrimitiveTypeName()) {
                        case BOOLEAN:
                            attribute = AttributeFactory.createAttribute(name, Ontology.BINOMINAL);
                            break;

                        case INT32:
                            attribute = AttributeFactory.createAttribute(name, Ontology.INTEGER);
                            break;

                        case INT64:
                            attribute = AttributeFactory.createAttribute(name, Ontology.INTEGER);
                            break;

                        case INT96:
                            attribute = AttributeFactory.createAttribute(name, Ontology.INTEGER);
                            break;

                        case FLOAT:

                        case FIXED_LEN_BYTE_ARRAY:
                            attribute = AttributeFactory.createAttribute(name, Ontology.REAL);
                            break;

                        case DOUBLE:
                            attribute = AttributeFactory.createAttribute(name, Ontology.REAL);
                            break;

                        case BINARY:
                            attribute = AttributeFactory.createAttribute(name, Ontology.POLYNOMINAL);
                            break;
                        default:
                            attribute = AttributeFactory.createAttribute(name, Ontology.STRING);
                            break;
                    }

                    listOfAttrs.add(attribute);
                }

                ExampleSetBuilder builder = ExampleSets.from(listOfAttrs).withExpectedSize(size);
                PageReadStore store;
                while ((store = reader.readNextRowGroup()) != null) {
                    final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                    final RecordReader<Group> recordReader = columnIO.getRecordReader(store, new GroupRecordConverter(schema));
                    for (int i = 0; i < size; i++) {
                        Group group = recordReader.read();
                        double[] doubleArray = new double[listOfAttrs.size()];
                        for (int j = 0; j < listOfAttrs.size(); j++) {
                            Attribute attr = listOfAttrs.get(j);
                            String attributeType = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attr.getValueType());
                            int repetitionCount = group.getFieldRepetitionCount(j);
                            if (repetitionCount == 1) {
                                switch (attributeType) {
                                    case "attribute_value":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "nominal":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "numeric":
                                        doubleArray[j] = group.getDouble(j, 0);
                                        break;
                                    case "integer":
                                        doubleArray[j] = group.getLong(j, 0);
                                        break;
                                    case "real":
                                        doubleArray[j] = group.getDouble(j, 0);
                                        break;
                                    case "text":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "binominal":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "polynominal":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "file_path":
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                    case "date_time":
                                    case "time":
                                    case "date":
                                        doubleArray[j] = group.getLong(j, 0);
                                        break;
                                    default:
                                        doubleArray[j] = attr.getMapping().mapString(group.getString(j, 0));
                                        break;
                                }
                            } else if (repetitionCount == 0) {
                                doubleArray[j] = Double.NaN;
                            }
                        }
                        builder.addRow(doubleArray);
                    }
                }
                // 构造ExampleSet
                exampleSet = builder.build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return exampleSet;
    }
}