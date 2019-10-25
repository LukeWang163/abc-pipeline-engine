package base.operators.operator.generator.generators;

import base.operators.operator.Operator;
import base.operators.parameter.ParameterType;
import base.operators.parameter.conditions.EqualStringCondition;
import java.util.List;
import java.util.Objects;


abstract class AbstractDataGenerator implements base.operators.operator.generator.generators.DataGenerator {
    private final Operator parent;
    AbstractDataGenerator(Operator parent) {
        Objects.requireNonNull(parent, "parent must not be null");
        this.parent = parent;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> parameterTypes = getParameterTypesInternal();
        parameterTypes.forEach(type -> {
            type.registerDependencyCondition(new EqualStringCondition(getParent(), "generator_type", !type.isOptionalWithoutConditions(), new String[] {getGeneratorType().getName() }));
            type.setOptional(true);
        });
        return parameterTypes;
    }

    Operator getParent() { return this.parent; }

    protected abstract List<ParameterType> getParameterTypesInternal();

    protected abstract base.operators.operator.generator.generators.DataGenerators.GeneratorType getGeneratorType();
}

