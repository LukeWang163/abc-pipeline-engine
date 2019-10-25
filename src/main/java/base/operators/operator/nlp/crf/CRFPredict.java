package base.operators.operator.nlp.crf;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * 对应crf_test
 *
 * @author zhifac
 */
public class CRFPredict
{
    private static class Option
    {
        @Argument(description = "set FILE for model file", alias = "m", required = true)
        String model;
        @Argument(description = "output n-best results", alias = "n")
        Integer nbest = 0;
        @Argument(description = "set INT for verbose level", alias = "v")
        Integer verbose = 0;
        @Argument(description = "set cost factor", alias = "c")
        Double cost_factor = 1.0;
        @Argument(description = "output file path", alias = "o")
        String output;
        @Argument(description = "show this help and exit", alias = "h")
        Boolean help = false;
    }

    public static boolean run(String[] args)
    {
        Option cmd = new Option();
        List<String> unkownArgs = null;
//        try
//        {
//            unkownArgs = Args.parse(cmd, args, false);
//        }
//        catch (IllegalArgumentException e)
//        {
//            Args.usage(cmd);
//            return false;
//        }
        if (cmd.help)
        {
            Args.usage(cmd);
            return true;
        }
        int nbest = cmd.nbest;
        int vlevel = cmd.verbose;
        double costFactor = cmd.cost_factor;
        //String model = cmd.model;
        //String outputFile = cmd.output;
        String[] restArgs = args;
        //String[] restArgs = unkownArgs.toArray(new String[0]);
        String model = restArgs[0];

        String outputFile = restArgs[2];

        TaggerImpl tagger = new TaggerImpl(TaggerImpl.Mode.TEST);
        try
        {
            InputStream stream = IOUtil.newInputStream(model);
            if (!tagger.open(stream, nbest, vlevel, costFactor))
            {
                System.err.println("open error");
                return false;
            }

            if (restArgs.length == 0)
            {
                return false;
            }

            OutputStreamWriter osw = null;
            if (outputFile != null)
            {
                osw = new OutputStreamWriter(IOUtil.newOutputStream(outputFile));
            }
            String inputFile = restArgs[1];

                InputStream fis = IOUtil.newInputStream(inputFile);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

                while (true)
                {
                    TaggerImpl.ReadStatus status = tagger.read(br);
                    if (TaggerImpl.ReadStatus.ERROR == status)
                    {
                        System.err.println("read error");
                        return false;
                    }
                    else if (TaggerImpl.ReadStatus.EOF == status && tagger.empty())
                    {
                        break;
                    }
                    if (!tagger.parse())
                    {
                        System.err.println("parse error");
                        return false;
                    }
                    if (osw == null)
                    {
                        System.out.print(tagger.toString());
                    }
                    else
                    {
                        osw.write(tagger.toString());
                    }
                }
                if (osw != null)
                {
                    osw.flush();
                }
                br.close();

            if (osw != null)
            {
                osw.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean run(FeatureIndex featureIndex, List<List<String> > text, List<String> result)
    {

        TaggerImpl tagger = new TaggerImpl(TaggerImpl.Mode.TEST);
        try {

            if (!tagger.open(featureIndex)) {
                System.err.println("open error");
                return false;
            }


            for (List<String> list : text) {

                TaggerImpl.ReadStatus status = tagger.process(list);
                if (TaggerImpl.ReadStatus.ERROR == status) {
                    System.err.println("read error");
                    return false;
                }

                if (!tagger.parse()) {
                    System.err.println("parse error");
                    return false;
                } else {
                    //result.add(tagger.toString());
                    tagger.resultString(result);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        return true;
    }

    public static void main(String[] args)
    {
        args = new String[]{"data2\\CRF.bin", "data2/text.txt","data2/out"};
        CRFPredict.run(args);
    }
}
