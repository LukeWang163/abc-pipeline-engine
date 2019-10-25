package base.operators.operator.nlp.crf;


import java.util.List;

/**
 * 对应crf_learn
 *
 * @author zhifac
 */
public class CRFLearn
{
    public static class Option
    {
        @Argument(description = "use features that occur no less than INT(default 1)", alias = "f")
        public Integer freq = 1;
        @Argument(description = "set INT for max iterations in LBFGS routine(default 10k)", alias = "m")
        public  Integer maxiter = 10000;
        @Argument(description = "set FLOAT for cost parameter(default 1.0)", alias = "c")
        public  Double cost = 1.0;
        @Argument(description = "set FLOAT for termination criterion(default 0.0001)", alias = "e")
        public  Double eta = 0.0001;
        @Argument(description = "build also text model file for debugging", alias = "t")
        public  Boolean textmodel = true;
        @Argument(description = "(CRF|CRF-L1|CRF-L2|MIRA)\", \"select training algorithm", alias = "a")
        public  String algorithm = "CRF-L2";
        @Argument(description = "set INT for number of iterations variable needs to be optimal before considered for shrinking. (default 20)", alias = "H")
        //该参数未使用
        public  Integer shrinking_size = 20;
        @Argument(description = "show this help and exit", alias = "h")
        public  Boolean help = false;
        @Argument(description = "number of threads(default auto detect)")
        public  Integer thread = Runtime.getRuntime().availableProcessors();
    }



    public static FeatureIndex run(String template, List text, Option option)
    {

        int freq = option.freq;
        int maxiter = option.maxiter;
        double C = option.cost;
        double eta = option.eta;
        boolean textmodel = option.textmodel;
        //暂时设为1
        int threadNum = 1;
        //int threadNum = Runtime.getRuntime().availableProcessors();

        int shrinkingSize = option.shrinking_size;
        String algorithm = option.algorithm;
        algorithm = algorithm.toLowerCase();
        Encoder.Algorithm algo = null;
        if (algorithm.equals("crf") || algorithm.equals("crf-l2"))
        {
            algo = Encoder.Algorithm.CRF_L2;
        }
        else if (algorithm.equals("crf-l1"))
        {
            algo = Encoder.Algorithm.CRF_L1;
        }
        else if (algorithm.equals("mira"))
        {
            algo = Encoder.Algorithm.MIRA;
        }
        else
        {
            System.err.println("unknown algorithm: " + algorithm);
            return null;
        }

        Encoder encoder = new Encoder();
        return encoder.learn(template, text, textmodel, maxiter, freq, eta, C, threadNum, shrinkingSize, algo);





    }

    public static void main(String[] args)
    {
        String [] arg = {"data2/template.txt","data2/corpus.tsv","data2/CRF.bin"};
        ///run(arg);
    }
}
