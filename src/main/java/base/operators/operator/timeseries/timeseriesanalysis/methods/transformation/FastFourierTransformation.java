package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.util.ArrayList;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FastFourierTransformation implements MultivariateSpaceTransformation, SpaceTransformationOnMultivariateSeries {
   public static final String AMPLITUDE_POSTFIX = "_Amplitude";
   public static final String PHASE_POSTFIX = "_Phase";

   public MultivariateValueSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         ArrayList fftList = this.computeFFT(timeSeries.getValues(), timeSeries.getName(), -1);
         return MultivariateValueSeries.create(fftList, true);
      }
   }

   public MultivariateValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         ArrayList fftList = this.computeFFT(valueSeries.getValues(), valueSeries.getName(), -1);
         return MultivariateValueSeries.create(fftList, true);
      }
   }

   public MultivariateValueSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         ArrayList seriesList = new ArrayList();
         int padding = -1;

         for(int i = 0; i < multivariateTimeSeries.getSeriesCount(); ++i) {
            seriesList.addAll(this.computeFFT(multivariateTimeSeries.getValues(i), multivariateTimeSeries.getName(i), padding));
            if (padding == -1) {
               padding = ((ValueSeries)seriesList.get(i)).getLength();
            }
         }

         return MultivariateValueSeries.create(seriesList, false);
      }
   }

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         ArrayList seriesList = new ArrayList();
         int padding = -1;

         for(int i = 0; i < multivariateValueSeries.getSeriesCount(); ++i) {
            seriesList.addAll(this.computeFFT(multivariateValueSeries.getValues(i), multivariateValueSeries.getName(i), padding));
            if (padding == -1) {
               padding = ((ValueSeries)seriesList.get(i)).getLength() * 2;
            }
         }

         return MultivariateValueSeries.create(seriesList, false);
      }
   }

   private ArrayList computeFFT(double[] values, String seriesName, int length) {
      FastFourierTransformer fftTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
      double[] paddedValues = this.padding(values, length);
      Complex[] fftResult = fftTransformer.transform(paddedValues, TransformType.FORWARD);
      int returnLength = fftResult.length / 2;
      double[] amplitude = new double[returnLength];
      double[] phase = new double[returnLength];

      for(int i = 0; i < returnLength; ++i) {
         amplitude[i] = Math.sqrt(Math.pow(fftResult[i].getReal(), 2.0D) + Math.pow(fftResult[i].getImaginary(), 2.0D));
         phase[i] = Math.atan2(fftResult[i].getImaginary(), fftResult[i].getReal()) * 180.0D / 3.141592653589793D;
      }

      String amplitudeName = seriesName + "_Amplitude";
      String phaseName = seriesName + "_Phase";
      ValueSeries amplitudeSeries = ValueSeries.create(amplitude, amplitudeName);
      ValueSeries phaseSeries = ValueSeries.create(phase, phaseName);
      ArrayList fftList = new ArrayList();
      fftList.add(amplitudeSeries);
      fftList.add(phaseSeries);
      return fftList;
   }

   private double[] padding(double[] values, int length) {
      if (length == -1) {
         int power = (int)(Math.log((double)values.length) / Math.log(2.0D));
         length = (int)Math.pow(2.0D, (double)power);
      }

      double[] padded = new double[length];

      for(int i = 0; i < padded.length; ++i) {
         padded[i] = values[i];
      }

      return padded;
   }
}
