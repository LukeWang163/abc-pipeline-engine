package base.operators.operator.scripting.python;

import base.operators.RapidMiner;
import base.operators.operator.scripting.PluginInitPythonScripting;
import base.operators.operator.scripting.AbstractScriptingLanguageOperator;
import base.operators.operator.scripting.ScriptRunner;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.TextType;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.I18N;
import base.operators.tools.LogService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PythonScriptingOperator extends AbstractScriptingLanguageOperator implements PythonBinarySupplier {
    private static final String PREFERENCES_TAB = "python_scripting";
    private static final String PARAMETER_SCRIPT = "script";

    public PythonScriptingOperator(OperatorDescription description) {
        super(description);
        if (RapidMiner.isInitialized()) {
            PluginInitPythonScripting.initPluginForFirstTime();
        }

    }

    private ProcessSetupError getErrorForOperatorsPythonNotSetProperlyProblem(List<QuickFix> fixes) {
        String condaPkgMng = PluginInitPythonScripting.getPackageManager(0);
//        String venvwPkgMng = PluginInitPythonScripting.getPackageManager(1);
        String i18nKey = null;
        String param = "";

        try {
            if (condaPkgMng.equals(this.getParameterAsString("package_manager"))) {
                param = this.getParameterAsString("conda_environment");
                i18nKey = "python_scripting.conda_env_not_found";
            }
//            else if (venvwPkgMng.equals(this.getParameterAsString("package_manager"))) {
//                param = this.getParameterAsString("venvw_environment");
//                i18nKey = "python_scripting.venvw_env_not_found";
//            }
            else {
                param = this.getParameterAsString("python_binary");
                i18nKey = "python_scripting.not_found";
            }

            fixes.add(new ParameterSettingQuickFix(this, param));
        } catch (UndefinedParameterError var7) {
        }

        return new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, i18nKey, new Object[]{param});
    }

    @Override
    protected void showSetupProblems() {
        if (RapidMiner.isInitialized()) {
            Path pythonPath;
            try {
                pythonPath = this.getPythonBinary();
            } catch (UndefinedParameterError var4) {
                return;
            }

            List<QuickFix> fixes = new ArrayList();
            ProcessSetupError error = null;
            if (pythonPath != null && PythonSetupTester.INSTANCE.isPythonInstalled(pythonPath.toString())) {
                error = null;
            } else if (this.getParameterAsBoolean("use_default_python")) {
                error = null;
            } else {
                error = this.getErrorForOperatorsPythonNotSetProperlyProblem(fixes);
            }

            if (error != null) {
                this.addError(error);
            }

        }
    }

    @Override
    public void doWork() throws OperatorException {
        Path pythonBinaryPath = this.getPythonBinary();
        String pythonBinary;
        if (pythonBinaryPath == null) {
            pythonBinary = this.getPythonEnvironmentName();
            if (pythonBinary == null) {
                pythonBinary = "";
            }

            throw new UserError(this, "python_scripting.setup_test_environment.failure", new Object[]{pythonBinary});
        } else {
            pythonBinary = pythonBinaryPath.toString();
            LogService.getRoot().info(String.format("Using Python binary '%s' to run Python code in operator '%s'.", pythonBinary, this.getName()));
            if (PythonSetupTester.INSTANCE.isPythonInstalled(pythonBinary)) {
                super.doWork();
            } else {
                throw new UserError(this, "python_scripting.setup_test.failure", new Object[]{pythonBinary});
            }
        }
    }

    @Override
    protected ScriptRunner getScriptRunner() throws UndefinedParameterError {
        ScriptRunner runner = new PythonScriptRunner(this.getScript(), this, this);
        runner.registerLogger(this.getLogger());
        return runner;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeBoolean useDefaultPython = new ParameterTypeBoolean("use_default_python", I18N.getGUIMessage("gui.parameters.python_scripting.preferences.use_default_python", new Object[0]), true, true);
        types.add(useDefaultPython);
//        ParameterTypeCategory packageManagers = new ParameterTypeCategory("package_manager", I18N.getGUIMessage("gui.parameters.python_scripting.preferences.package_manager", new Object[0]), PluginInitPythonScripting.getPackageManagers(), 0);
//        packageManagers.setExpert(true);
//        packageManagers.registerDependencyCondition(new BooleanParameterCondition(this.getParameterHandler(), "use_default_python", true, false));
//        types.add(packageManagers);
//        ParameterTypeSuggestion condaEnvs = new ParameterTypeScriptingEnvironment("conda_environment", I18N.getGUIMessage("gui.parameters.python_scripting.preferences.conda_environment", new Object[0]), new CondaEnvironmentSuggestionProvider());
//        condaEnvs.setOptional(true);
//        condaEnvs.setExpert(true);
//        condaEnvs.registerDependencyCondition(new BooleanParameterCondition(this.getParameterHandler(), "use_default_python", true, false));
//        condaEnvs.registerDependencyCondition(new EqualStringCondition(this.getParameterHandler(), "package_manager", true, new String[]{PluginInitPythonScripting.getPackageManager(0)}));
//        types.add(condaEnvs);
//        ParameterTypeSuggestion venevwEnvs = new ParameterTypeScriptingEnvironment("venvw_environment", I18N.getGUIMessage("gui.parameters.python_scripting.preferences.venvw_environment", new Object[0]), new VenvwEnvironmentSuggestionProvider());
//        venevwEnvs.setOptional(true);
//        venevwEnvs.setExpert(true);
//        venevwEnvs.registerDependencyCondition(new BooleanParameterCondition(this.getParameterHandler(), "use_default_python", true, false));
//        venevwEnvs.registerDependencyCondition(new EqualStringCondition(this.getParameterHandler(), "package_manager", true, new String[]{PluginInitPythonScripting.getPackageManager(1)}));
//        types.add(venevwEnvs);
//        ParameterTypeSuggestion binaries = new ParameterTypeScriptingEnvironment("python_binary", I18N.getGUIMessage("gui.parameters.python_scripting.preferences.python_binary", new Object[0]), new PythonBinariesSuggestionProvider());
//        binaries.setOptional(true);
//        binaries.setExpert(true);
//        binaries.registerDependencyCondition(new BooleanParameterCondition(this.getParameterHandler(), "use_default_python", true, false));
//        binaries.registerDependencyCondition(new EqualStringCondition(this.getParameterHandler(), "package_manager", true, new String[]{PluginInitPythonScripting.getPackageManager(2)}));
//        types.add(binaries);
//        this.configurationLink.setHidden(true);
//        types.add(this.configurationLink);
        return types;
    }

    @Override
    protected String getScriptParameterName() {
        return "script";
    }

    @Override
    protected String getScriptParameterDescription() {
        return "The python script to execute.";
    }

    @Override
    protected TextType getScriptParameterTextType() {
        return TextType.PYTHON;
    }

    @Override
    protected String getScriptParameterTemplateScript() {
        return "import pandas\n\n# idsw_main is a mandatory function, \n# the number of arguments has to be the number of input ports (can be none)\ndef idsw_main(data):\n    print('Hello, world!')\n    # output can be found in Log View\n    print(type(data))\n\n    #your code goes here\n\n    #for example:\n    data2 = pandas.DataFrame([3,5,77,8])\n\n    # connect 2 output ports to see the results\n    return data, data2";
    }

    @Override
    protected String getScriptFileExtension() {
        return "py";
    }

    @Override
    public Path getPythonBinary() throws UndefinedParameterError {
        if (this.getParameterAsBoolean("use_default_python")) {
            return Paths.get("D:\\anaconda3\\python.exe");
            //PluginInitPythonScripting.getDefaultPythonBinary();
        } else {
            String packageManager = this.getParameterAsString("package_manager");
            String path;
            if (packageManager.equals(PluginInitPythonScripting.getPackageManager(0))) {
                path = this.getParameterAsString("conda_environment");
                if (path != null) {
                    return PythonSetupTester.INSTANCE.getFullPathForCondaEnvironment(path);
                } else {
                    throw new UndefinedParameterError("conda_environment");
                }
            } else if (packageManager.equals(PluginInitPythonScripting.getPackageManager(1))) {
                path = this.getParameterAsString("venvw_environment");
                if (path != null) {
                    return PythonSetupTester.INSTANCE.getFullPathForVenvwEnvironment(path);
                } else {
                    throw new UndefinedParameterError("venvw_environment");
                }
            } else {
                path = this.getParameterAsString("python_binary");
                if (path != null) {
                    return PythonSetupTester.INSTANCE.getFullPathForPythonBinary(path);
                } else {
                    throw new UndefinedParameterError("python_binary");
                }
            }
        }
    }

    @Override
    public String getPythonEnvironmentName() throws UndefinedParameterError {
        if (this.getParameterAsBoolean("use_default_python")) {
            return PluginInitPythonScripting.getDefaultPythonEnvironmentName();
        } else {
            String packageManager = this.getParameterAsString("package_manager");
            String paramName;
            if (packageManager.equals(PluginInitPythonScripting.getPackageManager(0))) {
                paramName = "conda_environment";
            } else if (packageManager.equals(PluginInitPythonScripting.getPackageManager(1))) {
                paramName = "venvw_environment";
            } else {
                paramName = "python_binary";
            }

            return this.getParameterAsString(paramName);
        }
    }
}
