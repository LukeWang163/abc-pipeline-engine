package base.operators.operator.timeseries.timeseriesanalysis.tools;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.util.List;

public class SeriesUtils {
   public static boolean hasNaNValues(double[] values) {
      double[] var1 = values;
      int var2 = values.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         double d = var1[var3];
         if (Double.isNaN(d)) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasInfiniteValues(double[] values) {
      double[] var1 = values;
      int var2 = values.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         double d = var1[var3];
         if (Double.isInfinite(d)) {
            return true;
         }
      }

      return false;
   }

   public static Class checkSeriesClasses(Series... seriesArray) {
      if (seriesArray == null) {
         throw new ArgumentIsNullException("series array");
      } else if (seriesArray.length == 0) {
         throw new ArgumentIsEmptyException("series array");
      } else {
         Series[] var1 = seriesArray;
         int var2 = seriesArray.length;

         int var3;
         for(var3 = 0; var3 < var2; ++var3) {
            Series series = var1[var3];
            if (series == null) {
               throw new ArgumentIsEmptyException("series");
            }
         }

         Class classOfFirstSeries = seriesArray[0].getClass();
         Series[] var12 = seriesArray;
         var3 = seriesArray.length;

         for(int var13 = 0; var13 < var3; ++var13) {
            Series series = var12[var13];
            if (!series.getClass().equals(classOfFirstSeries)) {
               StringBuilder builder = new StringBuilder("Classes of series don't match! ");
               Series[] var7 = seriesArray;
               int var8 = seriesArray.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  Series s2 = var7[var9];
                  builder.append("class of ").append(s2.getName()).append(": ").append(s2.getClass().getName()).append("; ");
               }

               throw new IllegalArgumentException(builder.toString());
            }
         }

         return classOfFirstSeries;
      }
   }

   public static void checkList(List list, String listName) {
      if (list == null) {
         throw new ArgumentIsNullException(listName);
      } else if (list.isEmpty()) {
         throw new ArgumentIsEmptyException(listName);
      }
   }

   public static void checkArray(Object[] array, String arrayName) {
      if (array == null) {
         throw new ArgumentIsNullException(arrayName);
      } else if (array.length == 0) {
         throw new ArgumentIsEmptyException(arrayName);
      }
   }
}
