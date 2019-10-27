package base.operators.operator.etl.trans;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.etl.trans.ftp.FTPUtils;
import base.operators.operator.etl.trans.ftp.MVSFileParser;
import base.operators.operator.etl.trans.ftp.PDIFTPClient;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.preprocessing.filter.ExampleFilter;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Ontology;
import com.enterprisedt.net.ftp.*;


import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPTraverseOperator extends Operator {

    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public FTPTraverseOperator(OperatorDescription description) {
        super(description);
    }

    private String serverName;
    private String serverPort;
    private String userName;
    private String password;

    private String proxyHost;
    private String proxyPort; /* string to allow variable substitution */
    private String proxyUsername;
    private String proxyPassword;

    private String remoteDirectory;
    private String wildcard;

    private boolean binaryMode;
    private int timeout;
    private boolean activeConnection;
    private String controlEncoding; /* how to convert list of filenames e.g. */

    private String socksProxyHost;
    private String socksProxyPort;
    private String socksProxyUsername;
    private String socksProxyPassword;

    private static final String[] control_encodings_en = {"ISO-8859-1","US-ASCII","UTF-8","UTF-16BE","UTF-16LE","UTF-16"};

    private static final String FTP_SERVER_ADDRESS = "ftp_server_address";
    private static final String FTP_SERVER_PORT = "ftp_server_port";
    private static final String USER_NAME = "user_name";
    private static final String PASSWORD = "password";

    private static final String USE_PROXY = "use_proxy";
    private static final String PROXY_HOST = "proxy_host";
    private static final String PROXY_PORT = "proxy_port";
    private static final String PROXY_USER_NAME = "proxy_user_name";
    private static final String PROXY_PASSWORD = "proxy_password";

    private static final String REMOTE_DIRECTORY = "remote_directory";
    private static final String FILE_NAME_MATCHING_REGEX = "file_name_matching_regex";
    private static final String BINARY_MODE = "binary_mode";
    private static final String TIMEOUT = "timeout";
    private static final String ACTIVE_CONNECTION = "active_connection";
    private static final String CONTROL_ENCODING = "control_encoding";

    private static final String USE_SOCKS_PROXY = "use_socks_proxy";
    private static final String SOCKS_PROXY_HOST = "socks_proxy_host";
    private static final String SOCKS_PROXY_PORT = "socks_proxy_port";
    private static final String SOCKS_PROXY_USER_NAME = "socks_proxy_user_name";
    private static final String SOCKS_PROXY_PASSWORD = "socks_proxy_password";

    static String FILE_SEPARATOR = "/";
    public int index = 0;
    public String ftp_home = "/home/test/";
    Map<String, Long> subFolderTime;
    //存储路径以及该路径到根节点的距离，以方便进行子文件夹大小计算时，从最底端的子文件夹开始计算，再计算上一层父文件夹的大小
    Map<String, Integer> subFolderDistance;


    @Override
    public void doWork() throws OperatorException {
        index = 0;
        serverName = getParameterAsString(FTP_SERVER_ADDRESS);
        serverPort = getParameterAsString(FTP_SERVER_PORT);
        userName = getParameterAsString(USER_NAME);
        password = getParameterAsString(PASSWORD);

        if(getParameterAsBoolean(USE_PROXY)){
            proxyHost = getParameterAsString(PROXY_HOST);
            proxyPort = getParameterAsString(PROXY_PORT);
            proxyUsername = getParameterAsString(PROXY_USER_NAME);
            proxyPassword = getParameterAsString(PROXY_PASSWORD);
        }

        remoteDirectory = getParameterAsString(REMOTE_DIRECTORY);
        try {
            remoteDirectory = normalizePath(remoteDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
        wildcard = getParameterAsString(FILE_NAME_MATCHING_REGEX);

        binaryMode = getParameterAsBoolean(BINARY_MODE);
        timeout = getParameterAsInt(TIMEOUT);
        activeConnection = getParameterAsBoolean(ACTIVE_CONNECTION);
        controlEncoding = getParameterAsString(CONTROL_ENCODING);
        if(getParameterAsBoolean(USE_SOCKS_PROXY)){
            socksProxyHost = getParameterAsString(SOCKS_PROXY_HOST);
            socksProxyPort = getParameterAsString(SOCKS_PROXY_PORT);
            socksProxyUsername = getParameterAsString(SOCKS_PROXY_USER_NAME);
            socksProxyPassword = getParameterAsString(SOCKS_PROXY_PASSWORD);
        }

        subFolderTime = new HashMap<>();
        subFolderDistance = new HashMap<>();

        List<Attribute> attributeList = new ArrayList<>();
        Attribute id_attribute = AttributeFactory.createAttribute("id", Ontology.INTEGER);
        attributeList.add(id_attribute);
        Attribute path_attribute = AttributeFactory.createAttribute("path", Ontology.STRING);
        attributeList.add(path_attribute);
        Attribute parent_path_attribute = AttributeFactory.createAttribute("parent_path", Ontology.STRING);
        attributeList.add(parent_path_attribute);
        Attribute size_attribute = AttributeFactory.createAttribute("size(byte)", Ontology.NUMERICAL);
        attributeList.add(size_attribute);
        Attribute modify_attribute = AttributeFactory.createAttribute("last_modify_time", Ontology.DATE_TIME);
        attributeList.add(modify_attribute);
        Attribute type_attribute = AttributeFactory.createAttribute("type", Ontology.STRING);
        attributeList.add(type_attribute);
        Attribute is_file_attribute = AttributeFactory.createAttribute("is_file", Ontology.BINOMINAL);
        attributeList.add(is_file_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        FTPClient ftpclient = null;
        try {
            // Create ftp client to host:port ...
            ftpclient = FTPUtils.createAndSetUpFtpClient(serverName, serverPort, userName, password, proxyHost, proxyPort, proxyUsername, proxyPassword, activeConnection, timeout, controlEncoding, socksProxyHost, socksProxyPort, socksProxyUsername, socksProxyPassword);

            // set BINARY
            if ( binaryMode ) {
                ftpclient.setType( FTPTransferType.BINARY );
            }

            // Fix for PDI-2534 - add auxilliary FTP File List parsers to the ftpclient object.
            String system = ftpclient.system();
            MVSFileParser parser = new MVSFileParser();
            FTPFileFactory factory = new FTPFileFactory( system );
            factory.addParser( parser );
            ftpclient.setFTPFileFactory( factory );

            listFile(ftpclient, remoteDirectory, attributeList, exampleTable);

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            if ( ftpclient != null && ftpclient.connected() ) {
                try {
                    ftpclient.quit();
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }

            FTPClient.clearSOCKS();
        }
        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        //下边添加文件夹列，主要是计算子文件夹的大小
        ArrayList<Map.Entry<String, Integer>> subFolderDistanceSorted = new ArrayList<Map.Entry<String, Integer>>(subFolderDistance.entrySet());
        //排序
        Collections.sort(subFolderDistanceSorted, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> map1, Map.Entry<String, Integer> map2) {
                return (map2.getValue() - map1.getValue());
            }
        });
        int size = exampleTable.size();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        for(Map.Entry<String, Integer> entry : subFolderDistanceSorted){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(attributeList.get(0), size);
            size++;
            dataRow.set(attributeList.get(1), attributeList.get(1).getMapping().mapString(entry.getKey()));
            dataRow.set(attributeList.get(2), attributeList.get(2).getMapping().mapString(entry.getKey().substring(0, entry.getKey().lastIndexOf(FILE_SEPARATOR))));
            dataRow.set(attributeList.get(4), subFolderTime.get(entry.getKey()));
            dataRow.set(attributeList.get(5), attributeList.get(5).getMapping().mapString("floder"));
            dataRow.set(attributeList.get(6), attributeList.get(6).getMapping().mapString("false"));
            //按照路径path筛选父路径parent_path,计算子文件夹大小
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", parent_path_attribute.getName()+"="+entry.getKey());
            ExampleSet filterExampleSet = examplesFilter.apply(exampleSet);
            filterExampleSet.recalculateAllAttributeStatistics();
            double value = filterExampleSet.getStatistics(size_attribute, Statistics.SUM);
            dataRow.set(attributeList.get(3), value);
            exampleTable.addDataRow(dataRow);
        }
        exampleSetOutput.deliver(exampleSet);
    }

    public void listFile(FTPClient ftpClient, String dir, List<Attribute> attributes, MemoryExampleTable exampleTable){
        Pattern pattern;
        if ( !( wildcard ==null) ) {
            pattern = Pattern.compile( wildcard );
        } else {
            pattern = null;
        }
        if (!"".equals( dir )) {
            try {
                //更换目录到当前目录
                // move to spool dir ...
                ftpClient.chdir(dir);
                FTPFile[] files = ftpClient.dirDetails(null);
                for (FTPFile file : files) {
                    if (file.isFile()) {
                        boolean toBeProcessed = true;
                        // First see if the file matches the regular expression!
                        if ( pattern != null ) {
                            Matcher matcher = pattern.matcher( file.getName() );
                            toBeProcessed = matcher.matches();
                        }
                        if(toBeProcessed){
                            DataRowFactory factory = new DataRowFactory(0, '.');
                            DataRow dataRow = factory.create(attributes.size());
                            dataRow.set(attributes.get(0), index);
                            index++;
                            String filePath = file.getPath() + FILE_SEPARATOR + file.getName();
                            dataRow.set(attributes.get(1), attributes.get(1).getMapping().mapString(filePath));
                            dataRow.set(attributes.get(2), attributes.get(2).getMapping().mapString(filePath.substring(0, filePath.lastIndexOf(FILE_SEPARATOR))));
                            dataRow.set(attributes.get(3), file.size());
                            dataRow.set(attributes.get(4), file.lastModified().getTime());
                            dataRow.set(attributes.get(5), attributes.get(5).getMapping().mapString(file.getName().lastIndexOf( '.' )==-1 ? "unknow" : file.getName().substring(file.getName().lastIndexOf( '.' )+1)));
                            dataRow.set(attributes.get(6), attributes.get(6).getMapping().mapString("true"));
                            exampleTable.addDataRow(dataRow);
                        }
                    } else if (file.isDir()) {
                        // 需要加此判断。否则，ftp默认将‘项目文件所在目录之下的目录（./）’与‘项目文件所在目录向上一级目录下的目录（../）’都纳入递归，这样下去就陷入一个死循环了。需将其过滤掉。
                        if (!".".equals(file.getName()) && !"..".equals(file.getName())) {
                            String subDir = file.getPath()+FILE_SEPARATOR+file.getName();
                            subFolderTime.put(subDir, file.lastModified().getTime());
                            subFolderDistance.put(subDir, subDir.split(FILE_SEPARATOR.equals("\\")?"\\\\":FILE_SEPARATOR).length);
                            listFile(ftpClient, subDir, attributes, exampleTable);
                            ftpClient.chdir(dir);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else{
            try {
                throw new UserError(this, "Remote directory should start with '/' and end with '/'.");
            } catch (UserError userError) {
                userError.printStackTrace();
            }
        }
    }

    /**
     * normalize / to \ and remove trailing slashes from a path
     *
     * @param path
     * @return normalized path
     * @throws Exception
     */
    public String normalizePath( String path ) throws Exception {

        String normalizedPath = path.replaceAll( "\\\\", FILE_SEPARATOR );
        while ( normalizedPath.endsWith( FILE_SEPARATOR ) ) {
            normalizedPath = normalizedPath.substring( 0, normalizedPath.length() - 1 );
        }
        return normalizedPath;
    }


    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString(FTP_SERVER_ADDRESS,"The address of FTP server.", false,false));
        types.add(new ParameterTypeString(FTP_SERVER_PORT,"The port of FTP server.", "21",false));
        types.add(new ParameterTypeString(USER_NAME,"The user name of FTP server.", false,false));
        types.add(new ParameterTypePassword(PASSWORD,"The password of FTP server."));

        types.add(new ParameterTypeBoolean(USE_PROXY,"Whether to use proxy.", false, false));
        ParameterType type = new ParameterTypeString(PROXY_HOST,"The address of proxy.", true,false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_PROXY, false,true));
        types.add(type);
        type = new ParameterTypeString(PROXY_PORT,"The port of proxy.", true,false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_PROXY, false,true));
        types.add(type);
        type = new ParameterTypeString(PROXY_USER_NAME,"The user name of proxy.", true,false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_PROXY, false,true));
        types.add(type);
        type = new ParameterTypePassword(PROXY_PASSWORD,"The password of proxy.");
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_PROXY, false,true));
        types.add(type);

        types.add(new ParameterTypeString(REMOTE_DIRECTORY,"The path of FTP to traverse", false,false));
        types.add(new ParameterTypeRegexp(FILE_NAME_MATCHING_REGEX,"The regex to match file name.", true,false));
        types.add(new ParameterTypeBoolean(BINARY_MODE,"The mode of FTP transfer type.", false,false));
        types.add(new ParameterTypeInt(TIMEOUT,"Connection timeout settings.",1, Integer.MAX_VALUE, 0,false));
        types.add(new ParameterTypeBoolean(ACTIVE_CONNECTION,"Use active FTP connection.", false,false));
        types.add(new ParameterTypeCategory(CONTROL_ENCODING,"Control encoding.", control_encodings_en, 0,false));

        types.add(new ParameterTypeBoolean(USE_SOCKS_PROXY,"Whether to use socks proxy.", false, false));
        type = new ParameterTypeString(SOCKS_PROXY_HOST,"The address of socks proxy.", true,false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_SOCKS_PROXY, false,true));
        types.add(type);
        type = new ParameterTypeString(SOCKS_PROXY_PORT,"The port of socks proxy.", "1080",false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_SOCKS_PROXY, false,true));
        types.add(type);
        type = new ParameterTypeString(SOCKS_PROXY_USER_NAME,"The user name of socks proxy.", true,false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_SOCKS_PROXY, false,true));
        types.add(type);
        type = new ParameterTypePassword(SOCKS_PROXY_PASSWORD,"The password of socks proxy.");
        type.registerDependencyCondition(new BooleanParameterCondition(this, USE_SOCKS_PROXY, false,true));
        types.add(type);

        return types;
    }
}
