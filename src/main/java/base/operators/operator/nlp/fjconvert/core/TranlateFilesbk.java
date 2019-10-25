
/**  
* @Title: TranlateFiles.java
* @Package nlp.fjconvert
* @Description: TODO(用一句话描述该文件做什么)
* @author Hubery
* @date 2017年2月23日
* @version V1.0  
*/

package base.operators.operator.nlp.fjconvert.core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: TranlateFiles
 * @Description: TODO(对整个目录下文本进行转换，输出结果保持原有目录结构不变)
 * @author Hubery
 * @date 2017年2月23日
 *
 */

public class TranlateFilesbk {
	public static void main(String[] args)
	{

		long start = System.currentTimeMillis();
		TranlateFilesbk t = new TranlateFilesbk();
		t.TranslateFiles("D:\\RUN\\NLP-corpus", "D:\\RUN\\NLP-corpus(转简体)");
		long end = System.currentTimeMillis();
		System.out.println("运行时间： " + (end - start) + "ms");

	}

	public void TranslateFiles(String readPath, String writePath)
	{
		File sourceFile = new File(readPath);// 定义处理的一级文件夹 ("utf8","")
		List<String> ls = new ArrayList<String>();
		getFileList(sourceFile, ls);
		ChineseConvert convert = ChineseConvert.getInstance(0); // 转换方式0，tw2s
		String encoding = "utf-8";
		for (String filePath : ls)
		{
			String new_filePath = filePath;
			System.out.println("待处理文件路径：" + filePath);
			new_filePath = new_filePath.replace(readPath, writePath); // 设置输出目录
			String[] outPath = new_filePath.split("\\\\"); // 检测输出目录是否存在，不存在则创建
			String out_path = "";
			for (int j = 0; j <= outPath.length - 2; j++)
			{
				out_path = out_path + outPath[j];
				if (j < outPath.length - 2)
				{
					out_path += "\\";
				}
			}
			System.out.println(out_path);
			if (!new File(out_path).exists())
				new File(out_path).mkdirs();

			new_filePath = new_filePath.replace(".utf8", "-简体.utf8"); // 将处理得到的文件名加上"-简体"以示区别
			new_filePath = new_filePath.replace(".txt", "-简体.txt");
			new_filePath = new_filePath.replace(".doc", "-简体.doc");
			new_filePath = new_filePath.replace(".docx", "-简体.docx");
			String route = filePath;
			try
			{
				File file = new File(route);
				if (file.isFile() && file.exists())
				{
					InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
					BufferedReader bufferedReader = new BufferedReader(read);
					OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new_filePath),
							encoding);
					String lineTxt = null;
					while ((lineTxt = bufferedReader.readLine()) != null)
					{
						lineTxt = convert.convert(lineTxt); // 将字符串进行按照用户选择的转换类型进行转换
						outputStreamWriter.write(lineTxt + "\r\n");
					}
					read.close();
					outputStreamWriter.close();
				} else
				{
					System.out.println("找不到指定的文件");
				}
			} catch (Exception e)
			{
				System.out.println("读取文件内容出错");
				e.printStackTrace();
			}
		}
	}

	private static boolean isExtendCodePoint(int codePoint)
	{ // 检测代码点位
		char c[] = Character.toChars(codePoint);
		if (c.length == 1)
			return false;
		return true;
	}

	public static void writeArrayListIntoFile(ArrayList<String> al, // 将转换后的结果输出到指定文件中
                                              File destFile, String encoding)
	{
		if (!destFile.isFile() || !destFile.exists())
		{
			destFile.mkdirs();
		}

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

	final static void getFileList(File dir, List<String> fileList)
	{ // 遍历获得文件夹下所有文件路径
		File[] fs = dir.listFiles();
		String fileName;
		try
		{
			for (int i = 0; i < fs.length; i++)
			{
				fileName = fs[i].getAbsolutePath();
				if (fs[i].isDirectory())
				{
					try
					{
						// System.out.println(fileName);
						getFileList(fs[i], fileList);
					} catch (Exception e)
					{
					}
				} else
				{
					fileList.add(fileName);
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void readFile(String fileName)
	{
		try
		{
			FileInputStream fInputStream = new FileInputStream(fileName);
			InputStreamReader inputStreamReader = new InputStreamReader(fInputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);
			String row;
			while ((row = in.readLine()) != null)
			{
				System.out.println(row);
			}

		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
