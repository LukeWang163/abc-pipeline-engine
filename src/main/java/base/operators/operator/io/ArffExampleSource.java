package base.operators.operator.io;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nio.file.FileInputPortHandler;
import base.operators.operator.ports.InputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDateFormat;
import base.operators.tools.I18N;
import base.operators.tools.LogService;
import base.operators.tools.StrictDecimalFormat;
import base.operators.tools.Tools;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class ArffExampleSource extends AbstractDataReader {
    public static final String PARAMETER_DATA_FILE = "data_file";
    private final InputPort fileInputPort = (InputPort)getInputPorts().createPort("file");
    private final FileInputPortHandler filePortHandler = new FileInputPortHandler(this, this.fileInputPort, "data_file");

    static  {
        AbstractReader.registerReaderDescription(new AbstractReader.ReaderDescription("arff", ArffExampleSource.class, "data_file"));
    }

    public ArffExampleSource(OperatorDescription description) {
        super(description);
        this.skipGuessingValueTypes = true;
    }

    @Override
    protected DataSet getDataSet() throws OperatorException, IOException {
        return new AbstractDataReader.DataSet() {
            private InputStream inputStream = null;
            private BufferedReader in = null;
            private StreamTokenizer tokenizer = null;
            private final NumberFormat numberFormat = StrictDecimalFormat.getInstance(ArffExampleSource.this);
            private final DateFormat dateFormat = new SimpleDateFormat();
            private String[] tokens = null;
            private final HashMap<Integer, String> datePattern = new HashMap();

            @Override
            public boolean next() {
                try {
                    Tools.getFirstToken(this.tokenizer);
                } catch (IOException e) {
                    return false;
                }
                if (this.tokenizer.ttype == -1) {
                    return false;
                }
                try {
                    this.tokens = new String[ArffExampleSource.this.getColumnCount()];
                    if (this.tokenizer.ttype == 123) {
                        for (int t = 0; t < this.tokens.length; t++) {
                            this.tokens[t] = "0";
                        }
                        while (true) {
                            if (this.tokenizer.nextToken() == 10) {
                                throw new IOException("unexpedted end of line " + this.tokenizer.lineno());
                            }
                            if (this.tokenizer.ttype == -1) {
                                throw new IOException("unexpedted end of file in line " + this.tokenizer.lineno());
                            }
                            if (this.tokenizer.ttype == 125) {
                                break;
                            }
                            int index = Integer.valueOf(this.tokenizer.sval).intValue();
                            Tools.getNextToken(this.tokenizer);
                            if (this.tokenizer.ttype == 63) {
                                this.tokens[index] = null; continue;
                            }
                            if (this.tokenizer.ttype != -3) {
                                throw new IOException("not a valid value '" + this.tokenizer.sval + "' in line " + this.tokenizer
                                        .lineno());
                            }
                            this.tokens[index] = this.tokenizer.sval;
                        }

                        Tools.getLastToken(this.tokenizer, true);
                    } else {
                        for (int i = 0; i < ArffExampleSource.this.getColumnCount(); i++) {
                            if (i > 0) {
                                try {
                                    Tools.getNextToken(this.tokenizer);
                                } catch (IOException e) {
                                    try {
                                        LogService.getRoot().log(Level.WARNING, I18N.getErrorMessage("arff_example_source.unexpected_end_of_file", new Object[] {ArffExampleSource.this.filePortHandler.getSelectedFile().getName() }));
                                        throw e;
                                    } catch (OperatorException operatorException) {}
                                }
                            }

                            if (this.tokenizer.ttype == 63) {
                                this.tokens[i] = null;
                            } else {
                                if (this.tokenizer.ttype != -3) {
                                    throw new IOException("not a valid value '" + this.tokenizer.sval + "' in line " + this.tokenizer
                                            .lineno());
                                }
                                this.tokens[i] = this.tokenizer.sval;
                            }
                        }
                        Tools.getLastToken(this.tokenizer, true);
                    }
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public void close() {
                try {
                    this.in.close();
                } catch (IOException iOException) {}
            }

            @Override
            public int getNumberOfColumnsInCurrentRow() { return this.tokens.length; }

            @Override
            public boolean isMissing(int columnIndex) { return (this.tokens[columnIndex] == null || this.tokens[columnIndex].isEmpty()); }

            @Override
            public Number getNumber(int columnIndex) {
                try {
                    return this.numberFormat.parse(this.tokens[columnIndex]);
                } catch (ParseException parseException) {

                    return null;
                }
            }

            @Override
            public String getString(int columnIndex) { return this.tokens[columnIndex]; }

            @Override
            public Date getDate(int columnIndex) throws OperatorException {
                String pattern = (String)this.datePattern.get(Integer.valueOf(columnIndex));
                try {
                    if (pattern != null) {
                        DateFormat format = ParameterTypeDateFormat.createCheckedDateFormat(ArffExampleSource.this, pattern);
                        return format.parse(this.tokens[columnIndex]);
                    }
                    return this.dateFormat.parse(this.tokens[columnIndex]);
                } catch (ParseException parseException) {

                    return null;
                }
            }
        };
    }

    private StreamTokenizer createTokenizer(Reader in) {
        StreamTokenizer tokenizer = new StreamTokenizer(in);
        tokenizer.resetSyntax();
        tokenizer.whitespaceChars(0, 32);
        tokenizer.wordChars(33, 255);
        tokenizer.whitespaceChars(44, 44);
        tokenizer.commentChar(37);
        tokenizer.quoteChar(34);
        tokenizer.quoteChar(39);
        tokenizer.ordinaryChar(123);
        tokenizer.ordinaryChar(125);
        tokenizer.eolIsSignificant(true);
        return tokenizer;
    }

    @Override
    protected boolean supportsEncoding() { return true; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        ParameterType type = FileInputPortHandler.makeFileParameterType(this, "data_file", "Name of the Arff file to read the data from.", "arff", () ->
                this.fileInputPort);
        type.setPrimary(true);
        types.add(type);
        types.addAll(super.getParameterTypes());
        types.addAll(StrictDecimalFormat.getParameterTypes(this));
        return types;
    }
}

