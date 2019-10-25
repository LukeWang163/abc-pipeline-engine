package base.operators.utils;

import com.csvreader.CsvReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class HDFSUtil {

	private final static Log log = LogFactory.getLog(HDFSUtil.class);

	public static FileSystem fs = null;


	public static void initFileSystem() {

		String kbsEnable = "false"; // ConfigUtil.getString(Constants.KERBEROS_ENABLE);
		String hdfsHaEnable = "false"; // ConfigUtil.getString(Constants.HDFS_HA_ENABLE);
		Configuration conf = new Configuration();
		conf.set("fs.default.name", "hdfs://10.111.25.138:8020"); // ConfigUtil.getString(Constants.HDFS_URL));
		try {
			fs = FileSystem.get(new URI("hdfs://10.111.25.138:8020"), conf, "hdfs"); //new URI(ConfigUtil.getString(Constants.HDFS_URL)), conf,  ConfigUtil.getString(Constants.HDFS_USER));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void closeFileSystem(){
		if(fs != null){
			try {
				fs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 获取文件系统____默认userId为当前用户的Id，keyCloakRealm为当前登陆用户的realm
	 * @return			文件系统
	 */
	public static FileSystem getFileSystem() {

		try {
			fs.getStatus();
		} catch (Exception e){
			initFileSystem();
		}
		return fs;
	}
	
	/**
	 * 获取文件系统——可通过指定userId和对应的keyCloakRealm获取文件系统
	 * @param userId			用户Id
	 * @param keyCloakRealm		userId对应的Realm域
	 * @return					HDFS文件系统
	 */
	public static FileSystem getFileSystemWithUser(String userId, String keyCloakRealm) {
		FileSystem fs = null;
		String kbsEnable = "false"; // ConfigUtil.getString(Constants.KERBEROS_ENABLE);
		String hdfsHaEnable = "false"; // ConfigUtil.getString(Constants.HDFS_HA_ENABLE);
		Configuration conf = new Configuration();
		conf.set("fs.default.name",  "hdfs://10.111.25.138:8020");
		try {
			fs = FileSystem.get(new URI("hdfs://10.111.25.138:8020"), conf, "hdfs");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fs;
	}

	/**
	 * 获取文件系统的配置项
	 * @return 文件系统的配置项
	 */
	public static Configuration getFileSystemConf() {
		String kbsEnable = "false"; //ConfigUtil.getString(Constants.KERBEROS_ENABLE);
		String hdfsHaEnable = "false"; //ConfigUtil.getString(Constants.HDFS_HA_ENABLE);
		Configuration conf = new Configuration();
		conf.set("fs.default.name", "hdfs://10.111.25.138:8020");
		return conf;
	}

	public static FileSystem getFileSystem(String url) {
		return getFileSystem(url, "hdfs");
	}

	public static FileSystem getFileSystem(String url, String username) {
		if (StringUtils.isBlank(url)) {
			return null;
		}

		Configuration conf = new Configuration();
		FileSystem fs = null;
		try {
			URI uri = new URI(url.trim());
			fs = FileSystem.get(uri, conf, username);
		} catch (URISyntaxException | IOException | InterruptedException e) {
			if (log.isDebugEnabled()) {
				log.debug("get FileSystem throw exception:", e);
			}
		}
		return fs;
	}

	public static String getZip(String src, String tempDir) {
		try {
			Files.createDirectories(Paths.get(tempDir));
			FileSystem fs = getFileSystem();
			fs.copyToLocalFile(new Path(src), new Path(tempDir));
			fs.close();
			return FileUtil.zip(tempDir + "/" + src.substring(src.lastIndexOf("/") + 1));
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("Compressed file throw exception:", e);
			}
			try {
				Files.deleteIfExists(Paths.get(tempDir));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	public static String getZip(String[] paths, String tempDir) {
		try {
			Files.createDirectories(Paths.get(tempDir));
			FileSystem fs = getFileSystem();
			for (String path : paths) {
				fs.copyToLocalFile(new Path(path), new Path(tempDir));
			}
			fs.close();
			return FileUtil.zip(tempDir);
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("Compressed file throw exception:", e);
			}
			try {
				Files.deleteIfExists(Paths.get(tempDir));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	public static FileStatus[] listFile(String path) {
		FileSystem fs = getFileSystem();
		try {
			if (log.isDebugEnabled()) {
				log.debug("fs == null ? " + (fs == null));
			}
			FileStatus[] status = fs.listStatus(new Path(path));
			fs.close();
			return status;
		} catch (IllegalArgumentException | IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("get hdfs file list throw exception:", e);
			}
		}
		return null;
	}

	// 实现hdfs文件上传功能
	public static void uploadFile(String dest, InputStream is) throws IOException {
		FileSystem fileSystem = getFileSystem();
		// Check if the file already exists
		Path path = new Path(dest);
		if (fileSystem.exists(path)) {
			System.out.println("File " + dest + " already exists");
			return;
		}

		// Create a new file and write data to it.
		FSDataOutputStream out = fileSystem.create(path);
		InputStream in = new BufferedInputStream(is);

		byte[] b = new byte[1024];
		int numBytes = 0;
		while ((numBytes = in.read(b)) > 0) {
			out.write(b, 0, numBytes);
		}
		// Close all the file descriptors
		in.close();
		out.close();
		fileSystem.close();
	}
	
	public static long copyFile(String src,String dest) throws IOException{
		FileSystem fs=getFileSystem();
		Path srcPath=new Path(src);
		Path destPath=new Path(dest);
		if(fs.exists(destPath)){
			fs.delete(destPath, true);
		}
	
		FSDataInputStream in=fs.open(srcPath);
		FSDataOutputStream out=fs.create(destPath);
		byte[] buffer=new byte[4096];
		int totalBytes=0;
		int readBytes=0;
		while((readBytes=in.read(buffer))>0){
			out.write(buffer,0,readBytes);
			totalBytes+=readBytes;
		}
		
		in.close();
		out.close();
		fs.close();
		return totalBytes;
	}
	
	/**
	 * 封装FileUtil的复制文件工具类
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public static void copy(String src,String dest) throws IOException{
		FileSystem fs=getFileSystem();
		// 不删除src源文件
		org.apache.hadoop.fs.FileUtil.copy(fs, new Path(src), fs, new Path(dest), false, fs.getConf());
	}

	// 实现文件删除
	public static void removeFile(String path) {
		FileSystem fs = getFileSystem();
		try {
			fs.deleteOnExit(new Path(path));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
	
	// 删除文件（非关闭hdfs）
	public static void deleteFile(String path){
		FileSystem fs=getFileSystem();
		try {
			fs.delete(new Path(path), true);
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		} 
	}

    // 删除文件（非关闭hdfs）
    public static void deleteFilePath(Path path){
        FileSystem fs=getFileSystem();
        try {
            fs.delete(path, true);
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }

	/**
	 * 读取CSV元数据
	 * @param path
	 * @return
	 */
	public static List<Map<String, String>> getCSVSchema(String path) {
		// 如果不是以csv结尾则直接返回
        if (!path.endsWith(".csv")) {
            return Collections.emptyList();
        }
        FileSystem fs = getFileSystem();
        InputStream in = null;
        try {
            in = fs.open(new Path(path));
            CsvReader reader = new CsvReader(in, ',', Charset.forName("UTF-8"));
            String[] header, firstRow;
            reader.readHeaders();
            header = reader.getHeaders();
            reader.readRecord();
            firstRow = reader.getValues();

            if(header==null || "".equals(String.join(",", header)) || firstRow==null || "".equals(String.join(",", firstRow))) {
                return Collections.emptyList();
            }

            if(header.length!=firstRow.length){
                return Collections.emptyList();
            }

            List<Map<String, String>> allTypes = new ArrayList<>();
            for (int i = 0, len = header.length; i < len; i++) {
                Map<String, String> typeMap = new LinkedHashMap<>();
                typeMap.put("name", header[i]);
                typeMap.put("type", DataTypeUtil.getType(firstRow[i]));
				allTypes.add(typeMap);
            }

            /*reader.readRecord();*/
            int countdown = 100;
            while (countdown-- > 0) {
                reader.readRecord();
                String[] row = reader.getValues();
                if (row.length != 0) {
                    for (int i = 0, len = header.length; i < len; i++) {
                        columns:
                        {
                            String newType = DataTypeUtil.getType(row[i]);
                            String oldType = allTypes.get(i).get("type");
                            if ("STRING".equals(oldType)) {
                                break columns;
                            } else if ("BIGINT".equals(oldType)) {
                                if ("BIGINT".equals(newType)) {
                                    break columns;
                                } else if ("DOUBLE".equals(newType)) {
                                    allTypes.get(i).put("type", "DOUBLE");
                                } else if ("STRING".equals(newType)) {
                                    allTypes.get(i).put("type", "STRING");
                                }
                            } else if ("DOUBLE".equals(oldType)) {
                                if ("BIGINT".equals(newType)) {
                                    break columns;
                                } else if ("DOUBLE".equals(newType)) {
                                    break columns;
                                } else if ("STRING".equals(newType)) {
                                    allTypes.get(i).put("type", "STRING");
                                }
                            }
                        }
                    }
                }
            }
            return allTypes;
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                fs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyList();
	}

	/**
	 * 读取CSV内容
	 * @param path
	 * @param count
	 * @return
	 */
	public static List<Map<String, Object>> readCSV(String path, int count) {
        if (!path.endsWith(".csv")) {
            return Collections.emptyList();
        }
        FileSystem fs = getFileSystem();
        InputStream in = null;
        try {
            in = fs.open(new Path(path));
            CsvReader reader = new CsvReader(in, ',', Charset.forName("UTF-8"));
            String[] header, line;
            reader.readHeaders();
            line = reader.getHeaders();
            header = line;

            List<Map<String, Object>> list = new ArrayList<>();
            while (count != 0 && line != null) {
                reader.readRecord();
                line = reader.getValues();

                Map<String, Object> map = new LinkedHashMap<>();
                if (null == line && list.isEmpty()) {
                    for (String aHeader : header) {
                        map.put(aHeader, "");
                    }
                } else if (null != line && line.length == 0) {
					return list;
				} else {
                    for (int i = 0; i < header.length; i++) {
                        map.put(header[i], line[i]);
                    }
                }
                list.add(map);
                count--;
            }
            return list;
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyList();
	}
	
	


	public static boolean makedir(String pathStr) {
		FileSystem fs = getFileSystem();
		try {
			return fs.mkdirs(new Path(pathStr));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String copyToLocalFile(String src, String dir) {
		FileSystem fs = getFileSystem();
		try {
			String name = src.substring(src.lastIndexOf("/") + 1);
			String dst = dir + "/" + name;
			File directory = new File(dir);
			if (!directory.exists()) {
				boolean isSuccessed = directory.mkdirs();
				if(!isSuccessed) {
					log.error(dir + "目录创建失败");
				}
			}
			fs.copyToLocalFile(false,new Path(src), new Path(dst),true);
			return dst;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void  rename(String src,String dst){
		FileSystem fs=getFileSystem();
		try {
			fs.rename(new Path(src), new Path(dst));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		} 
	}
	
	public static boolean exist(String path) {
		FileSystem fs = getFileSystem();
		try {
			return fs.exists(new Path(path));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	// 用于首次登陆时，通过管理员权限检查路径是否存在
	public static boolean existByAdmin(String path, FileSystem fs) {
		try {
			return fs.exists(new Path(path));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void copyFileAsStream(String fpath, OutputStream out) throws IOException, InterruptedException {
        Path path = new Path(fpath);
        FileSystem fs = getFileSystem();
        FSDataInputStream fsInput = fs.open(path);
        IOUtils.copyBytes(fsInput, out, 4096, false);
        fsInput.close();
        out.flush();
    }

	public static boolean isDirectory(String path) {
		FileSystem fs = getFileSystem();
		try {
			return fs.isDirectory(new Path(path));
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
