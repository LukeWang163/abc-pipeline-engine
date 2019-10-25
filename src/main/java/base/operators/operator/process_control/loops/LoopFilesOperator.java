package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.operator.Annotations;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nio.file.SimpleFileObject;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDirectory;
import base.operators.parameter.ParameterTypeRegexp;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.LogService;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class LoopFilesOperator
        extends AbstractLoopOperator<Path>
{
    private static final String PARAMETER_DIRECTORY = "directory";
    private static final String PARAMETER_FILTER_TYPE = "filter_type";
    private static final String PARAMETER_FILTER_REGEXP = "filter_by_regex";
    private static final String PARAMETER_FILTER_GLOB = "filter_by_glob";
    private static final String PARAMETER_RECURSIVE = "recursive";
    private static final String PARAMETER_ENABLE_MACROS = "enable_macros";
    private static final String PARAMETER_FILE_NAME_MACRO = "macro_for_file_name";
    private static final String PARAMETER_FILE_TYPE_MACRO = "macro_for_file_type";
    private static final String PARAMETER_FOLDER_NAME_MACRO = "macro_for_folder_name";
    private static final int NON_RECURSIVE_DEPTH = 1;

    private enum FilterMethod
    {
        GLOB("glob"), REGEX("regex");

        private String filterType;
        FilterMethod(String filterType) { this.filterType = filterType; }

        public String getFilterType() { return this.filterType; }

        public static FilterMethod getValueOf(String value) {
            for (FilterMethod method : values()) {
                if (method.getFilterType().matches(value)) {
                    return method;
                }
            }

            return GLOB;
        }

        public static String[] getValuesAsString() { return new String[] { GLOB.getFilterType(), REGEX.getFilterType() }; }
    }

    private final OutputPort fileObjectInnerSource = (OutputPort)getSubprocess(0).getInnerSources().createPort("file object");

    public LoopFilesOperator(OperatorDescription description) { super(description, new String[] { "File Processing" }); }

    @Override
    protected AbstractLoopOperator.LoopArguments<Path> prepareArguments(boolean executeParallely) throws OperatorException {
        List<Path> pathList = null;
        boolean recursive = getParameterAsBoolean("recursive");
        FilterMethod filterType = FilterMethod.getValueOf(getParameterAsString("filter_type"));
        pathList = getFiles(getParameterAsString("directory"),
                getParameterAsString((filterType == FilterMethod.GLOB) ? "filter_by_glob" : "filter_by_regex"), filterType, recursive);

        AbstractLoopOperator.LoopArguments<Path> arguments = new AbstractLoopOperator.LoopArguments<Path>();
        arguments.setDataForIteration(pathList);
        arguments.setNumberOfIterations(pathList.size());
        if (getParameterAsBoolean("enable_macros")) {
            arguments.setMacros(new HashMap());
            String nameMacro = getParameterAsString("macro_for_file_name");
            String typeMacro = getParameterAsString("macro_for_file_type");
            String folderMacro = getParameterAsString("macro_for_folder_name");

            if (nameMacro != null && !nameMacro.trim().isEmpty()) {
                arguments.getMacros().put("macro_for_file_name", nameMacro);
            }
            if (folderMacro != null && !folderMacro.trim().isEmpty()) {
                arguments.getMacros().put("macro_for_file_type", typeMacro);
            }
            if (typeMacro != null && !typeMacro.trim().isEmpty()) {
                arguments.getMacros().put("macro_for_folder_name", folderMacro);
            }
            if (arguments.getMacros().isEmpty()) {
                arguments.setMacros(null);
            }
        }
        return arguments;
    }

    @Override
    protected void setMacros(AbstractLoopOperator.LoopArguments<Path> arguments, MacroHandler macroHandler, int iteration) {
        if (arguments.getMacros() == null) {
            return;
        }
        String nameMacro = (String)arguments.getMacros().get("macro_for_file_name");
        String typeMacro = (String)arguments.getMacros().get("macro_for_file_type");
        String folderMacro = (String)arguments.getMacros().get("macro_for_folder_name");

        Path path = (Path)arguments.getDataForIteration().get(iteration);
        if (nameMacro != null) {
            macroHandler.addMacro(nameMacro, path.getFileName().toString());
        }
        if (folderMacro != null) {
            macroHandler.addMacro(folderMacro, path.getParent().toString());
        }
        if (typeMacro != null) {
            String typeValue = path.getFileName().toString();
            int lastIndex = typeValue.lastIndexOf('.');
            if (lastIndex >= 0) {
                typeValue = typeValue.substring(lastIndex + 1, typeValue.length());
            } else {
                typeValue = "";
            }
            macroHandler.addMacro(typeMacro, typeValue);
        }
    }

    @Override
    protected void prepareSingleRun(Path dataForIteration, AbstractLoopOperator<Path> operator) throws OperatorException {
        LoopFilesOperator castOperator = (LoopFilesOperator)operator;
        if (castOperator.fileObjectInnerSource.isConnected()) {
            IOObject ioObject = convertToIOObject(dataForIteration);
            castOperator.fileObjectInnerSource.deliver(ioObject);
        }
    }

    private IOObject convertToIOObject(Path path) {
        SimpleFileObject fileObject = new SimpleFileObject(path.toFile());
        Annotations annotations = fileObject.getAnnotations();
        annotations.setAnnotation("Filename", path.getFileName().toString());
        annotations.setAnnotation("Source", path.toString());
        return fileObject;
    }

    private List<Path> getFiles(String startDirectory, final String filterString, FilterMethod filterType, boolean recursive) {
        final List<Path> files = new ArrayList<Path>();

        FileSystem fileSystem = FileSystems.getDefault();
        final PathMatcher pathMatcher = fileSystem.getPathMatcher(filterType.getFilterType() + ":" + filterString);
        String path = startDirectory;

        Path p = fileSystem.getPath(startDirectory, new String[0]);
        if (!p.isAbsolute()) {
            String homeName = System.getProperty("user.home");
            if (homeName != null) {
                path = homeName + File.separator + startDirectory;
            }
        }

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (!attrs.isDirectory() && (filterString == null || filterString
                        .isEmpty() || pathMatcher.matches(file.getFileName()))) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            if (recursive) {
                Files.walkFileTree(fileSystem.getPath(path, new String[0]), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 2147483647, visitor);
            } else {

                Files.walkFileTree(fileSystem.getPath(path, new String[0]), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, visitor);
            }

        } catch (IOException e) {
            LogService.getRoot().log(Level.WARNING, "Failed to loop over file!", e);
        }

        return files;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        ParameterTypeDirectory parameterTypeDirectory = new ParameterTypeDirectory("directory", "Select the directory from where to start scanning for files.", false);

        parameterTypeDirectory.setPrimary(true);
        types.add(parameterTypeDirectory);

        types.add(new ParameterTypeCategory("filter_type", "Specifies how to filter file names. You can either use standard, command shell like glob filtering or a regular expression.",
                FilterMethod.getValuesAsString(), 0, false));
        ParameterTypeString parameterTypeString2 = new ParameterTypeString("filter_by_glob", "Specifies a glob expression which is used as filter for the file and directory names. It is more simple than regular expressions. A detailed explanation can be found in the help, but it is as used from operating systems. Ignored if empty.", true, false);
        parameterTypeString2.registerDependencyCondition(new EqualStringCondition(this, "filter_type", false, new String[] {FilterMethod.GLOB.getFilterType()}));
        types.add(parameterTypeString2);
        ParameterTypeRegexp parameterTypeRegexp = new ParameterTypeRegexp("filter_by_regex", "Specifies a regular expression which is used as filter for the file and directory names, e.g. 'a.*b' for all files starting with 'a' and ending with 'b'. Ignored if empty.", true, false);
        parameterTypeRegexp.registerDependencyCondition(new EqualStringCondition(this, "filter_type", false, new String[] {FilterMethod.REGEX.getFilterType() }));
        types.add(parameterTypeRegexp);
        types.add(new ParameterTypeBoolean("recursive", "Set whether to recursively search every directory. If set to true, the operator will include files inside sub-directories (and sub-sub-directories ...) of the selected directory.", false, false));
        types.add(new ParameterTypeBoolean("enable_macros", "If this parameter is enabled, you can name and extract three macros (for file name, file type and file folder)and use them in your subprocess.", false));
        ParameterTypeString parameterTypeString1 = new ParameterTypeString("macro_for_file_name", "If filled, a macro with this name will be set to the name of the current entry. To get access on the full path including the containing directory, combine this with the folder macro. Can be left blank.", "file_name", true);
        parameterTypeString1.registerDependencyCondition(new BooleanParameterCondition(this, "enable_macros", false, true));
        types.add(parameterTypeString1);
        parameterTypeString1 = new ParameterTypeString("macro_for_file_type", "Will be set to the file's extension. Can be left blank.", "file_type", true);
        parameterTypeString1.registerDependencyCondition(new BooleanParameterCondition(this, "enable_macros", false, true));
        types.add(parameterTypeString1);
        parameterTypeString1 = new ParameterTypeString("macro_for_folder_name", "If filled, a macro with this name will be set to the containing folder of the current file. To get access on the full path you can combine this with the name macro. Can be left blank.", "folder_name", true);
        parameterTypeString1.registerDependencyCondition(new BooleanParameterCondition(this, "enable_macros", false, true));
        types.add(parameterTypeString1);
        for (ParameterType currentType : types) {
            currentType.setExpert(false);
        }
        types.addAll(super.getParameterTypes());
        return types;
    }
}

