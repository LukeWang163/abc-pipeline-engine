package base.operators.operator.nlp.crf;

/**
 * 字符映射接口
 */
public interface CharacterMapping
{
    int getInitSize();

    int getCharsetSize();

    int zeroId();

    int[] toIdList(String key);

    int[] toIdList(int codePoint);

    String toString(int[] ids);
}
