package base.operators.h2o.model.custom.gbm;

import base.operators.h2o.model.custom.H2O;


public class IcedBitSet
{
    private byte[] _val;
    private int _byteoff;
    private int _nbits;
    private int _bitoff;

    public IcedBitSet(int nbits) { this(nbits, 0); }



    public IcedBitSet(int nbits, int bitoff) {
        if (bitoff + nbits <= 32) {
            bitoff = 0;
            nbits = 32;
        }
        fill((nbits <= 0) ? null : new byte[bytes(nbits)], 0, nbits, bitoff);
    }



    public void fill(byte[] v, int byteoff, int nbits, int bitoff) {
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
        if (byteoff < 0) {
            throw new IndexOutOfBoundsException("byteoff < 0: " + byteoff);
        }
        if (bitoff < 0) {
            throw new IndexOutOfBoundsException("bitoff < 0: " + bitoff);
        }
        assert v == null || byteoff + (nbits - 1 >> 3) + 1 <= v.length;
        this._val = v;
        this._nbits = nbits;
        this._bitoff = bitoff;
        this._byteoff = byteoff;
    }

    public boolean contains(int idx) {
        if (idx < 0) {
            throw new IndexOutOfBoundsException("idx < 0: " + idx);
        }
        idx -= this._bitoff;
        return (idx >= 0 && idx < this._nbits && (this._val[this._byteoff + (idx >> 3)] & 1 << (idx & 0x7)) != 0);
    }

    public void set(int idx) {
        idx -= this._bitoff;
        if (idx < 0 || idx >= this._nbits) {
            throw new IndexOutOfBoundsException("Must have " + this._bitoff + " <= idx <= " + (this._bitoff + this._nbits - 1) + ": " + idx);
        }
        if (this._byteoff != 0) {
            throw H2O.fail();
        }
        this._val[idx >> 3] = (byte)(this._val[idx >> 3] | 1 << (idx & 0x7));
    }

    public void clear(int idx) {
        idx -= this._bitoff;
        if (idx < 0 || idx >= this._nbits) {
            throw new IndexOutOfBoundsException("Must have 0 <= idx <= " + Integer.toString(this._nbits - 1) + ": " + idx);
        }
        if (this._byteoff != 0) {
            throw H2O.fail();
        }
        this._val[idx >> 3] = (byte)(this._val[idx >> 3] & (1 << (idx & 0x7) ^ 0xFFFFFFFF));
    }

    public int cardinality() {
        int nbits = 0;
        int bytes = numBytes();
        if (this._byteoff != 0) {
            throw H2O.fail();
        }
        for (int i = 0; i < bytes; i++) {
            nbits += Integer.bitCount(this._val[i]);
        }
        return nbits;
    }


    public int size() { return this._nbits; }



    private static int bytes(int nbits) { return (nbits - 1 >> 3) + 1; }



    public int numBytes() { return bytes(this._nbits); }



    public int max() { return this._bitoff + this._nbits; }


    public void fill2(byte[] bits, AutoBuffer ab) {
        fill(bits, ab.position(), 32, 0);
        ab.skip(4);
    }


    public void fill3(byte[] bits, AutoBuffer ab) {
        int bitoff = ab.get2();
        int nbytes = ab.get2();
        fill(bits, ab.position(), nbytes << 3, bitoff);
        ab.skip(nbytes);
    }

    public String toStrArray() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(this._val[this._byteoff]);
        int bytes = bytes(this._nbits);
        for (int i = 1; i < bytes; i++) {
            sb.append(", ").append(this._val[this._byteoff + i]);
        }
        sb.append("}");
        return sb.toString();
    }
}
