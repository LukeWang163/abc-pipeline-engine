package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.RandomUtils;
import base.operators.h2o.model.custom.deeplearning.Storage;

import java.util.Arrays;
import java.util.Random;

public class Dropout {
    private Random _rand;
    private byte[] _bits;
    private double _rate;

    public byte[] bits() { return this._bits; }

    @Override
    public String toString() {
        String s = "Dropout: " + super.toString();
        s = s + "\nRandom: " + this._rand.toString();
        s = s + "\nDropout rate: " + this._rate;
        s = s + "\nbits: ";
        for (int i = 0; i < this._bits.length * 8; i++) {
            s = s + (unit_active(i) ? "1" : "0");
        }
        return s + "\n";
    }


    Dropout(int units) {
        this._bits = new byte[(units + 7) / 8];
        this._rand = RandomUtils.getRNG(new long[] { 0L });
        this._rate = 0.5D;
    }

    Dropout(int units, double rate) {
        this(units);
        this._rate = rate;
    }

    public void randomlySparsifyActivation(Storage.Vector a, long seed) {
        if (a instanceof Storage.DenseVector) {
            randomlySparsifyActivation((Storage.DenseVector)a, seed);
        } else {
            throw new UnsupportedOperationException("randomlySparsifyActivation not implemented for this type: " + a
                    .getClass().getSimpleName());
        }
    }


    private void randomlySparsifyActivation(Storage.DenseVector a, long seed) {
        if (this._rate == 0.0D) {
            return;
        }
        setSeed(seed);
        for (int i = 0; i < a.size(); i++) {
            if (this._rand.nextFloat() < this._rate) {
                a.set(i, 0.0D);
            }
        }
    }


    public void fillBytes(long seed) {
        setSeed(seed);
        if (this._rate == 0.5D) {
            this._rand.nextBytes(this._bits);
        } else {
            Arrays.fill(this._bits, (byte)0);
            for (int i = 0; i < this._bits.length * 8; i++) {
                if (this._rand.nextFloat() > this._rate) {
                    this._bits[i / 8] = (byte)(this._bits[i / 8] | 1 << i % 8);
                }
            }
        }
    }


    public boolean unit_active(int o) { return ((this._bits[o / 8] & 1 << o % 8) != 0); }


    private void setSeed(long seed) {
        if (seed >>> 32 < 65535L) {
            seed |= 0x5B93000000000000L;
        }
        if (seed << 32 >>> 32 < 65535L) {
            seed |= 0xDB910000L;
        }
        this._rand.setSeed(seed);
    }
}
