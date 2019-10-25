package base.operators.operator.scripting.python;

import base.operators.RapidMiner;
import base.operators.operator.scripting.PluginInitPythonScripting;
import base.operators.operator.scripting.SetupTester;
import base.operators.operator.scripting.os.OSCommandRunner;
import base.operators.operator.scripting.os.SingletonOSCommandFactory;
import base.operators.operator.scripting.parameter.TestActionResult;
import base.operators.gui.tools.ProgressThread;
import base.operators.tools.I18N;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import base.operators.tools.SystemInfoUtilities;
import base.operators.tools.Tools;
import base.operators.tools.SystemInfoUtilities.OperatingSystem;
import base.operators.tools.config.actions.ActionResult;
import base.operators.tools.config.actions.SimpleActionResult;
import base.operators.tools.config.actions.ActionResult.Result;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class PythonSetupTester implements SetupTester {
    private static final int PANDAS_MINIMUM_MAJOR_VERSION = 0;
    private static final int PANDAS_MINIMUM_MINOR_VERSION = 12;
    private static final String PANDAS_MINUMUM_VERSION = String.format("%d.%d.%d", 0, 12, 0);
    private static final int MODULE_TEST_FAILED = 77;
    private static final String PYTHON_FILE_EXTENSION = ".py";
    private static final String C_PICKLE_MODULE_NAME = "cPickle";
    private static final String PANDAS_MODULE_NAME = "pandas";
    private static final String VERSION = "--version";
    private static final String IMPORT = "import ";
    private static final String IMPORT_TEST = "import sys%ntry:%n    import  %s%nexcept:%n    sys.exit(%d)";
    private static final String PYTHON = "python";
    private static final String[] LINUX_MAC_FOLDERS = new String[]{"/usr/bin", "/usr/local/bin"};
    private static final String WINDOWS_PYTHON_BINARY_MATCH_PATTERN = "python(\\.exe)?";
    private static final String LINUX_MAC_PYTHON_BINARY_MATCH_PATTERN = "python([2-3](\\.[0-9])?)?([dum])?";
    private static final String PYTHON_EXE = "python.exe";
    private static final String[] WINDOWS_PATH_PREFIXES = new String[]{"C:/", "C:/Program Files"};
    private static final String[] PYTHON_DIRECTORY_MATCH_PATTERN = new String[]{"miniconda*", "anaconda.*", "python.*"};
    public static final PythonSetupTester INSTANCE = new PythonSetupTester();
    private static final OSCommandRunner commandRunner = SingletonOSCommandFactory.getCommandFactory();
    private static final String PYTHON2_IMPORT = "import sys\nif sys.version_info < (3, 0):\n    import ";
    private static final String PYTHON2_IMPORT_TEST = "import sys%nif sys.version_info < (3, 0):%n    try:%n        import %s%n    except:%n        sys.exit(%d)";
    private static final String PANDAS_VERSION_TEST = "import pandas%nimport sys%nif float(pandas.__version__.split('.')[1])<%d:%n    sys.exit(%d)";
    private static final String PARAMETER_LIST_ITEM_SEPARATOR = ",";
    private static final String DEFAULT_CONDA_ENVIRONMENT = "base";
    private static final Object LOCK = new Object();
    private static final Pattern patternPandasVersion = Pattern.compile("pandas==(?<major>[0-9])\\.(?<minor>[1-9][0-9]*)((<?minorminor>\\.[1-9][0-9]*))?");
    private volatile List<String> pythonBinaries = null;
    private volatile boolean wasAlreadyInitialized = false;
    private volatile Boolean condaInstalled = null;
    private volatile List<String> condaEnvironments = null;
    private volatile Map<String, Path> condaEnvironmentPaths = null;
    private volatile Boolean venvwInstalled = null;
    private volatile List<String> venvwEnvironments = null;
    private volatile Map<String, Path> venvEnvironmentPaths = null;

    private PythonSetupTester() {
    }

    private Path getPythonExecutable(Path path, Path baseCondaExecutable) {
        Path firstGuess;
        if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
            firstGuess = path.resolve("python.exe");
            if (firstGuess.toFile().exists()) {
                return firstGuess;
            }

            Path secondGuess = path.resolve("Scripts\\python.exe");
            if (secondGuess.toFile().exists()) {
                return secondGuess;
            }
        } else {
            firstGuess = path.resolve("bin").resolve("python");
            if (firstGuess.toFile().exists()) {
                return firstGuess;
            }
        }

        if (baseCondaExecutable != null && baseCondaExecutable.toFile().exists()) {
            LogService.getRoot().warning(String.format("No Python executable found for the conda environment at: %s. Using the base executable (%s) instead. You may have not configured properly your conda environment!", path, baseCondaExecutable));
            return baseCondaExecutable;
        } else {
            throw new IllegalStateException(String.format("No appropriate Python executable found, searching the path: %s.", path));
        }
    }

    public void refreshCondaCache() {
        synchronized(LOCK) {
            this.condaInstalled = null;
            this.condaEnvironments = null;
        }

        this.listCondaEnvironments(true);
    }

    public void refreshVirtualEnvwrapperCache() {
        synchronized(LOCK) {
            this.venvwInstalled = null;
            this.venvwEnvironments = null;
        }

        this.listVirtualenvwrapperEnvironments(true);
    }

    public void refreshPythonBinariesCache(ProgressThread progressThread) {
        LogService.getRoot().info("Refreshing Python settings. This may take some time...");
        this.listPythonBinaries(true, progressThread);
        LogService.getRoot().info("Refreshing Python settings DONE.");
    }

    public boolean isPythonInstalled(String pythonPath) {
        return this.scriptingPathTest(pythonPath);
    }

    public boolean isPandasInstalled(String pythonPath) {
        return this.isModuleInstalled("pandas", pythonPath, false);
    }

    public boolean isCPickleInstalled(String pythonPath) {
        return this.isModuleInstalled("cPickle", pythonPath, true);
    }

    private Path quotePath(Path pythonPath) {
        return pythonPath == null ? null : this.quotePath(pythonPath.toString());
    }

    private Path quotePath(String pythonPath) {
        if (pythonPath == null) {
            return null;
        } else {
            if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS && pythonPath.matches(".*\\s+.*") && !pythonPath.startsWith("\"") && !pythonPath.endsWith("\"")) {
                pythonPath = "\"" + pythonPath + "\"";
            }

            return Paths.get(pythonPath);
        }
    }

    private String getInstalledModules(String pythonPath) {
        PythonProcessBuilder pb = new PythonProcessBuilder(pythonPath, new String[]{"-m", "pip", "freeze"});
        String installedModules = "";

        try {
            Process p = pb.start();
            installedModules = OSCommandRunner.readStream(p.getInputStream());
            if (installedModules.length() == 0) {
                String errorMsg = OSCommandRunner.readStream(p.getErrorStream());
                if (errorMsg.length() == 0) {
                    errorMsg = I18N.getErrorMessage("process.error.python_scripting.collecting_pip_packages.unknown_error", new Object[0]);
                } else {
                    errorMsg = I18N.getErrorMessage("process.error.python_scripting.collecting_pip_packages.error_prefix", new Object[]{errorMsg});
                }

                errorMsg = errorMsg.replace("\n", "<br>");
                installedModules = "<html><body><i><p style='color:gray';>" + errorMsg + "</p></i></body></html>";
            } else {
                installedModules = "<html><body>" + installedModules.replace("\n", "<br>") + "</html></body>";
            }
        } catch (IOException var6) {
        }

        return installedModules;
    }

    @Override
    public TestActionResult scriptingSetupTest(String pythonPath) {
        String HTML_TAG = "</html>";
        String pathForLogging;
        if (pythonPath == null) {
            pathForLogging = "<null>";
        } else {
            pathForLogging = pythonPath;
        }

        LogService.getRoot().fine(String.format("Testing Python executable at: %s", pathForLogging));
        if (pythonPath != null && pythonPath.contains("python")) {
            LogService.getRoot().finest("Running Python.");
            PythonProcessBuilder processBuilder = new PythonProcessBuilder(pythonPath, new String[]{"--version"});
            ActionResult firstResult = this.processTest(processBuilder);
            LogService.getRoot().finest("Result of the first test step: " + firstResult.getMessage());
            TestActionResult result = new TestActionResult(firstResult.getMessage(), firstResult.getResult());
            if (result.getResult() == Result.SUCCESS) {
                LogService.getRoot().finest("Running Python.");
                String installedModules = this.getInstalledModules(pythonPath);
                LogService.getRoot().finest("The following installed modules detected: " + installedModules.replaceAll("\\n", " "));
                result = new TestActionResult(result.getMessage(), installedModules, result.getResult());
                boolean canImportPandas = !this.moduleNotFound("pandas", pythonPath, false);
                boolean pandasPipVersionSufficient = true;
                if (!canImportPandas) {
                    Matcher pandasVersionMatcher = patternPandasVersion.matcher(installedModules);
                    String message;
                    if (pandasVersionMatcher.find()) {
                        pandasPipVersionSufficient = Integer.parseInt(pandasVersionMatcher.group("minor")) >= 12;
                        message = result.getMessage().substring(0, result.getMessage().indexOf("</html>")) + "<br/><font color = \"red\">" + I18N.getGUIMessage("setup.action.python_scripting.pandas_import.failure", new Object[0]) + "</font></html>";
                        LogService.getRoot().warning("Pandas module detected, but the extension is unable to load it. Check your installation.");
                        result = new TestActionResult(message, installedModules, Result.FAILURE);
                    } else {
                        message = result.getMessage().substring(0, result.getMessage().indexOf("</html>")) + "<br/><font color = \"red\">" + I18N.getGUIMessage("setup.action.python_scripting.pandas.failure", new Object[]{PANDAS_MINUMUM_VERSION}) + "</font></html>";
                        LogService.getRoot().warning("Pandas module not found!");
                        result = new TestActionResult(message, installedModules, Result.FAILURE);
                    }
                }

                String message;
                if (canImportPandas && this.pandasVersionNotSufficient(pythonPath) || !canImportPandas && !pandasPipVersionSufficient) {
                    message = result.getMessage().substring(0, result.getMessage().indexOf("</html>")) + "<br/><font color = \"red\">" + I18N.getGUIMessage("setup.action.python_scripting.pandas_version.failure", new Object[]{PANDAS_MINUMUM_VERSION}) + "</font></html>";
                    LogService.getRoot().warning("Pandas version not sufficient!");
                    result = new TestActionResult(message, installedModules, Result.FAILURE);
                }

                if (this.moduleNotFound("cPickle", pythonPath, true)) {
                    message = result.getMessage().substring(0, result.getMessage().indexOf("</html>")) + "<br/><br/>" + I18N.getGUIMessage("setup.action.python_scripting.cpickle.failure", new Object[0]) + "</html>";
                    LogService.getRoot().warning("CPickle module not found!");
                    result = new TestActionResult(message, installedModules, result.getResult());
                }
            }

            LogService.getRoot().finest(String.format("Tested Python executable: %s", pathForLogging));
            return result;
        } else {
            LogService.getRoot().warning(String.format("No Python found on path: %s", pathForLogging));
            String message = I18N.getGUIMessage("setup.action.python_scripting.path.failure", new Object[0]);
            return new TestActionResult(message, Result.FAILURE);
        }
    }

    @Override
    public void autodetectPath() {
        LogService.getRoot().info("Initializing Python Scripting Extension for the first time. Thank you for downloading!");
        if (this.isCondaInstalled()) {
            LogService.getRoot().info("Conda installation detected, skipping search for other Python installations.");
            ParameterService.setParameterValue("rapidminer.python_scripting.package_manager", PluginInitPythonScripting.getPackageManager(0));
            ParameterService.setParameterValue("rapidminer.python_scripting.conda_environment", "base");
            ParameterService.saveParameters();
        } else {
            LogService.getRoot().info("No conda installation is detected. Searching for other Python installations...");
            boolean isHeadless = RapidMiner.getExecutionMode().isHeadless();
            this.forceRefreshPythonBinaries((ProgressThread)null, isHeadless);
            int size = this.pythonBinaries.size();
            LogService.getRoot().info(String.format("Found and kept %d potential Python %s", size, size == 1 ? "binary." : "binaries."));
            if (!this.pythonBinaries.isEmpty()) {
                ParameterService.setParameterValue("rapidminer.python_scripting.package_manager", PluginInitPythonScripting.getPackageManager(2));
                ParameterService.setParameterValue("rapidminer.python_scripting.python_binary", (String)this.pythonBinaries.get(0));
                ParameterService.saveParameters();
            } else {
                LogService.getRoot().info("No Python installation found.");
            }
        }

    }

    private boolean scriptingPathTest(String pythonPath) {
        if (!pythonPath.contains("python")) {
            return false;
        } else {
            PythonProcessBuilder processBuilder = new PythonProcessBuilder(pythonPath, new String[]{"--version"});
            return this.processTestFast(processBuilder);
        }
    }

    private void saveList(String key, List<String> value) {
        Optional<String> commaSeparatedValues = value.stream().reduce((s1, s2) -> {
            return s1 + "," + s2;
        });
        if (commaSeparatedValues.isPresent()) {
            ParameterService.setParameterValue(key, (String)commaSeparatedValues.get());
        } else {
            ParameterService.setParameterValue(key, "");
        }

        ParameterService.saveParameters();
    }

    public List<String> listPythonBinaries() {
        return this.listPythonBinaries(false);
    }

    private List<String> listPythonBinaries(boolean forceRefresh) {
        return this.listPythonBinaries(forceRefresh, (ProgressThread)null);
    }

    private List<String> listPythonBinaries(boolean forceRefresh, ProgressThread progressThread) {
        if (forceRefresh) {
            this.forceRefreshPythonBinaries(progressThread, false);
        } else if (this.pythonBinaries == null) {
            String cachedEnvironments = ParameterService.getParameterValue("rapidminer.python_scripting.cached.binaries");
            if (!this.wasAlreadyInitialized && cachedEnvironments != null && cachedEnvironments.length() > 0) {
                this.wasAlreadyInitialized = true;
                this.pythonBinaries = Arrays.asList(cachedEnvironments.split(","));
            } else {
                this.forceRefreshPythonBinaries(progressThread, false);
            }
        }

        return this.pythonBinaries;
    }

    Set<Path> getDirsToCheck() {
        String[] pathPrefixes;
        if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
            pathPrefixes = WINDOWS_PATH_PREFIXES;
        } else {
            pathPrefixes = LINUX_MAC_FOLDERS;
        }

        String dirMsg = "Adding directory {0} to search paths";
        Set<Path> dirsToCheck = new LinkedHashSet<>();
        String[] var4 = PluginInitPythonScripting.getCurrentSearchPath().split(",");
        int var5 = var4.length;

        int var6;
        for(var6 = 0; var6 < var5; ++var6) {
            String dir = var4[var6];
            Path p = Paths.get(dir);
            if (dirsToCheck.add(p)) {
                LogService.getRoot().log(Level.FINEST, dirMsg, dir.replace("\\\\", "\\"));
            }
        }

        String pathVariable = System.getenv("PATH");
        String[] var11;
        int var13;
        String dir;
        if (pathVariable != null) {
            var11 = pathVariable.split(File.pathSeparator);
            var6 = var11.length;

            for(var13 = 0; var13 < var6; ++var13) {
                dir = var11[var13];
                if (dirsToCheck.add(Paths.get(dir))) {
                    LogService.getRoot().log(Level.FINEST, dirMsg, dir);
                }
            }
        }

        var11 = pathPrefixes;
        var6 = pathPrefixes.length;

        for(var13 = 0; var13 < var6; ++var13) {
            dir = var11[var13];
            Path p = Paths.get(dir);
            if (dirsToCheck.add(p)) {
                LogService.getRoot().log(Level.FINEST, dirMsg, dir);
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            dirsToCheck.add(Paths.get(userHome));
        }

        return dirsToCheck;
    }

    private static String getFileMatchPattern() {
        String pattern;
        if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
            pattern = "python(\\.exe)?";
        } else {
            pattern = "python([2-3](\\.[0-9])?)?([dum])?";
        }

        if ("\\".equals(File.separator)) {
            pattern = "(.*\\\\)?" + pattern;
        } else {
            pattern = "(.*" + File.separator + ")?" + pattern;
        }

        return pattern;
    }

    public boolean isPythonExecutable(Path path) {
        return path.toString().matches(getFileMatchPattern());
    }

    private void updateProgressThread(ProgressThread pt, int current, int max) {
        if (pt != null) {
//            pt.getProgressListener().setTotal(max);
//            pt.getProgressListener().setCompleted(current);
        }

    }

    private void forceRefreshPythonBinaries(ProgressThread progressThread, boolean findFirstOnly) {
        this.wasAlreadyInitialized = true;
        List<String> results = new ArrayList<>();
        List<String> oldValues = this.pythonBinaries;
        Set<Path> dirsToCheck = this.getDirsToCheck();
        Set<Path> scannedDirs = new HashSet<>();
        this.updateProgressThread(progressThread, 0, dirsToCheck.size());
        int completedDirs = 0;
        Iterator<Path> var8 = dirsToCheck.iterator();

        while(var8.hasNext()) {
            Path p = var8.next();
            if (progressThread != null && progressThread.isCancelled()) {
                this.pythonBinaries = oldValues;
                return;
            }

            try {
                Files.walkFileTree(p, new PythonSetupTester.PythonBinarySearcher(p, results, scannedDirs, progressThread, findFirstOnly));
                if (findFirstOnly && !results.isEmpty()) {
                    break;
                }
            } catch (IOException var11) {
                LogService.getRoot().fine("Error during scanning file system: " + var11.getMessage());
            } catch (PythonSetupTester.RefreshCancelledException var12) {
                this.pythonBinaries = oldValues;
                return;
            }

            ++completedDirs;
            this.updateProgressThread(progressThread, completedDirs, dirsToCheck.size());
        }

        results.sort(new PythonBinaryComparator());
        results = PythonBinaryComparator.sortByDirectory(results);
        this.pythonBinaries = results;
        this.saveList("rapidminer.python_scripting.cached.binaries", this.pythonBinaries);
    }

    public Path getFullPathForPythonBinary(String path) {
        return this.quotePath(path);
    }

    boolean isCondaInstalled() {
        return this.isCondaInstalled(false);
    }

    private boolean isCondaInstalled(boolean forceRefresh) {
        if (!forceRefresh && this.condaInstalled != null) {
            LogService.getRoot().finest("Conda installation flag was already initialized.");
        } else {
            synchronized(LOCK) {
                if (forceRefresh || this.condaInstalled == null) {
                    LogService.getRoot().finest("Trying to detect Conda installation...");

                    try {
                        this.condaInstalled = commandRunner.runCondaVersionCommand().length() > 0;
                    } catch (IOException var5) {
                        this.condaInstalled = false;
                    }

                    if (this.condaInstalled) {
                        LogService.getRoot().finest("Detected Conda installation on the system.");
                    } else {
                        LogService.getRoot().fine("No Conda installation detected: check your installation and configure search path parameter.");
                    }
                }
            }
        }

        return this.condaInstalled;
    }

    public List<String> listCondaEnvironments() {
        return this.listCondaEnvironments(false);
    }

    private List<String> listCondaEnvironments(boolean forceRefresh) {
        String CONDA_ENV_ALREADY_INITIALIZED = "Conda environment list already initialized.";
        if (this.isCondaInstalled(forceRefresh)) {
            if (!forceRefresh && this.condaEnvironments != null) {
                LogService.getRoot().finest("Conda environment list already initialized.");
            } else {
                LogService.getRoot().finest("Force refreshing list of Conda environments...");
                this.forceRefreshCondaEnvironments();
            }
        } else if (this.condaEnvironments == null) {
            synchronized(LOCK) {
                if (this.condaEnvironments == null) {
                    LogService.getRoot().finest("Initializing list of Conda environments to an empty list: no Conda installation found.");
                    this.condaEnvironments = new ArrayList<>();
                    this.condaEnvironmentPaths = new HashMap<>();
                } else {
                    LogService.getRoot().finest("Conda environment list already initialized.");
                }
            }
        } else {
            LogService.getRoot().finest("Conda environment list already initialized.");
        }

        return this.condaEnvironments;
    }

    private String getEnvironmentName(Path pathToEnvDir, Path baseEnvironmentPath, List<String> duplicatedEnvNames) {
        if (pathToEnvDir.equals(baseEnvironmentPath)) {
            return "base";
        } else {
            String simpleName = pathToEnvDir.getFileName().toString();
            return duplicatedEnvNames != null && duplicatedEnvNames.contains(simpleName) ? String.format("%s [%s]", simpleName, pathToEnvDir.toAbsolutePath().toString()) : simpleName;
        }
    }

    private void forceRefreshCondaEnvironments() {
        String response = "empty";
        synchronized(LOCK) {
            try {
                LogService.getRoot().finest("Collecting list of Conda environments...");
                response = commandRunner.runCondaEnvironmentListCommand();
                JSONObject root = new JSONObject(response);
                Path baseEnvironmentPath = Paths.get(root.getString("root_prefix"));
                Path baseEnvironmentExecutable = this.getPythonExecutable(baseEnvironmentPath, (Path)null);
                LogService.getRoot().fine(String.format("Path for base environment is: %s", baseEnvironmentPath.toString()));
                JSONArray envs = root.getJSONArray("envs");

//                List<String> duplicatedEnvs = null;
//                List<String> duplicatedEnvs = (List)((Map)StreamSupport.stream(envs.spliterator(), false).map(o ->
//                        Paths.get((String)o, new String[0])).map(p ->
//                        getEnvironmentName(p, baseEnvironmentPath, null)).collect(Collectors.toMap(Function.identity(), o ->
//                        Integer.valueOf(1), (o1, o2) ->
//                        Integer.valueOf(Integer.sum(((Integer)o1).intValue(), ((Integer)o2).intValue()))))).entrySet().stream().filter(entry ->
//                        (((Integer)entry.getValue()).intValue() > 1))
//                        .map(Entry::getKey).collect(Collectors.toList());
                Map<Integer, Integer> duplicatedEnvsMap = (Map) StreamSupport.stream(envs.spliterator(), false).map(o ->
                        Paths.get((String)o, new String[0])).map(p ->
                        getEnvironmentName(p, baseEnvironmentPath, null)).collect(Collectors.toMap(Function.identity(), o ->
                        Integer.valueOf(1), (o1, o2) ->
                        Integer.valueOf(Integer.sum(((Integer)o1).intValue(), ((Integer)o2).intValue()))));
                List<String> duplicatedEnvs = (List)duplicatedEnvsMap.entrySet().stream().filter(entry ->
                        (((Integer)entry.getValue()).intValue() > 1))
                        .map(Entry::getKey).collect(Collectors.toList());

                Map<String, Path> envMap = (Map<String, Path>)StreamSupport.stream(envs.spliterator(), false).map((o) -> {
                    return Paths.get((String)o);
                }).collect(Collectors.toMap((p) -> {
                    return this.getEnvironmentName(p, baseEnvironmentPath, duplicatedEnvs);
                }, (p) -> {
                    return this.getPythonExecutable(p, baseEnvironmentExecutable);
                }));
                envMap.values().forEach((p) -> {
                    LogService.getRoot().fine("Found conda environment: " + p.toString());
                });
                List<String> envList = new ArrayList<>(envMap.keySet());
                envList.sort((o1, o2) -> {
                    if ("base".equals(o1)) {
                        return -1;
                    } else {
                        return "base".equals(o2) ? 1 : o1.compareTo(o2);
                    }
                });
                this.condaEnvironments = envList;
                this.condaEnvironmentPaths = envMap;
                LogService.getRoot().finest("Collected list of Conda environments.");
            } catch (JSONException | IOException var11) {
                LogService.getRoot().warning("Could not parse result of listing Conda environments: " + var11.getMessage());
                LogService.getRoot().fine("Last command output was: " + response);
                this.condaEnvironments = new ArrayList<>();
                this.condaEnvironmentPaths = new HashMap<>();
            }

        }
    }

    public boolean isCondaExecutable(Path pythonPath) {
        if (this.condaEnvironmentPaths == null) {
            this.refreshCondaCache();
        }

        return this.condaEnvironmentPaths.values().stream().anyMatch((p) -> {
            return p.equals(pythonPath);
        });
    }

    public Path getFullPathForCondaEnvironment(String environment) {
        if (this.condaEnvironmentPaths == null) {
            this.refreshCondaCache();
        }

        synchronized(LOCK) {
            if (this.condaEnvironmentPaths == null) {
                return null;
            } else {
                Path result = this.quotePath((Path)this.condaEnvironmentPaths.get(environment));
                LogService.getRoot().finest(String.format("Executable location for Conda environment %s is %s.", environment, result));
                return result;
            }
        }
    }

    private boolean isVirtualenvwrapperInstalled(boolean forceRefresh) {
        if (!forceRefresh && this.venvwInstalled != null) {
            LogService.getRoot().finest("Virtualenvwrapper installation flag was already initialized.");
        } else {
            synchronized(LOCK) {
                if (forceRefresh || this.venvwInstalled == null) {
                    LogService.getRoot().finest("Trying to detect Virtualenvwrapper installation...");

                    try {
                        this.venvwInstalled = commandRunner.runVenvwVersionCommand().length() > 0;
                    } catch (IOException var5) {
                        this.venvwInstalled = false;
                    }

                    if (this.venvwInstalled) {
                        LogService.getRoot().finest("Detected Virtualenvwrapper installation on the system.");
                    } else {
                        LogService.getRoot().fine("No Virtualenvwrapper installation detected: check your installation and configure search path parameter.");
                    }
                }
            }
        }

        return this.venvwInstalled;
    }

    public List<String> listVirtualenvwrapperEnvironments() {
        return this.listVirtualenvwrapperEnvironments(false);
    }

    private List<String> listVirtualenvwrapperEnvironments(boolean forceRefresh) {
        String VENVW_ENV_ALREADY_INITIALIZED = "Virtualenvwrapper environment list already initialized.";
        if (this.isVirtualenvwrapperInstalled(forceRefresh)) {
            if (!forceRefresh && this.venvwEnvironments != null) {
                LogService.getRoot().finest("Virtualenvwrapper environment list already initialized.");
            } else {
                LogService.getRoot().finest("Force refreshing list of Virtualenvwrapper environments...");
                this.forceRefreshVenvwEnvironments();
            }
        } else if (this.venvwEnvironments == null) {
            synchronized(LOCK) {
                if (this.venvwEnvironments == null) {
                    LogService.getRoot().finest("Initializing list of Virtualenvwrapper environments to an empty list: no Virtualenvwrapper installation found.");
                    this.venvwEnvironments = new ArrayList<>();
                    this.venvEnvironmentPaths = new HashMap<>();
                } else {
                    LogService.getRoot().finest("Virtualenvwrapper environment list already initialized.");
                }
            }
        } else {
            LogService.getRoot().finest("Virtualenvwrapper environment list already initialized.");
        }

        synchronized(LOCK) {
            return this.venvwEnvironments;
        }
    }

    private void forceRefreshVenvwEnvironments() {
        synchronized(LOCK) {
            String workonHomeOutput = "<not initalized>";

            try {
                LogService.getRoot().finest("Collecting list of Virtualenvwrapper environments...");
                String[] envs = commandRunner.runVenvwEnvironmentListCommand().split("\n");
                LogService.getRoot().finest("Determining WORKON_HOME...");
                workonHomeOutput = commandRunner.runPrintWorkonHomeCommand();
                workonHomeOutput = workonHomeOutput.replaceAll("(.|\\n)*WORKON_HOME=", "");
                Path baseDir = Paths.get(workonHomeOutput.trim());
                LogService.getRoot().fine(String.format("Workon-home: %s", baseDir.toString()));
                this.venvwEnvironments = (List<String>)Arrays.stream(envs).filter((s) -> {
                    return s.length() > 0 && !s.contains(" ") && !s.contains("=");
                }).sorted().collect(Collectors.toList());
                this.venvEnvironmentPaths = (Map<String, Path>)this.venvwEnvironments.stream().collect(Collectors.toMap(Function.identity(), (s) -> {
                    return this.getPythonExecutable(baseDir.resolve(s), (Path)null);
                }));
                this.venvwEnvironments.forEach((s) -> {
                    LogService.getRoot().fine("Detected Virtualenvwrapper environment: " + s);
                });
                LogService.getRoot().finest("Collected list of Virtualenvwrapper environments.");
            } catch (InvalidPathException var6) {
                LogService.getRoot().warning(String.format("Cannot get WORKON_HOME variable. Invalid path: '%s'.", workonHomeOutput));
            } catch (IOException var7) {
                LogService.getRoot().warning(String.format("Could not parse result of listing Virtualenvwrapper environments: %s", var7.getMessage()));
                this.venvwEnvironments = new ArrayList<>();
                this.venvEnvironmentPaths = new HashMap<>();
            }

        }
    }

    public Path getFullPathForVenvwEnvironment(String environment) {
        if (this.venvEnvironmentPaths == null) {
            this.refreshVirtualEnvwrapperCache();
        }

        synchronized(LOCK) {
            if (this.venvEnvironmentPaths == null) {
                return null;
            } else {
                Path result = this.quotePath((Path)this.venvEnvironmentPaths.get(environment));
                LogService.getRoot().finest(String.format("Executable location for Virtualenvwrapper environment %s is %s.", environment, result));
                return result;
            }
        }
    }

    private boolean isModuleInstalled(String moduleName, String pythonPath, boolean onlyPython2) {
        String test;
        if (onlyPython2) {
            test = "import sys\nif sys.version_info < (3, 0):\n    import " + moduleName;
        } else {
            test = "import " + moduleName;
        }

        return this.checkScriptForSuccess(test, pythonPath, ".py");
    }

    private boolean moduleNotFound(String moduleName, String pythonPath, boolean onlyPython2) {
        String test;
        if (onlyPython2) {
            test = String.format("import sys%nif sys.version_info < (3, 0):%n    try:%n        import %s%n    except:%n        sys.exit(%d)", moduleName, 77);
        } else {
            test = String.format("import sys%ntry:%n    import  %s%nexcept:%n    sys.exit(%d)", moduleName, 77);
        }

        return this.checkScriptForExitCode(test, pythonPath, 77, ".py");
    }

    public boolean pandasVersionNotSufficient(String pythonPath) {
        String script = String.format("import pandas%nimport sys%nif float(pandas.__version__.split('.')[1])<%d:%n    sys.exit(%d)", 12, 77);
        return this.checkScriptForExitCode(script, pythonPath, 77, ".py");
    }

    private String getSubdirectoryList(String directory) {
        return SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS ? directory + "," + directory + File.separator + "Scripts" : directory + "," + directory + File.separator + "bin";
    }

    private String existsCaseInsensitive(String directory) {
        Path dir = Paths.get(directory);
        if (dir.toFile().exists()) {
            return directory;
        } else {
            Path parent = dir.getParent();
            String[] matches = parent.toFile().list((dir1, name) -> {
                return name.equalsIgnoreCase(dir.getFileName().toString());
            });
            return matches != null && matches.length > 0 ? (String)Arrays.stream(matches).sorted().findFirst().get() : null;
        }
    }

    private String appendPotentialCondaLocation(String locations, String potentialLocation) {
        String caseSensitivePotentialLocation = this.existsCaseInsensitive(potentialLocation);
        if (caseSensitivePotentialLocation == null) {
            return locations;
        } else {
            return locations.length() > 0 ? locations + "," + this.getSubdirectoryList(caseSensitivePotentialLocation) : this.getSubdirectoryList(potentialLocation);
        }
    }

    public String getDefaultSearchPathValues() {
        String result = "";
        String userHome = System.getProperty("user.home");
        List<String> condaInstallationTypes = Arrays.asList("Anaconda2", "Anaconda3", "Miniconda2", "Miniconda3");
        Iterator<String> var4;
        String condaDir;
        if (userHome != null) {
            for(var4 = condaInstallationTypes.iterator(); var4.hasNext(); result = this.appendPotentialCondaLocation(result, userHome + File.separator + condaDir)) {
                condaDir = var4.next();
            }
        }

        if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
            for(var4 = condaInstallationTypes.iterator(); var4.hasNext(); result = this.appendPotentialCondaLocation(result, String.format("%s%sAppData%sLocal%sContinuum%s%s", userHome, File.separator, File.separator, File.separator, File.separator, condaDir))) {
                condaDir = var4.next();
                result = this.appendPotentialCondaLocation(result, String.format("C:%sProgramData%s%s", File.separator, File.separator, condaDir));
            }

            result = result.replaceAll("\\\\", "\\\\\\\\");
        }

        return result;
    }

    private ActionResult processTest(PythonProcessBuilder processBuilder) {
        processBuilder.redirectErrorStream(true);

        Result result;
        String message;
        try {
            Process process = processBuilder.start();
            int exit = process.waitFor();
            String version;
            if (exit == 0) {
                result = Result.SUCCESS;
                version = Tools.parseInputStreamToString(process.getInputStream(), true);
                message = I18N.getGUIMessage("setup.action.python_scripting.test_setup.success", new Object[]{version});
            } else {
                result = Result.FAILURE;
                version = Tools.parseInputStreamToString(process.getInputStream(), true);
                message = I18N.getGUIMessage("setup.action.python_scripting.test_setup.failure", new Object[]{version});
            }
        } catch (InterruptedException | IOException var7) {
            result = Result.FAILURE;
            message = I18N.getGUIMessage("setup.action.python_scripting.test_setup.failure", new Object[]{var7.getLocalizedMessage()});
        }

        return new SimpleActionResult(message, result);
    }

    private boolean processTestFast(PythonProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.start();
            int exit = process.waitFor();
            return exit == 0;
        } catch (InterruptedException | IOException var4) {
            return false;
        }
    }

    private boolean checkScriptForSuccess(String script, String path, String fileExtension) {
        Path tempPath = null;

        boolean var6;
        try {
            tempPath = Files.createTempFile("check", fileExtension);
            Files.write(tempPath, script.getBytes(StandardCharsets.UTF_8), new OpenOption[]{StandardOpenOption.WRITE});
            PythonProcessBuilder processBuilder = new PythonProcessBuilder(path, new String[]{tempPath.toAbsolutePath().toString()});
            var6 = this.processTestFast(processBuilder);
            return var6;
        } catch (IOException var16) {
            var6 = false;
        } finally {
            if (tempPath != null) {
                try {
                    Files.delete(tempPath);
                } catch (IOException var15) {
                }
            }

        }

        return var6;
    }

    private boolean checkScriptForExitCode(String script, String scriptingPath, int exitCode, String fileExtension) {
        Path tempPath = null;

        boolean var7;
        try {
            tempPath = Files.createTempFile("check", fileExtension);
            Files.write(tempPath, script.getBytes(StandardCharsets.UTF_8), new OpenOption[]{StandardOpenOption.WRITE});
            PythonProcessBuilder processBuilder = new PythonProcessBuilder(scriptingPath, new String[]{tempPath.toAbsolutePath().toString()});
            Process process = processBuilder.start();
            int exit = process.waitFor();
            boolean var9 = exit == exitCode;
            return var9;
        } catch (InterruptedException | IOException var19) {
            var7 = false;
        } finally {
            if (tempPath != null) {
                try {
                    Files.delete(tempPath);
                } catch (IOException var18) {
                }
            }

        }

        return var7;
    }

    private class PythonBinarySearcher implements FileVisitor<Path> {
        private final ProgressThread progressThread;
        private final String fileMatchPattern;
        private final Path path;
        private final List<String> results;
        private final Set<Path> scannedDirs;
        private final boolean findFirstOnly;

        private PythonBinarySearcher(Path path, List<String> results, Set<Path> scannedDirs, ProgressThread pt, boolean findFirstOnly) {
            this.progressThread = pt;
            this.fileMatchPattern = PythonSetupTester.getFileMatchPattern();
            this.path = path;
            this.results = results;
            this.scannedDirs = scannedDirs;
            this.findFirstOnly = findFirstOnly;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (this.progressThread != null && this.progressThread.isCancelled()) {
                throw new PythonSetupTester.RefreshCancelledException();
            } else if (this.scannedDirs.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            } else {
                String name;
                if (dir.getNameCount() > 0) {
                    name = dir.getName(dir.getNameCount() - 1).toString();
                } else {
                    name = dir.toString();
                }

                boolean isPythonFolder = false;
                String[] var5 = PythonSetupTester.PYTHON_DIRECTORY_MATCH_PATTERN;
                int var6 = var5.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    String dirMatchPattern = var5[var7];
                    isPythonFolder = isPythonFolder || name.toLowerCase().matches(dirMatchPattern);
                }

                boolean isPythonBinFolder = "bin".equalsIgnoreCase(name);
                if (!Files.isReadable(dir) || !this.path.equals(dir) && !isPythonBinFolder && !isPythonFolder) {
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    this.scannedDirs.add(dir);
                    return FileVisitResult.CONTINUE;
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (Files.isReadable(file)) {
                if (this.progressThread != null && this.progressThread.isCancelled()) {
                    throw new PythonSetupTester.RefreshCancelledException();
                } else {
                    String entry = file.toAbsolutePath().toString();
                    if (entry.matches(this.fileMatchPattern)) {
                        LogService.getRoot().fine(String.format("Detected Python binary at: %s", entry));
                        this.results.add(entry);
                    }

                    return FileVisitResult.CONTINUE;
                }
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (this.progressThread != null && this.progressThread.isCancelled()) {
                throw new PythonSetupTester.RefreshCancelledException();
            } else {
                if (exc != null) {
                    LogService.getRoot().fine(String.format("Error during scanning file '%s': %s", file, exc.getMessage()));
                }

                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (this.progressThread != null && this.progressThread.isCancelled()) {
                throw new PythonSetupTester.RefreshCancelledException();
            } else {
                if (exc != null) {
                    LogService.getRoot().fine(String.format("Error during scanning directory '%s': %s", dir, exc.getMessage()));
                }

                return this.findFirstOnly && !this.results.isEmpty() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        }
    }

    private static class RefreshCancelledException extends RuntimeException {
        private static final long serialVersionUID = 5132990303924172383L;

        private RefreshCancelledException() {
        }
    }
}
