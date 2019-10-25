package base.operators.operator.nio.xml;

import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.UserError;
import base.operators.operator.nio.model.DataResultSet;
import base.operators.operator.nio.model.ParseException;
import base.operators.operator.nio.model.ParsingError;
import base.operators.tools.ProgressListener;
import base.operators.tools.xml.MapBasedNamespaceContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.util.*;

public class XMLResultSet implements DataResultSet{
    private NodeList exampleNodes = null;
    private XPathExpression[] attributeExpressions = null;
    private String[] attributeNames = null;
    private int[] attributeValueTypes = null;
    private int currentExampleIndex = -1;
    private String[] currentExampleValues = null;
    private OperatorVersion operatorVersion;
    private final int exampleNodesLength;

    public XMLResultSet(Operator callingOperator, XMLResultSetConfiguration configuration, OperatorVersion operatorVersion) throws OperatorException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        this.operatorVersion = operatorVersion;
        Map<String, String> namespacesMap = configuration.getNamespacesMap();
        xpath.setNamespaceContext(new MapBasedNamespaceContext(namespacesMap, configuration.getDefaultNamespaceURI()));
        XPathExpression exampleExpression = null;
        try {
            String exampleXPath = configuration.getExampleXPath();
            if (exampleXPath == null) {
                throw new UserError(callingOperator, 217, new Object[] { "xpath_for_examples", (callingOperator != null) ? callingOperator.getName() : "unnamed", "" });
            }
            exampleExpression = xpath.compile(exampleXPath);
        } catch (XPathExpressionException e1) {
            throw new UserError(null, 214, new Object[] { configuration.getExampleXPath() });
        }

        int i = 0;
        List<String> attributeXPathsList = configuration.getAttributeXPaths();
        this.attributeExpressions = new XPathExpression[attributeXPathsList.size()];
        this.attributeNames = new String[attributeXPathsList.size()];
        for (String expressionString : attributeXPathsList) {
            this.attributeNames[i] = expressionString;
            try {
                this.attributeExpressions[i] = xpath.compile(expressionString);
            } catch (XPathExpressionException e) {
                throw new UserError(null, 214, new Object[] { expressionString });
            }
            i++;
        }
        this.attributeValueTypes = new int[attributeXPathsList.size()];
        Arrays.fill(this.attributeValueTypes, 1);
        this.currentExampleValues = new String[attributeXPathsList.size()];

        try {
            this.exampleNodes = (NodeList)exampleExpression.evaluate(configuration.getDocumentObjectModel(), XPathConstants.NODESET);

            this.exampleNodesLength = this.exampleNodes.getLength();
        } catch (UserError e) {
            e.setOperator(callingOperator);
            throw e;
        } catch (XPathExpressionException e) {
            throw new UserError(callingOperator, 214, new Object[] { configuration.getExampleXPath() });
        }
        //关闭
        try{
            configuration.getInputStream().close();
        }catch (Exception e){

        }
    }

    @Override
    public boolean hasNext() { return (this.exampleNodesLength > this.currentExampleIndex + 1); }

    @Override
    public void next(ProgressListener listener) throws OperatorException {
        this.currentExampleIndex++;
        if (this.currentExampleIndex >= this.exampleNodesLength) {
            throw new NoSuchElementException("No further match to examples XPath expression in XML file. Accessed " + this.currentExampleIndex + " but has has " + this.exampleNodesLength);
        }

        for (int i = 0; i < this.attributeExpressions.length; i++) {
            try {
                Node item = this.exampleNodes.item(this.currentExampleIndex);
                if (item.getParentNode() != null)
                {
                    item.getParentNode().removeChild(item);
                }
                if (this.operatorVersion.compareTo(XMLExampleSource.CHANGE_5_1_013_NODE_OUTPUT) > 0) {
                    NodeList nodeList = (NodeList)this.attributeExpressions[i].evaluate(item, XPathConstants.NODESET);
                    this.currentExampleValues[i] = XMLDomHelper.nodeListToString(nodeList);
                } else {
                    this.currentExampleValues[i] = (String)this.attributeExpressions[i].evaluate(item, XPathConstants.STRING);
                }
            } catch (XPathExpressionException e) {
                this.currentExampleValues[i] = null;
            } catch (TransformerException e) {
                this.currentExampleValues[i] = null;
            }
        }
    }

    @Override
    public int getNumberOfColumns() { return this.attributeNames.length; }

    @Override
    public String[] getColumnNames() { return this.attributeNames; }

    @Override
    public boolean isMissing(int columnIndex) { return (this.currentExampleValues[columnIndex] == null); }

    @Override
    public Number getNumber(int columnIndex) throws ParseException { throw new ParseException(new ParsingError(this.currentExampleIndex, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, "")); }

    @Override
    public String getString(int columnIndex) throws ParseException { return this.currentExampleValues[columnIndex]; }

    @Override
    public Date getDate(int columnIndex) throws ParseException { throw new ParseException(new ParsingError(this.currentExampleIndex, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, "")); }

    @Override
    public DataResultSet.ValueType getNativeValueType(int columnIndex) throws ParseException { return DataResultSet.ValueType.STRING; }

    @Override
    public void close() throws OperatorException {}

    @Override
    public void reset(ProgressListener listener) throws OperatorException { this.currentExampleIndex = -1; }

    @Override
    public int[] getValueTypes() { return this.attributeValueTypes; }

    @Override
    public int getCurrentRow() { return this.currentExampleIndex; }
}

