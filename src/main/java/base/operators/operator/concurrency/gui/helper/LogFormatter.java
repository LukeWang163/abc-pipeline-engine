package base.operators.operator.concurrency.gui.helper;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LogFormatter extends SimpleFormatter {
    public static final String LOG_LEVEL_SEPARATOR = ": ";
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue()
        {
            return (DateFormat)DateFormat.getDateTimeInstance().clone();
        }
    };

    @Override
    public String format(LogRecord logRecord) {
        StringBuilder b = new StringBuilder();
        b.append(((DateFormat)DATE_FORMAT.get()).format(new Date(logRecord.getMillis())));
        b.append(" ");
        b.append(logRecord.getLevel().getLocalizedName());
        b.append(": ");
        b.append(formatMessage(logRecord));
        b.append("\n");
        return b.toString();
    }
}
