package base.operators.operator.timeseries.timeseriesanalysis.demo;

import java.io.IOException;
import java.util.Random;

public class TestArimaTrainer {
   private static Random randomGenerator = new Random(4247L);

   public static void main(String[] args) throws IOException {
      double[] sigmaSquares = new double[]{0.04D, 0.64D, 1.69D};
      double[] arCoefficients = new double[]{0.5D, -0.15D, 0.1D, 0.08D, -0.01D};
      double[] maCoefficients = new double[]{-0.38D, 0.2D, -0.12D, -0.05D, 0.03D};
      int n = 10000;
      int p = 5;
      int q = 2;
      double sigmaSquare = sigmaSquares[0];
      double[] originalParameters = getOriginalParameters(p, q, arCoefficients, maCoefficients, sigmaSquare);
      double[] initialParameters = getInitialParametersFromOriginalParameters(originalParameters, 0.03D, true, false);
      ArimaTestCase testCase = new ArimaTestCase(originalParameters, initialParameters, p, q, n, 1992L);
      testCase.applyArimaTrainer();
   }

   private static double[] getInitialParametersFromOriginalParameters(double[] originalParameters, double differ, boolean randomSign, boolean normalDistributed) {
      double[] parameters = new double[originalParameters.length];

      for(int i = 0; i < parameters.length; ++i) {
         double summand = differ;
         if (normalDistributed) {
            summand = randomGenerator.nextGaussian() * differ;
         } else if (randomSign && randomGenerator.nextBoolean()) {
            summand = -differ;
         }

         parameters[i] = originalParameters[i] + summand;
      }

      return parameters;
   }

   private static double[] getOriginalParameters(int p, int q, double[] arCoefficients, double[] maCoefficients, double sigma) {
      double[] parameters = new double[p + q + 1];
      int counter = 0;

      int i;
      for(i = 0; i < p; ++i) {
         parameters[counter] = arCoefficients[i];
         ++counter;
      }

      for(i = 0; i < q; ++i) {
         parameters[counter] = maCoefficients[i];
         ++counter;
      }

      parameters[counter] = sigma;
      return parameters;
   }
}
