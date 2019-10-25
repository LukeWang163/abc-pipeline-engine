package base.operators.operator.scripting;

import base.operators.RapidMiner;
import base.operators.operator.scripting.parameter.CondaEnvironmentSuggestionProvider;
import base.operators.operator.scripting.parameter.ParameterTypeScriptingEnvironment;
import base.operators.operator.scripting.parameter.PythonBinariesSuggestionProvider;
import base.operators.operator.scripting.parameter.VenvwEnvironmentSuggestionProvider;
import base.operators.gui.tools.ProgressThread;
import base.operators.operator.scripting.python.PythonSetupTester;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDirectory;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeSuggestion;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.parameter.conditions.ParameterCondition;
import base.operators.tools.I18N;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import base.operators.tools.parameter.ParameterChangeListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;

public final class PluginInitPythonScripting {
    public static final String PROPERTY_INITIALIZED_V82 = "operator.python_scripting.initialized";
    public static final String PROPERTY_PYTHON_PATH_V82 = "operator.python_scripting.path";
    private static final String[] PACKAGE_MANAGERS = new String[]{"conda (anaconda)", "virtualenvwrapper", "specific python binaries"};
    public static final int PACKAGE_MANAGER_DEFAULT_INDEX = 0;
    public static final int PACKAGE_MANAGER_CONDA_INDEX = 0;
    public static final int PACKAGE_MANAGER_VENVW_INDEX = 1;
    public static final int PACKAGE_MANAGER_BINARIES_INDEX = 2;
    private static final String PROPERTY_EXTENSION_INITIALIZED = "operator.python_scripting.hidden.initialized";
    private static final String PROPERTY_PYTHON_SEARCH_PATHS = "operator.python_scripting.search_paths";
    private static final String PROPERTY_PYTHON_SEARCH_PATH = "operator.python_scripting.search_path";
    public static final String PROPERTY_PACKAGE_MANAGER = "operator.python_scripting.package_manager";
    public static final String PROPERTY_CONDA_ENVIRONMENT = "operator.python_scripting.conda_environment";
    public static final String PROPERTY_VENVW_ENVIRONMENT = "operator.python_scripting.venvw_environment";
    public static final String PROPERTY_PYTHON_BINARY = "operator.python_scripting.python_binary";
    public static final String PROPERTY_CACHED_BINARIES = "operator.python_scripting.cached.binaries";
    private static volatile String currentSearchPath = null;
    private static boolean initialized = false;

    private PluginInitPythonScripting() {
    }

