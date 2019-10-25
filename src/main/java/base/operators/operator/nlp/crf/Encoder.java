package base.operators.operator.nlp.crf;




import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 训练入口
 *
 * @author zhifac
 */
public class Encoder
{
    public static int MODEL_VERSION = 100;

    public enum Algorithm
    {
        CRF_L2, CRF_L1, MIRA;

        public static Algorithm fromString(String algorithm)
        {
            algorithm = algorithm.toLowerCase();
            if (algorithm.equals("crf") || algorithm.equals("crf-l2"))
            {
                return Algorithm.CRF_L2;
            }
            else if (algorithm.equals("crf-l1"))
            {
                return Algorithm.CRF_L1;
            }
            else if (algorithm.equals("mira"))
            {
                return Algorithm.MIRA;
            }
            throw new IllegalArgumentException("invalid algorithm: " + algorithm);
        }
    }

    public Encoder()
    {
    }

    /**
     * 训练
     * @param template    模板
     * @param text     训练数据
     * @param ops
     * @param path     上传地址
     * @param textModelFile 是否输出文本形式的模型文件
     * @param maxitr        最大迭代次数
     * @param freq          特征最低频次
     * @param eta           收敛阈值
     * @param C             cost-factor
     * @param threadNum     线程数
     * @param shrinkingSize
     * @param algorithm     训练算法
     * @return
     */
    public FeatureIndex learn(String template, List<List<String> > text, boolean textModelFile,
                         int maxitr, int freq, double eta, double C, int threadNum, int shrinkingSize,
                         Algorithm algorithm) {
        if (eta <= 0) {
            System.err.println("eta must be > 0.0");
            return null;
        }
        if (C < 0.0) {
            System.err.println("C must be >= 0.0");
            return null;
        }
        if (shrinkingSize < 1) {
            System.err.println("shrinkingSize must be >= 1");
            return null;
        }
        if (threadNum <= 0) {
            System.err.println("thread must be  > 0");
            return null;
        }
        EncoderFeatureIndex featureIndex = new EncoderFeatureIndex(threadNum);
        List<TaggerImpl> x = new ArrayList<TaggerImpl>();
        if (!featureIndex.open(template, text)) {
            System.err.println("Fail to open " + template + "/text");
        }


        int lineNo = 0;
        for (List<String> list : text) {
            TaggerImpl tagger = new TaggerImpl(TaggerImpl.Mode.LEARN);
            tagger.open(featureIndex);
            // TaggerImpl.ReadStatus status = tagger.read(br);
            TaggerImpl.ReadStatus status = tagger.process(list);
            if (status == TaggerImpl.ReadStatus.ERROR) {
                System.err.println("error when reading " + list);
                return null;
            }
            if (!tagger.empty()) {
                if (!tagger.shrink()) {
                    System.err.println("fail to build feature index ");
                    return null;
                }
                tagger.setThread_id_(lineNo % threadNum);
                x.add(tagger);
            }

            if (++lineNo % 100 == 0) {
                System.out.print(lineNo + ".. ");
            }
        }

        featureIndex.shrink(freq, x);

        double[] alpha = new double[featureIndex.size()];
        Arrays.fill(alpha, 0.0);
        featureIndex.setAlpha_(alpha);

        System.out.println("Number of sentences: " + x.size());
        System.out.println("Number of features:  " + featureIndex.size());
        System.out.println("Number of thread(s): " + threadNum);
        System.out.println("Freq:                " + freq);
        System.out.println("eta:                 " + eta);
        System.out.println("C:                   " + C);
        System.out.println("shrinking size:      " + shrinkingSize);

        switch (algorithm) {
            case CRF_L1:
                if (!runCRF(x, featureIndex, alpha, maxitr, C, eta, shrinkingSize, threadNum, true)) {
                    System.err.println("CRF_L1 execute error");
                    return null;
                }
                break;
            case CRF_L2:
                if (!runCRF(x, featureIndex, alpha, maxitr, C, eta, shrinkingSize, threadNum, false)) {
                    System.err.println("CRF_L2 execute error");
                    return null;
                }
                break;
            default:
                break;
        }


        return featureIndex;

    }

