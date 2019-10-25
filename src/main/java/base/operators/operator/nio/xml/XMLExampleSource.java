package base.operators.operator.nio.xml;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.nio.model.AbstractDataResultSetReader;
import base.operators.operator.nio.model.DataResultSetFactory;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDateFormat;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.StrictDecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class XMLExampleSource extends AbstractDataResultSetReader {
    public static final String PARAMETER_FILE = "file";
    public static final String PARAMETER_XPATH_FOR_EXAMPLES = "xpath_for_examples";
    public static final String PARAMETER_XPATHS_FOR_ATTRIBUTES = "xpaths_for_attributes";
    public static final String PARAMETER_XPATH_FOR_ATTRIBUTE = "xpath_for_attribute";
    public static final String PARAMETER_USE_NAMESPACES = "use_namespaces";
    public static final String PARAMETER_USE_DEFAULT_NAMESPACE = "use_default_namespace";
    public static final String PARAMETER_DEFAULT_NAMESPACE = "default_namespace";
    public static final String PARAMETER_NAMESPACES = "namespaces";
    public static final String PARAMETER_NAMESPACE = "namespace";
    public static final String PARAMETER_NAMESPACE_ID = "id";
    public static final String PARAMETER_STORAGE_TYPE = "storage_type";
    public static final OperatorVersion CHANGE_5_1_013_NODE_OUTPUT = new OperatorVersion(5, 1, 13);

    public XMLExampleSource(OperatorDescription description) { super(description); }

    @Override
    protected DataResultSetFactory getDataResultSetFactory() throws OperatorException { return new XMLResultSetConfiguration(this); }

    @Override
    protected NumberFormat getNumberFormat() throws OperatorException { return StrictDecimalFormat.getInstance(this, true); }

    @Override
    protected boolean isSupportingFirstRowAsNames() { return false; }

    @Override
    protected String getFileParameterName() { return "file"; }

    @Override
    protected String getFileExtension() { return "xml"; }

//    //添加：不从fileinputPort判断
//    @Override
//    public boolean isFileSpecified() {
//        return false;
//    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
//        ParameterTypeConfiguration parameterTypeConfiguration = new ParameterTypeConfiguration(XMLExampleSourceConfigurationWizardCreator.class, this);
//        parameterTypeConfiguration.setExpert(false);
//        types.add(parameterTypeConfiguration);
        types.add(new ParameterTypeString(PARAMETER_STORAGE_TYPE, "storage type", "hdfs"));
        types.add(makeFileParameterType());
        types.add(new ParameterTypeString("xpath_for_examples", "The matches of this XPath Expression will form the examples. Each match becomes one example whose attribute values are extracted from the matching part of the xml file.", false));
        types.add(new ParameterTypeEnumeration("xpaths_for_attributes", "This XPaths expressions will be evaluated for each match to the XPath expression for examples to derive values for attributes. Each expression forms one attribute in the resulting ExampleSet.", new ParameterTypeString("xpath_for_attribute", "This XPath expression will be evaluated agains each match to the XPath expression for examples to derive values for this attribute. Each line in this list forms one attribute in the resulting ExampleSet."), false));
        types.add(new ParameterTypeBoolean("use_namespaces", "If not checked namespaces in the XML document will be completely ignored. This might make formulating XPath expressions easier, but elements with the same name might collide if separated by namespace.", true));
        ParameterTypeList parameterTypeList = new ParameterTypeList("namespaces", "Specifies pairs of identifier and namespace for use in XPath queries. The namespace for (x)html is bound automatically to the identifier h.", new ParameterTypeString("id", "The id of this namespace. With this id the namespace can be referred to in the XPath expression."), new ParameterTypeString("namespace", "The namespace to which the id should be bound.", false));
        parameterTypeList.registerDependencyCondition(new BooleanParameterCondition(this, "use_namespaces", false, true));
        types.add(parameterTypeList);
        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("use_default_namespace", "If checkedyou can specify an namespace uri that will be used when no namespace is specified in the XPath expression.", true);
        parameterTypeBoolean.registerDependencyCondition(new BooleanParameterCondition(this, "use_namespaces", false, true));
        types.add(parameterTypeBoolean);
        ParameterTypeString parameterTypeString = new ParameterTypeString("default_namespace", "This is the default namespace that will be assumed for all elements in the XPath expression that have no explict namespace mentioned.", true);
        parameterTypeString.registerDependencyCondition(new BooleanParameterCondition(this, "use_default_namespace", false, true));
        types.add(parameterTypeString);
        types.addAll(StrictDecimalFormat.getParameterTypes(this, true));
        ParameterTypeDateFormat parameterTypeDateFormat = new ParameterTypeDateFormat();
        parameterTypeDateFormat.setDefaultValue("yyyy-MM-dd");
        types.add(parameterTypeDateFormat);
        types.addAll(super.getParameterTypes());
        return types;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        OperatorVersion[] changes = super.getIncompatibleVersionChanges();
        changes = (OperatorVersion[])Arrays.copyOf(changes, changes.length + 1);
        changes[changes.length - 1] = CHANGE_5_1_013_NODE_OUTPUT;
        return changes;
    }
}
