package base.operators.operator.nio.xml;

import base.operators.RapidMiner;
import base.operators.gui.tools.VersionNumber;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.UserError;
import base.operators.operator.nio.model.*;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeList;
import base.operators.tools.I18N;
import base.operators.tools.LogService;
import base.operators.tools.ProgressListener;
import base.operators.utils.HDFSUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class XMLResultSetConfiguration implements DataResultSetFactory{
    private String fileName;
    private InputStream fileInputStream;
    private Map<String, String> namespaceMap = new HashMap(); private String exampleXPath; private List<String> attributeXPaths; private boolean isNamespaceAware; public XMLResultSetConfiguration() {
    VersionNumber rmVersion = RapidMiner.getVersion();
    this.xmlExampleSourceCompatibilityVersion = new OperatorVersion(rmVersion.getMajorNumber(), rmVersion.getMinorNumber(), rmVersion.getPatchLevel());}
    private String defaultNamespaceURI; private Document prefetchedDocument; private OperatorVersion xmlExampleSourceCompatibilityVersion;

    public void setDefaultNamespaceURI(String defaultNamespaceURI) { this.defaultNamespaceURI = defaultNamespaceURI; }

    public XMLResultSetConfiguration(XMLExampleSource operator) throws OperatorException {
        this();
        VersionNumber rmVersion = RapidMiner.getVersion();
        if (operator != null) {
            XMLExampleSource xmlExampleSource = operator;
            this.xmlExampleSourceCompatibilityVersion = xmlExampleSource.getCompatibilityLevel();
        } else {
            this.xmlExampleSourceCompatibilityVersion = new OperatorVersion(rmVersion.getMajorNumber(), rmVersion.getMinorNumber(), rmVersion.getPatchLevel());
        }

        if (operator.isFileSpecified()) {
            //this.fileName = operator.getSelectedFile().getAbsolutePath();
            this.fileName = operator.getParameter(operator.getFileParameterName());


            if ("local".equals(operator.getParameter(operator.PARAMETER_STORAGE_TYPE).toLowerCase())) {
                try {
                    this.fileInputStream = new FileInputStream(new File(this.fileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if ("hdfs".equals(operator.getParameter(operator.PARAMETER_STORAGE_TYPE).toLowerCase())) {
                FileSystem fs = HDFSUtil.getFileSystem();
                try {
                    this.fileInputStream = fs.open(new Path(this.fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        if (operator.isParameterSet("xpath_for_examples")) {
            this.exampleXPath = operator.getParameterAsString("xpath_for_examples");
        }

        if (operator.getParameterAsBoolean("use_default_namespace") && operator
                .isParameterSet("default_namespace")) {
            this.defaultNamespaceURI = operator.getParameterAsString("default_namespace");
        } else {
            this.defaultNamespaceURI = null;
        }

        this.isNamespaceAware = operator.getParameterAsBoolean("use_namespaces");
        if (this.isNamespaceAware && operator.isParameterSet("namespaces")) {
            for (String[] pair : operator.getParameterList("namespaces")) {
                this.namespaceMap.put(pair[0], pair[1]);
            }
        }

        this.attributeXPaths = new ArrayList();
        if (operator.isParameterSet("xpaths_for_attributes"))
        {
            for (String attributeXPath : ParameterTypeEnumeration.transformString2Enumeration(operator.getParameterAsString("xpaths_for_attributes"))) {
                this.attributeXPaths.add(attributeXPath);
            }
        }
    }

    @Override
    public DataResultSet makeDataResultSet(Operator operator) throws OperatorException { return new XMLResultSet(operator, this, this.xmlExampleSourceCompatibilityVersion); }

    @Override
    public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException { return new DefaultPreview(makeDataResultSet(null), listener); }

    @Override
    public String getResourceName() { return this.fileName; }

    public String getResourceIdentifier() { return this.fileName; }

    public InputStream getInputStream(){
        return this.fileInputStream;
    }

    @Override
    public ExampleSetMetaData makeMetaData() {
        ExampleSetMetaData emd = new ExampleSetMetaData();
        emd.numberOfExamplesIsUnkown();
        return emd;
    }

    @Override
    public void setParameters(AbstractDataResultSetReader operator) {
        operator.setParameter("file", this.fileName);
        operator.setParameter("xpath_for_examples", this.exampleXPath);
        operator.setParameter("use_namespaces", Boolean.toString(this.isNamespaceAware));
        operator.setParameter("use_default_namespace", Boolean.toString((getDefaultNamespaceURI() != null)));
        if (getDefaultNamespaceURI() != null) {
            operator.setParameter("default_namespace", getDefaultNamespaceURI());
        }

        List<String[]> list = new LinkedList<String[]>();
        for (Map.Entry<String, String> entry : this.namespaceMap.entrySet()) {
            list.add(new String[] { (String)entry.getKey(), (String)entry.getValue() });
        }
        operator.setParameter("namespaces", ParameterTypeList.transformList2String(list));
        operator.setParameter("xpaths_for_attributes",
                ParameterTypeEnumeration.transformEnumeration2String(this.attributeXPaths));
    }

    @Override
    public void close() {}

    public boolean isNamespaceAware() { return this.isNamespaceAware; }

    public String getExampleXPath() { return this.exampleXPath; }

    public void setExampleXPath(String exampleXPath) { this.exampleXPath = exampleXPath; }

    public List<String> getAttributeXPaths() { return this.attributeXPaths; }

    public void setAttributeXPaths(List<String> attributeXPaths) { this.attributeXPaths = attributeXPaths; }

    public Map<String, String> getNamespacesMap() { return this.namespaceMap; }

    public String getNamespaceId(String namespaceURI) {
        for (Map.Entry<String, String> entry : this.namespaceMap.entrySet()) {
            if (((String)entry.getValue()).equals(namespaceURI)) {
                return (String)entry.getKey();
            }
        }
        return null;
    }

    public void setResourceIdentifier(String resourceIdentifier) {
        this.fileName = resourceIdentifier;
        this.prefetchedDocument = null;
    }

    public Document getDocumentObjectModel() throws OperatorException {
        if (this.prefetchedDocument == null) {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(false);
            domFactory.setNamespaceAware(isNamespaceAware());
            try {
                domFactory.setFeature("http://xml.org/sax/features/namespaces", isNamespaceAware());
                domFactory.setFeature("http://xml.org/sax/features/validation", false);
                domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                DocumentBuilder builder = domFactory.newDocumentBuilder();
                String resourceIdentifier = getResourceIdentifier();
                if (resourceIdentifier == null) {
//                    throw new UserError(null, "file_consumer.no_file_defined");
                }
                //this.prefetchedDocument = builder.parse(new File(resourceIdentifier));
                if (this.fileInputStream == null) {
                    throw new UserError(null, "file_consumer.no_file_defined");
                }
                this.prefetchedDocument = builder.parse(this.fileInputStream);
                return this.prefetchedDocument;
            } catch (ParserConfigurationException e) {
                LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "base.operators.operator.nio.xml.XMLResultSetConfiguration.configuring_xml_parser_error", new Object[] { e }), e);
                throw new OperatorException("Failed to configure XML parser: " + e, e);
            } catch (SAXException e) {
                LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "base.operators.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", new Object[] { e }), e);

                throw new UserError(null, 401, new Object[] { e.getMessage() });
            } catch (CharConversionException e) {
                LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "base.operators.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", new Object[] { e }), e);
                throw new UserError(null, 401, new Object[] { e.getMessage() });
            } catch (IOException e) {
                LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "base.operators.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", new Object[] { e }), e);
                throw new UserError(null, 302, new Object[] { getResourceIdentifier(), e.getMessage() });
            }
        }
        return this.prefetchedDocument;
    }

    public String getDefaultNamespaceURI() { return this.defaultNamespaceURI; }

    public void setNamespacesMap(Map<String, String> idNamespaceMap) { this.namespaceMap = idNamespaceMap; }

    public void setNamespaceAware(boolean b) { this.isNamespaceAware = b; }

    public OperatorVersion getXmlExampleSourceCompatibilityVersion() { return this.xmlExampleSourceCompatibilityVersion; }
}