    /**
     * CRF训练
     *
     * @param x             句子列表
     * @param featureIndex  特征编号表
     * @param alpha         特征函数的代价
     * @param maxItr        最大迭代次数
     * @param C             cost factor
     * @param eta           收敛阈值
     * @param shrinkingSize 未使用
     * @param threadNum     线程数
     * @param orthant       是否使用L1范数
     * @return 是否成功
     */
    private boolean runCRF(List<TaggerImpl> x,
                           EncoderFeatureIndex featureIndex,
                           double[] alpha,
                           int maxItr,
                           double C,
                           double eta,
                           int shrinkingSize,
                           int threadNum,
                           boolean orthant)
    {
        double oldObj = 1e+37;
        int converge = 0;
        LbfgsOptimizer lbfgs = new LbfgsOptimizer();
        List<CRFEncoderThread> threads = new ArrayList<CRFEncoderThread>();

        for (int i = 0; i < threadNum; i++)
        {
            CRFEncoderThread thread = new CRFEncoderThread(alpha.length);
            thread.start_i = i;
            thread.size = x.size();
            thread.threadNum = threadNum;
            thread.x = x;
            threads.add(thread);
        }

        int all = 0;
        for (int i = 0; i < x.size(); i++)
        {
            all += x.get(i).size();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        for (int itr = 0; itr < maxItr; itr++)
        {
            featureIndex.clear();

            try
            {
                executor.invokeAll(threads);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }

            for (int i = 1; i < threadNum; i++)
            {
                threads.get(0).obj += threads.get(i).obj;
                threads.get(0).err += threads.get(i).err;
                threads.get(0).zeroone += threads.get(i).zeroone;
            }
            for (int i = 1; i < threadNum; i++)
            {
                for (int k = 0; k < featureIndex.size(); k++)
                {
                    threads.get(0).expected[k] += threads.get(i).expected[k];
                }
            }
            int numNonZero = 0;
            if (orthant)
            {
                for (int k = 0; k < featureIndex.size(); k++)
                {
                    threads.get(0).obj += Math.abs(alpha[k] / C);
                    if (alpha[k] != 0.0)
                    {
                        numNonZero++;
                    }
                }
            }
            else
            {
                numNonZero = featureIndex.size();
                for (int k = 0; k < featureIndex.size(); k++)
                {
                    threads.get(0).obj += (alpha[k] * alpha[k] / (2.0 * C));
                    threads.get(0).expected[k] += alpha[k] / C;
                }
            }
            for (int i = 1; i < threadNum; i++)
            {
                // try to free some memory
                threads.get(i).expected = null;
            }

            double diff = (itr == 0 ? 1.0 : Math.abs(oldObj - threads.get(0).obj) / oldObj);
            StringBuilder b = new StringBuilder();
            b.append("iter=").append(itr);
            b.append(" terr=").append(1.0 * threads.get(0).err / all);
            b.append(" serr=").append(1.0 * threads.get(0).zeroone / x.size());
            b.append(" act=").append(numNonZero);
            b.append(" obj=").append(threads.get(0).obj);
            b.append(" diff=").append(diff);
            System.out.println(b.toString());

            oldObj = threads.get(0).obj;

            if (diff < eta)
            {
                converge++;
            }
            else
            {
                converge = 0;
            }

            if (itr > maxItr || converge == 3)
            {
                break;
            }

            int ret = lbfgs.optimize(featureIndex.size(), alpha, threads.get(0).obj, threads.get(0).expected, orthant, C);
            if (ret <= 0)
            {
                return false;
            }
        }
        executor.shutdown();
        try
        {
            executor.awaitTermination(-1, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("fail waiting server to shutdown");
        }
        return true;
    }



    public static void main(String[] args)
    {
        if (args.length < 3)
        {
            System.err.println("incorrect No. of args");
            return;
        }
        String templFile = args[0];
        String trainFile = args[1];
        String modelFile = args[2];
        Encoder enc = new Encoder();
        long time1 = new Date().getTime();
       // if (!enc.learn(templFile, trainFile, modelFile, false, 100000, 1, 0.0001, 1.0, 1, 20, Algorithm.CRF_L2))
        {
            System.err.println("error training model");
            return;
        }
      //  System.out.println(new Date().getTime() - time1);
    }
}
