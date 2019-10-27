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
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.preprocessing.filter.ExampleFilter;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPFileFactory;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.google.common.annotations.VisibleForTesting;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPGetOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public FTPGetOperator(OperatorDescription description) {
        super(description);
    }
    //服务器
    private String serverName;
    private String serverPort;
    private String userName;
    private String password;

    private String proxyHost;
    private String proxyPort; /* string to allow variable substitution */
    private String proxyUsername;
    private String proxyPassword;

    private boolean binaryMode;
    private int timeout;
    private boolean activeConnection;
    private String controlEncoding; /* how to convert list of filenames e.g. */
    //socks代理
    private String socksProxyHost;
    private String socksProxyPort;
    private String socksProxyUsername;
    private String socksProxyPassword;
    //远程
    private String remoteDirectory;
    private String wildcard;
    private boolean whetherDeleteRemoteFile;
    private boolean whetherMoveRemoteFile;
    private String movetodirectory;
    private boolean whetherNewFolder;
    //本地
    private String localTargetDirectory;
    private boolean adddate;
    private boolean addtime;
    private boolean isSpecifyDateTimeFormat;
    private String date_time_format;
    private boolean notOverwriteFiles; /* Don't overwrite files */
    public int ifFileExists;

    static String REMOTE_FILE_SEPARATOR;
    static String LOCAL_FILE_SEPARATOR;
    private static SimpleDateFormat daf;
    String targetFilename = null;
    long NrfilesRetrieved = 0;
    int size;
    Map<String, Long> subFolderTime;
    //存储路径以及该路径到根节点的距离，以方便进行子文件夹大小计算时，从最底端的子文件夹开始计算，再计算上一层父文件夹的大小
    Map<String, Integer> subFolderDistance;

    private static final String FTP_SERVER_ADDRESS = "ftp_server_address";
    private static final String FTP_SERVER_PORT = "ftp_server_port";
    private static final String USER_NAME = "user_name";
    private static final String PASSWORD = "password";

    private static final String USE_PROXY = "use_proxy";
    private static final String PROXY_HOST = "proxy_host";
    private static final String PROXY_PORT = "proxy_port";
    private static final String PROXY_USER_NAME = "proxy_user_name";
    private static final String PROXY_PASSWORD = "proxy_password";

    private static final String BINARY_MODE = "binary_mode";
    private static final String TIMEOUT = "timeout";
    private static final String ACTIVE_CONNECTION = "active_connection";
    private static final String CONTROL_ENCODING = "control_encoding";
    private static final String[] control_encodings_en = {"ISO-8859-1","US-ASCII","UTF-8","UTF-16BE","UTF-16LE","UTF-16"};

    private static final String USE_SOCKS_PROXY = "use_socks_proxy";
    private static final String SOCKS_PROXY_HOST = "socks_proxy_host";
    private static final String SOCKS_PROXY_PORT = "socks_proxy_port";
    private static final String SOCKS_PROXY_USER_NAME = "socks_proxy_user_name";
    private static final String SOCKS_PROXY_PASSWORD = "socks_proxy_password";

    private static final String REMOTE_DIRECTORY = "remote_directory";
    private static final String FILE_NAME_MATCHING_REGEX = "file_name_matching_regex";
    private static final String WHETHER_DELETE_REMOTE_FILE = "whether_delete_remote_file";
    private static final String WHETHER_MOVE_REMOTE_FILE = "whether_move_remote_file";
    private static final String MOVE_TO_DIRECTORY = "move_to_directory";
    private static final String WHETHER_NEW_FOLDER = "whether_new_folder";

    private static final String LOCAL_TARGET_DIRECTORY = "local_target_directory";
    private static final String SET_DATE_TIME_FORMAT = "set_date_time_format";
    private final String[]  date_time_mode = {"file_name_does_not_contain_date_time","file_name_contain_date(yyyyMMdd)","file_name_contain_time(HHmmssSSS)", "file_name_contain_date_and_time"};
    private static final String DATE_TIME_FORMAT = "date_time_format";
    //    private final String[] dateTimeFormat = {
