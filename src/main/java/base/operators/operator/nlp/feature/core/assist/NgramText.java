package base.operators.operator.nlp.feature.core.assist;

import java.util.ArrayList;
import java.util.List;

public class NgramText {

    //输入的原始语句，分词之后的单词列表
    public List<String> rawText = new ArrayList<>();
    //ngram信息结果列表
    public List<String> ngramText = new ArrayList<>();;
    //ngram大小
    public int ngramSize;

    public NgramText(List<String> rawText, int ngramSize){
        this.rawText = rawText;
        this.ngramSize = ngramSize;
        getNgramText();
    }

    public void getNgramText(){

        //生成该文档的ngram文本信息。
        for (int j = 0; j < this.rawText.size(); j++){
            //根据文本长度和ngram的值确定遍历的右边界，针对(当前值下标+ngram)的大小进行分别赋值遍历的右边界。
            Integer rightBoundary = j + this.ngramSize > this.rawText.size()? this.rawText.size() - j : this.ngramSize;
            for (int u = 1; u <= rightBoundary; u++){
                String subListString = String.join(" ", this.rawText.subList(j, j + u));
                this.ngramText.add(subListString);
            }
        }
    }
}
