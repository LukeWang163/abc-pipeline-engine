package base.operators.operator.nlp.ner.time;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * 新版时间表达式识别的主要工作类
 * <p>
 */
public class TimeAnnotate implements Serializable {

    private static final long serialVersionUID = 463541045644656392L;

    private String timeBase;
    private String oldTimeBase;
    private static Pattern patterns = null;
    private String target;
    private ArrayList<TimeUnit> timeToken = new ArrayList();

    private boolean isPreferFuture = true;

    /*public TimeAnnotate(List<String> patternList) {
        if (patterns == null) {
            try {
                patterns = initPattern(patternList);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Read model error!");
            }
        }
    }*/
    public TimeAnnotate() {
        if (patterns == null) {
            try {
                patterns = initPattern();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Read model error!");
            }
        }
    }

    /**
     * 参数为正则表达式
     *
     * @param path
     */
    public TimeAnnotate(String regex) {
        if (patterns == null) {
            try {
                patterns = initPattern(regex);
                //如果输入的内容不是正则表达式中的内容，继续初始化
                if (patterns == null){

                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Read model error!");
            }
        }
    }


    /**
     * 构造函数
     * 
     * @param regex 正则表达式
     * @param isPreferFuture 未来趋势标识
     */
    public TimeAnnotate(String regex, boolean isPreferFuture) {
        this.isPreferFuture = isPreferFuture;
        if (patterns == null) {
            try {
                patterns = initPattern(regex);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.print("Read model error!");
            }
        }
    }

    /**
     * TimeNormalizer的构造方法，根据提供的待分析字符串和timeBase进行时间表达式提取
     * 在构造方法中已完成对待分析字符串的表达式提取工作
     *
     * @param target   待分析字符串
     * @param timeBase 给定的timeBase
     * @return 返回值
     */
    public ArrayList<TimeUnit> parse(String target, String timeBase) {
        this.target = target;
        this.timeBase = timeBase;
        this.oldTimeBase = timeBase;
        // 字符串预处理
        preHandling();
        timeToken = TimeEx(this.target, timeBase);
        return timeToken;
    }

    /**
     * 同上的TimeNormalizer的构造方法，timeBase取默认的系统当前时间
     *
     * @param target 待分析字符串
     * @return 时间单元数组
     */
    public ArrayList<TimeUnit> parse(String target) {
        this.target = target;
        this.timeBase = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());// TODO
        // Calendar.getInstance().getTime()换成new
        // Date？
        this.oldTimeBase = timeBase;
        preHandling();// 字符串预处理
        timeToken = TimeEx(this.target, timeBase);
        return timeToken;
    }

    //

    /**
     * timeBase的get方法
     *
     * @return 返回值
     */
    public String getTimeBase() {
        return timeBase;
    }

    /**
     * oldTimeBase的get方法
     *
     * @return 返回值
     */
    public String getOldTimeBase() {
        return oldTimeBase;
    }

    public boolean isPreferFuture() {
        return isPreferFuture;
    }

    public void setPreferFuture(boolean isPreferFuture) {
        this.isPreferFuture = isPreferFuture;
    }

    /**
     * timeBase的set方法
     *
     * @param s timeBase
     */
    public void setTimeBase(String s) {
        timeBase = s;
    }

    /**
     * 重置timeBase为oldTimeBase
     */
    public void resetTimeBase() {
        timeBase = oldTimeBase;
    }

    /**
     * 时间分析结果以TimeUnit组的形式出现，此方法为分析结果的get方法
     *
     * @return 返回值
     */
    public ArrayList<TimeUnit> getTimeUnit() {
        return timeToken;
    }

    /**
     * 待匹配字符串的清理空白符和语气助词以及大写数字转化的预处理
     */
    private void preHandling() {
        target = StringPreHandlingModule.delKeyword(target, "\\s+"); // 清理空白符
        target = StringPreHandlingModule.delKeyword(target, "[的]+"); // 清理语气助词
        target = StringPreHandlingModule.numberTranslator(target);// 大写数字转化
        // TODO 处理大小写标点符号
    }

