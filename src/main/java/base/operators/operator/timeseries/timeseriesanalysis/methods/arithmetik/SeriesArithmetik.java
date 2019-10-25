package base.operators.operator.timeseries.timeseriesanalysis.methods.arithmetik;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IndicesNotEqualException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class SeriesArithmetik {
   private static final Map concatStringMap = createMap();

   private static Map createMap() {
      Map map = new EnumMap(ArithmetikMode.class);
      map.put(ArithmetikMode.ADDITION, " + ");
      map.put(ArithmetikMode.SUBSTRACTION, " - ");
      map.put(ArithmetikMode.MULTIPLICATION, " * ");
      map.put(ArithmetikMode.DIVISION, " / ");
      return map;
   }

   public static Series add(Series series1, Series series2) {
      return compute(series1, series2, ArithmetikMode.ADDITION);
   }

   public static Series substract(Series series1, Series series2) {
      return compute(series1, series2, ArithmetikMode.SUBSTRACTION);
   }

   public static Series multiply(Series series1, Series series2) {
      return compute(series1, series2, ArithmetikMode.MULTIPLICATION);
   }

   public static Series divide(Series series1, Series series2) {
      return compute(series1, series2, ArithmetikMode.DIVISION);
   }

   public static ValueSeries add(ValueSeries valueSeries1, ValueSeries valueSeries2) {
      return compute(valueSeries1, valueSeries2, ArithmetikMode.ADDITION);
   }

   public static ValueSeries substract(ValueSeries valueSeries1, ValueSeries valueSeries2) {
      return compute(valueSeries1, valueSeries2, ArithmetikMode.SUBSTRACTION);
   }

   public static ValueSeries multiply(ValueSeries valueSeries1, ValueSeries valueSeries2) {
      return compute(valueSeries1, valueSeries2, ArithmetikMode.MULTIPLICATION);
   }

   public static ValueSeries divide(ValueSeries valueSeries1, ValueSeries valueSeries2) {
      return compute(valueSeries1, valueSeries2, ArithmetikMode.DIVISION);
   }

   public static TimeSeries add(TimeSeries timeSeries1, TimeSeries timeSeries2) {
      return compute(timeSeries1, timeSeries2, ArithmetikMode.ADDITION);
   }

   public static TimeSeries substract(TimeSeries timeSeries1, TimeSeries timeSeries2) {
      return compute(timeSeries1, timeSeries2, ArithmetikMode.SUBSTRACTION);
   }

   public static TimeSeries multiply(TimeSeries timeSeries1, TimeSeries timeSeries2) {
      return compute(timeSeries1, timeSeries2, ArithmetikMode.MULTIPLICATION);
   }

   public static TimeSeries divide(TimeSeries timeSeries1, TimeSeries timeSeries2) {
      return compute(timeSeries1, timeSeries2, ArithmetikMode.DIVISION);
   }

   public static Series compute(Series series1, Series series2, ArithmetikMode mode) {
      SeriesUtils.checkSeriesClasses(series1, series2);
      return (Series)(series1 instanceof ValueSeries ? compute((ValueSeries)series1, (ValueSeries)series2, mode) : compute((TimeSeries)series1, (TimeSeries)series2, mode));
   }

   public static ValueSeries compute(ValueSeries valueSeries1, ValueSeries valueSeries2, ArithmetikMode mode) {
      if (valueSeries1 == null) {
         throw new ArgumentIsNullException("first value series");
      } else if (valueSeries2 == null) {
         throw new ArgumentIsNullException("second value series");
      } else if (valueSeries1.getLength() != valueSeries2.getLength()) {
         throw new IllegalSeriesLengthException(valueSeries1.getName(), valueSeries2.getName(), valueSeries1.getLength(), valueSeries2.getLength());
      } else {
         String resultSeriesName = valueSeries1.getName() + (String)concatStringMap.get(mode) + valueSeries2.getName();
         if (valueSeries1.hasDefaultIndices()) {
            return valueSeries2.hasDefaultIndices() ? ValueSeries.create(computeValues(valueSeries1.getValues(), valueSeries2.getValues(), mode), resultSeriesName) : ValueSeries.create(valueSeries2.getIndices(), computeValues(valueSeries1.getValues(), valueSeries2.getValues(), mode), resultSeriesName);
         } else if (valueSeries2.hasDefaultIndices()) {
            return ValueSeries.create(valueSeries1.getIndices(), computeValues(valueSeries1.getValues(), valueSeries2.getValues(), mode), resultSeriesName);
         } else if (!Arrays.equals(valueSeries1.getIndices(), valueSeries2.getIndices())) {
            throw new IndicesNotEqualException(valueSeries1.getName(), valueSeries2.getName());
         } else {
            return ValueSeries.create(valueSeries1.getIndices(), computeValues(valueSeries1.getValues(), valueSeries2.getValues(), mode), resultSeriesName);
         }
      }
   }

   public static TimeSeries compute(TimeSeries timeSeries1, TimeSeries timeSeries2, ArithmetikMode mode) {
      if (timeSeries1 == null) {
         throw new ArgumentIsNullException("first time series");
      } else if (timeSeries2 == null) {
         throw new ArgumentIsNullException("second time series");
      } else if (timeSeries1.getLength() != timeSeries2.getLength()) {
         throw new IllegalSeriesLengthException(timeSeries1.getName(), timeSeries2.getName(), timeSeries1.getLength(), timeSeries2.getLength());
      } else if (!timeSeries1.getIndices().equals(timeSeries2.getIndices())) {
         throw new IndicesNotEqualException(timeSeries1.getName(), timeSeries2.getName());
      } else {
         String resultSeriesName = timeSeries1.getName() + (String)concatStringMap.get(mode) + timeSeries2.getName();
         return TimeSeries.create(timeSeries1.getIndices(), computeValues(timeSeries1.getValues(), timeSeries2.getValues(), mode), resultSeriesName);
      }
   }

   private static double[] computeValues(double[] values1, double[] values2, ArithmetikMode mode) {
      double[] result = new double[values1.length];

      for(int i = 0; i < result.length; ++i) {
         switch(mode) {
         case ADDITION:
            result[i] = values1[i] + values2[i];
            break;
         case SUBSTRACTION:
            result[i] = values1[i] - values2[i];
            break;
         case MULTIPLICATION:
            result[i] = values1[i] * values2[i];
            break;
         case DIVISION:
            result[i] = values1[i] / values2[i];
         }
      }

      return result;
   }

   public static enum ArithmetikMode {
      ADDITION,
      SUBSTRACTION,
      MULTIPLICATION,
      DIVISION;
   }
}
