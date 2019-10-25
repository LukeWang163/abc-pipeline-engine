package base.operators.operator.timeseries.operator.forecast.arima;

import base.operators.operator.timeseries.operator.forecast.AbstractForecastModel;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.Arima;

public class ArimaModel extends AbstractForecastModel {
   private static final long serialVersionUID = -3934333732331592636L;

   public ArimaModel(Arima arima, Series series, String indicesAttributeName) {
      super(arima, series, indicesAttributeName);
   }

   public String toResultString() {
      String resultString = "Arima Model trained on the following Time Series:\n";
      resultString = resultString + "Name of Time Series: " + this.series.getName();
      if (this.indicesAttributeName != "") {
         resultString = resultString + "\tName of indices Attribute: " + this.indicesAttributeName;
      }

      resultString = resultString + "\tNumber of values: " + this.series.getLength() + "\n\n";
      resultString = resultString + "Resulting Arima Model: \n";
      resultString = resultString + ((Arima)this.forecastModel).toString();
      return resultString;
   }

   public ArimaModel copy() {
      return new ArimaModel((Arima)((Arima)this.forecastModel).copy(), this.series.clone(), this.indicesAttributeName);
   }
}