    /**
     * 有基准时间输入的时间表达式识别
     * <p>
     * 这是时间表达式识别的主方法， 通过已经构建的正则表达式对字符串进行识别，并按照预先定义的基准时间进行规范化
     * 将所有别识别并进行规范化的时间表达式进行返回， 时间表达式通过TimeUnit类进行定义
     *
     * @param  tar 输入文本字符串
     * @param  timebase 输入基准时间
     * @return TimeUnit[] 时间表达式类型数组
     */
    private ArrayList<TimeUnit> TimeEx(String tar, String timebase) {
        Matcher match;
        int startline = -1, endline = -1;

        String[] temp = new String[99];
        int rpointer = 0;// 计数器，记录当前识别到哪一个字符串了
        TimeUnit[] Time_Result = null;

        match = patterns.matcher(tar);
        boolean startmark = true;
        while (match.find()) {
            startline = match.start();
            if (endline == startline) // 假如下一个识别到的时间字段和上一个是相连的 @author kexm
            {
                rpointer--;
                temp[rpointer] = temp[rpointer] + match.group();// 则把下一个识别到的时间字段加到上一个时间字段去
            } else {
                if (!startmark) {
                    rpointer--;
                    rpointer++;
                }
                startmark = false;
                temp[rpointer] = match.group();// 记录当前识别到的时间字段，并把startmark开关关闭。这个开关貌似没用？
            }
            endline = match.end();
            rpointer++;
        }
        if (rpointer > 0) {
            rpointer--;
            rpointer++;
        }
        Time_Result = new TimeUnit[rpointer];
        /**时间上下文： 前一个识别出来的时间会是下一个时间的上下文，用于处理：周六3点到5点这样的多个时间的识别，第二个5点应识别到是周六的。*/
        TimePoint contextTp = new TimePoint();
        int indexCount=0;
        for (int j = 0; j < rpointer; j++) {
			int startIndex= tar.indexOf(temp[j],indexCount);
			if(startIndex==-1) break;
            Time_Result[j] = new TimeUnit(temp[j], startIndex,this, contextTp);
            contextTp = Time_Result[j]._tp;
			//System.out.println(dex);
			indexCount=startIndex+temp[j].length();
        }
        /**过滤无法识别的字段*/
       // Time_Result = filterTimeUnit(Time_Result);
       // return Time_Result;
        // Time_Result = filterTimeUnit(Time_Result);
        return filterTimeUnit(Time_Result);
    }

    /**
     * 过滤timeUnit中无用的识别词。无用识别词识别出的时间是1970.01.01 00:00:00(fastTime=-28800000)
     *
     * @param timeUnit
     * @return
     */
    public static ArrayList<TimeUnit> filterTimeUnit(TimeUnit[] timeUnit) {
    	ArrayList<TimeUnit> list = new ArrayList<>();
        if (timeUnit == null || timeUnit.length < 1) {
            return list;
        }
       // List<TimeUnit> list = new ArrayList<>();
        for (TimeUnit t : timeUnit) {
            if (t.getTime().getTime() != -28800000) {
                list.add(t);
            }
        }
       // TimeUnit[] newT = new TimeUnit[list.size()];
       // newT = list.toArray(newT);
        return list;
    }

    private Pattern readModel(String file) throws Exception {
        ObjectInputStream in;
        if (file.startsWith("jar:file") || file.startsWith("file:")) {
            in = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new URL(file).openStream())));
        } else {
            in = new ObjectInputStream(
                    new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
        }
        return readModel(in);
    }

    private Pattern readModel(ObjectInputStream in) throws Exception {
        Pattern p = (Pattern) in.readObject();
        return Pattern.compile(p.pattern());
    }

    public static void writeModel(Object p, String path) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(path))));
        out.writeObject(p);
        out.close();
    }
    
    /**
     * 初始化Pattern
     * @param regex 正则表达式
     * @return
     * add by Binson
     */
    private Pattern initPattern(String regex) {
    	 return Pattern.compile(regex);
    }

    /**
     * 初始化Pattern ， 表达式从TimeExp.xml配置文件里读取
     * @return
     * add by Binson
     */
    private Pattern initPattern() {
    	//String regex = ConfigUtil.getString(ConfigUtil.TIME_EXP);
    	List<String> ruleList = XmlParser.getTimePatterns();
    	//构造完整的正则表达式
    	String regex = "";
    	for (String rule : ruleList) {
			regex = regex + rule + "|";
		}
    	
    	//删除最后一个“|”
    	regex = regex.substring(0, regex.length() - 1);
    	
        return Pattern.compile(regex);
    }


}
