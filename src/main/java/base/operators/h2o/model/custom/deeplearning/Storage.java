package base.operators.h2o.model.custom.deeplearning;

import java.io.Serializable;
import java.util.TreeMap;

public class Storage {
    public Storage() {
    }

    static final class SparseColMatrix implements Storage.Matrix, Serializable {
        private static final long serialVersionUID = 6312531378498560356L;
        private TreeMap<Integer, Float>[] _cols;
        private int _rows;

        SparseColMatrix(int rows, int cols) {
            this((Storage.Matrix)null, rows, cols);
        }

        SparseColMatrix(Storage.Matrix v, int rows, int cols) {
            this._rows = rows;
            this._cols = new TreeMap[cols];

            int row;
            for(row = 0; row < cols; ++row) {
                this._cols[row] = new TreeMap();
            }

            if (v != null) {
                for(row = 0; row < rows; ++row) {
                    for(int col = 0; col < cols; ++col) {
                        if (v.get(row, col) != 0.0F) {
                            this.add(row, col, v.get(row, col));
                        }
                    }
                }
            }

        }

        @Override
        public float get(int row, int col) {
            Float v = (Float)this._cols[col].get(row);
            return v == null ? 0.0F : v;
        }

        @Override
        public void add(int row, int col, float val) {
            this.set(row, col, this.get(row, col) + val);
        }

        @Override
        public void set(int row, int col, float val) {
            this._cols[col].put(row, val);
        }

        @Override
        public int cols() {
            return this._cols.length;
        }

        @Override
        public int rows() {
            return this._rows;
        }

        @Override
        public long size() {
            return (long)this._rows * (long)this._cols.length;
        }

        TreeMap<Integer, Float> col(int col) {
            return this._cols[col];
        }

        @Override
        public float[] raw() {
            throw new UnsupportedOperationException("raw access to the data in a sparse matrix is not implemented.");
        }
    }

    public static final class SparseRowMatrix implements Storage.Matrix, Serializable {
        private static final long serialVersionUID = 7653033677388289450L;
        private TreeMap<Integer, Float>[] _rows;
        private int _cols;

        SparseRowMatrix(int rows, int cols) {
            this((Storage.Matrix)null, rows, cols);
        }

        SparseRowMatrix(Storage.Matrix v, int rows, int cols) {
            this._rows = new TreeMap[rows];

            int row;
            for(row = 0; row < rows; ++row) {
                this._rows[row] = new TreeMap();
            }

            this._cols = cols;
            if (v != null) {
                for(row = 0; row < rows; ++row) {
                    for(int col = 0; col < cols; ++col) {
                        if (v.get(row, col) != 0.0F) {
                            this.add(row, col, v.get(row, col));
                        }
                    }
                }
            }

        }

        @Override
        public float get(int row, int col) {
            Float v = (Float)this._rows[row].get(col);
            return v == null ? 0.0F : v;
        }

        @Override
        public void add(int row, int col, float val) {
            this.set(row, col, this.get(row, col) + val);
        }

        @Override
        public void set(int row, int col, float val) {
            this._rows[row].put(col, val);
        }

        @Override
        public int cols() {
            return this._cols;
        }

        @Override
        public int rows() {
            return this._rows.length;
        }

        @Override
        public long size() {
            return (long)this._rows.length * (long)this._cols;
        }

        TreeMap<Integer, Float> row(int row) {
            return this._rows[row];
        }

        @Override
        public float[] raw() {
            throw new UnsupportedOperationException("raw access to the data in a sparse matrix is not implemented.");
        }
    }

    public static final class DenseColMatrix implements Storage.Matrix, Serializable {
        private static final long serialVersionUID = -8626527330653939511L;
        private float[] _data;
        private int _cols;
        private int _rows;

        DenseColMatrix(int rows, int cols) {
            this(new float[cols * rows], rows, cols);
        }

