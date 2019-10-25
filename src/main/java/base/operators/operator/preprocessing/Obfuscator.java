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
import base.operators.tools.RandomGenerator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Obfuscator extends AbstractDataProcessing {
    public static final String PARAMETER_OBFUSCATION_MAP_FILE = "obfuscation_map_file";
    public Obfuscator(OperatorDescription description) { super(description); }

    @Override
    protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
        metaData.clear();
        metaData.attributesAreSuperset();
        return metaData;
    }

    @Override
    public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
        Map<String, String> obfuscatorMap = new HashMap<String, String>();
        RandomGenerator random = RandomGenerator.getRandomGenerator(this);
        Iterator<Attribute> i = exampleSet.getAttributes().allAttributes();
        while (i.hasNext()) {
            obfuscateAttribute((Attribute)i.next(), obfuscatorMap, random);
        }
        File file = getParameterAsFile("obfuscation_map_file", true);
        if (file != null) {
            try {
                writeObfuscatorMap(obfuscatorMap, file);
            } catch (IOException e) {
                throw new UserError(this, '?', new Object[] { getParameterAsString("obfuscation_map_file"), e.getMessage() });
            }
        }
        return exampleSet;
    }

    private void obfuscateAttribute(Attribute attribute, Map<String, String> obfuscatorMap, RandomGenerator random) {
        String oldName = attribute.getName();
        String newName = random.nextString(8);
        attribute.setName(newName);
        attribute.setConstruction(newName);
        obfuscatorMap.put(newName, oldName);

        if (attribute.isNominal()) {
            Iterator<String> v = attribute.getMapping().getValues().iterator();
            while (v.hasNext()) {
                String oldValue = (String)v.next();
                String newValue = random.nextString(8);
                Tools.replaceValue(attribute, oldValue, newValue);
                obfuscatorMap.put(oldName + ":" + newValue, oldValue);
            }
        }
    }

    private void writeObfuscatorMap(Map<String, String> obfuscatorMap, File file) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(file));
            Iterator<Map.Entry<String, String>> i = obfuscatorMap.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, String> e = (Map.Entry)i.next();
                out.println((String)e.getKey() + "\t" + (String)e.getValue());
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeFile parameterTypeFile = new ParameterTypeFile("obfuscation_map_file", "File where the obfuscator map should be written to.", "obf", true);
        parameterTypeFile.setPrimary(true);
        types.add(parameterTypeFile);
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

    @Override
    public boolean writesIntoExistingData() { return false; }

    @Override
    public ResourceConsumptionEstimator getResourceConsumptionEstimator() { return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Obfuscator.class, null); }
}

