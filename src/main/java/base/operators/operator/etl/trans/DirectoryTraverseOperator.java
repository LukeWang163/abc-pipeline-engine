package base.operators.operator.etl.trans;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.preprocessing.filter.ExampleFilter;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeRegexp;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryTraverseOperator extends Operator {

    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public DirectoryTraverseOperator(OperatorDescription description) {
        super(description);
    }

    private static final String DIRECTORY_PATH = "directory_path";
    private static final String WILDCARD_INCLUDE = "wildcard_include";
    private static final String WILDCARD_EXCLUDE = "wildcard_exclude";

    String sourcesdirectory;
    String pathSeperator;
    String wildcard;
    String wildcardexclude;
    static int id = 0;
    Map<String, Long> subFolderTime;
    //存储路径以及该路径到根节点的距离，以方便进行子文件夹大小计算时，从最底端的子文件夹开始计算，再计算上一层父文件夹的大小
    Map<String, Integer> subFolderDistance;

    public void doWork() throws OperatorException {
        sourcesdirectory = getParameterAsString(DIRECTORY_PATH);
        pathSeperator = sourcesdirectory.indexOf("/")==-1 ? "\\" : "/";
        wildcard = getParameterAsString(WILDCARD_INCLUDE);
        wildcardexclude = getParameterAsString(WILDCARD_EXCLUDE);
        subFolderTime = new HashMap<>();
        subFolderDistance = new HashMap<>();
        //构造第一个输出
        id = 0;
        List<Attribute> attributeList = new ArrayList<>();
        Attribute id_attribute = AttributeFactory.createAttribute("id", Ontology.INTEGER);
        attributeList.add(id_attribute);
        Attribute path_attribute = AttributeFactory.createAttribute("path", Ontology.STRING);
        attributeList.add(path_attribute);
        Attribute parent_path_attribute = AttributeFactory.createAttribute("parent_path", Ontology.STRING);
        attributeList.add(parent_path_attribute);
        Attribute size_attribute = AttributeFactory.createAttribute("size(byte)", Ontology.NUMERICAL);
        attributeList.add(size_attribute);
        Attribute modify_attribute = AttributeFactory.createAttribute("last_modify_time", Ontology.DATE_TIME);
        attributeList.add(modify_attribute);
        Attribute type_attribute = AttributeFactory.createAttribute("type", Ontology.STRING);
        attributeList.add(type_attribute);
        Attribute is_file_attribute = AttributeFactory.createAttribute("is_file", Ontology.BINOMINAL);
        attributeList.add(is_file_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        traverseFolder(sourcesdirectory, attributeList, exampleTable);
        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        //下边添加文件夹列，主要是计算子文件夹的大小
        ArrayList<Map.Entry<String, Integer>> subFolderDistanceSorted = new ArrayList<Map.Entry<String, Integer>>(subFolderDistance.entrySet());
        //排序
        Collections.sort(subFolderDistanceSorted, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> map1, Map.Entry<String, Integer> map2) {
                return (map2.getValue() - map1.getValue());
            }
        });
        int size = exampleTable.size();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        for(Map.Entry<String, Integer> entry : subFolderDistanceSorted){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(attributeList.get(0), size);
            size++;
            dataRow.set(attributeList.get(1), attributeList.get(1).getMapping().mapString(entry.getKey()));
            dataRow.set(attributeList.get(2), attributeList.get(2).getMapping().mapString(entry.getKey().substring(0, entry.getKey().lastIndexOf(pathSeperator))));
            dataRow.set(attributeList.get(4), subFolderTime.get(entry.getKey()));
            dataRow.set(attributeList.get(5), attributeList.get(5).getMapping().mapString("floder"));
            dataRow.set(attributeList.get(6), attributeList.get(6).getMapping().mapString("false"));
            //按照路径path筛选父路径parent_path,计算子文件夹大小
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", parent_path_attribute.getName()+"="+entry.getKey());
            ExampleSet filterExampleSet = examplesFilter.apply(exampleSet);
            filterExampleSet.recalculateAllAttributeStatistics();
            double value = filterExampleSet.getStatistics(size_attribute, Statistics.SUM);
            dataRow.set(attributeList.get(3), value);
            exampleTable.addDataRow(dataRow);
        }

        exampleSetOutput.deliver(exampleSet);
    }

    public void traverseFolder(String path, List<Attribute> attributes, MemoryExampleTable exampleTable) {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null == files || files.length == 0) {
                System.out.println("文件夹是空的!");
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        subFolderTime.put(file2.getAbsolutePath(), file2.lastModified());
                        subFolderDistance.put(file2.getAbsolutePath(), file2.getAbsolutePath().split(pathSeperator.equals("\\")?"\\\\":pathSeperator).length);
                        traverseFolder(file2.getAbsolutePath(), attributes, exampleTable);
                    } else {
                        Pattern patternInclude = null;
                        Pattern patternExclude = null;
                        boolean getIt = true;
                        boolean getItexclude = false;

                        if(wildcard!=null && !"".equals(wildcard)){
                            patternInclude = Pattern.compile( wildcard );
                            if(patternInclude!=null){
                                Matcher matcher = patternInclude.matcher(file2.getPath().substring(file2.getPath().lastIndexOf(pathSeperator)+1));
                                getIt = matcher.matches();
                            }
                        }
                        if(wildcardexclude!=null && !"".equals(wildcardexclude)){
                            patternExclude = Pattern.compile( wildcardexclude );
                            if(patternExclude!=null){
                                Matcher matcher = patternExclude.matcher(file2.getPath().substring(file2.getPath().lastIndexOf(pathSeperator)+1));
                                getItexclude = matcher.matches();
                            }
                        }
                        if(getIt&&!getItexclude){
                            DataRowFactory factory = new DataRowFactory(0, '.');
                            DataRow dataRow = factory.create(attributes.size());
                            dataRow.set(attributes.get(0), id);
                            id++;
                            dataRow.set(attributes.get(1), attributes.get(1).getMapping().mapString(file2.getAbsolutePath()));
                            dataRow.set(attributes.get(2), attributes.get(2).getMapping().mapString(file2.getParent()));
                            dataRow.set(attributes.get(3), file2.length());
                            dataRow.set(attributes.get(4), file2.lastModified());
                            dataRow.set(attributes.get(5), attributes.get(5).getMapping().mapString(file2.getPath().lastIndexOf(".")==-1? "unknow":file2.getPath().substring(file2.getPath().lastIndexOf(".")+1)));
                            dataRow.set(attributes.get(6), attributes.get(6).getMapping().mapString("true"));
                            exampleTable.addDataRow(dataRow);
                        }
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeString(DIRECTORY_PATH,"The path is to traverse.", false,false));
        types.add(new ParameterTypeRegexp(WILDCARD_INCLUDE,"Wildcard include of file name.", true,false));
        types.add(new ParameterTypeRegexp(WILDCARD_EXCLUDE,"Wildcard exclude of file name.", true,false));

        return types;
    }
}

