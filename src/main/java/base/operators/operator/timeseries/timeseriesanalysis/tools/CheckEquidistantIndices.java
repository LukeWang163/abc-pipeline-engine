package base.operators.operator.timeseries.timeseriesanalysis.tools;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;

public class CheckEquidistantIndices {
   public static boolean isEquidistant(ValueSeries valueSeries) {
      return isEquidistant((ValueSeries)valueSeries, (Double)null);
   }

   public static boolean isEquidistant(ValueSeries valueSeries, double epsilon) {
      return isEquidistant(valueSeries, new Double(epsilon));
   }

   private static boolean isEquidistant(ValueSeries valueSeries, Double epsilon) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else if (valueSeries.hasDefaultIndices()) {
         return true;
      } else if (valueSeries.getLength() == 1) {
         return true;
      } else {
         return epsilon == null ? checkValueIndices(valueSeries.getIndices()) : checkValueIndices(valueSeries.getIndices(), epsilon);
      }
   }

   public static boolean isEquidistant(MultivariateValueSeries multivariateValueSeries) {
      return isEquidistant((MultivariateValueSeries)multivariateValueSeries, (Double)null);
   }

   public static boolean isEquidistant(MultivariateValueSeries multivariateValueSeries, double epsilon) {
      return isEquidistant(multivariateValueSeries, new Double(epsilon));
   }

   private static boolean isEquidistant(MultivariateValueSeries multivariateValueSeries, Double epsilon) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else if (multivariateValueSeries.hasDefaultIndices()) {
         return true;
      } else if (multivariateValueSeries.getLength() == 1) {
         return true;
      } else {
         return epsilon == null ? checkValueIndices(multivariateValueSeries.getIndices()) : checkValueIndices(multivariateValueSeries.getIndices(), epsilon);
      }
   }

   public static boolean isEquidistant(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return timeSeries.getLength() == 1 ? true : checkInstantIndices(timeSeries.getIndices());
      }
   }

   public static boolean isEquidistant(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         return multivariateTimeSeries.getLength() == 1 ? true : checkInstantIndices(multivariateTimeSeries.getIndices());
      }
   }

   private static boolean checkValueIndices(double[] indices) {
      double firstDiff = indices[1] - indices[0];
      double resolutionfirstDiff = Math.ulp(indices[0]) + Math.ulp(indices[1]);

      for(int i = 1; i < indices.length - 1; ++i) {
         double resolutionCurrentDiff = Math.ulp(indices[i]) + Math.ulp(indices[i + 1]);
         if (!doubleEqualwithEpsilon(indices[i + 1] - indices[i], firstDiff, Math.max(resolutionfirstDiff, resolutionCurrentDiff))) {
            return false;
         }
      }

      return true;
   }

   private static boolean checkValueIndices(double[] indices, double epsilon) {
      double firstDiff = indices[1] - indices[0];

      for(int i = 1; i < indices.length - 1; ++i) {
         if (!doubleEqualwithEpsilon(indices[i + 1] - indices[i], firstDiff, epsilon)) {
            return false;
         }
      }

      return true;
   }

   private static boolean doubleEqualwithEpsilon(double d1, double d2, double epsilon) {
      return Math.abs(d2 - d1) <= epsilon;
   }

   private static boolean checkInstantIndices(ArrayList indices) {
      Duration firstDiff = Duration.between((Temporal)indices.get(0), (Temporal)indices.get(1));

      for(int i = 1; i < indices.size() - 1; ++i) {
         if (!instantEqualwithEpsilon((Instant)indices.get(i), (Instant)indices.get(i + 1), firstDiff)) {
            return false;
         }
      }

      return true;
   }

   private static boolean instantEqualwithEpsilon(Instant t1, Instant t2, Duration firstDiff) {
      return firstDiff.equals(Duration.between(t1, t2));
   }
}
