package base.operators.operator.etl.trans;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class AddCheckSumOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public AddCheckSumOperator(OperatorDescription description){
        super(description);
        String[] attributes = new String[0];
        try {
            attributes = getParameterAsString(ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        for (int i = 0; i < attributes.length; i++) {
            exampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, attributes[i])));
        }


    }

    public static final String ATTRIBUTES_NAME = "attributes_name";
    public static final String CHECKSUM_TYPE = "checksum_type";
    public static final String RESULT_TYPE = "result_type";
    public static final String RESULT_ATTRIBUTE_NAME = "result_attribute_name";
    public static final String IS_COMPATIBILITY_MODE = "is_compatibility_mode";

    public static final String TYPE_CRC32 = "CRC32";
    public static final String TYPE_ADLER32 = "ADLER32";
    public static final String TYPE_MD5 = "MD5";
    public static final String TYPE_SHA1 = "SHA-1";
    public static String[] checksumtypeCodes = { TYPE_CRC32, TYPE_ADLER32, TYPE_MD5, TYPE_SHA1 };
    public static final String[] resultTypeCodes = { "string", "hexadecimal"};

    private String checksumtype;
    private int resultType;
    private String resultName;
    private MessageDigest digest;
    private boolean isCompatibilityMode;


    @Override
    public void doWork() throws OperatorException {
        String[] attributesName = getParameterAsString(ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        checksumtype = getParameterAsString(CHECKSUM_TYPE);
        resultType = getParameterAsInt(RESULT_TYPE);
        resultName = getParameterAsString(RESULT_ATTRIBUTE_NAME);
        isCompatibilityMode = getParameterAsBoolean(IS_COMPATIBILITY_MODE);

        ExampleSet input = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = input.getAttributes();
        ExampleTable exampleTable = input.getExampleTable();
        Attribute result_attribute = null;

        if(checksumtype.equals(TYPE_ADLER32)|| checksumtype.equals(TYPE_CRC32)){
            result_attribute = AttributeFactory.createAttribute(resultName, Ontology.NUMERICAL);
        }else{
            result_attribute = AttributeFactory.createAttribute(resultName, Ontology.STRING);
        }
        exampleTable.addAttribute(result_attribute);
        attributes.addRegular(result_attribute);

        if(checksumtype.equals(TYPE_MD5)|| checksumtype.equals(TYPE_SHA1)){
            try {
                digest = MessageDigest.getInstance(checksumtype);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < input.size(); i++) {
            Example example = input.getExample(i);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < attributesName.length; j++) {
                sb.append(example.getValueAsString(attributes.get(attributesName[j])));
            }
            String valueString = sb.toString();
            Long checksumLong = new Long(0);
            String checksumString = "";
            if(checksumtype.equals(TYPE_ADLER32)|| checksumtype.equals(TYPE_CRC32)){
                try {
                    checksumLong = calculCheckSum( valueString );
                    example.setValue(result_attribute, checksumLong.longValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                byte[] hashArray  = null;
                try {
                    hashArray = createCheckSum(valueString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(resultType==1){
                    checksumString =
                            isCompatibilityMode ? byteToHexEncode_compatible( hashArray ) : new String( Hex.encodeHex( hashArray ) );
                }else{
                    checksumString = getStringFromBytes( hashArray ) ;
                }
                example.setValue(result_attribute, result_attribute.getMapping().mapString(checksumString));

            }
        }
        exampleSetOutput.deliver(input);
    }

    private Long calculCheckSum( String str) throws Exception {
        Long retval;
        if ( checksumtype.equals( TYPE_CRC32 ) ) {
            CRC32 crc32 = new CRC32();
            crc32.update( str.getBytes() );
            retval = new Long( crc32.getValue() );
        } else {
            Adler32 adler32 = new Adler32();
            adler32.update( str.getBytes() );
            retval = new Long( adler32.getValue() );
        }

        return retval;
    }

    private byte[] createCheckSum( String str ) throws Exception {
        // Updates the digest using the specified array of bytes
        digest.update( str.getBytes() );
        // Completes the hash computation by performing final operations such as padding
        byte[] hash = digest.digest();
        // After digest has been called, the MessageDigest object is reset to its initialized state
        return hash;
    }

    private String byteToHexEncode_compatible( byte[] in ) {
        if ( in == null ) {
            return null;
        }
        final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        String hex = new String( in );

        char[] s = hex.toCharArray();
        StringBuffer hexString = new StringBuffer( 2 * s.length );

        for ( int i = 0; i < s.length; i++ ) {
            hexString.append( hexDigits[( s[i] & 0x00F0 ) >> 4] ); // hi nibble
            hexString.append( hexDigits[s[i] & 0x000F] ); // lo nibble
        }

        return hexString.toString();
    }
    private static String getStringFromBytes( byte[] bytes ) {
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < bytes.length; i++ ) {
            byte b = bytes[i];
            sb.append( 0x00FF & b );
            if ( i + 1 < bytes.length ) {
                sb.append( "-" );
            }
        }
        return sb.toString();
    }



    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(ATTRIBUTES_NAME, "Select the attributes to be used for adding check.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(CHECKSUM_TYPE, "Select the type of checksum calculation.", checksumtypeCodes, 0, false));
        ParameterType type = new ParameterTypeCategory(RESULT_TYPE, "Select the type of output.", resultTypeCodes, 0, false);
        type.registerDependencyCondition(new EqualStringCondition(this, CHECKSUM_TYPE, false,checksumtypeCodes[2],checksumtypeCodes[3]));
        types.add(type);

        types.add(new ParameterTypeString(RESULT_ATTRIBUTE_NAME, "Select the attribute name of the result.","result", false));

        type = new ParameterTypeBoolean(IS_COMPATIBILITY_MODE, "Compatibility of converting byte arrays into hexadecimal strings in MD5 and SHA1 methods", false, false);
        type.registerDependencyCondition(new EqualStringCondition(this, CHECKSUM_TYPE, false,checksumtypeCodes[2],checksumtypeCodes[3]));
        types.add(type);

        return types;
    }
}
