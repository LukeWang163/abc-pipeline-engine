package base.operators.h2o.model.custom;

public class MathUtils {
    public MathUtils() {
    }

    public static float approxInvSqrt(float x) {
        float xhalf = 0.5F * x;
        x = Float.intBitsToFloat(1597463007 - (Float.floatToIntBits(x) >> 1));
        return x * (1.5F - xhalf * x * x);
    }

    public static double approxSqrt(double x) {
        return Double.longBitsToDouble((Double.doubleToLongBits(x) >> 32) + 1072632448L << 31);
    }

    public static float approxSqrt(float x) {
        return Float.intBitsToFloat(532483686 + (Float.floatToRawIntBits(x) >> 1));
    }

    public static float sumSquares(float[] a, int from, int to) {
        assert from >= 0 && to <= a.length;

        float result = 0.0F;
        int cols = to - from;
        int extra = cols - cols % 8;
        int multiple = cols / 8 * 8 - 1;
        float psum1 = 0.0F;
        float psum2 = 0.0F;
        float psum3 = 0.0F;
        float psum4 = 0.0F;
        float psum5 = 0.0F;
        float psum6 = 0.0F;
        float psum7 = 0.0F;
        float psum8 = 0.0F;

        int c;
        for(c = from; c < from + multiple; c += 8) {
            psum1 += a[c] * a[c];
            psum2 += a[c + 1] * a[c + 1];
            psum3 += a[c + 2] * a[c + 2];
            psum4 += a[c + 3] * a[c + 3];
            psum5 += a[c + 4] * a[c + 4];
            psum6 += a[c + 5] * a[c + 5];
            psum7 += a[c + 6] * a[c + 6];
            psum8 += a[c + 7] * a[c + 7];
        }

        result += psum1 + psum2 + psum3 + psum4;
        result += psum5 + psum6 + psum7 + psum8;

        for(c = from + extra; c < to; ++c) {
            result += a[c] * a[c];
        }

        return result;
    }
}
