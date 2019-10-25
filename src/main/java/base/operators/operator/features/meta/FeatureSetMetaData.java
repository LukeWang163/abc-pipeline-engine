package base.operators.operator.features.meta;

import base.operators.operator.features.Feature;
import base.operators.operator.features.FeatureSet;
import base.operators.operator.features.FeatureSetIOObject;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.tools.Tools;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FeatureSetMetaData extends MetaData {
    private static final long serialVersionUID = 3962947286564720337L;
    private static final int MAX_FEATURES_SHORTENED = 100;
    private List<Feature> allFeatures = new LinkedList();

    public FeatureSetMetaData() {
        super(FeatureSetIOObject.class);
    }

    FeatureSetMetaData(MetaData metaData) {
        super(FeatureSetIOObject.class);
        if (metaData instanceof ExampleSetMetaData) {
            ExampleSetMetaData exampleSetMD = (ExampleSetMetaData)metaData;
            Iterator var3 = exampleSetMD.getAllAttributes().iterator();

            while(var3.hasNext()) {
                AttributeMetaData amd = (AttributeMetaData)var3.next();
                this.allFeatures.add(new Feature(amd.getName(), "[" + amd.getName() + "]", amd.isNumerical(), 1));
            }
        }

    }

    public FeatureSetMetaData(FeatureSetIOObject featureSetIOObject, boolean shortened) {
        super(FeatureSetIOObject.class);
        FeatureSet featureSet = featureSetIOObject.getFeatureSet();
        int maxFeatures = featureSet.getNumberOfFeatures();
        if (shortened) {
            maxFeatures = Math.min(featureSet.getNumberOfFeatures(), 100);
        }

        for(int i = 0; i < maxFeatures; ++i) {
            this.allFeatures.add(new Feature(featureSet.get(i)));
        }

    }

    ExampleSetMetaData transformExampleSetMetaData(ExampleSetMetaData exampleSetMetaData) {
        ExampleSetMetaData clone = exampleSetMetaData.clone();
        clone.removeAllAttributes();
        Iterator var3 = this.allFeatures.iterator();

        while(var3.hasNext()) {
            Feature feature = (Feature)var3.next();
            if (feature.getComplexity() == 1) {
                clone.addAttribute(exampleSetMetaData.getAttributeByName(feature.getName()));
            } else {
                clone.addAttribute(new AttributeMetaData(feature.getName(), 2));
            }
        }

        return clone;
    }

    @Override
    public FeatureSetMetaData clone() {
        FeatureSetMetaData clone = (FeatureSetMetaData)super.clone();
        Iterator var2 = this.allFeatures.iterator();

        while(var2.hasNext()) {
            Feature feature = (Feature)var2.next();
            clone.allFeatures.add(new Feature(feature));
        }

        return clone;
    }

    public String getDescription() {
//        StringBuilder builder = new StringBuilder(super.getDescription());
        StringBuilder builder = new StringBuilder();
        builder.append("<br/>Number of features: ");
        builder.append(Integer.toString(this.allFeatures.size()));
        if (this.allFeatures.size() > 0) {
            builder.append("<table><thead><tr><th>Name</th><th>Expression</th><th>Complexity</th></tr></thead><tbody>");
            Iterator var2 = this.allFeatures.iterator();

            while(var2.hasNext()) {
                Feature feature = (Feature)var2.next();
                builder.append("<tr><td>");
                builder.append(feature.getName());
                builder.append("</td><td>");
                builder.append(feature.getExpression());
                builder.append("</td><td>");
                builder.append(feature.getComplexity());
                builder.append("</td></tr>");
            }

            builder.append("</tbody></table>");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetMetaData: #features: ").append(this.allFeatures.size()).append(Tools.getLineSeparator());
        Iterator var2 = this.allFeatures.iterator();

        while(var2.hasNext()) {
            Feature feature = (Feature)var2.next();
            builder.append(feature.toString()).append(Tools.getLineSeparator());
        }

        return builder.toString();
    }
}
