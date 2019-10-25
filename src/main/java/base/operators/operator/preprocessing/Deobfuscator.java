package base.operators.operator.preprocessing;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.OperatorResourceConsumptionHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Deobfuscator extends AbstractDataProcessing {
    public static final String PARAMETER_OBFUSCATION_MAP_FILE = "obfuscation_map_file";
    public Deobfuscator(OperatorDescription description) { super(description); }

    @Override
    protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
        metaData.clear();
        metaData.attributesAreSuperset();
        return metaData;
    }

    @Override
    public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
        File file = getParameterAsFile("obfuscation_map_file");
        Map<String, String> obfuscatorMap = null;
        try {
            obfuscatorMap = readObfuscatorMap(file);
        } catch (IOException e) {
            throw new UserError(this, '?', new Object[] { getParameterAsString("obfuscation_map_file"), e.getMessage() });
        }

        Iterator<Attribute> i = exampleSet.getAttributes().allAttributes();
        while (i.hasNext()) {
            deObfuscateAttribute((Attribute)i.next(), obfuscatorMap);
        }
        return exampleSet;
    }

    private void deObfuscateAttribute(Attribute attribute, Map<String, String> obfuscatorMap) {
        String obfuscatedName = attribute.getName();
        String newName = (String)obfuscatorMap.get(obfuscatedName);
        if (newName != null) {
            attribute.setName(newName);
            attribute.setConstruction(newName);
        } else {
            logWarning("No name found in obfuscating map for attribute '" + obfuscatedName + "'.");
        }

        if (attribute.isNominal()) {
            Iterator<String> v = attribute.getMapping().getValues().iterator();
            while (v.hasNext()) {
                String obfuscatedValue = (String)v.next();
                String newValue = (String)obfuscatorMap.get(newName + ":" + obfuscatedValue);
                if (newValue != null) {
                    Tools.replaceValue(attribute, obfuscatedValue, newValue); continue;
                }
                logWarning("No value found in obfuscating map for value '" + obfuscatedValue + "' of attribute '" + attribute.getName() + "'.");
            }
        }
    }

    private Map<String, String> readObfuscatorMap(File file) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s");
                map.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return map;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeFile parameterTypeFile = new ParameterTypeFile("obfuscation_map_file", "File where the obfuscator map was written to.", "obf", false);
        parameterTypeFile.setExpert(false);
        parameterTypeFile.setPrimary(true);
        types.add(parameterTypeFile);
        return types;
    }

    @Override
    public boolean writesIntoExistingData() {
        return false;
    }

    @Override
    public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
        return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Deobfuscator.class, null);
    }
}
