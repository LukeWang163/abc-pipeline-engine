package base.operators.operator.timeseries.operator.forecast;

import base.operators.operator.ResultObjectAdapter;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ForecastModel;

public abstract class AbstractForecastModel extends ResultObjectAdapter {
   protected ForecastModel forecastModel;
   protected Series series;
   protected String indicesAttributeName;
   private static final long serialVersionUID = -3934333732331592636L;

   public AbstractForecastModel(ForecastModel forecastModel, Series series, String indicesAttributeName) {
      this.forecastModel = forecastModel;
      this.series = series;
      this.indicesAttributeName = indicesAttributeName;
   }

   public String toResultString() {
      String resultString = "Forecast Model trained on the following Time Series:\n";
      resultString = resultString + "Name of Time Series: " + this.series.getName();
      if (this.indicesAttributeName != "") {
         resultString = resultString + "\tName of indices Attribute: " + this.indicesAttributeName;
      }

      resultString = resultString + "\tNumber of values: " + this.series.getLength() + "\n\n";
      resultString = resultString + "Resulting Forecast Model: \n";
      resultString = resultString + this.forecastModel.toString();
      return resultString;
   }

   public abstract AbstractForecastModel copy();

   public ForecastModel getModel() {
      return this.forecastModel;
   }

   public void setModel(ForecastModel forecastModel) {
      this.forecastModel = forecastModel;
   }

   public Series getSeries() {
      return this.series;
   }

   public void setSeries(Series series) {
      this.series = series;
   }

   public String getIndicesAttributeName() {
      return this.indicesAttributeName;
   }

   public void setIndicesAttributeName(String indicesAttributeName) {
      this.indicesAttributeName = indicesAttributeName;
   }
}
