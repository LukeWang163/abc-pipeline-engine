package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import java.util.ArrayList;
import java.util.Arrays;

public class Differentiation implements ValueSeriesTransformation, TimeSeriesTransformation, MultivariateValueSeriesTransformation, MultivariateTimeSeriesTransformation {
   private DifferentiationMethod differentiationMethod;
   private int lag;

   private Differentiation(int lag, DifferentiationMethod differentiationMethod) {
      this.differentiationMethod = differentiationMethod;
      this.lag = lag;
   }

   public static Differentiation createSimpleDifferentiation() {
      return new Differentiation(1, DifferentiationMethod.SUBTRACTION);
   }

   public static Differentiation createDifferentiation(int lag, DifferentiationMethod differentiationMethod) {
      if (differentiationMethod == null) {
         throw new ArgumentIsNullException("differentation method");
      } else if (lag < 1) {
         throw new IllegalIndexArgumentException("lag value", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         return new Differentiation(lag, differentiationMethod);
      }
   }

   public ValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else if (this.lag >= valueSeries.getLength()) {
         throw new IllegalArgumentException("The length of the provided valueSeries must be greater then the specified lag value (" + this.lag + ")");
      } else if (valueSeries.hasDefaultIndices()) {
         return ValueSeries.create(applyDifferentiation(valueSeries.getValues(), this.lag, this.differentiationMethod), valueSeries.getName());
      } else {
         double[] newIndices = Arrays.copyOf(valueSeries.getIndices(), valueSeries.getLength());
         return ValueSeries.create(newIndices, applyDifferentiation(valueSeries.getValues(), this.lag, this.differentiationMethod), valueSeries.getName());
      }
   }

   public TimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else if (this.lag >= timeSeries.getLength()) {
         throw new IllegalArgumentException("The length of the provided valueSeries must be greater then the specified lag value (" + this.lag + ")");
      } else {
         double[] differentiatedValues = applyDifferentiation(timeSeries.getValues(), this.lag, this.differentiationMethod);
         ArrayList newIndices = (ArrayList)timeSeries.getIndices().clone();
         return TimeSeries.create(newIndices, differentiatedValues, timeSeries.getName());
      }
   }

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         ArrayList seriesList = new ArrayList();

         for(int i = 0; i < multivariateValueSeries.getSeriesCount(); ++i) {
            seriesList.add(this.compute(multivariateValueSeries.getValueSeries(i)));
         }

         return MultivariateValueSeries.create(seriesList, false);
      }
   }

   public MultivariateTimeSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         ArrayList seriesList = new ArrayList();

         for(int i = 0; i < multivariateTimeSeries.getSeriesCount(); ++i) {
            seriesList.add(this.compute(multivariateTimeSeries.getTimeSeries(i)));
         }

         return MultivariateTimeSeries.create(seriesList);
      }
   }

   private static double[] applyDifferentiation(double[] values, int lag, DifferentiationMethod differentiationMethod) {
      double[] differencedValues = new double[values.length];

      for(int i = 0; i < values.length; ++i) {
         if (i - lag < 0) {
            differencedValues[i] = Double.NaN;
         } else {
            differencedValues[i] = calculateDifference(values[i - lag], values[i], differentiationMethod);
         }
      }

      return differencedValues;
   }

   private static double calculateDifference(double value1, double value2, DifferentiationMethod differentiationMethod) {
      switch(differentiationMethod) {
      case SUBTRACTION:
         return value2 - value1;
      case RATIO:
         return value2 / value1;
      case DIRECTION:
         if (!Double.isNaN(value1) && !Double.isNaN(value2)) {
            if (value1 < value2) {
               return 1.0D;
            }

            if (value1 > value2) {
               return -1.0D;
            }

            return 0.0D;
         }

         return Double.NaN;
      default:
         throw new IllegalArgumentException("Invalid ENUM Type:" + differentiationMethod.toString());
      }
   }

   public static double[] diff(double[] values, int order) {
      double[] tempValues = values;

      for(int k = 0; k < order; ++k) {
         tempValues = applyDifferentiation(tempValues, 1, DifferentiationMethod.SUBTRACTION);
      }

      return Arrays.copyOfRange(tempValues, order, tempValues.length);
   }

   private static double[] singleDiff(double[] values) {
      double[] tempValues = new double[values.length - 1];

      for(int i = 1; i < values.length; ++i) {
         tempValues[i - 1] = values[i] - values[i - 1];
      }

      return tempValues;
   }

   public static enum DifferentiationMethod {
      SUBTRACTION,
      RATIO,
      DIRECTION;
   }
}
