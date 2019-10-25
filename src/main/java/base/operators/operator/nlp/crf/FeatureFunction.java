/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/12/9 20:57</create-date>
 *
 * <copyright file="FeatureFunction.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package base.operators.operator.nlp.crf;

import java.io.DataOutputStream;

/**
 * 特征函数，其实是tag.size个特征函数的集合
 * @author hankcs
 */
public class FeatureFunction implements ICacheAble
{
    /**
     * 环境参数
     */
    char[] o;
    /**
     * 标签参数
     */
//    String s;

    /**
     * 权值，按照index对应于tag的id
     */
    double[] w;

    public FeatureFunction(char[] o, int tagSize)
    {
        this.o = o;
        w = new double[tagSize];
    }

    public FeatureFunction()
    {
    }

    public FeatureFunction(String o, int tagSize)
    {
        this(o.toCharArray(), tagSize);
    }

    @Override
    public void save(DataOutputStream out) throws Exception
    {
        out.writeInt(o.length);
        for (char c : o)
        {
            out.writeChar(c);
        }
        out.writeInt(w.length);
        for (double v : w)
        {
            out.writeDouble(v);
        }
    }


}
