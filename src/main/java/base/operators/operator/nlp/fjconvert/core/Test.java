package base.operators.operator.nlp.fjconvert.core;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Test {
	public static void main(String[] args) {
		String encoding = "utf-8";
		System.out.println("请输入路径");
		Scanner sca = new Scanner(System.in);
		String route = sca.next();
		System.out.println("转换方式:");
		System.out.println(
				"conversion options are tw2s = 0；t2s = 2；s2t = 3；s2tw = 4；t2tw = 9" + "\n" + "请输入选择的转换方式对应的数字");
		Scanner sca2 = new Scanner(System.in);
		int type = sca.nextInt();

		ChineseConvert convert = ChineseConvert.getInstance(type);
		sca.close();
		sca2.close();
		try
		{
			File file = new File(route);
			if (file.isFile() && file.exists())
			{ // 判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				ArrayList<String> strArray = new ArrayList<String>();
				int i = 0;
				while ((lineTxt = bufferedReader.readLine()) != null)
				{
					lineTxt = convert.convert(lineTxt); // 将字符串进行按照用户选择的转换类型进行转换
					strArray.add(lineTxt);
				}
				writeArrayListIntoFile(strArray, new File("/Users/donglinkun/Desktop/未命名文件夹/S-as-testref.txt"), "utf-8");// 处理后的语料输出路径																						// S-
				read.close();
								
			} else
			{
				System.out.println("找不到指定的文件");
			}
		} catch (Exception e)
		{
			System.out.println("读取文件内容出错");
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		//System.out.println("运行时间： " + (end - start) + "ms");
	}
	private static boolean isExtendCodePoint(int codePoint)
	{
		char c[] = Character.toChars(codePoint);
		if (c.length == 1)
		return false;
		return true;
	}
	public static void writeArrayListIntoFile(ArrayList<String> al, // 将转换后的结果输出到指定文件中
                                              File destFile, String encoding)
	{ 
		System.out.println("Line Num ： " + al.size());
		try
		{
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(destFile), encoding);
			if (al.size() > 0)
			{
				for (String word : al)
				{
					outputStreamWriter.write(word + "\r\n");
				}
			}
			outputStreamWriter.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
