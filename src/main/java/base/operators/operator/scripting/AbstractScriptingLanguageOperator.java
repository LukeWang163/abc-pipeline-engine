package base.operators.operator.scripting;

import base.operators.RapidMiner;
import base.operators.adaption.belt.IOTable;
import base.operators.adaption.belt.TableViewingTools;
import base.operators.operator.scripting.metadata.MetaDataCachingRule;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPortExtender;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeText;
import base.operators.parameter.TextType;
import base.operators.parameter.UndefinedParameterError;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

public abstract class AbstractScriptingLanguageOperator extends Operator {
    private static final String PARAMETER_SCRIPT_FILE = "script_file";
    private static final String PARAMETER_VIEW_SCRIPT = "view_script";
//    private final InputPort scriptInputPort = (InputPort)this.getInputPorts().createPort("script file");
//    private final FileInputPortHandler scriptInputHandler;
    private final InputPortExtender inExtender;
    private final OutputPortExtender outExtender;
    private final MetaDataCachingRule cachingRule;

    public AbstractScriptingLanguageOperator(OperatorDescription description) {
        super(description);
//        this.scriptInputHandler = new FileInputPortHandler(this, this.scriptInputPort, "script_file");
        this.inExtender = new InputPortExtender("input", this.getInputPorts());
        this.outExtender = new OutputPortExtender("output", this.getOutputPorts());
        this.cachingRule = new MetaDataCachingRule(this);
        this.inExtender.start();
        this.outExtender.start();
        if (!RapidMiner.getExecutionMode().isHeadless()) {
            this.getTransformer().addRule(this::showSetupProblems);
            this.getTransformer().addRule(this.cachingRule);
        }

    }

    protected abstract void showSetupProblems();

    protected abstract ScriptRunner getScriptRunner() throws UndefinedParameterError;

    public OutputPortExtender getOutExtender(){
        return this.outExtender;
    }

    @Override
    public void doWork() throws OperatorException {
        ScriptRunner scriptRunner = this.getScriptRunner();
        List<IOObject> inputs = this.checkInputTypes(scriptRunner);
        int numberOfOutputPorts = this.outExtender.getManagedPorts().size();

        try {
            List<IOObject> outputs = scriptRunner.run(inputs, numberOfOutputPorts);
            this.outExtender.deliver(outputs);
            this.cachingRule.setOperatorWorked();
        } catch (CancellationException var5) {
            this.checkForStop();
            throw new OperatorException("python_scripting.execution_interruption", var5, new Object[0]);
        } catch (IOException var6) {
            throw new OperatorException("python_scripting.execution_failed", var6, new Object[0]);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> parameterTypes = super.getParameterTypes();
        ParameterTypeText scriptParameter = new ParameterTypeText(this.getScriptParameterName(), this.getScriptParameterDescription(), this.getScriptParameterTextType(), this.getScriptParameterTemplateScript());
        scriptParameter.setExpert(false);
        scriptParameter.setTemplateText(this.getScriptParameterTemplateScript());
        scriptParameter.setPrimary(true);
        parameterTypes.add(scriptParameter);
//        ParameterType scriptFileParameter = new ParameterTypeFile("script_file", "A file containing an executable script", true, new String[]{this.getScriptFileExtension()});
//        scriptFileParameter.registerDependencyCondition(new PortConnectedCondition(this, () -> {
//            return this.scriptInputPort;
//        }, false, false));
//        scriptFileParameter.setExpert(false);
//        scriptFileParameter.setOptional(true);
//        scriptFileParameter.setPrimary(true);
//        scriptParameter.registerDependencyCondition(new ParameterCondition(this, true) {
//            @Override
//            public boolean isConditionFullfilled() {
//                return !AbstractScriptingLanguageOperator.this.hasScriptFile();
//            }
//        });
//        scriptParameter.registerDependencyCondition(new PortConnectedCondition(this, () -> {
//            return this.scriptInputPort;
//        }, true, false));
//        ParameterCondition paraCondition = new ParameterCondition(this, false) {
//            @Override
//            public boolean isConditionFullfilled() {
//                return AbstractScriptingLanguageOperator.this.hasScriptFile();
//            }
//        };
//        PortConnectedCondition portCondition = new PortConnectedCondition(this, () -> {
//            return this.scriptInputPort;
//        }, false, true);
//        parameterTypes.add(scriptFileParameter);
        return parameterTypes;
    }

//    private boolean hasScriptFile() {
//        try {
//            return this.getParameterAsFile("script_file") != null;
//        } catch (UserError var2) {
//            return false;
//        }
//    }

    protected String getScript() {
        try {
            return this.getParameter(this.getScriptParameterName());
        } catch (UndefinedParameterError var6) {
            return "";
        }
    }

    protected abstract String getScriptParameterName();

    protected abstract String getScriptParameterDescription();

    protected abstract TextType getScriptParameterTextType();

    protected abstract String getScriptParameterTemplateScript();

    protected abstract String getScriptFileExtension();

    private List<IOObject> checkInputTypes(ScriptRunner scriptRunner) throws UserError {
        List<Class<? extends IOObject>> supportedTypes = scriptRunner.getSupportedTypes();
        List<IOObject> inputs = this.inExtender.getData(IOObject.class, false);

        for(int index = 0; index < inputs.size(); ++index) {
            IOObject input = (IOObject)inputs.get(index);
            if (input instanceof IOTable) {
                inputs.set(index, TableViewingTools.getView((IOTable)input));
            } else {
                boolean contained = false;
                Iterator var7 = supportedTypes.iterator();

                while(var7.hasNext()) {
                    Class<? extends IOObject> type = (Class)var7.next();
                    if (type.isInstance(input)) {
                        contained = true;
                        break;
                    }
                }

                if (!contained) {
                    throw new UserError(this, "python_scripting.wrong_input", new Object[]{});
                }
            }
        }

        return inputs;
    }

}
