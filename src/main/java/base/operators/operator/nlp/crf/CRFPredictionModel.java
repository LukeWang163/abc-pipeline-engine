package base.operators.operator.nlp.crf;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.operator.learner.PredictionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zls
 * create time:  2019.07.16.
 * description:
 */
public class CRFPredictionModel extends PredictionModel {

    private FeatureIndex featureIndex;

    public CRFPredictionModel(ExampleSet exampleSet, FeatureIndex featureIndex){
        super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
                ExampleSetUtilities.TypesCompareOption.EQUAL);
        this.featureIndex = featureIndex;
    }

    @Override
    public boolean supportsConfidences(Attribute label) {
        return false;
    }

    @Override
    public ExampleSet performPrediction(ExampleSet exampleSet, Attribute attribute){
        Attributes attributes = exampleSet.getAttributes();
        // 读取输入数据
        List<List<String>> text = new ArrayList<>();
        String id = "";
        List<String> temp = new ArrayList<>();
        for (Example row : exampleSet) {
            String docId = row.getValueAsString(attributes.getId());
            if (!id.equals(docId)) {
                id = docId;
                if (temp.size() != 0) {
                    text.add(temp);
                    temp = new ArrayList<>();
                }
            }
            StringBuilder builder = new StringBuilder();
            for (Attribute s : attributes) {

                String docContent = row.getValueAsString(s);
                builder.append(docContent);
                builder.append("\t");
            }

            String line = builder.toString();
            temp.add(line.substring(0, line.length()-1));

        }
        text.add(temp);
        List<String> result = new ArrayList<>();
        CRFPredict.run(featureIndex, text, result);

        if(result.size() != exampleSet.size()){
            System.err.println("失败");
            return null;
        }

        for(int i=0; i<result.size(); ++i){
            exampleSet.getExample(i).setValue(attribute, attribute.getMapping().mapString(result.get(i)));
        }
        return exampleSet;
    }
}
