package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils;

import java.util.Arrays;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class KalmanFilter {
   private int m;
   private RealMatrix H;
   private RealVector H_vector;
   private RealMatrix Omega;
   private RealMatrix R;
   private RealVector stateVector_t;
   private RealVector stateVector_t_1;
   private RealMatrix S_t;
   private RealMatrix S_t_1;
   private double e_t;
   private double v_t;
   private double sigmaSquare;

   public KalmanFilter(int p, int q, double[] arCoefficients, double[] maCoefficients, double sigmaSquare, RealVector stateVector_0, RealMatrix S_0) {
      this.m = Math.max(p, q + 1);
      this.stateVector_t = stateVector_0;
      this.sigmaSquare = sigmaSquare;
      double[] hVectorElements = new double[this.m];
      hVectorElements[0] = 1.0D;
      this.H_vector = MatrixUtils.createRealVector(hVectorElements);
      double[][] hElements = new double[1][this.m];
      hElements[0] = hVectorElements;
      this.H = MatrixUtils.createRealMatrix(hElements);
      double[][] OmegaElements = new double[this.m][this.m];
      double[] thetaElements = new double[this.m];

      for(int row = 0; row < this.m; ++row) {
         if (row < p) {
            OmegaElements[row][0] = arCoefficients[row];
         }

         if (row + 1 < this.m) {
            OmegaElements[row][row + 1] = 1.0D;
         }

         if (row == 0) {
            thetaElements[row] = 1.0D;
         } else if (row - 1 < q) {
            thetaElements[row] = -maCoefficients[row - 1];
         }
      }

      this.Omega = MatrixUtils.createRealMatrix(OmegaElements);
      RealVector theta = MatrixUtils.createRealVector(thetaElements);
      this.R = theta.outerProduct(theta.mapMultiply(sigmaSquare));
      double[] vectorizedRElements = new double[this.R.getRowDimension() * this.R.getColumnDimension()];

      for(int col = 0; col < this.R.getColumnDimension(); ++col) {
         for(int row = 0; row < this.R.getRowDimension(); ++row) {
            vectorizedRElements[col * this.R.getRowDimension() + row] = this.R.getEntry(row, col);
         }
      }

      RealVector vectorizedR = MatrixUtils.createRealVector(vectorizedRElements);

      RealVector vectorizedS_0;
      try {
         vectorizedS_0 = MatrixUtils.inverse(MatrixUtils.createRealIdentityMatrix(this.m * this.m).subtract(this.getKroneckerProduct(this.Omega, this.Omega))).operate(vectorizedR);
      } catch (Exception var20) {
         System.out.println(Arrays.toString(arCoefficients) + "," + Arrays.toString(maCoefficients) + "," + sigmaSquare);
         throw var20;
      }

      double[][] S_0Elements = new double[this.m][this.m];

      for(int col = 0; col < this.m; ++col) {
         for(int row = 0; row < this.m; ++row) {
            S_0Elements[row][col] = vectorizedS_0.getEntry(col * this.m + row);
         }
      }

      S_0 = MatrixUtils.createRealMatrix(S_0Elements);
      this.S_t = S_0;
   }

   public void calculateConditionalStateVector() {
      this.stateVector_t_1 = this.Omega.operate(this.stateVector_t);
   }

   public void calculateConditionalCovarianceMatrix() {
      this.S_t_1 = this.R.add(this.Omega.multiply(this.S_t.multiply(this.Omega.transpose())));
   }

   public void calculateResidual(double z) {
      this.e_t = z - this.H_vector.dotProduct(this.Omega.operate(this.stateVector_t));
   }

   public void calculateOneStepError() {
      this.v_t = this.H_vector.dotProduct(this.S_t_1.operate(this.H_vector)) / this.sigmaSquare;
   }

   public void updateStateVector() {
      this.stateVector_t = this.stateVector_t_1.add(this.S_t_1.operate(this.H_vector.mapMultiply(this.e_t / (this.sigmaSquare * this.v_t))));
   }

   public void updateCovarianceMatrix() {
      this.S_t = this.S_t_1.subtract(this.S_t_1.multiply(this.H.transpose().multiply(this.H.multiply(this.S_t_1.scalarMultiply(1.0D / (this.sigmaSquare * this.v_t))))));
   }

   public void calculateFilter(double z) {
      this.calculateConditionalStateVector();
      this.calculateConditionalCovarianceMatrix();
      this.calculateResidual(z);
      this.calculateOneStepError();
   }

   public void updateFilter() {
      this.updateStateVector();
      this.updateCovarianceMatrix();
   }

   public double getResidual() {
      return this.e_t;
   }

   public double getOneStepError() {
      return this.v_t;
   }

   private RealMatrix getKroneckerProduct(RealMatrix A, RealMatrix B) {
      int nRowsA = A.getRowDimension();
      int nColsA = A.getColumnDimension();
      int nRowsB = B.getRowDimension();
      int nColsB = B.getColumnDimension();
      int nRowsC = nRowsA * nRowsB;
      int nColsC = nColsA * nColsB;
      RealMatrix C = MatrixUtils.createRealMatrix(nRowsC, nColsC);

      for(int row = 0; row < nRowsA; ++row) {
         for(int col = 0; col < nColsA; ++col) {
            C.setSubMatrix(B.scalarMultiply(A.getEntry(row, col)).getData(), row * nRowsB, col * nColsB);
         }
      }

      return C;
   }
}
