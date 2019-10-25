package base.operators.operator.preprocessing.cleansing;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.preprocessing.meta.QualityOutputRule;
import base.operators.operator.preprocessing.statistics.PreparationStatistics;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class QualityOperator extends Operator {
    public static final String DATA_INPUT_PORT_NAME = "example set";
    public static final String DATA_OUTPUT_PORT_NAME = "example set";
    private InputPort dataInputPort = this.getInputPorts().createPort("example set", ExampleSet.class);
    private OutputPort dataOutputPort = (OutputPort)this.getOutputPorts().createPort("example set");

    public QualityOperator(OperatorDescription description) {
        super(description);
        QualityOutputRule rule = new QualityOutputRule(this.dataInputPort, this.dataOutputPort);
        this.getTransformer().addRule(rule);
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet data = (ExampleSet)this.dataInputPort.getData(ExampleSet.class);
        Attribute label = data.getAttributes().getLabel();
        List<Attribute> attributes = new LinkedList();
        Attribute nameAtt = AttributeFactory.createAttribute("Attribute", 1);
        attributes.add(nameAtt);
        if (label != null) {
            Attribute correlationAtt = AttributeFactory.createAttribute("Correlation", 4);
            attributes.add(correlationAtt);
        }

        Attribute idAtt = AttributeFactory.createAttribute("ID-ness", 4);
        attributes.add(idAtt);
        Attribute stabilityAtt = AttributeFactory.createAttribute("Stabillity", 4);
        attributes.add(stabilityAtt);
        Attribute missingAtt = AttributeFactory.createAttribute("Missing", 4);
        attributes.add(missingAtt);
        Attribute textNessAtt = AttributeFactory.createAttribute("Text-ness", 4);
        attributes.add(textNessAtt);
        ExampleSetBuilder builder = ExampleSets.from(attributes);
        data.recalculateAllAttributeStatistics();
        PreparationStatistics prepStats = new PreparationStatistics(true);
        Iterator var12 = data.getAttributes().iterator();

        while(var12.hasNext()) {
            Attribute a = (Attribute)var12.next();
            prepStats.updateStatistics(data, a, false);
            int index = 0;
            double[] row = new double[attributes.size()];
            row[index++] = (double)nameAtt.getMapping().mapString(a.getName());
            if (label != null) {
                row[index++] = prepStats.getQualityMeasurementsHandler().getCorrelation(a);
            }

            row[index++] = prepStats.getQualityMeasurementsHandler().getIDNess(a);
            row[index++] = prepStats.getQualityMeasurementsHandler().getStability(a);
            row[index++] = prepStats.getQualityMeasurementsHandler().getMissing(a);
            row[index] = prepStats.getQualityMeasurementsHandler().getTextNess(a);
            builder.addRow(row);
        }

        this.dataOutputPort.deliver(builder.build());
    }
}
