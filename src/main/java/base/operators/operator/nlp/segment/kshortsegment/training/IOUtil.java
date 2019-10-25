package base.operators.operator.nlp.segment.kshortsegment.training;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 一些常用的IO操作
 *
 */
public class IOUtil {
	/**
	 * 序列化对象
	 *
	 * @param o
	 * @param path
	 * @return
	 */
	public static boolean saveObjectTo(Object o, String path) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(IOUtil.newOutputStream(path));
			oos.writeObject(o);
			oos.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * 反序列化对象
	 *
	 * @param path
	 * @return
	 */
	public static Object readObjectFrom(String path) {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(IOUtil.newInputStream(path));
			Object o = ois.readObject();
			ois.close();
			return o;
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * 一次性读入纯文本
	 *
	 * @param path
	 * @return
	 */
	public static String readTxt(String path) {
		if (path == null)
			return null;
		try {
			InputStream in = new FileInputStream(path);
			byte[] fileContent = new byte[in.available()];
			readBytesFromOtherInputStream(in, fileContent);
			in.close();
			//System.out.println("IO Utils OK:"+path);
			return new String(fileContent, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.out.println("IO Utils Error:"+path);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 快速保存
	 *
	 * @param path
	 * @param content
	 * @return
	 */
	public static boolean saveTxt(String path, String content) {
		try {
			FileChannel fc = new FileOutputStream(path).getChannel();
			fc.write(ByteBuffer.wrap(content.getBytes()));
			fc.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean saveTxt(String path, StringBuilder content) {
		return saveTxt(path, content.toString());
	}

	public static <T> boolean saveCollectionToTxt(Collection<T> collection, String path) {
		StringBuilder sb = new StringBuilder();
		for (Object o : collection) {
			sb.append(o);
			sb.append('\n');
		}
		return saveTxt(path, sb.toString());
	}

	/**
	 * 将整个文件读取为字节数组
	 *
	 * @param path
	 * @return
	 */
	public static byte[] readBytes(String path) {
		try {
			return readBytesFromFileInputStream(new FileInputStream(path));
		} catch (Exception e) {
		}

		return null;
	}

	public static String baseName(String path) {
		if (path == null || path.length() == 0)
			return "";
		path = path.replaceAll("[/\\\\]+", "/");
		int len = path.length(), upCount = 0;
		while (len > 0) {
			// remove trailing separator
			if (path.charAt(len - 1) == '/') {
				len--;
				if (len == 0)
					return "";
			}
			int lastInd = path.lastIndexOf('/', len - 1);
			String fileName = path.substring(lastInd + 1, len);
			if (fileName.equals(".")) {
				len--;
			} else if (fileName.equals("..")) {
				len -= 2;
				upCount++;
			} else {
				if (upCount == 0)
					return fileName;
				upCount--;
				len -= fileName.length();
			}
		}
		return "";
	}

	private static byte[] readBytesFromFileInputStream(FileInputStream fis) throws IOException {
		FileChannel channel = fis.getChannel();
		int fileSize = (int) channel.size();
		ByteBuffer byteBuffer = ByteBuffer.allocate(fileSize);
		channel.read(byteBuffer);
		byteBuffer.flip();
		byte[] bytes = byteBuffer.array();
		byteBuffer.clear();
		channel.close();
		fis.close();
		return bytes;
	}

	/**
	 * 将InputStream中的数据读入到字节数组中
	 *
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static byte[] readBytesFromOtherInputStream(InputStream is) throws IOException {
		byte[] targetArray = new byte[is.available()];
		readBytesFromOtherInputStream(is, targetArray);
		is.close();
		return targetArray;
	}

	/**
	 * 从InputStream读取指定长度的字节出来
	 * 
	 * @param is
	 *            流
	 * @param targetArray
	 *            output
	 * @throws IOException
	 */
	public static void readBytesFromOtherInputStream(InputStream is, byte[] targetArray) throws IOException {
		int len;
		int off = 0;
		while ((len = is.read(targetArray, off, targetArray.length - off)) != -1 && off < targetArray.length) {
			off += len;
		}
	}

	public static LinkedList<String> readLineList(String path) {
		LinkedList<String> result = new LinkedList<String>();
		String txt = readTxt(path);
		if (txt == null)
			return result;
		StringTokenizer tokenizer = new StringTokenizer(txt, "\n");
		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken());
		}

		return result;
	}

	/**
	 * 用省内存的方式读取大文件
	 *
	 * @param path
	 * @return
	 */
	public static LinkedList<String> readLineListWithLessMemory(String path) {
		LinkedList<String> result = new LinkedList<String>();
		String line = null;
		try {
			BufferedReader bw = new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
			while ((line = bw.readLine()) != null) {
				result.add(line);
			}
			bw.close();
		} catch (Exception e) {
		}

		return result;
	}

	public static boolean saveMapToTxt(Map<Object, Object> map, String path) {
		return saveMapToTxt(map, path, "=");
	}

	public static boolean saveMapToTxt(Map<Object, Object> map, String path, String separator) {
		map = new TreeMap<Object, Object>(map);
		return saveEntrySetToTxt(map.entrySet(), path, separator);
	}

	public static boolean saveEntrySetToTxt(Set<Map.Entry<Object, Object>> entrySet, String path, String separator) {
		StringBuilder sbOut = new StringBuilder();
		for (Map.Entry<Object, Object> entry : entrySet) {
			sbOut.append(entry.getKey());
			sbOut.append(separator);
			sbOut.append(entry.getValue());
			sbOut.append('\n');
		}
		return saveTxt(path, sbOut.toString());
	}

	/**
	 * 获取文件所在目录的路径
	 * 
	 * @param path
	 * @return
	 */
	public static String dirname(String path) {
		int index = path.lastIndexOf('/');
		if (index == -1)
			return path;
		return path.substring(0, index + 1);
	}

	public static LineIterator readLine(String path) {
		return new LineIterator(path);
	}

	/**
	 * 方便读取按行读取大文件
	 */
	public static class LineIterator implements Iterator<String> {
		BufferedReader bw;
		String line;

		public LineIterator(String path) {
			try {
				bw = new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
				line = bw.readLine();
			} catch (FileNotFoundException e) {
				// logger.warning("文件" + path + "不存在，接下来的调用会返回null" +
				// TextUtility.exceptionToString(e));
				bw = null;
			} catch (IOException e) {
				// logger.warning("在读取过程中发生错误" +
				// TextUtility.exceptionToString(e));
				bw = null;
			}
		}

		public void close() {
			if (bw == null)
				return;
			try {
				bw.close();
				bw = null;
			} catch (IOException e) {
				// logger.warning("关闭文件失败" + TextUtility.exceptionToString(e));
			}
			return;
		}

		@Override
		public boolean hasNext() {
			if (bw == null)
				return false;
			if (line == null) {
				try {
					bw.close();
					bw = null;
				} catch (IOException e) {
					// logger.warning("关闭文件失败" +
					// TextUtility.exceptionToString(e));
				}
				return false;
			}

			return true;
		}

		@Override
		public String next() {
			String preLine = line;
			try {
				if (bw != null) {
					line = bw.readLine();
					if (line == null && bw != null) {
						try {
							bw.close();
							bw = null;
						} catch (IOException e) {
							// logger.warning("关闭文件失败" +
							// TextUtility.exceptionToString(e));
						}
					}
				} else {
					line = null;
				}
			} catch (IOException e) {
				// logger.warning("在读取过程中发生错误" +
				// TextUtility.exceptionToString(e));
			}
			return preLine;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("只读，不可写！");
		}
	}

	/**
	 * 创建一个BufferedWriter
	 *
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public static BufferedWriter newBufferedWriter(String path) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(IOUtil.newOutputStream(path), "UTF-8"));
	}

	/**
	 * 创建一个BufferedReader
	 * 
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public static BufferedReader newBufferedReader(String path) throws IOException {
		return new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
	}

	public static BufferedWriter newBufferedWriter(String path, boolean append)
			throws FileNotFoundException, UnsupportedEncodingException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, append), "UTF-8"));
	}

	/**
	 * 创建输入流（经过IO适配器创建）
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream newInputStream(String path) throws IOException {
		return new FileInputStream(path);
	}

	/**
	 * 创建输出流（经过IO适配器创建）
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static OutputStream newOutputStream(String path) throws IOException {
		return new FileOutputStream(path);
	}

	/**
	 * 获取最后一个分隔符的后缀
	 * 
	 * @param name
	 * @param delimiter
	 * @return
	 */
	public static String getSuffix(String name, String delimiter) {
		return name.substring(name.lastIndexOf(delimiter) + 1);
	}

	/**
	 * 写数组，用制表符分割
	 * 
	 * @param bw
	 * @param params
	 * @throws IOException
	 */
	public static void writeLine(BufferedWriter bw, String... params) throws IOException {
		for (int i = 0; i < params.length - 1; i++) {
			bw.write(params[i]);
			bw.write('\t');
		}
		bw.write(params[params.length - 1]);
	}

}
