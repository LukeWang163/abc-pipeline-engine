package base.operators.h2o.model.custom;

public abstract class Iced<D extends Iced> extends Object implements Cloneable {
    @Override
    public final D clone() {
        try {
            return (D)(Iced)super.clone();
        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }
    }
}
