package base.operators.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTypeUtil {

	public static String getType(String str) {
		try {
            Long.parseLong(str);
            return "BIGINT";
        } catch (NumberFormatException e1){
		    try {
                Double.parseDouble(str);
                return "DOUBLE";
            } catch (NumberFormatException e2) {
                return "STRING";
            }
		}
	}

	private static boolean checkLegalFirstChar(char c) {
	    String regExp = "[A-Za-z]";
	    Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher("" + c);
        return m.matches();
    }

    private static boolean checkLegalChar(char c) {
	    String regExp = "[\\w]";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher("" + c);
        return m.matches();
    }

    public static boolean checkLegalStr(String str) {
	    boolean result = true;
	    char[] cTemp = str.trim().replace('-', '_').toCharArray();
	    if (cTemp.length==0) {
	        result = false;
        } else {
	        if (!checkLegalFirstChar(cTemp[0])) {
	            result = false;
            } else {
                for (int i = 1; i < cTemp.length; i++) {
                    if (!checkLegalChar(cTemp[i])) {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }
}
