package base.operators;

import base.operators.operator.concurrency.gui.helper.LogFormatter;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author zls
 * create time:  2019.09.23.
 * description:
 */
public class OperatorLogMapper extends Logger {

    public String logString ;
    public OperatorLogMapper(String name, Level level){
        super(name,null);
        super.setLevel(level);
        this.logString = "";
    }

    public OperatorLogMapper(String name){
        super(name,null);
        super.setLevel(Level.ALL);
        this.logString = "";
    }

    @Override
    public void log(LogRecord record) {
        logString  += (new LogFormatter().format(record));

    }

    public static void main(String[] args) {
        Logger logger = new OperatorLogMapper("a", Level.ALL);
        //logger.setLevel(Level.ALL);
        logger.warning("qq");
        logger.info("bbaa");
        System.out.println(((OperatorLogMapper) logger).logString);

    }
}
