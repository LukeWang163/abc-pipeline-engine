package base.operators.operator.legacy.io;

import base.operators.operator.AbstractIOObject;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractReader;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.OperatorService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class IOObjectReader
        extends AbstractReader<IOObject>
{
    public static final String PARAMETER_OBJECT_FILE = "object_file";
    public static final String PARAMETER_IO_OBJECT = "io_object";
    public static final String PARAMETER_IGNORE_TYPE = "ignore_type";

    static  {
        AbstractReader.registerReaderDescription(new AbstractReader.ReaderDescription("ioo", IOObjectReader.class, "object_file"));
    }

    private String[] objectArray = null;


    public IOObjectReader(OperatorDescription description) { super(description, IOObject.class); }

    @Override
    public MetaData getGeneratedMetaData() throws OperatorException {
        if (!getParameterAsBoolean("ignore_type")) {
            try {
                return new MetaData(getSelectedClass());
            } catch (UndefinedParameterError undefinedParameterError) {}
        }

        return super.getGeneratedMetaData();
    }

    @Override
    public IOObject read() throws OperatorException {
        IOObject object;
        getParameter("object_file");
        AbstractIOObject.InputStreamProvider inputStreamProvider = () -> {
            try {
                return getParameterAsInputStream("object_file");
            } catch (UserError e) {
                throw new IOException(e);
            }
        };
        try {
            object = AbstractIOObject.read(inputStreamProvider);
        } catch (IOException e) {
            throw new UserError(this, e, 302, new Object[] { getParameter("object_file"), e });
        }

        if (object == null) {
            throw new UserError(this, 302, new Object[] { getParameter("object_file"), "cannot load object file" });
        }
        Class<?> clazz = getSelectedClass();
        if (!clazz.isInstance(object) && !getParameterAsBoolean("ignore_type")) {
            throw new UserError(this, 942, new Object[] { getParameter("object_file"), clazz.getSimpleName(), object.getClass().getSimpleName() });
        }
        return object;
    }

    private Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
        int ioType = getParameterAsInt("io_object");
        if (getIOObjectNames() != null) {
            if (ioType != -1) {
                return OperatorService.getIOObjectClass(getIOObjectNames()[ioType]);
            }
            return IOObject.class;
        }

        return null;
    }

    private String[] getIOObjectNames() {
        if (this.objectArray == null) {
            Set<String> ioObjects = OperatorService.getIOObjectsNames();
            this.objectArray = (String[])ioObjects.toArray(new String[0]);
        }
        return this.objectArray;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("object_file", "Filename of the object file.", "ioo", false));
        types.add(new ParameterTypeBoolean("ignore_type", "Indicates if the execution should be aborted if type of read object does not match selected type.", false));

        ParameterTypeCategory parameterTypeCategory = new ParameterTypeCategory("io_object", "The class of the object(s) which should be saved.", getIOObjectNames(), 0, false);
        parameterTypeCategory.registerDependencyCondition(new BooleanParameterCondition(this, "ignore_type", false, false));
        types.add(parameterTypeCategory);

        return types;
    }
}
