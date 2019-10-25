package base.operators.operator.timeseries.timeseriesanalysis.feature;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import base.operators.operator.timeseries.timeseriesanalysis.functions.PolynomialSeriesFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class PolynomialFitCoefficients<I> implements SeriesValuesFeature<I, Double>
{
   public static final String FEATURE_NAME = "polynomial_fit";

   public static class PolynomialFitBuilder<T>
           extends AbstractFeaturesBuilder<T, Double>
   {
      public PolynomialFitBuilder<T> reset() {
         this.seriesValuesFeature = new PolynomialFitCoefficients();
         return this;
      }









      public PolynomialFitBuilder<T> degree(int degree) {
         ((PolynomialFitCoefficients)this.seriesValuesFeature).setDegree(degree);
         return this;
      }







      public PolynomialFitBuilder<T> calculateDiscrepancy() {
         ((PolynomialFitCoefficients)this.seriesValuesFeature).setAddDiscrepancy(true);
         return this;
      }



      public PolynomialFitBuilder<T> skipInvalidValues() { return (PolynomialFitBuilder)super.skipInvalidValues(); }
   }




   private int degree = 2;
   private boolean addDiscrepancy = false;
   private double discrepency = 0.0D;

   private boolean skipInvalidValues = false;

   private PolynomialSeriesFunction<I> seriesFunction = null;





   public String getName() { return "polynomial_fit"; }



   public String[] getFeatureNames() {
      List<String> resultList = new ArrayList<String>();
      for (int i = 0; i <= this.degree; i++) {
         resultList.add(getName() + "_coeff_" + i);
      }
      if (this.addDiscrepancy) {
         resultList.add(getName() + "_discr");
      }
      return (String[])resultList.toArray(new String[0]);
   }


   public void compute(IndexDimension<I> indexDimension, SeriesValues<Double> seriesValues) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      if (seriesValues == null) {
         throw new ArgumentIsNullException("series values");
      }
      if (indexDimension.getIndexType() == SeriesBuilder.IndexType.NOMINAL) {
         throw new IllegalArgumentException("Nominal index type is not supported for fitting a polynomial function.");
      }
      if (!this.skipInvalidValues && seriesValues.hasMissingValues()) {
         throw new SeriesContainsInvalidValuesException(seriesValues.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      }
      if (!this.skipInvalidValues && ((RealValues)seriesValues).hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(seriesValues.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      }
      WeightedObservedPoints obs = new WeightedObservedPoints();
      for (int i = 0; i < seriesValues.getLength(); i++) {
         Double value = (Double)seriesValues.getValue(i);
         if (!this.skipInvalidValues || (value != null && Double.isFinite(value.doubleValue())))
         {

            obs.add(indexDimension.getIndexValueAsDouble(i), value.doubleValue()); }
      }
      List<WeightedObservedPoint> observations = obs.toList();
      if (observations.isEmpty()) {
         throw new SeriesContainsInvalidValuesException(seriesValues.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.ONLY_NON_FINITE);
      }
      PolynomialCurveFitter fitter = PolynomialCurveFitter.create(this.degree);
      this.seriesFunction = new PolynomialSeriesFunction(fitter.fit(observations));
      if (this.addDiscrepancy) {
         this.discrepency = 0.0D;
         for (WeightedObservedPoint point : observations) {
            double fittedValue = this.seriesFunction.value(point.getX());
            this.discrepency += Math.pow(fittedValue - point.getY(), 2.0D);
         }
         this.discrepency /= observations.size();
      }
   }


   public List<Pair<String, Double>> getComputedFeatures() {
      if (this.seriesFunction == null) {
         throw new WrongExecutionOrderException(getName() + " was not applied on seriesValues.", new String[] { "compute(IndexDimension indexDimension, SeriesValues seriesValues)" });
      }

      double[] coeff = this.seriesFunction.getCoeff();
      List<Pair<String, Double>> result = new ArrayList<Pair<String, Double>>();
      for (int i = 0; i < coeff.length; i++) {
         result.add(new Pair(getName() + "_coeff_" + i, Double.valueOf(coeff[i])));
      }
      if (this.addDiscrepancy) {
         result.add(new Pair(getName() + "_discr", Double.valueOf(this.discrepency)));
      }
      return result;
   }







   public int getDegree() { return this.degree; }









   public void setDegree(int degree) {
      if (degree < 0) {
         throw new IllegalIndexArgumentException("polynomial degree", Integer.valueOf(degree), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      }
      this.degree = degree;
   }









   public boolean isAddDiscrepancy() { return this.addDiscrepancy; }












   public void setAddDiscrepancy(boolean addDiscrepancy) { this.addDiscrepancy = addDiscrepancy; }













   public void setSkipInvalidValues(boolean skipInvalidValues) { this.skipInvalidValues = skipInvalidValues; }








   public PolynomialSeriesFunction<I> getSeriesFunction() {
      if (this.seriesFunction == null) {
         throw new WrongExecutionOrderException(getName() + " was not applied on seriesValues.", new String[] { "compute(IndexDimension indexDimension, SeriesValues seriesValues)" });
      }

      return this.seriesFunction;
   }
}
