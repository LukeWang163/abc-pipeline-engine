package base.operators.operator.features.meta;

import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.CollectionMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.UndefinedParameterError;

public class AutomaticFeatureEngineeringOutputRule implements MDTransformationRule {
    private InputPort trainingDataInputPort;
    private InputPort innerPerformanceSink;
    private OutputPort featureSetOutputPort;
    private OutputPort populationOutputPort;
    private OutputPort optimizationLogOutputPort;
    private Operator parent;

    public AutomaticFeatureEngineeringOutputRule(InputPort trainingDataInputPort, InputPort innerPerformanceSink, OutputPort featureSetOutputPort, OutputPort populationOutputPort, OutputPort optimizationLogOutputPort, Operator parent) {
        this.trainingDataInputPort = trainingDataInputPort;
        this.innerPerformanceSink = innerPerformanceSink;
        this.featureSetOutputPort = featureSetOutputPort;
        this.populationOutputPort = populationOutputPort;
        this.optimizationLogOutputPort = optimizationLogOutputPort;
        this.parent = parent;
    }

    @Override
    public void transformMD() {
        if (this.trainingDataInputPort.isConnected() && this.trainingDataInputPort.getMetaData() != null && ExampleSet.class.isAssignableFrom(this.trainingDataInputPort.getMetaData().getObjectClass())) {
            try {
                int mode = this.parent.getParameterAsInt("mode");
                FeatureSetMetaData featureSetMD;
                CollectionMetaData populationMD;
                MetaData logMD;
                if (mode == 0) {
                    featureSetMD = new FeatureSetMetaData(this.trainingDataInputPort.getMetaData());
                    featureSetMD.addToHistory(this.featureSetOutputPort);
                    this.featureSetOutputPort.deliverMD(featureSetMD);
                    populationMD = new CollectionMetaData(featureSetMD.clone());
                    populationMD.addToHistory(this.populationOutputPort);
                    this.populationOutputPort.deliverMD(populationMD);
                    logMD = new MetaData(ExampleSet.class);
                    logMD.addToHistory(this.optimizationLogOutputPort);
                    this.optimizationLogOutputPort.deliverMD(logMD);
                    return;
                }

                if (this.innerPerformanceSink.isConnected() && this.innerPerformanceSink.getMetaData() != null && PerformanceVector.class.isAssignableFrom(this.innerPerformanceSink.getMetaData().getObjectClass())) {
                    featureSetMD = new FeatureSetMetaData((ExampleSetMetaData)this.trainingDataInputPort.getMetaData());
                    featureSetMD.addToHistory(this.featureSetOutputPort);
                    this.featureSetOutputPort.deliverMD(featureSetMD);
                    populationMD = new CollectionMetaData(featureSetMD.clone());
                    populationMD.addToHistory(this.populationOutputPort);
                    this.populationOutputPort.deliverMD(populationMD);
                    logMD = new MetaData(ExampleSet.class);
                    logMD.addToHistory(this.optimizationLogOutputPort);
                    this.optimizationLogOutputPort.deliverMD(logMD);
                    return;
                }
            } catch (UndefinedParameterError var5) {
                this.featureSetOutputPort.deliverMD((MetaData)null);
                this.populationOutputPort.deliverMD((MetaData)null);
                this.optimizationLogOutputPort.deliverMD((MetaData)null);
                return;
            }
        }

        this.featureSetOutputPort.deliverMD((MetaData)null);
        this.populationOutputPort.deliverMD((MetaData)null);
        this.optimizationLogOutputPort.deliverMD((MetaData)null);
    }
}
