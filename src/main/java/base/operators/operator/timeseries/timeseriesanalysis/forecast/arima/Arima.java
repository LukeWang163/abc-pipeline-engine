package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ForecastModel;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import java.util.ArrayList;
import org.apache.commons.math3.util.CombinatoricsUtils;

public class Arima extends ForecastModel {
   private static final long serialVersionUID = 5326963760682210901L;
   private int p;
   private int d;
   private int q;
   private double[] arCoefficients;
   private double[] maCoefficients;
   private double constant;
   private double[] residuals;

   private Arima(int p, int d, int q, double[] arCoefficients, double[] maCoefficients, double constant, double[] residuals, int forecastHorizon, String forecastSeriesName) {
      if (p != arCoefficients.length) {
         throw new IllegalArgumentException("Length of arCoefficients array is not equal provided p.\np: " + p + " arCoefficients.length: " + arCoefficients.length);
      } else if (d < 0) {
         throw new IllegalIndexArgumentException("d", d, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (q != maCoefficients.length) {
         throw new IllegalArgumentException("Length of maCoefficients array is not equal provided q.\nq: " + q + " maCoefficients.length: " + maCoefficients.length);
      } else {
         this.p = p;
         this.d = d;
         this.q = q;
         this.arCoefficients = arCoefficients;
         this.maCoefficients = maCoefficients;
         this.constant = constant;
         this.setResiduals(residuals);
         this.setForecastHorizon(forecastHorizon);
         this.setForecastSeriesName(forecastSeriesName);
      }
   }

   public static Arima create(int p, int d, int q, double[] arCoefficients, double[] maCoefficients, double constant, double[] residuals, int forecastHorizon, String forecastSeriesName) {
      return new Arima(p, d, q, arCoefficients, maCoefficients, constant, residuals, forecastHorizon, forecastSeriesName);
   }

   public static Arima create(int p, int d, int q, double[] arCoefficients, double[] maCoefficients, double constant, double[] residuals) {
      return new Arima(p, d, q, arCoefficients, maCoefficients, constant, residuals, 0, "Forecast");
   }

   public static Arima create(int d, double[] arCoefficients, double[] maCoefficients) {
      return new Arima(arCoefficients.length, d, maCoefficients.length, arCoefficients, maCoefficients, 0.0D, (double[])null, 0, "Forecast");
   }

   public static Arima create(int d, double[] arCoefficients, double[] maCoefficients, double constant) {
      return new Arima(arCoefficients.length, d, maCoefficients.length, arCoefficients, maCoefficients, constant, (double[])null, 0, "Forecast");
   }

   public MultivariateValueSeries forecast(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         ArrayList valuesList = new ArrayList();
         ArrayList nameList = new ArrayList();
         valuesList.add(this.forecastValues(valueSeries.getValues()));
         nameList.add(this.getForecastSeriesName());
         if (valueSeries.hasDefaultIndices()) {
            return MultivariateValueSeries.create(valuesList, nameList);
         } else {
            double[] indices = this.getIndicesForForecastedValues(valueSeries, ((double[])valuesList.get(0)).length);
            return MultivariateValueSeries.create(indices, valuesList, nameList);
         }
      }
   }

   public MultivariateTimeSeries forecast(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         ArrayList valuesList = new ArrayList();
         ArrayList nameList = new ArrayList();
         valuesList.add(this.forecastValues(timeSeries.getValues()));
         nameList.add(this.getForecastSeriesName());
         ArrayList indices = this.getIndicesForForecastedValues(timeSeries, ((double[])valuesList.get(0)).length);
         return MultivariateTimeSeries.create(indices, valuesList, nameList);
      }
   }

   public String toString() {
      new String();
      String result = "Arima Model (p: " + this.p + ", d: " + this.d + ", q: " + this.q + ")\n";
      result = result + "AR Coefficients: [";
      double[] var2 = this.arCoefficients;
      int var3 = var2.length;

      int var4;
      double value;
      for(var4 = 0; var4 < var3; ++var4) {
         value = var2[var4];
         result = result + value + ",";
      }

      result = result + "]\n";
      result = result + "MA Coefficients: [";
      var2 = this.maCoefficients;
      var3 = var2.length;

      for(var4 = 0; var4 < var3; ++var4) {
         value = var2[var4];
         result = result + value + ",";
      }

      result = result + "]\n";
      result = result + "constant: " + this.constant + "\n";
      return result;
   }

