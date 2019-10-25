package base.operators.operator.nlp.crf;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author zhanglsh01
 * create time:  2019.01.02.
 */
public class OverallSetting {

    public static Logger logger = Logger.getLogger("CRF");
    static {
        logger.setLevel(Level.WARNING);
    }

}
