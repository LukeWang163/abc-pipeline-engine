package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRowFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.io.BytewiseExampleSource;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DasyLabDataReader extends BytewiseExampleSource {
    public static final String PARAMETER_TIMESTAMP = "timestamp";
    public static final String[] PARAMETER_TIMESTAMP_OPTIONS = new String[]{"none", "relative", "absolute"};
    public static final int TIMESTAMP_NONE = 0;
    public static final int TIMESTAMP_RELATIVE = 1;
    public static final int TIMESTAMP_ABSOLUTE = 2;
    private static final String NOT_YET_IMPLEMENTED_ERROR_MESSAGE = "feature not yet implemented, ";
    private static final String FILE_HEADER_STRING = "DTDF";
    private static final byte STRING_TERMINATOR_BYTE = 0;
    private static final int FILE_TYPE_UNIVERSAL_FORMAT_1 = 1;

    public DasyLabDataReader(OperatorDescription description) {
        super(description);
    }

    @Override
    protected String getFileSuffix() {
        return "ddf";
    }

    @Override
    protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
        int timestampMode = this.getParameterAsInt("timestamp");
        byte[] buffer = new byte[500];
        int readBytes = -1;
        this.read(inputStream, buffer, 5);
        if (!this.extractString(buffer, 0, 4).equals("DTDF")) {
            throw new IOException("Wrong file format");
        } else {
            StringBuffer stringBuffer = new StringBuffer();

            while(true) {
                byte readByte = (byte)(255 & inputStream.read());
                if (readByte == -1) {
                    throw new IOException("Wrong file format");
                }

                if (readByte == 0) {
                    this.read(inputStream, buffer, 3);
                    if (!this.extractString(buffer, 1, 2).equals("IN")) {
                        throw new IOException("Wrong file format");
                    }

                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    int fileType = this.extract2ByteInt(buffer, 0, true);
                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    boolean separateFile = this.extract2ByteInt(buffer, 0, true) == 1;
                    if (separateFile) {
                        throw new IOException("feature not yet implemented, separate files not allowed");
                    }

                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 8);
                    this.read(inputStream, buffer, 4);
                    if (fileType != 1) {
                        throw new IOException("feature not yet implemented, file types other than universal format 1 not supported");
                    }

                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 2);
                    int numberOfChannels = this.extract2ByteInt(buffer, 0, true);
                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 32);
                    DasyLabDataReader.Channel[] channels = new DasyLabDataReader.Channel[numberOfChannels];

                    for(int i = 0; i < numberOfChannels; ++i) {
                        DasyLabDataReader.Channel channel = new DasyLabDataReader.Channel();
                        this.read(inputStream, buffer, 2);
                        this.read(inputStream, buffer, 2);
                        this.read(inputStream, buffer, 2);
                        this.read(inputStream, buffer, 8);
                        this.read(inputStream, buffer, 2);
                        channel.type = this.extract2ByteInt(buffer, 0, true);
                        if (channel.type == 20 || channel.type == 21) {
                            throw new IOException("feature not yet implemented, histogram data not supported");
                        }

                        this.read(inputStream, buffer, 2);
                        this.read(inputStream, buffer, 16);
                        this.read(inputStream, buffer, '\u0000');
                        readBytes = this.read(inputStream, buffer, '\u0000');
                        if (readBytes != -1) {
                            channel.name = this.extractString(buffer, 0, readBytes);
                        }

                        channels[i] = channel;
                    }

                    this.read(inputStream, buffer, 4);
                    if (!this.extractString(buffer, 0, 4).equals("DATA")) {
                        throw new IOException("Wrong file format");
                    }

                    ArrayList<Attribute> attributes = new ArrayList(numberOfChannels);
                    switch(timestampMode) {
                        case 0:
                        default:
                            break;
                        case 1:
                            attributes.add(AttributeFactory.createAttribute("timestamp", 4));
                            break;
                        case 2:
                            attributes.add(AttributeFactory.createAttribute("timestamp", 9));
                    }

                    for(int i = 0; i < numberOfChannels; ++i) {
                        Attribute attribute = AttributeFactory.createAttribute(channels[i].name, 4);
                        attributes.add(attribute);
                    }

                    this.read(inputStream, buffer, 2);
                    this.read(inputStream, buffer, 4);
                    double startTime = (double)((long)this.extractInt(buffer, 0, true) * 1000L);
                    this.read(inputStream, buffer, 8);
                    ExampleSetBuilder builder = ExampleSets.from(attributes);
                    builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
                    HashMap<Double, Double[]> valuesMap = new HashMap();
                    HashMap<Double, Integer> counterMap = new HashMap();
                    boolean eof = false;

                    while(!eof) {
                        readBytes = inputStream.read(buffer, 0, 20);
                        if (readBytes != 20) {
                            eof = true;
                            break;
                        }

                        int channelNr = this.extract2ByteInt(buffer, 0, true);
                        double time = this.extractDouble(buffer, 2, true);
                        double delay = this.extractDouble(buffer, 10, true);
                        int blockSize = this.extract2ByteInt(buffer, 18, true);

                        for(int i = 0; i < blockSize; ++i) {
                            readBytes = inputStream.read(buffer, 20, 4);
                            if (readBytes != 4) {
                                eof = true;
                                break;
                            }

                            double value = (double)this.extractFloat(buffer, 20, true);
                            Double[] values = null;
                            if (valuesMap.containsKey(time)) {
                                Integer counter = (Integer)counterMap.get(time) + 1;
                                counterMap.put(time, counter);
                                values = (Double[])valuesMap.get(time);
                            } else {
                                counterMap.put(time, 1);
                                values = new Double[timestampMode == 0 ? numberOfChannels : numberOfChannels + 1];

                                for(int j = 1; j < values.length; ++j) {
                                    values[j] = 0.0D / 0.0;
                                }

                                valuesMap.put(time, values);
                            }

                            if (values != null) {
                                switch(timestampMode) {
                                    case 0:
                                        values[channelNr] = value;
                                        break;
                                    case 1:
                                        values[0] = (double)((long)(time * 1000.0D));
                                        values[channelNr + 1] = value;
                                        break;
                                    case 2:
                                        values[0] = (double)((long)startTime + (long)(time * 1000.0D));
                                        values[channelNr + 1] = value;
                                }
                            }

                            if ((Integer)counterMap.get(time) == numberOfChannels) {
                                builder.addDataRow(dataRowFactory.create((Double[])valuesMap.get(time), (Attribute[])attributes.toArray(new Attribute[attributes.size()])));
                                counterMap.remove(time);
                                valuesMap.remove(time);
                            }

                            time += delay;
                        }
                    }

                    inputStream.close();
                    ExampleSet exampleSet = builder.build();
                    if (timestampMode != 0) {
                        exampleSet.getAttributes().setId((Attribute)attributes.get(0));
                    }

                    return exampleSet;
                }

                stringBuffer.append((char)readByte);
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeCategory("timestamp", "Specifies whether to include an absolute timestamp, a timestamp relative to the beginning of the file (in seconds) or no timestamp at all.", PARAMETER_TIMESTAMP_OPTIONS, 2));
        return types;
    }

    private static class Channel {
        private static final int HISTOGRAM = 20;
        private static final int HISTOGRAM_WITH_TIME_INFORMATION = 21;
        private int type;
        private String name;

        private Channel() {
        }
    }
}
