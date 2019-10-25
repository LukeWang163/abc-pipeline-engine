package base.operators.operator.nlp.autosummary.core.summary;

import java.util.Map;

public class ConfConfig {

	private static ConfConfig config;

	// 输出摘要句子个数
	private String size = "3";
	
	// 阻尼系数
	private double d = 0.85;
	
	// 迭代次数
	private int max_iter = 200;
	
	// 收敛系数
	private double min_diff = 0.0001;
	
	// 语言(0:Chinese/1:English)
	private int lang = 0;
	// 停用词表使用方式
	private int type = 0; // 0:系统停用词；1:自定义停用词;2:合并
	
	public ConfConfig() {

	}

    public ConfConfig(double d,int max_iter,double min_diff,String size,int lang,int type) {//需要修改，因为要传参
        this.d = d;
        this.max_iter = max_iter;
        this.min_diff = min_diff;
        this.size = size;
        this.lang = lang;
        this.type = type;
    }

	public static ConfConfig getInstance() {
		if (config == null) {
			config = new ConfConfig();
		}
		return config;
    }

    public static ConfConfig getInstance(double d,int max_iter,double min_diff,String size,int lang,int type) {
        if (config == null) {
            config = new ConfConfig(d,max_iter,min_diff,size,lang,type);
        }
        return config;
    }

    /**
     * 通过传参进行初始化
     * @param paraMap
     */
	public void init(Map<String, Object> paraMap) {//可以通过这个方法来传参
		if(paraMap.containsKey("language")){
			lang = (int) paraMap.get("lang");
		}
		if(paraMap.containsKey("size")){
			size = (String) paraMap.get("size");
		}
		if(paraMap.containsKey("type")){
			type = (int) paraMap.get("type");
		}
		if(paraMap.containsKey("d")){
			d = (Double) paraMap.get("d");
		}
		if(paraMap.containsKey("max_iter")){
			max_iter = (Integer) paraMap.get("max_iter");
		}
		if(paraMap.containsKey("min_diff")){
			min_diff = (Double) paraMap.get("min_diff");
		}
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public double getD() {
		return d;
	}

	public void setD(double d) {
		this.d = d;
	}

	public int getMax_iter() {
		return max_iter;
	}

	public void setMax_iter(int max_iter) {
		this.max_iter = max_iter;
	}

	public double getMin_diff() {
		return min_diff;
	}

	public void setMin_diff(double min_diff) {
		this.min_diff = min_diff;
	}

	public int getLang() {
		return lang;
	}

	public void setLang(int lang) {
		this.lang = lang;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