   public void setForecastHorizon(int forecastHorizon) {
      if (forecastHorizon < 0) {
         throw new IllegalIndexArgumentException("forecast horizon", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         this.forecastHorizon = forecastHorizon;
      }
   }

   public void setResiduals(double[] residuals) {
      this.residuals = residuals;
   }

   private double[] forecastValues(double[] values) {
      if (values.length < this.p + this.d) {
         throw new IllegalArgumentException("The values array is not long enough. It has to have at least p+d+1 entries. values.length: " + values.length + " p: " + this.p + " d: " + this.d);
      } else if (this.residuals.length < this.q) {
         throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.RESIDUALS_NOT_LONG_ENOUGH);
      } else {
         double[] result = new double[this.forecastHorizon];

         for(int h = 0; h < this.forecastHorizon; ++h) {
            result[h] = this.forecastOneValue(h + values.length, values, result, this.residuals);
         }

         return result;
      }
   }

   public double forecastOneValue(int forecastIndex, double[] values, double[] previousForecastedValues, double[] residuals) {
      int residualIndexOffset = values.length - residuals.length;
      double arCoeffSum = 0.0D;

      for(int i = 0; i < this.arCoefficients.length; ++i) {
         arCoeffSum += this.arCoefficients[i];
      }

      double result = this.constant * (1.0D - arCoeffSum);
      int maxResidualIndex = forecastIndex - residualIndexOffset;
      int i;
      if (maxResidualIndex - this.q < residuals.length) {
         for(i = 0; i <= this.q; ++i) {
            int residualIndex = maxResidualIndex - i;
            if (residualIndex < residuals.length && residualIndex >= 0) {
               double coefficient = 1.0D;
               if (i != 0) {
                  coefficient = this.maCoefficients[i - 1];
               }

               result += coefficient * residuals[residualIndex];
            }
         }
      }

      double coefficient;
      for(i = 1; i <= this.p; ++i) {
         coefficient = this.arCoefficients[i - 1];

         for(int k = 0; k <= this.d; ++k) {
            if (forecastIndex - i - k >= 0) {
               double value = this.getValueForCurrentTerm(forecastIndex - i - k, values, previousForecastedValues);
               result += coefficient * CombinatoricsUtils.binomialCoefficientDouble(this.d, k) * value;
            }
         }
      }

      for(i = 1; i <= this.d; ++i) {
         if (forecastIndex - i >= 0) {
            coefficient = this.getValueForCurrentTerm(forecastIndex - i, values, previousForecastedValues);
            result -= Math.pow(-1.0D, (double)i) * CombinatoricsUtils.binomialCoefficientDouble(this.d, i) * coefficient;
         }
      }

      return result;
   }

   private double getValueForCurrentTerm(int valueIndex, double[] values, double[] previousForecastedValues) {
      if (valueIndex < values.length) {
         return values[valueIndex];
      } else if (valueIndex - values.length >= previousForecastedValues.length) {
         throw new IllegalArgumentException("Not enough previously forecasted values provided.");
      } else {
         return previousForecastedValues[valueIndex - values.length];
      }
   }

   public ForecastModel copy() {
      return create(this.p, this.d, this.q, (double[])this.arCoefficients.clone(), (double[])this.maCoefficients.clone(), this.constant, (double[])this.residuals.clone(), this.forecastHorizon, this.getForecastSeriesName());
   }

   public double[] getArCoefficients() {
      return this.arCoefficients;
   }

   public double[] getMaCoefficients() {
      return this.maCoefficients;
   }

   public double[] getArimaParameters() {
      return ArimaUtils.getParametersArray(this.arCoefficients, this.maCoefficients, this.constant, true);
   }

   public int getP() {
      return this.p;
   }

   public int getD() {
      return this.d;
   }

   public int getQ() {
      return this.q;
   }

   public double[] getResiduals() {
      return this.residuals;
   }
}
