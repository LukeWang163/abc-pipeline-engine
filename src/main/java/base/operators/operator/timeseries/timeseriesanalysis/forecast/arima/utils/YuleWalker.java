package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils;

import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import java.util.Arrays;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.StatUtils;

public class YuleWalker {
   private int order;
   private double[] values;
   private boolean estimateConstant;

   private YuleWalker(int order, double[] values, boolean estimateConstant) {
      if (order <= 0) {
         throw new IllegalIndexArgumentException("order", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (order > values.length) {
         throw new IllegalArgumentException("Order has to be smaller or equal than the length of the values array.");
      } else {
         this.order = order;
         this.values = values;
         this.estimateConstant = estimateConstant;
      }
   }

   public static YuleWalker create(int order, double[] values, boolean estimateConstant) {
      return new YuleWalker(order, values, estimateConstant);
   }

   public double[] computeCoefficients() {
      double mean = 0.0D;
      double[] valuesZeroMean = Arrays.copyOf(this.values, this.values.length);
      mean = StatUtils.mean(this.values);

      for(int i = 0; i < this.values.length; ++i) {
         valuesZeroMean[i] -= mean;
      }

      double[] r = this.computeR(valuesZeroMean);
      RealMatrix toeplizMatrix = this.constructToeplitzMatrix(Arrays.copyOf(r, r.length - 1));
      DecompositionSolver solver = (new EigenDecomposition(toeplizMatrix)).getSolver();
      RealVector rho = solver.solve(MatrixUtils.createRealVector(Arrays.copyOfRange(r, 1, r.length)));
      if (this.estimateConstant) {
         rho = rho.append(mean);
      }

      return rho.toArray();
   }

   private double[] computeR(double[] values) {
      double squaredSum = 0.0D;

      for(int i = 0; i < values.length; ++i) {
         squaredSum += Math.pow(values[i], 2.0D);
      }

      double[] r = new double[this.order + 1];
      r[0] = squaredSum / (double)values.length;

      for(int k = 1; k < r.length; ++k) {
         r[k] = this.computeSumOfLaggedProducts(values, k);
      }

      return r;
   }

   private double computeSumOfLaggedProducts(double[] values, int lag) {
      int n = values.length;
      double sum = 0.0D;

      for(int i = 0; i < n - lag; ++i) {
         double prod = values[i] * values[i + lag];
         sum += prod;
      }

      return sum / (double)(n - lag);
   }

   private RealMatrix constructToeplitzMatrix(double[] values) {
      int n = values.length;
      double[] symmetricValues = new double[n * 2 - 1];
      symmetricValues[n - 1] = values[0];

      for(int i = 0; i < n; ++i) {
         symmetricValues[i] = values[n - i - 1];
         symmetricValues[symmetricValues.length - i - 1] = values[n - i - 1];
      }

      RealMatrix matrix = MatrixUtils.createRealMatrix(n, n);

      for(int i = 0; i < n; ++i) {
         for(int j = 0; j < n; ++j) {
            matrix.setEntry(i, j, symmetricValues[n + i - j - 1]);
         }
      }

      return matrix;
   }
}
