package base.operators.operator.collections;

import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.performance.PerformanceCriterion;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.tools.math.Averagable;
import base.operators.tools.math.RunVector;
import java.util.Iterator;
import java.util.List;

public class PerformanceRobustAverageBuilder extends Operator {
    private InputPortExtender inExtender = new InputPortExtender("performance vectors", this.getInputPorts(), new MetaData(PerformanceVector.class), 2);
    private final OutputPort runOutput = (OutputPort)this.getOutputPorts().createPort("average");

    public PerformanceRobustAverageBuilder(OperatorDescription description) {
        super(description);
        this.inExtender.start();
        this.getTransformer().addRule(this.inExtender.makeFlatteningPassThroughRule(this.runOutput));
    }

    @Override
    public void doWork() throws OperatorException {
        List<PerformanceVector> averageVectors = this.inExtender.getData(PerformanceVector.class, true);
        Class<? extends PerformanceVector> clazz = null;
        Iterator var3 = averageVectors.iterator();

        PerformanceVector result;
        while(var3.hasNext()) {
            result = (PerformanceVector)var3.next();
            if (clazz == null) {
                clazz = result.getClass();
            } else if (!result.getClass().equals(clazz)) {
                this.getLogger().warning("Received inputs of different types (" + clazz.getName() + " and " + result.getName() + "). Ignoring the latter.");
            }
        }

        if (averageVectors.size() > 2) {
            int smallestIndex = -1;
            int largestIndex = -1;
            double smallestFitness = 1.0D / 0.0;
            double largestFitness = -1.0D / 0.0;
            int counter = 0;

            for(Iterator var10 = averageVectors.iterator(); var10.hasNext(); ++counter) {
                PerformanceVector performanceVector = (PerformanceVector)var10.next();
                PerformanceCriterion mainCriterion = performanceVector.getMainCriterion();
                if (mainCriterion != null) {
                    double fitness = mainCriterion.getFitness();
                    if (fitness < smallestFitness) {
                        smallestFitness = fitness;
                        smallestIndex = counter;
                    }

                    if (fitness > largestFitness) {
                        largestFitness = fitness;
                        largestIndex = counter;
                    }
                }
            }

            if (smallestIndex > largestIndex) {
                if (smallestIndex >= 0) {
                    averageVectors.remove(smallestIndex);
                }

                if (largestIndex >= 0) {
                    averageVectors.remove(largestIndex);
                }
            } else {
                if (largestIndex >= 0) {
                    averageVectors.remove(largestIndex);
                }

                if (smallestIndex >= 0) {
                    averageVectors.remove(smallestIndex);
                }
            }
        }

        if (averageVectors.size() == 0) {
            throw new UserError(this, "averagable_input_missing", new Object[]{Averagable.class});
        } else {
            if (averageVectors.size() == 1) {
                this.runOutput.deliver((IOObject)averageVectors.get(0));
            } else {
                RunVector runVector = new RunVector();
                Iterator var18 = averageVectors.iterator();

                while(var18.hasNext()) {
                    PerformanceVector performanceVector = (PerformanceVector)var18.next();
                    runVector.addVector(performanceVector);
                }

                result = (PerformanceVector)runVector.average();
                result.setMainCriterionName(((PerformanceVector)averageVectors.get(0)).getMainCriterion().getName());
                this.runOutput.deliver(result);
            }

        }
    }
}
