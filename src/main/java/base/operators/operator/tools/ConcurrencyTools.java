package base.operators.operator.tools;

import base.operators.operator.ExecutionUnit;
import base.operators.operator.Operator;
import base.operators.tools.LogService;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class ConcurrencyTools
{
    private static final Field ENCLOSING_EXECUTION_UNIT = getEnclosingExecutionUnitField();

    public static <T extends Operator> T clone(T operator) {
        Operator clone = operator.cloneOperator(operator.getName(), true);
        setEnclosingProcess(clone, operator.getExecutionUnit());
        return (T)clone;
    }

    public static void setEnclosingProcess(Operator operator, ExecutionUnit process) {
        try {
            ENCLOSING_EXECUTION_UNIT.set(operator, process);
        } catch (SecurityException|IllegalAccessException|IllegalArgumentException|NullPointerException e) {
            throw new RuntimeException("Could not prepare concurrent execution of operator " + ((operator != null) ? operator.getName() : "null") + ".", e);
        }
    }


    private static Field getEnclosingExecutionUnitField() {
        Field enclosingExecutionUnit = null;
        try {
            enclosingExecutionUnit = Operator.class.getDeclaredField("enclosingExecutionUnit");
            enclosingExecutionUnit.setAccessible(true);
        } catch (SecurityException|IllegalArgumentException|NoSuchFieldException e) {
            LogService.getRoot().log(Level.SEVERE, "Could not access Operator.enclosingExecutionUnit", e);
        }
        return enclosingExecutionUnit;
    }
}
