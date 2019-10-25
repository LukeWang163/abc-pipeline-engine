package base.operators.h2o.model;

import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.operator.OperatorException;
import java.io.Serializable;
import java.util.Arrays;

public class GeneralizedLinearModel
        extends H2OModel
{
    private static final long serialVersionUID = 7764414471986370851L;
    private final boolean multinomialModel;
    private final String[] coefficientNames;
    private final double[] coefficients;
    private final double[] stdCoefficients;
    private final double[] zValues;
    private final double[] pValues;
    private final double[] stdError;
    private final double[][] multinominalCoefficients;
    private final double[][] multinominalStdCoefficients;
    private final String[] domain;
    GLMScore glmScore;

    public GeneralizedLinearModel(ExampleSet trainingExampleSet, H2ONativeModelObject h2oNativeModel, boolean multinomialModel, String[] coefficientNames, double[] coefficients, double[] stdCoefficients, double[] zValues, double[] pValues, double[] stdError, double[][] multinominalCoefficients, double[][] multinominalStdCoefficients, String[] domain, String modelString, String[] warnings, GLMScore glmScore) throws OperatorException {
        super(trainingExampleSet, h2oNativeModel, modelString, warnings, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET, ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);


        this.multinomialModel = multinomialModel;
        this.coefficientNames = coefficientNames;
        this.coefficients = coefficients;
        this.stdCoefficients = stdCoefficients;
        this.zValues = zValues;
        this.pValues = pValues;
        this.stdError = stdError;
        this.multinominalCoefficients = multinominalCoefficients;
        this.multinominalStdCoefficients = multinominalStdCoefficients;
        this.domain = domain;
        this.glmScore = glmScore;
    }


    public boolean isMultinomialModel() { return this.multinomialModel; }



    public String[] getCoefficientNames() { return this.coefficientNames; }



    public double[] getCoefficients() { return this.coefficients; }



    public double[] getStdCoefficients() { return this.stdCoefficients; }



    public double[] getZValues() { return this.zValues; }



    public double[] getPValues() { return this.pValues; }



    public double[] getStdErr() { return this.stdError; }



    public double[][] getMultinominalCoefficients() { return this.multinominalCoefficients; }



    public double[][] getMultinominalStdCoefficients() { return this.multinominalStdCoefficients; }



    public String[] getDomain() { return this.domain; }




    @Override
    public void score0(double[] data, double[] preds) { this.glmScore.map(data, preds); }
    public static class DataInfo implements Serializable, Cloneable { private static final long serialVersionUID = 1L; public int[] _catOffsets; public int _cats; public boolean _intercept; public int _responses; public boolean _weights; public int _nums;
        public boolean _skipMissing;
        private int[] _catModes;
        private int[] _fullCatOffsets;
        public boolean _offset;
        public int[] _numOffsets;
        public int[][] _intLvls;
        public double[] _normMul;
        public double[] _normSub;
        public double[] _normRespMul;
        public double[] _normRespSub;
        public double[] _numMeans;
        public boolean _fold;
        public int[][] _catLvls;
        public boolean _useAllFactorLevels;

        public final class Row { public int nBins;
            public int[] binIds;

            public Row(boolean sparse, int nNums, int nBins, int nresponses, int i, long start) {
                this.weight = 1.0D;
                this.offset = 0.0D;



                this.binIds = new int[nBins];
                this.numVals = new double[nNums];
                this.response = new double[nresponses];
                if (sparse) {
                    this.numIds = new int[nNums];
                }
                this.nNums = sparse ? 0 : nNums;
                this.cid = i;
            }
            public int[] numIds; public double[] numVals; public int nNums; public double[] response; public boolean response_bad; public boolean predictors_bad; public double weight; public double offset;
            public int cid;

            public final double innerProduct(double[] vec) {
                double res = 0.0D;
                int numStart = numStart();
                for (int i = 0; i < this.nBins; i++) {
                    res += vec[this.binIds[i]];
                }
                if (this.numIds == null) {
                    for (int i = 0; i < this.numVals.length; i++) {
                        res += this.numVals[i] * vec[numStart + i];
                    }
                } else {
                    for (int i = 0; i < this.nNums; i++) {
                        res += this.numVals[i] * vec[this.numIds[i]];
                    }
                }
                if (GeneralizedLinearModel.DataInfo.this._intercept) {
                    res += vec[vec.length - 1];
                }
                return res;
            }


            public final int numStart() { return GeneralizedLinearModel.DataInfo.this._catOffsets[GeneralizedLinearModel.DataInfo.this._cats]; }




            @Override
            public String toString() { return "?" + Arrays.toString(Arrays.copyOf(this.binIds, this.nBins)) + ", " + Arrays.toString(this.numVals); } }





























        public DataInfo(int[] _catOffsets, int _cats, boolean _intercept, int _responses, boolean _weights, int _nums, boolean _skipMissing, int[] _catModes, int[] _fullCatOffsets, boolean _offset, int[] _numOffsets, int[][] _intLvls, double[] _normMul, double[] _normSub, double[] _normRespMul, double[] _normRespSub, double[] _numMeans, boolean _fold, int[][] _catLvls, boolean _useAllFactorLevels) {
            this._catOffsets = _catOffsets;
            this._cats = _cats;
            this._intercept = _intercept;
            this._responses = _responses;
            this._weights = _weights;
            this._nums = _nums;
            this._skipMissing = _skipMissing;
            this._catModes = _catModes;
            this._fullCatOffsets = _fullCatOffsets;
            this._offset = _offset;
            this._numOffsets = _numOffsets;
            this._intLvls = _intLvls;
            this._normMul = _normMul;
            this._normSub = _normSub;
            this._normRespMul = _normRespMul;
            this._normRespSub = _normRespSub;
            this._numMeans = _numMeans;
            this._fold = _fold;
            this._catLvls = _catLvls;
            this._useAllFactorLevels = _useAllFactorLevels;
        }


        public DataInfo scoringInfo() {
            DataInfo res = null;
            try {
                res = (DataInfo)clone();
            } catch (CloneNotSupportedException cloneNotSupportedException) {}

            res._normMul = null;
            res._normRespSub = null;
            res._normRespMul = null;
            res._normRespSub = null;











            return res;
        }


        public int weightChunkId() { return this._cats + this._nums; }



        protected int[] fullCatOffsets() { return (this._fullCatOffsets == null) ? this._catOffsets : this._fullCatOffsets; }




        public final int getCategoricalId(int cid, int val) {
            boolean isIWV = false;
            if (!this._useAllFactorLevels && !isIWV) {
                val--;
            }
            int[] offs = fullCatOffsets();
            if (val + offs[cid] >= offs[cid + 1])
            {


                val = this._catModes[cid];
            }
            if (this._catLvls[cid] != null)
            {

                val = Arrays.binarySearch(this._catLvls[cid], val);
            }
            return (val < 0) ? -1 : (val + this._catOffsets[cid]);
        }


        public Row newDenseRow() { return new Row(false, numNums(), this._cats, this._responses, 0, 0L); }







        public final int numStart() { return this._catOffsets[this._cats]; }






        public final Row extractDenseRow(double[] chunks, int rid, Row row) {
            row.predictors_bad = false;
            row.response_bad = false;


            row.cid = rid;







            if (this._skipMissing) {
                int N = this._cats + this._nums;
                for (int i = 0; i < N; i++) {


                    if (Double.isNaN(chunks[i])) {
                        row.predictors_bad = true;
                        return row;
                    }
                }
            }
            int nbins = 0;
            for (int i = 0; i < this._cats; i++) {



                int cid = getCategoricalId(i, Double.isNaN(chunks[i]) ? this._catModes[i] : (int)chunks[i]);
                if (cid >= 0) {
                    row.binIds[nbins++] = cid;
                }
            }
            row.nBins = nbins;
            int n = this._nums;
            int numValsIdx = 0;
            for (int i = 0; i < n; i++) {




























                double d = chunks[this._cats + i];
                if (Double.isNaN(d)) {
                    d = this._numMeans[numValsIdx];
                }
                if (this._normMul != null && this._normSub != null) {
                    d = (d - this._normSub[numValsIdx]) * this._normMul[numValsIdx];
                }
                row.numVals[numValsIdx++] = d;
            }



















            return row;
        }

        public final int numNums() {
            int nnums = 0;
            if (this._numOffsets == null && this._intLvls.length > 0) {
                for (int[] _intLvl : this._intLvls) {
                    nnums += ((_intLvl == null) ? 0 : (_intLvl.length - 1));
                }



                return nnums + this._nums;
            }



            return this._nums;
        } }




    public static class ModelMetrics
            implements Serializable
    {
        private static final long serialVersionUID = 1L;
    }



    public static class ModelMetricsBinomial
            extends ModelMetrics
    {
        private static final long serialVersionUID = 1L;


        public GeneralizedLinearModel.AUC2 _auc;


        public ModelMetricsBinomial(GeneralizedLinearModel.AUC2 _auc) { this._auc = _auc; }
    }


    public static class AUC2
            implements Serializable
    {
        private static final long serialVersionUID = 1L;

        public int _max_idx;

        public double[] _ths;


        public double defaultThreshold() { return (this._max_idx == -1) ? 0.5D : this._ths[this._max_idx]; }



        public AUC2(int _max_idx, double[] _ths) {
            this._max_idx = _max_idx;
            this._ths = _ths;
        }
    }

    public static class GLMModel
            implements Serializable
    {
        private static final long serialVersionUID = 1L;
        GLMParameters _parms;
        GLMOutput _output;
        private double[] eta;

        public static class GLMParameters
                implements Serializable {
            private static final long serialVersionUID = 1L;
            public String _family;
            public String _link;
            public double _tweedie_link_power;

            public GLMParameters(String _family, String _link, double _tweedie_link_power) {
                this._family = _family;
                this._link = _link;
                this._tweedie_link_power = _tweedie_link_power;
            }
            public final double linkInv(double x) {
                double xx;
                switch (this._link) {

                    case "identity":
                        return x;
                    case "logit":
                        return 1.0D / (Math.exp(-x) + 1.0D);
                    case "log":
                        return Math.exp(x);
                    case "inverse":
                        xx = (x < 0.0D) ? Math.min(-1.0E-5D, x) : Math.max(1.0E-5D, x);
                        return 1.0D / xx;
                    case "tweedie":
                        return (this._tweedie_link_power == 0.0D) ? Math.max(2.0E-16D, Math.exp(x)) :
                                Math.pow(x, 1.0D / this._tweedie_link_power);
                }
                throw new RuntimeException("unexpected link function id  " + this);
            }
        }

        public static class GLMOutput
                implements Serializable {
            private static final long serialVersionUID = 1L;
            int _nclasses;
            double[][] _global_beta_multinomial;
            double[] _global_beta;
            public GeneralizedLinearModel.ModelMetrics _training_metrics;

            public GLMOutput(int _nclasses, double[][] _global_beta_multinomial, double[] _global_beta, GeneralizedLinearModel.ModelMetrics _training_metrics) {
                this._nclasses = _nclasses;
                this._global_beta_multinomial = _global_beta_multinomial;
                this._global_beta = _global_beta;
                this._training_metrics = _training_metrics;
            }







            public int nclasses() { return this._nclasses; }
        }







        public GLMModel(GLMParameters _parms, GLMOutput _output) {
            this._parms = _parms;
            this._output = _output;
        }

        public final double defaultThreshold() {
            if (this._output.nclasses() != 2 || this._output._training_metrics == null) {
                return 0.5D;
            }





            if (((GeneralizedLinearModel.ModelMetricsBinomial)this._output._training_metrics)._auc != null) {
                return ((GeneralizedLinearModel.ModelMetricsBinomial)this._output._training_metrics)._auc.defaultThreshold();
            }
            return 0.5D;
        }


        public double[] beta() { return this._output._global_beta; }




        public double[] scoreRow(GeneralizedLinearModel.DataInfo.Row r, double o, double[] preds) {
            if (this._parms._family.equals("multinomial")) {





                if (this.eta == null)
                {
                    this.eta = new double[this._output.nclasses()];
                }
                double[][] bm = this._output._global_beta_multinomial;
                double sumExp = 0.0D;
                double maxRow = 0.0D;
                for (int c = 0; c < bm.length; c++) {
                    this.eta[c] = r.innerProduct(bm[c]) + o;
                    if (this.eta[c] > maxRow) {
                        maxRow = this.eta[c];
                    }
                }
                for (int c = 0; c < bm.length; c++) {
                    this.eta[c] = Math.exp(this.eta[c] - maxRow); sumExp += Math.exp(this.eta[c] - maxRow);
                }
                sumExp = 1.0D / sumExp;
                for (int c = 0; c < bm.length; c++) {
                    preds[c + 1] = this.eta[c] * sumExp;
                }
                preds[0] = GeneralizedLinearModel.ArrayUtils.maxIndex(this.eta);
            } else {
                double mu = this._parms.linkInv(r.innerProduct(beta()) + o);
                if (this._parms._family.equals("binomial")) {
                    preds[0] = (mu >= defaultThreshold()) ? 1.0D : 0.0D;
                    preds[1] = 1.0D - mu;
                    preds[2] = mu;
                } else {
                    preds[0] = mu;
                }
            }
            return preds;
        }
    }



    public static class ArrayUtils
    {
        public static int maxIndex(double[] from) {
            int result = 0;
            for (int i = 1; i < from.length; i++) {
                if (from[i] > from[result]) {
                    result = i;
                }
            }
            return result;
        }
    }


    public static class GLMScore
            implements Serializable
    {
        private static final long serialVersionUID = 1L;

        GeneralizedLinearModel.DataInfo _dinfo;
        GeneralizedLinearModel.GLMModel _m;
        boolean _generatePredictions;

        public GLMScore(GeneralizedLinearModel.DataInfo _dinfo, GeneralizedLinearModel.GLMModel _m, boolean _generatePredictions) {
            this._dinfo = _dinfo.scoringInfo();
            this._m = _m;
            this._generatePredictions = _generatePredictions;
        }

        private void processRow(GeneralizedLinearModel.DataInfo.Row r, float[] res, double[] ps, double[] preds, int ncols) {
            if (this._dinfo._responses != 0) {
                res[0] = (float)r.response[0];
            }
            if (r.predictors_bad) {
                Arrays.fill(ps, 0.0/0.0);
            } else if (r.weight == 0.0D) {
                Arrays.fill(ps, 0.0D);
            } else {
                this._m.scoreRow(r, r.offset, ps);
            }




            if (this._generatePredictions) {
                for (int c = 0; c < ncols; c++) {
                    preds[c] = ps[c];
                }
            }
        }











        public void map(double[] chks, double[] preds) {
            double[] ps = new double[this._m._output._nclasses + 1];

            float[] res = new float[1];
            int nc = this._m._output.nclasses();
            int ncols = (nc == 1) ? 1 : (nc + 1);








            GeneralizedLinearModel.DataInfo.Row r = this._dinfo.newDenseRow();


            this._dinfo.extractDenseRow(chks, 0, r);
            processRow(r, res, ps, preds, ncols);
        }
    }
}
