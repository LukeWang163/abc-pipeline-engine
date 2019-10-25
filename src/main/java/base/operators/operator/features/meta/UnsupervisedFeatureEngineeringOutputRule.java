package base.operators.operator.features.meta;

import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.clustering.CentroidClusterModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.CollectionMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.UndefinedParameterError;

public class UnsupervisedFeatureEngineeringOutputRule implements MDTransformationRule {
    private InputPort trainingDataInputPort;
    private InputPort innerClusteredDataSink;
    private InputPort innerClusterModelSink;
    private OutputPort featureSetOutputPort;
    private OutputPort populationOutputPort;
    private OutputPort optimizationLogOutputPort;
    private Operator parent;

    public UnsupervisedFeatureEngineeringOutputRule(InputPort trainingDataInputPort, InputPort innerClusteredDataSink, InputPort innerClusterModelSink, OutputPort featureSetOutputPort, OutputPort populationOutputPort, OutputPort optimizationLogOutputPort, Operator parent) {
        this.trainingDataInputPort = trainingDataInputPort;
        this.innerClusteredDataSink = innerClusteredDataSink;
        this.innerClusterModelSink = innerClusterModelSink;
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
                if (mode == 0) {
                    MetaData featureSetMD = new FeatureSetMetaData((ExampleSetMetaData)this.trainingDataInputPort.getMetaData());
                    featureSetMD.addToHistory(this.featureSetOutputPort);
                    this.featureSetOutputPort.deliverMD(featureSetMD);
                    CollectionMetaData collectionMetaData = new CollectionMetaData(featureSetMD.clone());
                    collectionMetaData.addToHistory(this.populationOutputPort);
                    this.populationOutputPort.deliverMD(collectionMetaData);
                    MetaData logMD = new MetaData(ExampleSet.class);
                    logMD.addToHistory(this.optimizationLogOutputPort);
                    this.optimizationLogOutputPort.deliverMD(logMD);
                    return;
                }

                if (this.innerClusteredDataSink.isConnected() && this.innerClusteredDataSink.getMetaData() != null && ExampleSet.class.isAssignableFrom(this.innerClusteredDataSink.getMetaData().getObjectClass()) && this.innerClusterModelSink.isConnected() && this.innerClusterModelSink.getMetaData() != null && CentroidClusterModel.class.isAssignableFrom(this.innerClusterModelSink.getMetaData().getObjectClass())) {
                    MetaData featureSetMD = new FeatureSetMetaData((ExampleSetMetaData)this.trainingDataInputPort.getMetaData());
                    featureSetMD.addToHistory(this.featureSetOutputPort);
                    this.featureSetOutputPort.deliverMD(featureSetMD);
                    CollectionMetaData collectionMetaData = new CollectionMetaData(featureSetMD.clone());
                    collectionMetaData.addToHistory(this.populationOutputPort);
                    this.populationOutputPort.deliverMD(collectionMetaData);
                    MetaData logMD = new MetaData(ExampleSet.class);
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
