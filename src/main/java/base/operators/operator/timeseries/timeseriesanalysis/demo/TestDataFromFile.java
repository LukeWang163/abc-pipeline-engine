package base.operators.operator.timeseries.timeseriesanalysis.demo;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.ArimaTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import java.io.IOException;

public class TestDataFromFile {
   public static void main(String[] args) throws IOException {
      String file = "./arima_sim_r_lake_huron.json";
      ValueSeries testData = null;
      testData = SeriesIO.readValueSeriesFromJSON(file);
      SeriesIO.writeValueSeriesToJSON("./arimaTestData.json", testData);
      double[] values = testData.getValues();
      ArimaTrainer arimaTrainer = ArimaTrainer.create(1, 0, 0, true, false, ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD, ArimaUtils.OptimizationMethod.BOBYQA, 1000, false);
      arimaTrainer.trainArima(values);
   }
}