        DenseColMatrix(float[] v, int rows, int cols) {
            this._data = v;
            this._rows = rows;
            this._cols = cols;
        }

        DenseColMatrix(Storage.DenseRowMatrix m, int rows, int cols) {
            this(rows, cols);

            for(int row = 0; row < rows; ++row) {
                for(int col = 0; col < cols; ++col) {
                    this.set(row, col, m.get(row, col));
                }
            }

        }

        @Override
        public float get(int row, int col) {
            assert row < this._rows && col < this._cols;

            return this._data[col * this._rows + row];
        }

        @Override
        public void set(int row, int col, float val) {
            assert row < this._rows && col < this._cols;

            this._data[col * this._rows + row] = val;
        }

        @Override
        public void add(int row, int col, float val) {
            assert row < this._rows && col < this._cols;

            float[] var10000 = this._data;
            int var10001 = col * this._rows + row;
            var10000[var10001] += val;
        }

        @Override
        public int cols() {
            return this._cols;
        }

        @Override
        public int rows() {
            return this._rows;
        }

        @Override
        public long size() {
            return (long)this._rows * (long)this._cols;
        }

        @Override
        public float[] raw() {
            return this._data;
        }
    }

    public static final class DenseRowMatrix implements Storage.Matrix, Serializable {
        private static final long serialVersionUID = -3139230786059936526L;
        private float[] _data;
        private int _cols;
        private int _rows;

        public DenseRowMatrix() {
        }

        DenseRowMatrix(int rows, int cols) {
            this(new float[cols * rows], rows, cols);
        }

        DenseRowMatrix(float[] v, int rows, int cols) {
            this._data = v;
            this._rows = rows;
            this._cols = cols;
        }

        @Override
        public float get(int row, int col) {
            assert row < this._rows && col < this._cols : "_data.length: " + this._data.length + ", checking: " + row + " < " + this._rows + " && " + col + " < " + this._cols;

            return this._data[row * this._cols + col];
        }

        @Override
        public void set(int row, int col, float val) {
            assert row < this._rows && col < this._cols;

            this._data[row * this._cols + col] = val;
        }

        @Override
        public void add(int row, int col, float val) {
            assert row < this._rows && col < this._cols;

            float[] var10000 = this._data;
            int var10001 = row * this._cols + col;
            var10000[var10001] += val;
        }

        @Override
        public int cols() {
            return this._cols;
        }

        @Override
        public int rows() {
            return this._rows;
        }

        @Override
        public long size() {
            return (long)this._rows * (long)this._cols;
        }

        @Override
        public float[] raw() {
            return this._data;
        }
    }

    public static class DenseVector implements Storage.Vector, Serializable {
        private static final long serialVersionUID = 2613632149069054075L;
        private double[] _data;

        public DenseVector() {
        }

        DenseVector(int len) {
            this._data = new double[len];
        }

        DenseVector(double[] v) {
            this._data = v;
        }

        @Override
        public double get(int i) {
            return this._data[i];
        }

        @Override
        public void set(int i, double val) {
            this._data[i] = val;
        }

        @Override
        public void add(int i, double val) {
            double[] var10000 = this._data;
            var10000[i] += val;
        }

        @Override
        public int size() {
            return this._data.length;
        }

        @Override
        public double[] raw() {
            return this._data;
        }
    }

    public interface Tensor {
        float get(int var1, int var2, int var3);

        void set(int var1, int var2, int var3, float var4);

        void add(int var1, int var2, int var3, float var4);

        int slices();

        int cols();

        int rows();

        long size();

        float[] raw();
    }

    public interface Matrix {
        float get(int var1, int var2);

        void set(int var1, int var2, float var3);

        void add(int var1, int var2, float var3);

        int cols();

        int rows();

        long size();

        float[] raw();
    }

    public interface Vector {
        double get(int var1);

        void set(int var1, double var2);

        void add(int var1, double var2);

        int size();

        double[] raw();
    }
}
