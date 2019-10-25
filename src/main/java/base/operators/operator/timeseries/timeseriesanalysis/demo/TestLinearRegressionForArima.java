package base.operators.operator.timeseries.timeseriesanalysis.demo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;

public class TestLinearRegressionForArima {
   private static Random randomGenerator = new Random(1475L);

   public static void main(String[] args) throws IOException {
      int[] numberOfValues = new int[]{10, 100, 1000};
      int[] ps = new int[]{1, 2};
      int[] qs = new int[]{0, 1, 2};
      double[] sigmaSquares = new double[]{0.64D, 1.69D};
      long seed = 1871L;
      double[] arCoefficients = new double[]{0.5D, -0.15D, 0.1D, 0.08D, -0.01D};
      double[] maCoefficients = new double[]{-0.38D, 0.2D, -0.12D, -0.05D, 0.03D};
      BufferedWriter writer = Files.newBufferedWriter(Paths.get("./testLinearRegressionForArima.json"), Charset.forName("US_ASCII"));
      Throwable var10 = null;

      try {
         int i = 0;
         int[] var12 = numberOfValues;
         int var13 = numberOfValues.length;

         for(int var14 = 0; var14 < var13; ++var14) {
            int n = var12[var14];
            int[] var16 = ps;
            int var17 = ps.length;

            for(int var18 = 0; var18 < var17; ++var18) {
               int p = var16[var18];
               int[] var20 = qs;
               int var21 = qs.length;

               for(int var22 = 0; var22 < var21; ++var22) {
                  int q = var20[var22];
                  double[] var24 = sigmaSquares;
                  int var25 = sigmaSquares.length;

                  for(int var26 = 0; var26 < var25; ++var26) {
                     double sigmaSquare = var24[var26];
                     System.out.println(Instant.now().toString() + " i: " + i + " n: " + n + " p: " + p + " q: " + q + " sigmaSquare: " + sigmaSquare);
                     ++i;
                     double[] originalParameters = getOriginalParameters(p, q, arCoefficients, maCoefficients, sigmaSquare);
                     double[] initialParameters = getInitialParametersFromOriginalParameters(originalParameters, 0.03D, true, false);
                     ArimaTestCase testCase = new ArimaTestCase(originalParameters, initialParameters, p, q, n, seed);
                     testCase.applyLinearRegressionForOptimizationParameter();
                     String line = testCase.getJsonString() + "\n";
                     writer.write(line, 0, line.length());
                  }
               }
            }
         }
      } catch (Throwable var40) {
         var10 = var40;
         throw var40;
      } finally {
         if (writer != null) {
            if (var10 != null) {
               try {
                  writer.close();
               } catch (Throwable var39) {
                  var10.addSuppressed(var39);
               }
            } else {
               writer.close();
            }
         }

      }

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
