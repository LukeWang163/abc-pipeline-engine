package base.operators.h2o.model.custom.gbm;

public abstract class TreeVisitor<T extends Exception>
        extends Object
{
    protected final CompressedTree _ct;
    private final AutoBuffer _ts;
    private final IcedBitSet _gcmp;
    protected int _depth;
    protected int _nodes;

    protected void pre(int col, float fcmp, IcedBitSet gcmp, int equal) throws T {}

    protected void mid(int col, float fcmp, int equal) throws T {}

    protected void post(int col, float fcmp, int equal) throws T {}

    protected void leaf(float pred) throws T {}

    long result() { return 0L; }








    public TreeVisitor(CompressedTree ct) {
        this._ts = new AutoBuffer((this._ct = ct)._bits);
        this._gcmp = new IcedBitSet(0);
    }


    private final void leaf2(int mask) throws T {
        assert mask == 0 || ((mask & 0x10) == 16 && (mask & 0x20) == 32) : "Unknown mask: " + mask;











        leaf(this._ts.get4f());
    }

    public final void visit() throws T {
        int nodeType = this._ts.get1();
        int col = this._ts.get2();
        if (col == 65535) {
            leaf2(nodeType);

            return;
        }

        int equal = (nodeType & 0xC) >> 2;


        float fcmp = -1.0F;
        if (equal == 0 || equal == 1) {
            fcmp = this._ts.get4f();
        }
        else if (equal == 2) {
            this._gcmp.fill2(this._ct._bits, this._ts);
        } else {
            this._gcmp.fill3(this._ct._bits, this._ts);
        }



        int lmask = nodeType & 0x33;
        int rmask = (nodeType & 0xC0) >> 2;
        int skip = 0;
        switch (lmask) {
            case 0:
                skip = this._ts.get1();
                break;
            case 1:
                skip = this._ts.get2();
                break;
            case 2:
                skip = this._ts.get3();
                break;
            case 3:
                skip = this._ts.get4();
                break;
            case 16:
                skip = (this._ct._nclass < 256) ? 1 : 2;
                break;
            case 48:
                skip = 4;
                break;

            default:
                assert false : "illegal lmask value " + lmask; break;
        }
        pre(col, fcmp, this._gcmp, equal);
        this._depth++;
        if ((lmask & 0x10) == 16) {
            leaf2(lmask);
        } else {
            visit();
        }
        mid(col, fcmp, equal);
        if ((rmask & 0x10) == 16) {
            leaf2(rmask);
        } else {
            visit();
        }
        this._depth--;
        post(col, fcmp, equal);
        this._nodes++;
    }
}
