package base.operators.operator.timeseries.timeseriesanalysis.demo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;

public class TestArima {
   private static Random randomGenerator = new Random(4247L);

   public static void main(String[] args) throws IOException {
      int[] numberOfValues = new int[]{10, 100, 200, 300, 400, 500, 750, 1000, 2000, 3000, 5000, 10000};
      int[] ps = new int[]{1, 2, 3, 4};
      int[] qs = new int[]{0, 1, 2, 3, 4};
      double[] sigmaSquares = new double[]{0.04D, 0.64D, 1.69D};
      double[] arCoefficients = new double[]{0.5D, -0.15D, 0.1D, 0.08D, -0.01D};
      double[] maCoefficients = new double[]{-0.38D, 0.2D, -0.12D, -0.05D, 0.03D};
      BufferedWriter writer = Files.newBufferedWriter(Paths.get("./testArima.json"), Charset.forName("US_ASCII"));
      Throwable var8 = null;

      try {
         int i = 0;
         int[] var10 = numberOfValues;
         int var11 = numberOfValues.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            int n = var10[var12];
            int[] var14 = ps;
            int var15 = ps.length;

            for(int var16 = 0; var16 < var15; ++var16) {
               int p = var14[var16];
               int[] var18 = qs;
               int var19 = qs.length;

               for(int var20 = 0; var20 < var19; ++var20) {
                  int q = var18[var20];
                  double[] var22 = sigmaSquares;
                  int var23 = sigmaSquares.length;

                  for(int var24 = 0; var24 < var23; ++var24) {
                     double sigmaSquare = var22[var24];
                     System.out.println(Instant.now().toString() + " i: " + i + " n: " + n + " p: " + p + " q: " + q + " sigmaSquare: " + sigmaSquare);
                     ++i;
                     double[] originalParameters = getOriginalParameters(p, q, arCoefficients, maCoefficients, sigmaSquare);
                     double[] initialParameters = getInitialParametersFromOriginalParameters(originalParameters, 0.03D, true, false);
                     ArimaTestCase testCase = new ArimaTestCase(originalParameters, initialParameters, p, q, n, 1992L);
                     testCase.performGridOptimization();
                     String line = testCase.getJsonString() + "\n";
                     writer.write(line, 0, line.length());
                  }
               }
            }
         }
      } catch (Throwable var38) {
         var8 = var38;
         throw var38;
      } finally {
         if (writer != null) {
            if (var8 != null) {
               try {
                  writer.close();
               } catch (Throwable var37) {
                  var8.addSuppressed(var37);
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
