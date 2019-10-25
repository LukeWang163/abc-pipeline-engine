package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRowFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.io.process.XMLTools;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.nio.file.FileInputPortHandler;
import base.operators.operator.ports.InputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.ParameterService;
import base.operators.tools.RandomGenerator;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XrffExampleSource extends AbstractExampleSource {
    public static final String PARAMETER_DATA_FILE = "data_file";
    public static final String PARAMETER_ID_ATTRIBUTE = "id_attribute";
    public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
    public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";
    public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";
    public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
    private final InputPort fileInputPort = (InputPort)getInputPorts().createPort("file");
    private final FileInputPortHandler filePortHandler = new FileInputPortHandler(this, this.fileInputPort, "data_file");

    public XrffExampleSource(OperatorDescription description) { super(description); }

    public ExampleSet createExampleSet() throws OperatorException {
        String idName = getParameterAsString("id_attribute");
        Attribute label = null;
        Attribute id = null;
        Attribute weight = null;
        boolean instanceWeightsUsed = false;
        ExampleSetBuilder builder = null;
        try {
            Document document = null;
            try {
                document = XMLTools.createDocumentBuilder().parse(this.filePortHandler.openSelectedFile());
            } catch (SAXException e1) {
                throw new IOException(e1.getMessage(), e1);
            }

            Element datasetElement = document.getDocumentElement();
            if (!datasetElement.getTagName().equals("dataset")) {
                throw new IOException("Outer tag of XRFF file must be <dataset>.");
            }

            Element headerElement = retrieveSingleNode(datasetElement, "header");
            Element attributesElement = retrieveSingleNode(headerElement, "attributes");

            List<Attribute> attributeList = new LinkedList<Attribute>();
            NodeList attributes = attributesElement.getChildNodes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node node = attributes.item(i);
                if (node instanceof Element) {
                    Element attribute = (Element)node;
                    String tagName = attribute.getTagName();
                    if (!tagName.equals("attribute")) {
                        throw new IOException("Only tags <attribute> are allowed inside <attributes>, was " + tagName);
                    }

                    String name = attribute.getAttribute("name");
                    if (name == null) {
                        throw new IOException("The tag <attribute> needs a 'name' attribute.");
                    }
                    String classAttribute = attribute.getAttribute("class");
                    boolean isClass = (classAttribute != null && classAttribute.equals("yes"));
                    String valueType = attribute.getAttribute("type");
                    if (valueType == null) {
                        throw new IOException("The tag <attribute> needs a 'type' attribute.");
                    }

                    Attribute att = createAttribute(name, valueType);
                    if (att.isNominal()) {
                        Element labelsElement = retrieveSingleNode(attribute, "labels", false);
                        if (labelsElement != null) {
                            NodeList labels = labelsElement.getChildNodes();
                            for (int j = 0; j < labels.getLength(); j++) {
                                Node labelNode = labels.item(j);
                                if (labelNode instanceof Element) {
                                    String labelTagName = labelNode.getNodeName();
                                    if (!labelTagName.equals("label")) {
                                        throw new IOException("Only tags <label> are allowed inside <labels>, was " + labelTagName);
                                    }

                                    String labelValue = labelNode.getTextContent();
                                    att.getMapping().mapString(labelValue);
                                }
                            }
                        }
                    }

                    if (isClass) {
                        label = att;
                    }

                    if (idName != null && name.equals(idName)) {
                        id = att;
                    }

                    attributeList.add(att);
                }
            }

            weight = AttributeFactory.createAttribute("weight", 4);
            attributeList.add(weight);


            builder = ExampleSets.from(attributeList);
            int datamanagement = getParameterAsInt("datamanagement");

            if (!Boolean.parseBoolean(ParameterService.getParameterValue("rapidminer.system.legacy_data_mgmt"))) {
                datamanagement = 0;
                builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
            }

            DataRowFactory factory = new DataRowFactory(datamanagement, getParameterAsString("decimal_point_character").charAt(0));
            Attribute[] attributeArray = new Attribute[attributeList.size()];
            attributeList.toArray(attributeArray);
            Element bodyElement = retrieveSingleNode(datasetElement, "body");
            Element instancesElement = retrieveSingleNode(bodyElement, "instances");
            NodeList instances = instancesElement.getChildNodes();
            int maxRows = getParameterAsInt("sample_size");
            double sampleProb = getParameterAsDouble("sample_ratio");
            RandomGenerator random = RandomGenerator.getRandomGenerator(this);
            int counter = 0;

            for (int i = 0; i < instances.getLength(); i++) {
                Node node = instances.item(i);
                if (node instanceof Element) {
                    Element instance = (Element)node;
                    String tagName = instance.getTagName();
                    if (!tagName.equals("instance")) {
                        throw new IOException("Only tags <instance> are allowed inside <instances>, was " + tagName);
                    }

                    NodeList values = instance.getChildNodes();
                    int elementCount = 0;
                    for (int j = 0; j < values.getLength(); j++) {
                        if (values.item(j) instanceof Element) {
                            elementCount++;
                        }
                    }

                    if (elementCount != attributeList.size() - 1)
                    {
                        throw new IOException("Number of values must be the same than the number of attributes.");
                    }
                    String[] valueArray = new String[attributeList.size()];
                    int index = 0;
                    for (int j = 0; j < values.getLength(); j++) {
                        Node valueNode = values.item(j);
                        if (valueNode instanceof Element) {
                            Element valueElement = (Element)valueNode;
                            String valueTagName = valueElement.getTagName();
                            if (!valueTagName.equals("value")) {
                                throw new IOException("Only tags <value> are allowed inside <instance>, was " + valueTagName);
                            }


                            valueArray[index++] = valueNode.getTextContent();
                        }
                    }

                    String weightString = instance.getAttribute("weight");
                    if (weightString != null && weightString.length() > 0) {
                        valueArray[valueArray.length - 1] = weightString;
                        instanceWeightsUsed = true;
                    } else {
                        valueArray[valueArray.length - 1] = "1.0";
                    }

                    if (maxRows > -1 && counter >= maxRows) {
                        break;
                    }

                    counter++;

                    if (maxRows != -1 || random.nextDouble() <= sampleProb)
                    {
                        builder.addDataRow(factory.create(valueArray, attributeArray)); }
                }
            }
        } catch (IOException e) {
            throw new UserError(this, '?', new Object[] { this.filePortHandler.getSelectedFileDescription(), e.getMessage() });
        }

        if (label != null) {
            builder.withRole(label, "label");
        }
        builder.withRole(weight, "weight");
        if (id != null) {
            builder.withRole(id, "id");
        }

        ExampleSet result = builder.build();
        if (!instanceWeightsUsed) {
            result.getAttributes().remove(weight);
            result.getExampleTable().removeAttribute(weight);
        }

        return result;
    }


    private Element retrieveSingleNode(Element element, String nodeName) throws IOException { return retrieveSingleNode(element, nodeName, true); }



    private Element retrieveSingleNode(Element element, String nodeName, boolean exceptionOnFail) throws IOException {
        NodeList headerElements = element.getElementsByTagName(nodeName);
        if (headerElements.getLength() == 0) {
            if (exceptionOnFail) {
                throw new IOException("A dataset must define a <" + nodeName + "> section for attribute meta data description.");
            }

            return null;
        }

        if (headerElements.getLength() > 1) {
            if (exceptionOnFail) {
                throw new IOException("A dataset must not define more than one <" + nodeName + "> section.");
            }
            return null;
        }

        return (Element)headerElements.item(0);
    }

    private Attribute createAttribute(String name, String type) {
        int valueType = 1;
        if (type.equalsIgnoreCase("numeric")) {
            valueType = 2;
        } else if (type.equalsIgnoreCase("real")) {
            valueType = 4;
        } else if (type.equalsIgnoreCase("integer")) {
            valueType = 3;
        } else if (type.equalsIgnoreCase("string")) {
            valueType = 5;
        } else if (type.equalsIgnoreCase("date")) {
            valueType = 10;
        }
        return AttributeFactory.createAttribute(name, valueType);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = FileInputPortHandler.makeFileParameterType(this, "data_file", "Name of the Xrff file to read the data from.", "xrff", () -> this.fileInputPort);
        type.setPrimary(true);
        types.add(type);
        types.add(new ParameterTypeString("id_attribute", "The (case sensitive) name of the id attribute"));
        DataManagementParameterHelper.addParameterTypes(types, this);
        types.add(new ParameterTypeString("decimal_point_character", "Character that is used as decimal point.", "."));
        ParameterTypeDouble parameterTypeDouble = new ParameterTypeDouble("sample_ratio", "The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0D, 1.0D, 1.0D);
        parameterTypeDouble.setExpert(false);
        types.add(parameterTypeDouble);
        types.add(new ParameterTypeInt("sample_size", "The exact number of samples which should be read (-1 = use sample ratio; if not -1, sample_ratio will not have any effect)", -1, 2147483647, -1));
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }
}

