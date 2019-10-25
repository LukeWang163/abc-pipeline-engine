package base.operators.h2o.model.custom.gbm;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public class CompressedTree implements Serializable {
    private static final long serialVersionUID = 2477713587346345434L;
    final byte[] _bits = null;
    final int _nclass = 0;
    final long _seed = 0L;

    public CompressedTree() {
    }

    public double score(double[] row) {
        return this.score(row, false);
    }

    public double score(double[] row, boolean computeLeafAssignment) {
        AutoBuffer ab = new AutoBuffer(this._bits);
        IcedBitSet ibs = null;
        long bitsRight = 0L;
        int level = 0;

        int lmask;
        do {
            int nodeType = ab.get1U();
            int colId = ab.get2();
            if (colId == '\uffff') {
                return (double)this.scoreLeaf(ab);
            }

            int equal = (nodeType & 12) >> 2;

            assert equal >= 0 && equal <= 3 : "illegal equal value " + equal + " at " + ab + " in bitpile " + Arrays.toString(this._bits);

            float splitVal = -1.0F;
            if (equal != 0 && equal != 1) {
                if (ibs == null) {
                    ibs = new IcedBitSet(0);
                }

                if (equal == 2) {
                    ibs.fill2(this._bits, ab);
                } else {
                    ibs.fill3(this._bits, ab);
                }
            } else {
                splitVal = ab.get4f();
            }

            lmask = nodeType & 51;
            int rmask = (nodeType & 192) >> 2;
            int skip = 0;
            switch(lmask) {
                case 0:
                    skip = ab.get1U();
                    break;
                case 1:
                    skip = ab.get2();
                    break;
                case 2:
                    skip = ab.get3();
                    break;
                case 3:
                    skip = ab.get4();
                    break;
                case 16:
                    skip = this._nclass < 256 ? 1 : 2;
                    break;
                case 48:
                    skip = 4;
                    break;
                default:
                    assert false : "illegal lmask value " + lmask + " at " + ab + " in bitpile " + Arrays.toString(this._bits);
            }

            double d = row[colId];
            if (Double.isNaN(d) || equal == 0 && d >= (double)splitVal || equal == 1 && d == (double)splitVal || (equal == 2 || equal == 3) && ibs.contains((int)d)) {
                ab.skip(skip);
                if (computeLeafAssignment && level < 64) {
                    bitsRight |= (long)(1 << level);
                }

                lmask = rmask;
            } else {
                assert !Double.isNaN(d);
            }

            ++level;
        } while((lmask & 16) != 16);

        if (computeLeafAssignment) {
            bitsRight |= (long)(1 << level);
            return Double.longBitsToDouble(bitsRight);
        } else {
            return (double)this.scoreLeaf(ab);
        }
    }

    public String getDecisionPath(double[] row) {
        double d = this.score(row, true);
        long l = Double.doubleToRawLongBits(d);
        StringBuilder sb = new StringBuilder();
        int pos = 0;

        for(int i = 0; i < 64; ++i) {
            long right = l >> i & 1L;
            sb.append(right == 1L ? "R" : "L");
            if (right == 1L) {
                pos = i;
            }
        }

        return sb.substring(0, pos);
    }

    private float scoreLeaf(AutoBuffer ab) {
        return ab.get4f();
    }

    public Random rngForChunk(int cidx) {
        Random rand = new Random(this._seed);

        for(int i = 0; i < cidx; ++i) {
            rand.nextLong();
        }

        long seed = rand.nextLong();
        return new Random(seed);
    }
}
