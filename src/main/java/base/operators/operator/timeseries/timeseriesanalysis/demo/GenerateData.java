package base.operators.operator.timeseries.timeseriesanalysis.demo;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.Arima;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GenerateData {
   public static ArrayList generateRandomTimeIndices(int start, int end, int listSize, Long seed) {
      ArrayList timeIndices = new ArrayList();
      Random generator = new Random();
      if (seed != null) {
         generator.setSeed(seed);
      }

      for(int i = 0; i < listSize; ++i) {
         timeIndices.add(Instant.ofEpochSecond((long)start + (long)generator.nextInt(end)));
      }

      return timeIndices;
   }

   public static ArrayList generateRandomTimeIndices(int start, int end, int listSize) {
      return generateRandomTimeIndices(start, end, listSize, (Long)null);
   }

   public static ArrayList generateRandomSortedTimeIndices(int start, int end, int listSize, Long seed) {
      ArrayList timeIndices = generateRandomTimeIndices(start, end, listSize, seed);
      timeIndices.sort((Comparator)null);
      return timeIndices;
   }

   public static ArrayList generateRandomSortedTimeIndices(int start, int end, int listSize) {
      return generateRandomSortedTimeIndices(start, end, listSize, (Long)null);
   }

   public static double[] generateRandomSortedDoubleIndices(double start, double stop, int length, Long seed) {
      ArrayList indices = new ArrayList();
      Random generator = new Random();
      if (seed != null) {
         generator.setSeed(seed);
      }

      for(int i = 0; i < length; ++i) {
         indices.add(start + generator.nextDouble() * (stop - start));
      }

      indices.sort((Comparator)null);
      double[] result = new double[length];

      for(int i = 0; i < length; ++i) {
         result[i] = (Double)indices.get(i);
      }

      return result;
   }

   public static double[] generateRandomSortedDoubleIndices(double start, double stop, int length) {
      return generateRandomSortedDoubleIndices(start, stop, length, (Long)null);
   }

   public static List generateRandomSortedDoubleIndicesList(double start, double stop, int length) {
      return convertArrayToList(generateRandomSortedDoubleIndices(start, stop, length));
   }

   public static double[] generateRandomValues(double start, double stop, int length, boolean addIrregularValues, Long seed) {
      double[] result = new double[length];
      Random generator = new Random();
      if (seed != null) {
         generator.setSeed(seed);
      }

      for(int i = 0; i < length; ++i) {
         if (addIrregularValues && !generator.nextBoolean()) {
            if (generator.nextBoolean()) {
               result[i] = Double.NaN;
            } else if (generator.nextBoolean()) {
               result[i] = Double.POSITIVE_INFINITY;
            } else {
               result[i] = Double.NEGATIVE_INFINITY;
            }
         } else {
            result[i] = start + generator.nextDouble() * (stop - start);
         }
      }

      return result;
   }

   public static double[] generateRandomValues(double start, double stop, int length, boolean addIrregularValues) {
      return generateRandomValues(start, stop, length, addIrregularValues, (Long)null);
   }

   public static double[] generateRandomValues(double start, double stop, int length, Long seed) {
      return generateRandomValues(start, stop, length, false, seed);
   }

   public static double[] generateRandomValues(double start, double stop, int length) {
      return generateRandomValues(start, stop, length, (Long)null);
   }

   public static List generateRandomValuesList(double start, double stop, int length) {
      return convertArrayToList(generateRandomValues(start, stop, length, false));
   }

   public static ArrayList generateEquidistantInstancesMilli(long startMilli, int stepSize, int numberOfElements) {
      ArrayList instantIndicesEquidistant;
      for(instantIndicesEquidistant = new ArrayList(); numberOfElements > 0; --numberOfElements) {
         instantIndicesEquidistant.add(Instant.ofEpochMilli(startMilli));
         startMilli += (long)stepSize;
      }

      return instantIndicesEquidistant;
   }

   public static ArrayList generateEquidistantInstancesSeconds(long startSec, int stepSize, int numberOfElements) {
      ArrayList instantIndicesEquidistant;
      for(instantIndicesEquidistant = new ArrayList(); numberOfElements > 0; --numberOfElements) {
         instantIndicesEquidistant.add(Instant.ofEpochSecond(startSec));
         startSec += (long)stepSize;
      }

      return instantIndicesEquidistant;
   }

   public static ValueSeries generateArimaSeries(Arima arima, int sigma, int length) {
      Random generator = new Random();
      double[] residuals = new double[length];

      for(int i = 0; i < length; ++i) {
         residuals[i] = generator.nextGaussian() * (double)sigma;
      }

      arima.setResiduals(residuals);
      arima.setForecastHorizon(length);
      double[] result = new double[length];

      for(int i = 0; i < length; ++i) {
         result[i] = arima.forecastOneValue(i, result, (double[])null, residuals);
      }

      return ValueSeries.create(result);
   }

   public static ValueSeries generateArimaSeries(int p, int d, int q, double[] arCoefficients, double[] maCoefficients, double constant, double sigma, int length, Random generator) {
      double[] residuals = new double[length];

      for(int i = 0; i < length; ++i) {
         residuals[i] = generator.nextGaussian() * sigma;
      }

      Arima arima = Arima.create(p, d, q, arCoefficients, maCoefficients, constant, residuals, length, "Forecast");
      double[] result = new double[length];

      for(int i = 0; i < length; ++i) {
         result[i] = arima.forecastOneValue(i, result, (double[])null, residuals);
      }

      return ValueSeries.create(result);
   }

   public static ValueSeries generateArimaSeries(int p, int d, int q, double[] arCoefficients, double[] maCoefficients, double constant, double sigma, int length, Long seed) {
      Random generator;
      if (seed == null) {
         generator = new Random();
      } else {
         generator = new Random(seed);
      }

      return generateArimaSeries(p, d, q, arCoefficients, maCoefficients, constant, sigma, length, generator);
   }

   public static List convertArrayToList(double[] array) {
      List list = new ArrayList();
      double[] var2 = array;
      int var3 = array.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         double d = var2[var4];
         if (Double.isNaN(d)) {
            list.add(Double.NaN);
         } else {
            list.add(d);
         }
      }

      return list;
   }

   public static List convertArrayToList(String[] array) {
      List list = new ArrayList();
      String[] var2 = array;
      int var3 = array.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String s = var2[var4];
         list.add(s);
      }

      return list;
   }

   public static List convertArrayToList(int[] array) {
      List list = new ArrayList();
      int[] var2 = array;
      int var3 = array.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         int i = var2[var4];
         list.add(i);
      }

      return list;
   }
}
