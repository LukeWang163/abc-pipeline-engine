package base.operators.h2o.model.custom.gbm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Random;


public final class AutoBuffer {
    ByteBuffer _bb;
    private ByteChannel _chan;
    private OutputStream _os;
    private InputStream _is;
    private short[] _typeMap;
    private int _oldPrior;
    private boolean _read;
    private boolean _firstPage;
    int _size;
    long _time_start_ms;
    long _time_close_ms;
    long _time_io_ns;
    final byte _persist;
    static final int MTU = 1492;
    static final Random RANDOM_TCP_DROP = null;
    static final Charset UTF_8 = Charset.forName("UTF-8");
    private byte _msg_priority;
    private static final boolean DEBUG = Boolean.getBoolean("h2o.find-ByteBuffer-leaks");
    private static long HWM = 0L;
    static final String JSON_NAN = "NaN";
    static final String JSON_POS_INF = "Infinity";
    static final String JSON_NEG_INF = "-Infinity";

    public boolean isClosed() {
        return this._bb == null;
    }

    AutoBuffer(Object dummyH2o, byte[] buf, int off, int len) {
        this._oldPrior = -1;

        assert buf != null : "null fed to ByteBuffer.wrap";

        this._bb = ByteBuffer.wrap(buf, off, len).order(ByteOrder.nativeOrder());
        this._chan = null;
        this._read = true;
        this._firstPage = true;
        this._persist = 0;
        this._size = len;
    }

    public AutoBuffer(byte[] buf) {
        this((Object)null, buf, 0, buf.length);
    }

    int size() {
        return this._size;
    }

    public int position() {
        return this._bb.position();
    }

    public AutoBuffer position(int p) {
        this._bb.position(p);
        return this;
    }

    public void skip(int skip) {
        this._bb.position(this._bb.position() + skip);
    }

    private void raisePriority() {
        if (this._oldPrior == -1) {
            assert this._chan instanceof SocketChannel;

            this._oldPrior = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(9);
        }

    }

    private void restorePriority() {
        if (this._oldPrior != -1) {
            Thread.currentThread().setPriority(this._oldPrior);
            this._oldPrior = -1;
        }
    }

    AutoBuffer clearForWriting(byte priority) {
        assert this._read;

        this._read = false;
        this._msg_priority = priority;
        this._bb.clear();
        this._firstPage = true;
        return this;
    }

    public AutoBuffer flipForReading() {
        assert !this._read;

        this._read = true;
        this._bb.flip();
        this._firstPage = true;
        return this;
    }

    private ByteBuffer getSp(int sz) {
        return sz > this._bb.remaining() ? this.getImpl(sz) : this._bb;
    }

    private ByteBuffer getSz(int sz) {
        assert this._firstPage : "getSz() is only valid for early UDP bytes";

        if (sz > this._bb.limit()) {
            this.getImpl(sz);
        }

        this._bb.position(sz);
        return this._bb;
    }

    private ByteBuffer getImpl(int sz) {
        assert this._read : "Reading from a buffer in write mode";

        this._bb.compact();

        assert this._bb.position() + sz <= this._bb.capacity() : "(" + this._bb.position() + "+" + sz + " <= " + this._bb.capacity() + ")";

        long ns = System.nanoTime();

        while(this._bb.position() < sz) {
            try {
                int res = this._is == null ? this._chan.read(this._bb) : this._is.read(this._bb.array(), this._bb.position(), this._bb.remaining());
                if (res <= 0) {
                    throw new AutoBuffer.AutoBufferException(new EOFException("Reading " + sz + " bytes, AB=" + this));
                }

                if (this._is != null) {
                    this._bb.position(this._bb.position() + res);
                }

                this._size += res;
            } catch (IOException var5) {
                if (var5.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
                    throw new AutoBuffer.AutoBufferException(var5);
                }

                if (var5.getMessage().equals("An established connection was aborted by the software in your host machine")) {
                    throw new AutoBuffer.AutoBufferException(var5);
                }

                throw new RuntimeException(var5);
            }
        }

        this._time_io_ns += System.nanoTime() - ns;
        this._bb.flip();
        this._firstPage = false;
        return this._bb;
    }

    public String getStr(int off, int len) {
        return new String(this._bb.array(), this._bb.arrayOffset() + off, len, UTF_8);
    }

    public boolean getZ() {
        return this.get1() != 0;
    }

    public byte get1() {
        return this.getSp(1).get();
    }

    public int get1U() {
        return this.get1() & 255;
    }

    public char get2() {
        return this.getSp(2).getChar();
    }

    public int get3() {
        this.getSp(3);
        return this.get1U() | this.get1U() << 8 | this.get1U() << 16;
    }

    public int get4() {
        return this.getSp(4).getInt();
    }

    public float get4f() {
        return this.getSp(4).getFloat();
    }

    public long get8() {
        return this.getSp(8).getLong();
    }

    public double get8d() {
        return this.getSp(8).getDouble();
    }

    int get1U(int off) {
        return this._bb.get(off) & 255;
    }

    int get4(int off) {
        return this._bb.getInt(off);
    }

    long get8(int off) {
        return this._bb.getLong(off);
    }

    int getInt() {
        int x = this.get1U();
        if (x <= 253) {
            return x - 1;
        } else if (x == 255) {
            return (short)this.get2();
        } else {
            assert x == 254;

            return this.get4();
        }
    }

    long getZA() {
        int x = this.getInt();
        if (x == -1) {
            return -1L;
        } else {
            int nz = this.getInt();
            return (long)x << 32 | (long)nz;
        }
    }

    int read(byte[] buf, int off, int len) {
        int sz = Math.min(this._bb.remaining(), len);
        this._bb.get(buf, off, sz);
        return sz;
    }

    int getCtrl() {
        return this.getSz(1).get(0) & 255;
    }

    int getPort() {
        return this.getSz(3).getChar(1);
    }

    int getTask() {
        return this.getSz(7).getInt(3);
    }

    int getFlag() {
        return this.getSz(8).get(7);
    }

    public boolean[] getAZ() {
        int len = this.getInt();
        if (len == -1) {
            return null;
        } else {
            boolean[] r = new boolean[len];

            for(int i = 0; i < len; ++i) {
                r[i] = this.getZ();
            }

            return r;
        }
    }

    public static class AutoBufferException extends RuntimeException {
        public final IOException _ioe;

        AutoBufferException(IOException ioe) {
            this._ioe = ioe;
        }
    }
}
