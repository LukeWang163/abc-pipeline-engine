package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractExampleSource;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.ParameterService;
import base.operators.tools.Tools;
import base.operators.tools.io.Encoding;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class C45ExampleSource extends AbstractExampleSource {
    public static final String PARAMETER_C45_FILESTEM = "c45_filestem";
    public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
    public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

    public C45ExampleSource(OperatorDescription description) {
        super(description);
    }

    @Override
    public ExampleSet createExampleSet() throws OperatorException {
        File file = this.getParameterAsFile("c45_filestem");
        Attribute label = AttributeFactory.createAttribute("label", 1);
        List<Attribute> attributes = new LinkedList();
        File nameFile = this.getFile(file, "names");
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader in = null;
        Pattern separatorPattern = Pattern.compile(",");

        int colonIndex;
        int lineCounter;
        String[] tokens;
        Attribute attribute;
        try {
            fis = new FileInputStream(nameFile);
            isr = new InputStreamReader(fis, Encoding.getEncoding(this));
            in = new BufferedReader(isr);
            String line = null;

            while((line = in.readLine()) != null) {
                line = line.trim();
                int commentIndex = line.indexOf(124);
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                if (line.length() > 0 && line.charAt(line.length() - 1) == '.') {
                    line = line.substring(0, line.length() - 1).trim();
                }

                if (line.length() != 0) {
                    colonIndex = line.indexOf(58);
                    if (colonIndex >= 0) {
                        String attributeName = line.substring(0, colonIndex).trim();
                        String typeString = line.substring(colonIndex + 1).trim();
                        int valueType = 1;
                        if (typeString.equals("continuous")) {
                            valueType = 4;
                        }

                        attribute = AttributeFactory.createAttribute(attributeName, valueType);
                        if (valueType == 1 && !typeString.equals("discrete")) {
                            tokens = Tools.quotedSplit(typeString, separatorPattern);
                            String[] var18 = tokens;
                            int var19 = tokens.length;

                            for(int var20 = 0; var20 < var19; ++var20) {
                                String s = var18[var20];
                                attribute.getMapping().mapString(s.trim());
                            }
                        }

                        attributes.add(attribute);
                    } else {
                        String[] possibleClasses = line.split(",");
                        possibleClasses = Tools.quotedSplit(line, separatorPattern);
                        String[] var13 = possibleClasses;
                        lineCounter = possibleClasses.length;

                        for(int var15 = 0; var15 < lineCounter; ++var15) {
                            String s = var13[var15];
                            label.getMapping().mapString(s.trim());
                        }
                    }
                }
            }
        } catch (IOException var54) {
            throw new UserError(this, 302, new Object[]{nameFile, var54.getMessage()});
        } finally {
            try {
                if (in != null) {
                    in.close();
                } else if (isr != null) {
                    isr.close();
                } else if (fis != null) {
                    fis.close();
                }
            } catch (Exception var49) {
                this.logError("Cannot close stream to file " + file);
            }

        }

        attributes.add(label);
        File dataFile = this.getFile(file, "data");

        try {
            fis = new FileInputStream(dataFile);
            isr = new InputStreamReader(fis, Encoding.getEncoding(this));
            in = new BufferedReader(isr);
        } catch (IOException var50) {
            throw new UserError(this, 301, new Object[]{dataFile});
        } catch (RuntimeException var51) {
            try {
                fis.close();
            } catch (IOException var47) {
                var51.addSuppressed(var47);
            }

            throw var51;
        }

        ExampleSet var66;
        try {
            ExampleSetBuilder builder = ExampleSets.from(attributes);
            colonIndex = this.getParameterAsInt("datamanagement");
            if (!Boolean.parseBoolean(ParameterService.getParameterValue("rapidminer.system.legacy_data_mgmt"))) {
                colonIndex = 0;
                builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
            }

            DataRowFactory factory = new DataRowFactory(colonIndex, this.getParameterAsString("decimal_point_character").charAt(0));
            Attribute[] attributeArray = new Attribute[attributes.size()];
            attributes.toArray(attributeArray);
            lineCounter = 0;
            attribute = null;

            String line;
            while((line = in.readLine()) != null) {
                ++lineCounter;
                line = line.trim();
                int commentIndex = line.indexOf(124);
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                if (line.length() > 0 && line.charAt(line.length() - 1) == '.') {
                    line = line.substring(0, line.length() - 1).trim();
                }

                if (line.length() != 0) {
                    tokens = Tools.quotedSplit(line, separatorPattern);
                    if (tokens.length != attributes.size()) {
                        in.close();
                        throw new UserError(this, 302, new Object[]{file, "Line " + lineCounter + ": the number of tokens in each line must be the same as the number of attributes (" + attributes.size() + "), was: " + tokens.length});
                    }

                    DataRow row = factory.create(tokens, attributeArray);
                    builder.addDataRow(row);
                }
            }

            var66 = builder.withRole(label, "label").build();
        } catch (IOException var52) {
            throw new UserError(this, 302, new Object[]{nameFile, var52.getMessage()});
        } finally {
            try {
                if (in != null) {
                    in.close();
                } else if (isr != null) {
                    isr.close();
                } else if (fis != null) {
                    fis.close();
                }
            } catch (Exception var48) {
                this.logError("Cannot close stream to file " + dataFile);
            }

        }

        return var66;
    }

    private File getFile(File file, String extension) {
        String name = file.getName();
        String fileStem = null;
        if (name.indexOf(46) < 0) {
            fileStem = name;
        } else {
            fileStem = name.substring(0, name.lastIndexOf(46));
        }

        return new File(file.getParent() + File.separator + fileStem + "." + extension);
    }

    @Override
    protected boolean supportsEncoding() {
        return true;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList();
        ParameterTypeFile type = new ParameterTypeFile("c45_filestem", "The path to either the C4.5 names file, the data file, or the filestem (without extensions). Both files must be in the same directory.", (String)null, false);
        type.setPrimary(true);
        types.add(type);
        DataManagementParameterHelper.addParameterTypes(types, this);
        types.add(new ParameterTypeString("decimal_point_character", "Character that is used as decimal point.", ".", false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}

