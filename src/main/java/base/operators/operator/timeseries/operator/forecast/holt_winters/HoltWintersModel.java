package base.operators.operator.timeseries.operator.forecast.holt_winters;

import base.operators.operator.timeseries.operator.forecast.AbstractForecastModel;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.holtwinters.HoltWinters;

public class HoltWintersModel extends AbstractForecastModel {
   private static final long serialVersionUID = -6912545734885864271L;

   public HoltWintersModel(HoltWinters holtWinters, Series series, String indicesAttributeName) {
      super(holtWinters, series, indicesAttributeName);
   }

   public String toResultString() {
      String resultString = "Holt-Winters Model trained on the following Time Series:\n";
      resultString = resultString + "Name of Time Series: " + this.series.getName();
      if (this.indicesAttributeName != "") {
         resultString = resultString + "\tName of indices Attribute: " + this.indicesAttributeName;
      }

      resultString = resultString + "\tNumber of values: " + this.series.getLength() + "\n\n";
      resultString = resultString + "Resulting Holt-Winters Model: \n";
      resultString = resultString + ((HoltWinters)this.forecastModel).toString();
      return resultString;
   }

   public HoltWintersModel copy() {
      return new HoltWintersModel((HoltWinters)((HoltWinters)this.forecastModel).copy(), this.series.clone(), this.indicesAttributeName);
   }
}
