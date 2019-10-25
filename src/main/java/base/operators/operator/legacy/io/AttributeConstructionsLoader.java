package base.operators.operator.legacy.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.generator.GenerationException;
import base.operators.io.process.XMLTools;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.LoggingHandler;
import base.operators.tools.expression.ExampleResolver;
import base.operators.tools.expression.ExpressionException;
import base.operators.tools.expression.ExpressionParser;
import base.operators.tools.expression.ExpressionParserBuilder;
import base.operators.tools.expression.ExpressionRegistry;
import base.operators.tools.expression.internal.ExpressionParserUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AttributeConstructionsLoader
        extends Operator
{
    private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
    private OutputPort exampleSetOutput = (OutputPort)getOutputPorts().createPort("example set");

    public static final String PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE = "attribute_constructions_file";

    public static final String PARAMETER_KEEP_ALL = "keep_all";

    public AttributeConstructionsLoader(OperatorDescription description) {
        super(description);
        getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetInput, this.exampleSetOutput, SetRelation.SUPERSET)
        {
            @Override
            public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError
            {
                metaData.clearRegular();
                return metaData;
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet)this.exampleSetInput.getData(ExampleSet.class);
        boolean keepAll = getParameterAsBoolean("keep_all");
        List<Attribute> oldAttributes = new LinkedList<Attribute>();
        for (Attribute attribute : exampleSet.getAttributes()) {
            oldAttributes.add(attribute);
        }
        File file = getParameterAsFile("attribute_constructions_file");
        List<Attribute> generatedAttributes = new LinkedList<Attribute>();
        if (file != null) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                generatedAttributes = generateAll(this, exampleSet, in);
            } catch (IOException e) {
                throw new UserError(this, e, 302, new Object[] { file.getName(), e.getMessage() });
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        getLogger().warning("Cannot close stream to file " + file);
                    }
                }
            }
        }

        if (!keepAll) {
            for (Attribute oldAttribute : oldAttributes) {
                if (!generatedAttributes.contains(oldAttribute)) {
                    exampleSet.getAttributes().remove(oldAttribute);
                }
            }
        }

        this.exampleSetOutput.deliver(exampleSet);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("attribute_constructions_file", "Filename for the attribute constructions file.", "att", false, false));
        types.add(new ParameterTypeBoolean("keep_all", "If set to true, all the original attributes are kept, otherwise they are removed from the example set.", false, false));
        return types;
    }

    private List<Attribute> generateAll(LoggingHandler logging, ExampleSet exampleSet, InputStream in) throws IOException, GenerationException, ProcessStoppedException {
        LinkedList<Attribute> generatedAttributes = new LinkedList<Attribute>();
        Document document = null;
        try {
            document = XMLTools.createDocumentBuilder().parse(in);
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        }

        Element constructionsElement = document.getDocumentElement();
        if (!constructionsElement.getTagName().equals("constructions")) {
            throw new IOException("Outer tag of attribute constructions file must be <constructions>");
        }

        NodeList constructions = constructionsElement.getChildNodes();
        for (int i = 0; i < constructions.getLength(); i++) {
            Node node = constructions.item(i);
            if (node instanceof Element) {
                Element constructionTag = (Element)node;
                String tagName = constructionTag.getTagName();
                if (!tagName.equals("attribute")) {
                    throw new IOException("Only <attribute> tags are allowed for attribute description files, but found " + tagName);
                }
                String attributeName = constructionTag.getAttribute("name");
                String attributeConstruction = constructionTag.getAttribute("construction");
                if (attributeName == null) {
                    throw new IOException("<attribute> tag needs 'name' attribute.");
                }
                if (attributeConstruction == null) {
                    throw new IOException("<attribute> tag needs 'construction' attribute.");
                }
                if (attributeConstruction.equals(attributeName)) {
                    Attribute presentAttribute = exampleSet.getAttributes().get(attributeName);
                    if (presentAttribute != null) {
                        generatedAttributes.add(presentAttribute);
                    } else {

                        throw new GenerationException("No such attribute: " + attributeName);
                    }
                } else {
                    ExpressionParserBuilder builder = new ExpressionParserBuilder();

                    builder = builder.withModules(ExpressionRegistry.INSTANCE.getAll());
                    ExampleResolver resolver = new ExampleResolver(exampleSet);
                    builder = builder.withDynamics(resolver);

                    ExpressionParser parser = builder.build();
                    try {
                        generatedAttributes.add(ExpressionParserUtils.addAttribute(exampleSet, attributeName, attributeConstruction, parser, resolver, null));
                    }
                    catch (ExpressionException e) {
                        throw new GenerationException(e.getShortMessage());
                    }
                }
            }
        }
        return generatedAttributes;
    }
}
