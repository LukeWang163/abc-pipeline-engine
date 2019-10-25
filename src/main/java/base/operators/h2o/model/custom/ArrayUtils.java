package base.operators.h2o.model.custom;

public class ArrayUtils {
    public ArrayUtils() {
    }

    public static double[] mult(double[] nums, double n) {
        for(int i = 0; i < nums.length; ++i) {
            nums[i] *= n;
        }

        return nums;
    }

    public static int[] unpackInts(long... longs) {
        int len = 2 * longs.length;
        int[] result = new int[len];
        int i = 0;
        long[] var4 = longs;
        int var5 = longs.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            long l = var4[var6];
            result[i++] = (int)(l & 4294967295L);
            result[i++] = (int)(l >> 32);
        }

        return result;
    }

    public static double maxValue(double[] ary) {
        return maxValue(ary, 0, ary.length);
    }

    public static double maxValue(double[] ary, int from, int to) {
        double result = ary[from];

        for(int i = from + 1; i < to; ++i) {
            if (ary[i] > result) {
                result = ary[i];
            }
        }

        return result;
    }

    public static int maxIndex(double[] from) {
        int result = 0;

        for(int i = 1; i < from.length; ++i) {
            if (from[i] > from[result]) {
                result = i;
            }
        }

        return result;
    }
}
