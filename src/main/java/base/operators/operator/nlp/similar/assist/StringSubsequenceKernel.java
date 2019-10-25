package base.operators.operator.nlp.similar.assist;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StringSubsequenceKernel {
	public List<String> text;
	
	public String separator = "";
	
	public int k = 2;
	
	public double lambda = 0.9; 
	
	public StringSubsequenceKernel(List<String> text){
		this.text = text;
	}
	public StringSubsequenceKernel(List<String> text , String separator){
		this.text = text;
		this.separator = separator;
	}
	public StringSubsequenceKernel(List<String> text , String separator, int k, double lambda){
		this.text = text;
		this.separator = separator;
		this.k = k;
		this.lambda = lambda;
	}
	public double[][] kernel(){
		List<List<String>> text_split = new ArrayList<List<String>>(); 
		List<List<String>> combinations = new ArrayList<List<String>>(); 
		//将多个字符串按照字符或者分隔符进行切割
		if ("".equals(this.separator)){
			for (int i = 0; i < this.text.size(); i++){
				List<String> per_text = new ArrayList<String>();
				for (int j = 0; j < this.text.get(i).length(); j++){
					per_text.add(this.text.get(i).substring(j, j + 1));
				}
				text_split.add(per_text);
				combinations.addAll(combinationsK(per_text, k));
			}
		}else{
			for (int i = 0; i < this.text.size(); i++){
				List<String> per_text = new ArrayList<String>();
				for (int j = 0; j < this.text.get(i).split(this.separator).length; j++){
					per_text.add(this.text.get(i).split(separator)[j]);
				}
				text_split.add(per_text);
				combinations.addAll(combinationsK(per_text, k));
			}
		}
		
		double[][] score = new double[this.text.size()][combinations.size()];
		for (int w = 0; w < text_split.size(); w++){
			for (int u = 0; u < combinations.size(); u++){
				if (text_split.get(w).containsAll(combinations.get(u))){
					int exponent = getExponent(text_split.get(w),combinations.get(u));
					score[w][u] = Math.pow(this.lambda, exponent);
				}
			}
		}
		return score;
		
	}
	/*
	 * 对给定列表中的元素，进行两两组合（K-K组合)
	 * @param List 待组合的列表
	 * @param k 组合的个数
	 * @return 返回组合的左右可能
	 * */
	public static <T> List<List<T>> combinationsK(List<T> list, int k){
		if (k == 0 || list.isEmpty()) {//去除K大于list.size的情况。即取出长度不足K时清除此list
            return Collections.emptyList();
        }
        if (k == 1) {//递归调用最后分成的都是1个1个的，从这里面取出元素
            return list.stream().map(e -> Stream.of(e).collect(Collectors.toList())).collect(Collectors.toList());
        }
        Map<Boolean, List<T>> headAndTail = split(list, 1);
        List<T> head = headAndTail.get(true);
        List<T> tail = headAndTail.get(false);
        List<List<T>> c1 = combinationsK(tail, (k - 1)).stream().map(e -> {
            List<T> l = new ArrayList<>();
            l.addAll(head);
            l.addAll(e);
            return l;
        }).collect(Collectors.toList());
        List<List<T>> c2 = combinationsK(tail, k);
        c1.addAll(c2);
        return c1;
	}
	/**
	* 根据k将集合分成两组
	**/
	public static <T> Map<Boolean, List<T>> split(List<T> list, int k) {
		return IntStream
				.range(0, list.size())
				.mapToObj(i -> new SimpleEntry<>(i, list.get(i)))
				.collect(Collectors.partitioningBy(entry -> entry.getKey() < k, Collectors.mapping(SimpleEntry::getValue, Collectors.toList())));
	}
	
	/**
	* 求出list2元素在list1中下标的最大值和最小值的差
	**/
	public static <T> int getExponent(List<T> list1, List<T> list2) {
		int max = 0;
		int min = 0;
		for (T item : list2){
			max = list1.lastIndexOf(item) >= max ? list1.lastIndexOf(item) : max;
			min = list1.indexOf(item) <= min ? list1.indexOf(item) : min;
		}
		return max - min;
	}
	
//	public static void main(String[] args){
//		List<String> list = new ArrayList<String>();
//		list.add("wo");
//		list.add("ni");
//		list.add("ta");
//		list.add("e");
//		List<String> list1 = new ArrayList<String>();
//		list1.add("ta");
//		list1.add("la");
//		System.out.println(getExponent(list,list1));
//	}
}
