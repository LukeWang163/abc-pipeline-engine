package abc_pipeline_engine.utils;

import java.text.SimpleDateFormat;
import java.util.Date;


public class DateTimeUtil {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static int compareTime(String time1, String time2) {
    	try {
			return sdf.parse(time1).compareTo(sdf.parse(time2));
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
    	return 0;
    }
    
	public static String getCurrentTime() {
		
		return sdf.format(new Date());
	}
	
	public static String getCurrentTime(long num){
		return sdf.format(new Date(num));
	}

	public static String converTime(String str) {
		if (null != str) {
			str = str.replaceAll("[A-Z]", " ");
		}
		return str;
	}
	public static String getDistanceTime(String str1, String str2) {
        Date one;
        Date two;
        long day = 0;
        long hour = 0;
        long min = 0;
        long sec = 0;
        try {
            one = sdf.parse(str1);
            two = sdf.parse(str2);
            long time1 = one.getTime();
            long time2 = two.getTime();
            long diff ;
            if(time1<time2) {
                diff = time2 - time1;
            } else {
                diff = time1 - time2;
            }
            day = diff / (24 * 60 * 60 * 1000);
            hour = (diff / (60 * 60 * 1000) - day * 24);
            min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
            sec = (diff/1000-day*24*60*60-hour*60*60-min*60);
        } catch (java.text.ParseException e) {
			e.printStackTrace();
		}
        return day + " " + hour + ":" + min + ":" + sec;
    }
	
	/**
	 * 简单的ID生成器
	 * @return
	 */
	public static long getId(){
		return (new Date()).getTime();
	}
	

}
