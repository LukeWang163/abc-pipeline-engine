package base.operators.operator.scripting.python;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;

public class PythonDecimalFormat extends DecimalFormat {
    private static final long serialVersionUID = 1L;

    public PythonDecimalFormat() {
        this.setParseIntegerOnly(false);
        DecimalFormatSymbols symbols = this.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setInfinity("inf");
        symbols.setExponentSeparator("e");
        this.setDecimalFormatSymbols(symbols);
        this.setGroupingUsed(false);
    }

    @Override
    public Number parse(String source) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() < source.length()) {
            if (!source.contains("e+")) {
                throw new ParseException("Unparseable number: \"" + source + "\"", parsePosition.getIndex());
            }

            String fixedSource = source.replaceFirst("e\\+", "e");
            parsePosition = new ParsePosition(0);
            result = this.parse(fixedSource, parsePosition);
            if (parsePosition.getIndex() < fixedSource.length()) {
                throw new ParseException("Unparseable number: \"" + fixedSource + "\"", parsePosition.getIndex());
            }
        }

        return result;
    }
}
