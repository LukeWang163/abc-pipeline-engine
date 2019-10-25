package base.operators.operator.timeseries.timeseriesanalysis.functions;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.util.ArrayList;
import java.util.List;


public class PolynomialSeriesFunction<I> implements SeriesFunction<I, Double>
{
   private PolynomialFunction function;
   private String name;
   public static final String DEFAULT_NAME = "polynomial function";

   public PolynomialSeriesFunction(double[] coeff) {
      this.function = null;
      this.name = null;

















      this.function = new PolynomialFunction(coeff);
      this.name = "polynomial function";
   }














   public PolynomialSeriesFunction(double[] coeff, String name) {
      this.function = null;
      this.name = null;
      this.function = new PolynomialFunction(coeff);
      this.name = name;
   }


   public SeriesValues<Double> evaluate(IndexDimension<I> indexDimension, String evaluatedSeriesName) {
      List<Double> fittedValues = new ArrayList<Double>();
      for (int i = 0; i < indexDimension.getLength(); i++) {
         fittedValues.add(Double.valueOf(this.function.value(indexDimension.getIndexValueAsDouble(i))));
      }
      if (evaluatedSeriesName == null || evaluatedSeriesName.isEmpty()) {
         return new RealValues(fittedValues, false);
      }
      return new RealValues(fittedValues, false, evaluatedSeriesName);
   }










   public double value(double x) { return this.function.value(x); }




   public String getName() { return this.name; }








   public int getDegree() { return this.function.degree(); }











   public double[] getCoeff() { return this.function.getCoefficients(); }












   public void setCoeff(double[] coeff) { this.function = new PolynomialFunction(coeff); }
}
