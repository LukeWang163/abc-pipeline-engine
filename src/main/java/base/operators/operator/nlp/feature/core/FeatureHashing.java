package base.operators.operator.nlp.feature.core;

import base.operators.operator.nlp.feature.core.assist.MurmurHash3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 对于给定分好词的文档，产生针对于n-gram文本信息的哈希特征。
 */
public class FeatureHashing {
    //hashBiteSize:哈希位数,如果不选默认为10
    public int hashBiteSize;
    //ngramSize:ngram长度
    public int ngramSize;

    public FeatureHashing(int hashBiteSize, int ngramSize){
        this.hashBiteSize = hashBiteSize;
        this.ngramSize = ngramSize;
    }
    /**
     * 给定单列文档（分好词的文档，以空格隔开），计算给定文档和n-gram的值，计算得到的n-gram文本信息的哈希特征。
     * @param text:待分析的文档,m行；
     * @return int[] result:每个文档的哈希特征值,result[dimension]代表第dimension个特征。
     */
    public int[] getSingleTextFeatureHashing(String text){
        if (hashBiteSize < 0) {
            hashBiteSize = 10;
        }
        //每列文档产生的特征维度
        int dimension = (int) (Math.pow(2, hashBiteSize));
        //文档最终哈希结果的存放result
        int[] result  = new int[dimension];
        //该文档按照空格切分
        List<String> text_split = new ArrayList<String>(Arrays.asList(text.split("\\s+")));
        //定义该文档的初始Hash特征，全为0
        Arrays.fill(result, 0);
        //产生该文档的ngram文本信息，以及计算相应哈希值。
        for (int j = 0; j < text_split.size(); j++){
            //根据文本长度和ngram的值确定遍历的右边界，针对(当前值下标+ngram)的大小进行分别赋值遍历的右边界。
            Integer rightBoundary = j + ngramSize > text_split.size()? text_split.size() - j : ngramSize;
            for (int u = 1; u <= rightBoundary; u++){
                String subListString = String.join("", text_split.subList(j, j + u));
                //得到该子串的哈希值
                int hashCode = MurmurHash3.murmurhash3_x86_32(subListString, 0, subListString.length(), 0x1234ABCD);
                //该子串的哈希值对Math.pow(2, hashBiteSize)取余，得到下标，并对该下标对应的值加1
                if (hashCode < 0){
                    int hashCodeRemainder = (int) (dimension - 1 - (-hashCode) % dimension);
                    result[hashCodeRemainder] = result[hashCodeRemainder] + 1;
                }else{
                    int hashCodeRemainder = (int) (hashCode % dimension);
                    result[hashCodeRemainder] = result[hashCodeRemainder] + 1;
                }
            }
        }

        return result;
    }
}