//            "yyyyMMddHHmmss",
//            "yyyy/MM/dd",
//            "yyyy-MM-dd",
//            "yyyyMMdd",
//            "MM/dd/yyyy",
//            "MM-dd-yyyy",
//            "MM/dd/yy",
//            "MM-dd-yy",
//            "dd/MM/yyyy",
//            "dd-MM-yyyy"};
    private final String[] dateTimeFormat = {
            "yyyyMMddHHmmss",
            "yyyy-MM-dd",
            "yyyyMMdd",
            "MM-dd-yyyy",
            "MM-dd-yy",
            "dd-MM-yyyy"};

    private static final String NOT_OVERWRITE_FILES = "not_overwrite_files";

    private static final String  IF_FILE_EXIST = "if_file_exist";
    private final String[] file_exist_operation_en = {"skip","give unique name","fail"};

    @Override
    public void doWork() throws OperatorException {
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

        remoteDirectory = getParameterAsString(REMOTE_DIRECTORY);
        REMOTE_FILE_SEPARATOR = remoteDirectory.indexOf("/")==-1? "\\" :"/";
        if(remoteDirectory.endsWith(REMOTE_FILE_SEPARATOR)){
            remoteDirectory  = remoteDirectory.substring(0,remoteDirectory.length()-1);
        }else if("/".equals(REMOTE_FILE_SEPARATOR)&&!remoteDirectory.startsWith(REMOTE_FILE_SEPARATOR)){
            remoteDirectory = REMOTE_FILE_SEPARATOR+remoteDirectory;
        }

        wildcard = getParameterAsString(FILE_NAME_MATCHING_REGEX);
        whetherDeleteRemoteFile = getParameterAsBoolean(WHETHER_DELETE_REMOTE_FILE);
        whetherMoveRemoteFile = getParameterAsBoolean(WHETHER_MOVE_REMOTE_FILE);
        movetodirectory = getParameterAsString(MOVE_TO_DIRECTORY);

        whetherNewFolder = getParameterAsBoolean(WHETHER_NEW_FOLDER);

        localTargetDirectory = getParameterAsString(LOCAL_TARGET_DIRECTORY);
        LOCAL_FILE_SEPARATOR = localTargetDirectory.indexOf("/")==-1? "\\" :"/";

        if(localTargetDirectory.endsWith(LOCAL_FILE_SEPARATOR)){
            localTargetDirectory  = localTargetDirectory.substring(0,localTargetDirectory.length()-1);
        }else if("/".equals(LOCAL_FILE_SEPARATOR)&&!localTargetDirectory.startsWith(LOCAL_FILE_SEPARATOR)){
            localTargetDirectory = LOCAL_FILE_SEPARATOR+localTargetDirectory;
        }

        adddate = getParameterAsInt(SET_DATE_TIME_FORMAT)==1;
        addtime = getParameterAsInt(SET_DATE_TIME_FORMAT)==2;
        isSpecifyDateTimeFormat = getParameterAsInt(SET_DATE_TIME_FORMAT)==3;
        date_time_format = getParameterAsString(DATE_TIME_FORMAT);
        notOverwriteFiles = getParameterAsBoolean(NOT_OVERWRITE_FILES);
        ifFileExists = getParameterAsInt(IF_FILE_EXIST);
        size = 0;

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

            ftpclient.chdir( remoteDirectory );

            if(movetodirectory!=null){
                // Folder exists?
                boolean folderExist = true;
                String originalLocation = ftpclient.pwd();
                try {
                    // does not work for folders, see PDI-2567: folderExist=ftpclient.exists(realMoveToFolder);
                    // try switching to the 'move to' folder.
                    ftpclient.chdir(movetodirectory);
                    // Switch back to the previous location.
                    ftpclient.chdir( originalLocation );
                } catch ( Exception e ) {
                    folderExist = false;
                    // Assume folder does not exist !!
                }

                if ( !folderExist ) {
                    if ( whetherNewFolder ) {
                        ftpclient.mkdir( movetodirectory );
                    }
                }
            }

            // set transfertype ...
            if ( binaryMode ) {
                ftpclient.setType( FTPTransferType.BINARY );
            } else {
                ftpclient.setType( FTPTransferType.ASCII );
            }

            Pattern pattern = null;
            if (wildcard!=null) {
                pattern = Pattern.compile( wildcard );
            }
            downloadFile(ftpclient, remoteDirectory, pattern, movetodirectory, attributeList, exampleTable);

        }catch ( Exception e ) {
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
        int number = exampleTable.size();
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
            dataRow.set(attributeList.get(0), number);
            number++;
            dataRow.set(attributeList.get(1), attributeList.get(1).getMapping().mapString(entry.getKey()));
            dataRow.set(attributeList.get(2), attributeList.get(2).getMapping().mapString(entry.getKey().substring(0, entry.getKey().lastIndexOf(LOCAL_FILE_SEPARATOR))));
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

    private void downloadFile( FTPClient ftpclient, String path, Pattern pattern, String realMoveToFolder, List<Attribute> attributes, MemoryExampleTable exampleTable) throws Exception {
        ftpclient.chdir(path);
        FTPFile[] ftpFiles = ftpclient.dirDetails( null );
        // Get the files in the list...
        for ( FTPFile ftpFile : ftpFiles ) {
            if(ftpFile.isDir()){
                String localDir = (localTargetDirectory+path.replace(remoteDirectory,"") + LOCAL_FILE_SEPARATOR + ftpFile.getName()).replace("\\", LOCAL_FILE_SEPARATOR).replace("/", LOCAL_FILE_SEPARATOR);
                subFolderTime.put(localDir, ftpFile.lastModified().getTime());
                subFolderDistance.put(localDir, localDir.split(LOCAL_FILE_SEPARATOR.equals("\\")?"\\\\":LOCAL_FILE_SEPARATOR).length);
                downloadFile(ftpclient, ftpFile.getPath()+REMOTE_FILE_SEPARATOR+ftpFile.getName(), pattern, realMoveToFolder, attributes, exampleTable);
                ftpclient.chdir(path);
            }else{
                boolean getIt = true;
                if ( pattern != null ) {
                    Matcher matcher = pattern.matcher( ftpFile.getName() );
                    getIt = matcher.matches();
                }
                if(getIt){
                    String localFilename = ftpFile.getName();
                    targetFilename = returnTargetFilename(localFilename, path);
                    if ( ( !notOverwriteFiles ) || ( notOverwriteFiles && needsDownload( targetFilename ) ) ) {
                        File file = new File(targetFilename.substring(0, targetFilename.lastIndexOf(LOCAL_FILE_SEPARATOR)+1));
                        //如果路径不存在，新建
                        if(!file.exists()&&!file.isDirectory()) {
                            file.mkdirs();
                        }
                        ftpclient.get( targetFilename, ftpFile.getName() );
                        DataRowFactory dataRowFactory = new DataRowFactory(0, '.');
                        DataRow dataRow = dataRowFactory.create(attributes.size());
                        dataRow.set(attributes.get(0), size);
                        size++;
                        dataRow.set(attributes.get(1), attributes.get(1).getMapping().mapString(targetFilename));
                        dataRow.set(attributes.get(2), attributes.get(2).getMapping().mapString(targetFilename.substring(0, targetFilename.lastIndexOf(LOCAL_FILE_SEPARATOR))));
                        dataRow.set(attributes.get(3), ftpFile.size());
                        dataRow.set(attributes.get(4), ftpFile.lastModified().getTime());
                        dataRow.set(attributes.get(5), attributes.get(5).getMapping().mapString(ftpFile.getName().lastIndexOf(".")==-1?"unknow":ftpFile.getName().substring(ftpFile.getName().lastIndexOf(".")+1)));
                        dataRow.set(attributes.get(6), attributes.get(6).getMapping().mapString("true"));
                        exampleTable.addDataRow(dataRow);
                        // Update retrieved files
                        updateRetrievedFiles();
                        // Delete the file if this is needed!
                        if ( whetherDeleteRemoteFile ) {
                            ftpclient.delete( ftpFile.getName() );
                        } else if ( whetherMoveRemoteFile ) {
                            // Try to move file to destination folder ...
                            ftpclient.rename( ftpFile.getName(), realMoveToFolder + REMOTE_FILE_SEPARATOR + ftpFile.getName() );
                        }
                    }
                }
            }
        }
    }

    private void updateRetrievedFiles() {
        NrfilesRetrieved++;
    }

    /**
     * @param filename the filename from the FTP server
     *
     * @return the calculated target filename
     */
    @VisibleForTesting
    String returnTargetFilename( String filename, String directory) {
        String retval = null;
        // Replace possible environment variables...
        if ( filename != null ) {
            retval = filename;
        } else {
            return null;
        }

        int lenstring = retval.length();
        int lastindexOfDot = retval.lastIndexOf( "." );
        if ( lastindexOfDot == -1 ) {
            lastindexOfDot = lenstring;
        }

        String fileExtension = retval.substring( lastindexOfDot, lenstring );
        retval = retval.substring( 0, lastindexOfDot );

        if ( daf == null ) {
            daf = new SimpleDateFormat();
        }
        Date now = new Date();

        if ( isSpecifyDateTimeFormat && date_time_format!=null) {
            daf.applyPattern( date_time_format );
            String dt = daf.format( now );
            retval += "_" + dt;
        } else {
            if ( adddate ) {
                daf.applyPattern( "yyyyMMdd" );
                String d = daf.format( now );
                retval += "_" + d;
            }
            if ( addtime ) {
                daf.applyPattern( "HHmmssSSS" );
                String t = daf.format( now );
                retval += "_" + t;
            }
        }
        retval += fileExtension;

        // Add foldername to filename
        retval = localTargetDirectory + directory.replace(remoteDirectory,"") + LOCAL_FILE_SEPARATOR + retval;

        retval = retval.replace("\\", LOCAL_FILE_SEPARATOR).replace("/", LOCAL_FILE_SEPARATOR);

        return retval;
    }


    /**
     * See if the filename on the FTP server needs downloading. The default is to check the presence of the file in the
     * target directory. If you need other functionality, extend this class and build it into a plugin.
     *
     * @param filename
     *          The local filename to check
     * @return true if the file needs downloading
     */
    protected boolean needsDownload( String filename ) {
        boolean retval = false;
        File file = new File( filename );

        if ( !file.exists() ) {
            // Local file not exists!
            return true;
        } else {

            // Local file exists!
            if ( ifFileExists == 1 ) {

                // Create file with unique name
                int lenstring = targetFilename.length();
                int lastindexOfDot = targetFilename.lastIndexOf( '.' );
                if ( lastindexOfDot == -1 ) {
                    lastindexOfDot = lenstring;
                }
                targetFilename =
                        targetFilename.substring( 0, lastindexOfDot )
                                + new SimpleDateFormat("yyyyddMM_hhmmssSSS").format( new Date())
                                + targetFilename.substring( lastindexOfDot, lenstring );

                return true;
            } else if ( ifFileExists == 2 ) {
                try {
                    throw new UserError(this, "local file exists.");
                } catch (UserError userError) {
                    userError.printStackTrace();
                }

            }
        }

        return retval;
    }

    @Override
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

        types.add(new ParameterTypeString(REMOTE_DIRECTORY,"The path of FTP to traverse", false,false));
        types.add(new ParameterTypeRegexp(FILE_NAME_MATCHING_REGEX,"The regex to match file name.", true,false));
        types.add(new ParameterTypeBoolean(WHETHER_DELETE_REMOTE_FILE, "Whether to delete files after get those files.",false, false));
        types.add(new ParameterTypeBoolean(WHETHER_MOVE_REMOTE_FILE, "Whether to delete files after get those files.",false, false));
        type = new ParameterTypeString(MOVE_TO_DIRECTORY,"Move file to the directory.", true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WHETHER_MOVE_REMOTE_FILE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(WHETHER_NEW_FOLDER,"Whether create new folder when move file to the directory.", false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WHETHER_MOVE_REMOTE_FILE, false, true));
        types.add(type);

        types.add(new ParameterTypeString(LOCAL_TARGET_DIRECTORY,"Local target directory.",  false,false));
        types.add(new ParameterTypeCategory(SET_DATE_TIME_FORMAT,"Whether to set the format of date and time.", date_time_mode,0, false));
        type = new ParameterTypeCategory(DATE_TIME_FORMAT,"The format of date and time.", dateTimeFormat,0, false);
        type.registerDependencyCondition(new EqualStringCondition(this, SET_DATE_TIME_FORMAT, false, date_time_mode[3]));
        types.add(type);
//        type = new ParameterTypeBoolean(IS_ADD_DATE_BEFORE_EXTENSOIN_NAME,"Whether add date before extension name.", false, false);
//        type.registerDependencyCondition(new EqualStringCondition(this, SET_DATE_TIME_FORMAT, false, date_time_mode[3]));
//        types.add(type);

        types.add(new ParameterTypeBoolean(NOT_OVERWRITE_FILES, "Don't overwrite files.",false, false));
        type = new ParameterTypeCategory(IF_FILE_EXIST, "Which operation is selected when the file exists.", file_exist_operation_en,0, false );
        type.registerDependencyCondition(new BooleanParameterCondition(this, NOT_OVERWRITE_FILES, false, true));
        types.add(type);

        return types;
    }
}