    public static void initPlugin() {
        ParameterTypeEnumeration searchPaths = new ParameterTypeEnumeration("operator.python_scripting.search_paths", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.search_paths", new Object[0]), new ParameterTypeDirectory("operator.python_scripting.search_path", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.search_path", new Object[0]), true));
        ParameterService.registerParameter(searchPaths);
        ParameterType packageManagers = new ParameterTypeCategory("operator.python_scripting.package_manager", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.package_manager", new Object[0]), PACKAGE_MANAGERS, 0);
        packageManagers.registerDependencyCondition(new ParameterCondition((ParameterHandler)null, "operator.python_scripting.search_paths", true) {
            @Override
            public boolean isConditionFullfilled() {
                try {
                    if (this.parameterHandler != null) {
                        PluginInitPythonScripting.currentSearchPath = (String)this.parameterHandler.getParameter("operator.python_scripting.search_paths");
                    }
                } catch (UndefinedParameterError var4) {
                    ByteArrayOutputStream stacktrace = new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(stacktrace);
                    var4.printStackTrace(printStream);
                    LogService.getRoot().warning(stacktrace.toString());
                }

                return true;
            }
        });
        ParameterService.registerParameter(packageManagers);
        ParameterTypeSuggestion condaEnvs = new ParameterTypeScriptingEnvironment("operator.python_scripting.conda_environment", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.default_conda", new Object[0]), new CondaEnvironmentSuggestionProvider());
        condaEnvs.setOptional(true);
        condaEnvs.setExpert(false);
        condaEnvs.registerDependencyCondition(new EqualStringCondition((ParameterHandler)null, "operator.python_scripting.package_manager", true, new String[]{PACKAGE_MANAGERS[0]}));
        ParameterService.registerParameter(condaEnvs);
        ParameterTypeSuggestion venvwEnvs = new ParameterTypeScriptingEnvironment("operator.python_scripting.venvw_environment", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.default_venvw", new Object[0]), new VenvwEnvironmentSuggestionProvider());
        venvwEnvs.setOptional(true);
        venvwEnvs.setExpert(false);
        venvwEnvs.registerDependencyCondition(new EqualStringCondition((ParameterHandler)null, "operator.python_scripting.package_manager", true, new String[]{PACKAGE_MANAGERS[1]}));
        ParameterService.registerParameter(venvwEnvs);
        ParameterTypeSuggestion binaries = new ParameterTypeScriptingEnvironment("operator.python_scripting.python_binary", I18N.getGUIMessage("gui.dialog.python_scripting.preferences.default_python_binary", new Object[0]), new PythonBinariesSuggestionProvider());
        binaries.setOptional(true);
        binaries.setExpert(false);
        binaries.registerDependencyCondition(new EqualStringCondition((ParameterHandler)null, "operator.python_scripting.package_manager", true, new String[]{PACKAGE_MANAGERS[2]}));
        ParameterService.registerParameter(binaries);
    }

    private static void autodetectPath() {
        String searchPathValues = ParameterService.getParameterValue("operator.python_scripting.search_paths");
//        if (searchPathValues == null || searchPathValues.length() == 0) {
//            ParameterService.setParameterValue("operator.python_scripting.search_paths", PythonSetupTester.INSTANCE.getDefaultSearchPathValues());
//        }

        ParameterService.saveParameters();
    }

    public static void initPluginForFirstTime() {
        if (!initialized) {
            initialized = true;
            String initializedString = ParameterService.getParameterValue("operator.python_scripting.hidden.initialized");
            final String packageManager = ParameterService.getParameterValue("operator.python_scripting.package_manager");
            final String binaryPath = ParameterService.getParameterValue("operator.python_scripting.python_binary");
            LogService.getRoot().finest(String.format("%s=%s", "operator.python_scripting.hidden.initialized", initializedString));
            if (!Boolean.parseBoolean(initializedString)) {
                String version82PythonBinary = ParameterService.getParameterValue("operator.python_scripting.path");
                if (version82PythonBinary != null && version82PythonBinary.length() > 0) {
                    LogService.getRoot().info(String.format("Using Python binary defined in the previous version of Python Scripting extension: %s", version82PythonBinary));
                    ParameterService.setParameterValue("operator.python_scripting.package_manager", PACKAGE_MANAGERS[2]);
                    ParameterService.setParameterValue("operator.python_scripting.python_binary", version82PythonBinary);
                } else if (RapidMiner.getExecutionMode().isHeadless()) {
                    callAutoDetect(packageManager, binaryPath);
                } else {
                    ProgressThread t = new ProgressThread(I18N.getGUIMessage("python_scripting.init_thread", new Object[0]), true) {
                        @Override
                        public void run() {
                            PluginInitPythonScripting.callAutoDetect(packageManager, binaryPath);
                        }
                    };
                    t.setIndeterminate(true);
                    t.setCancelable(false);
                    t.start();
                }

                ParameterService.setParameterValue("operator.python_scripting.hidden.initialized", String.valueOf(true));
                ParameterService.saveParameters();
            }

        }
    }

    private static void callAutoDetect(String packageManager, String binaryPath) {
        boolean skipAutodetectPath = !PACKAGE_MANAGERS[0].equals(packageManager);
        if (PACKAGE_MANAGERS[2].equals(packageManager) && (binaryPath == null || binaryPath.trim().length() == 0)) {
            skipAutodetectPath = false;
        }

        if (skipAutodetectPath) {
            LogService.getRoot().info("Python environment/binary is already configured in settings.");
        } else {
            autodetectPath();
        }

        Path defaultPythonBinary = getDefaultPythonBinary();
        if (defaultPythonBinary != null) {
            ParameterService.setParameterValue("operator.python_scripting.initialized", String.valueOf(true));
            ParameterService.setParameterValue("operator.python_scripting.path", defaultPythonBinary.toString());
        }

    }


    public static void initFinalChecks() {
    }

    public static void initPluginManager() {
        ParameterService.registerParameterChangeListener(new ParameterChangeListener() {
            private boolean updated = true;

            @Override
            public void informParameterSaved() {
                if (this.updated) {
                    this.updated = false;
                    Path defaultPythonBinary = PluginInitPythonScripting.getDefaultPythonBinary();
                    if (defaultPythonBinary != null) {
                        ParameterService.setParameterValue("operator.python_scripting.path", defaultPythonBinary.toString());
                        ParameterService.saveParameters();
                    }
                }

            }

            @Override
            public void informParameterChanged(String key, String value) {
                if (Arrays.asList("operator.python_scripting.package_manager", "operator.python_scripting.conda_environment", "operator.python_scripting.venvw_environment", "operator.python_scripting.python_binary", "operator.python_scripting.cached.binaries", "operator.python_scripting.hidden.initialized", "operator.python_scripting.search_paths").contains(key)) {
                    this.updated = true;
                }

            }
        });
    }

    public static Boolean useExtensionTreeRoot() {
        return false;
    }

    public static Path getDefaultPythonBinary() {
        String packageManager = "conda (anaconda)";//ParameterService.getParameterValue("operator.python_scripting.package_manager");
        String path;
        if (packageManager.equals(PACKAGE_MANAGERS[0])) {
            path = ParameterService.getParameterValue("operator.python_scripting.conda_environment");
            return path != null ? PythonSetupTester.INSTANCE.getFullPathForCondaEnvironment(path) : null;
        } else if (packageManager.equals(PACKAGE_MANAGERS[1])) {
            path = ParameterService.getParameterValue("operator.python_scripting.venvw_environment");
            return path != null ? PythonSetupTester.INSTANCE.getFullPathForVenvwEnvironment(path) : null;
        } else {
            path = ParameterService.getParameterValue("operator.python_scripting.python_binary");
            return path != null ? PythonSetupTester.INSTANCE.getFullPathForPythonBinary(path) : null;
        }
    }

    public static String getDefaultPythonEnvironmentName() {
        String packageManager = "conda (anaconda)";//ParameterService.getParameterValue("operator.python_scripting.package_manager");
        if (packageManager.equals(PACKAGE_MANAGERS[0])) {
            return ParameterService.getParameterValue("operator.python_scripting.conda_environment");
        } else {
            return packageManager.equals(PACKAGE_MANAGERS[1]) ? ParameterService.getParameterValue("operator.python_scripting.venvw_environment") : ParameterService.getParameterValue("operator.python_scripting.python_binary");
        }
    }

    public static String getCurrentSearchPath() {
        if (currentSearchPath == null) {
            String value = ParameterService.getParameterValue("operator.python_scripting.search_paths");
            return value == null ? "" : value;
        } else {
            return currentSearchPath;
        }
    }

    public static String getPackageManager(int index) {
        return PACKAGE_MANAGERS[index];
    }

    public static String[] getPackageManagers() {
        return PACKAGE_MANAGERS;
    }

//    static {
//        try {
//            JarVerifier.verify(new Class[]{LicenseManagerRegistry.INSTANCE.get().getClass(), RapidMiner.class, PluginInitProfessional.class, PluginInitPythonScripting.class});
//        } catch (GeneralSecurityException var1) {
//            throw new RuntimeException(var1);
//        }
//    }
}
