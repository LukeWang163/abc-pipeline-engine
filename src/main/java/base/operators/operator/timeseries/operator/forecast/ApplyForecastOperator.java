package base.operators.operator.timeseries.operator.forecast;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ForecastModel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ApplyForecastOperator extends Operator {
   protected InputPort forecastModelInputPort = this.getInputPorts().createPort("forecast model", AbstractForecastModel.class);
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("example set");
   protected OutputPort originalForecastModelOutputPort = (OutputPort)this.getOutputPorts().createPort("original");
   public static final String PARAMETER_FORECAST_HORIZON = "forecast_horizon";
   public static final String PARAMETER_ADD_ORIGINAL_TIME_SERIES = "add_original_time_series";
   public static final String PARAMETER_ADD_COMBINED_TIME_SERIES = "add_combined_time_series";
   private boolean callProgressSteps = false;

   public ApplyForecastOperator(OperatorDescription description) {
      super(description);
      this.getTransformer().addGenerationRule(this.exampleSetOutputPort, ExampleSet.class);
      this.getTransformer().addPassThroughRule(this.forecastModelInputPort, this.originalForecastModelOutputPort);
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            if (ApplyForecastOperator.this.forecastModelInputPort.isConnected()) {
               ExampleSetMetaData md = new ExampleSetMetaData();
               String seriesName = (String)ApplyForecastOperator.this.forecastModelInputPort.getMetaData().getMetaData("Series name");
               String indicesName = (String)ApplyForecastOperator.this.forecastModelInputPort.getMetaData().getMetaData("Indices name");
               if (indicesName != null) {
                  Integer indicesType = (Integer)ApplyForecastOperator.this.forecastModelInputPort.getMetaData().getMetaData("Indices type");
                  if (indicesType == null) {
                     indicesType = 0;
                  }

                  md.addAttribute(new AttributeMetaData(indicesName, indicesType, "id"));
               }

               if (seriesName != null) {
                  md.addAttribute(new AttributeMetaData("forecast of " + seriesName, 2, "prediction"));
                  if (ApplyForecastOperator.this.getParameterAsBoolean("add_original_time_series")) {
                     md.addAttribute(new AttributeMetaData(seriesName, 2));
                  }

                  if (ApplyForecastOperator.this.getParameterAsBoolean("add_combined_time_series")) {
                     md.addAttribute(new AttributeMetaData(seriesName + " and forecast", 2));
                  }
               }

               try {
                  MDInteger numberOfExamples = new MDInteger(ApplyForecastOperator.this.getParameterAsInt("forecast_horizon"));
                  if (ApplyForecastOperator.this.getParameterAsBoolean("add_combined_time_series") || ApplyForecastOperator.this.getParameterAsBoolean("add_original_time_series")) {
                     MDInteger lengthOfOriginalSeries = (MDInteger)ApplyForecastOperator.this.forecastModelInputPort.getMetaData().getMetaData("Length of series");
                     if (lengthOfOriginalSeries != null) {
                        numberOfExamples.add(lengthOfOriginalSeries);
                     }
                  }

                  md.setNumberOfExamples(numberOfExamples);
               } catch (UndefinedParameterError var6) {
                  var6.printStackTrace();
               }

               ApplyForecastOperator.this.exampleSetOutputPort.deliverMD(md);
            }

         }
      });
   }

   public void doWork() throws OperatorException {
      this.callProgressSteps = true;
      AbstractForecastModel forecastModel = (AbstractForecastModel)this.forecastModelInputPort.getData(AbstractForecastModel.class);
      this.exampleSetOutputPort.deliver(this.performForecast(forecastModel));
      this.originalForecastModelOutputPort.deliver(forecastModel.copy());
      this.getProgress().complete();
   }

   public ExampleSet performForecast(AbstractForecastModel forecastModel) throws OperatorException {
      boolean addOriginalSeries = this.getParameterAsBoolean("add_original_time_series");
      boolean addCombinedSeries = this.getParameterAsBoolean("add_combined_time_series");
      Series series = forecastModel.getSeries();
      if (this.callProgressSteps) {
         int totalStepLength = this.getParameterAsInt("forecast_horizon");
         if (addOriginalSeries || addCombinedSeries) {
            totalStepLength += series.getLength();
         }

         totalStepLength = totalStepLength / 2000000 + 1;
         this.getProgress().setCheckForStop(true);
         this.getProgress().setTotal(totalStepLength);
      }

      ForecastModel model = forecastModel.getModel();
      int forecastHorizon = this.getParameterAsInt("forecast_horizon");
      if (forecastHorizon <= 0) {
         throw new UserError(this, "116", new Object[]{"forecast_horizon", "forecast_horizon has to be larger than 0 and was " + forecastHorizon});
      } else {
         model.setForecastHorizon(this.getParameterAsInt("forecast_horizon"));
         List listOfNewAtts = new LinkedList();
         String indicesName = forecastModel.getIndicesAttributeName();
         String seriesName = series.getName();
         String combinedName = seriesName + " and forecast";
         String forecastName = "forecast of " + seriesName;
         Attribute indicesAttribute = this.getIndicesAttribute(series, indicesName);
         if (indicesAttribute != null) {
            listOfNewAtts.add(indicesAttribute);
         }

         Attribute forecastValuesAttribute = AttributeFactory.createAttribute(forecastName, 2);
         listOfNewAtts.add(forecastValuesAttribute);
         Attribute combinedSeriesAttribute;
         if (addOriginalSeries) {
            combinedSeriesAttribute = AttributeFactory.createAttribute(seriesName, 2);
            listOfNewAtts.add(combinedSeriesAttribute);
         }

         if (addCombinedSeries) {
            combinedSeriesAttribute = AttributeFactory.createAttribute(combinedName, 2);
            listOfNewAtts.add(combinedSeriesAttribute);
         }

         ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
         if (indicesAttribute != null) {
            builder.withRole(indicesAttribute, "id");
         }

         builder.withRole(forecastValuesAttribute, "prediction");
         if (addOriginalSeries || addCombinedSeries) {
            for(int i = 0; i < series.getLength(); ++i) {
               double[] row = new double[listOfNewAtts.size()];
               int j = 0;

               for(Iterator var18 = listOfNewAtts.iterator(); var18.hasNext(); ++j) {
                  Attribute attribute = (Attribute)var18.next();
                  if (!attribute.getName().equals(seriesName) && !attribute.getName().equals(combinedName)) {
                     if (attribute.getName().equals(indicesName)) {
                        row[j] = this.getIndexValue(series, i);
                     } else if (attribute.getName().equals(forecastName)) {
                        row[j] = Double.NaN;
                     }
                  } else {
                     row[j] = series.getValue(i);
                  }
               }

               builder.addRow(row);
               if (this.callProgressSteps && (i + 1) % 2000000 == 0) {
                  this.getProgress().step();
               }
            }
         }

         Series forecast = this.applyForecast(model, series);

         for(int i = 0; i < forecast.getLength(); ++i) {
            double[] row = new double[listOfNewAtts.size()];
            int j = 0;

            for(Iterator var27 = listOfNewAtts.iterator(); var27.hasNext(); ++j) {
               Attribute attribute = (Attribute)var27.next();
               if (!attribute.getName().equals(forecastName) && !attribute.getName().equals(combinedName)) {
                  if (attribute.getName().equals(indicesName)) {
                     row[j] = this.getIndexValue(forecast, i);
                  } else {
                     row[j] = Double.NaN;
                  }
               } else {
                  row[j] = forecast.getValue(i);
               }
            }

            builder.addRow(row);
            if (this.callProgressSteps && (i + series.getLength() + 1) % 2000000 == 0) {
               this.getProgress().step();
            }
         }

         return builder.build();
      }
   }

   private Series applyForecast(ForecastModel forecastModel, Series series) throws OperatorException {
      if (series.getClass() == ValueSeries.class) {
         return forecastModel.forecast((ValueSeries)series).getValueSeries("Forecast");
      } else if (series.getClass() == TimeSeries.class) {
         return forecastModel.forecast((TimeSeries)series).getTimeSeries("Forecast");
      } else {
         throw new OperatorException("Provided series is neither a ValueSeries nor a TimeSeries");
      }
   }

   private double getIndexValue(Series series, int i) throws OperatorException {
      if (series.getClass() == ValueSeries.class) {
         return this.getIndexValueFromValueSeries((ValueSeries)series, i);
      } else if (series.getClass() == TimeSeries.class) {
         return (double)this.getIndexValueFromTimeSeries((TimeSeries)series, i);
      } else {
         throw new OperatorException("Provided series is neither a ValueSeries nor a TimeSeries");
      }
   }

   private long getIndexValueFromTimeSeries(TimeSeries series, int i) {
      return series.getIndex(i).toEpochMilli();
   }

   private double getIndexValueFromValueSeries(ValueSeries series, int i) {
      return series.getIndex(i);
   }

   private Attribute getIndicesAttribute(Series series, String indicesName) {
      Attribute indicesAttribute = null;
      if (series.getClass() == ValueSeries.class) {
         ValueSeries valueSeries = (ValueSeries)series;
         if (!valueSeries.hasDefaultIndices()) {
            indicesAttribute = AttributeFactory.createAttribute(indicesName, 2);
         }
      } else if (series.getClass() == TimeSeries.class) {
         indicesAttribute = AttributeFactory.createAttribute(indicesName, 9);
      }

      return indicesAttribute;
   }

   public List getParameterTypes() {
      List types = super.getParameterTypes();
      types.add(new ParameterTypeInt("forecast_horizon", "This parameter specifies the length of the forecast.", 1, Integer.MAX_VALUE, 5, false));
      types.add(new ParameterTypeBoolean("add_original_time_series", "If this parameter is set to true an attribute containing the original values will be added.", true, false));
      types.add(new ParameterTypeBoolean("add_combined_time_series", "If this parameter is set to true an attribute containing the original as well as the forecasted values will be added.", true, false));
      return types;
   }
}
