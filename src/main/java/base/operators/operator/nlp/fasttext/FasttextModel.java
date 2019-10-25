package base.operators.operator.nlp.fasttext;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.operator.learner.PredictionModel;
import base.operators.tools.Tools;

/**
 * @author zls
 * create time:  2019.07.23.
 * description:
 */
public class FasttextModel extends PredictionModel {

    private Model model;
    Dictionary dict;

    public FasttextModel(ExampleSet exampleSet, Model model, Dictionary dict){
        super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
                ExampleSetUtilities.TypesCompareOption.EQUAL);
        this.model = model;
        this.dict = dict;
    }

    @Override
    public boolean supportsConfidences(Attribute label) {
        return false;
    }

    @Override
    public ExampleSet performPrediction(ExampleSet exampleSet, Attribute attribute){

       return new FastText().predict(exampleSet, model, dict, attribute, 1);
    }

    @Override
    public String getName() {
        return "Fasttext Model";
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        result.append("Total number of words: " + dict.nwords() + Tools.getLineSeparator());
        result.append("Total number of labels: " + dict.nlabels() + Tools.getLineSeparator());
        result.append("The following arguments are optional:"+ Tools.getLineSeparator()
                + "  -lr                 learning rate [" + dict.getArgs().lr + "]"+ Tools.getLineSeparator()
                + "  -lrUpdateRate       change the rate of updates for the learning rate [" + dict.getArgs().lrUpdateRate + "]"+ Tools.getLineSeparator()
                + "  -dim                size of word vectors [" + dict.getArgs().dim + "]"+ Tools.getLineSeparator()
                + "  -ws                 size of the context window [" + dict.getArgs().ws + "]"+ Tools.getLineSeparator()
                + "  -epoch              number of epochs [" + dict.getArgs().epoch + "]"+ Tools.getLineSeparator()
                + "  -minCount           minimal number of word occurences [" + dict.getArgs().minCount + "]"+ Tools.getLineSeparator()
                + "  -minCountLabel      minimal number of label occurences [" + dict.getArgs().minCountLabel + "]"+ Tools.getLineSeparator()
                + "  -neg                number of negatives sampled [" + dict.getArgs().neg + "]"+ Tools.getLineSeparator()
                + "  -wordNgrams         max length of word ngram [" + dict.getArgs().wordNgrams + "]"+ Tools.getLineSeparator()
                + "  -loss               loss function {ns, hs, softmax} [ns]"+ Tools.getLineSeparator()
                + "  -bucket             number of buckets [" + dict.getArgs().bucket + "]"+ Tools.getLineSeparator()
                + "  -minn               min length of char ngram [" +dict.getArgs(). minn + "]"+ Tools.getLineSeparator()
                + "  -maxn               max length of char ngram [" + dict.getArgs().maxn + "]"+ Tools.getLineSeparator()
                + "  -thread             number of threads [" + dict.getArgs().thread + "]"+ Tools.getLineSeparator()
                + "  -t                  sampling threshold [" + dict.getArgs().t + "]"+ Tools.getLineSeparator()
                + "  -label              labels prefix [" + dict.getArgs().label + "]"+ Tools.getLineSeparator()
                + "  -verbose            verbosity level [" + dict.getArgs().verbose + "]"+ Tools.getLineSeparator()
                + "  -pretrainedVectors  pretrained word vectors for supervised learning []");
        return result.toString();
    }
}
