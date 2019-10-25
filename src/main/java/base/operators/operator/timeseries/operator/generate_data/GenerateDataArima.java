package base.operators.operator.timeseries.operator.generate_data;

import base.operators.example.Attribute;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.demo.GenerateData;
import base.operators.tools.RandomGenerator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GenerateDataArima extends ExampleSetTimeSeriesOperator {
   public static final String PARAMETER_AR_COEFFICIENTS = "coefficients_of_the_auto-regressive_terms";
   public static final String PARAMETER_MA_COEFFICIENTS = "coefficients_of_the_moving-average_terms";
   public static final String PARAMETER_COEFFICIENT = "coefficient";
   public static final String PARAMETER_CONSTANT = "constant";
   public static final String PARAMETER_SIGMA = "standard_deviation_of_the_innovations";
   public static final String PARAMETER_LENGTH = "length";

   public GenerateDataArima(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            ExampleSetMetaData md = new ExampleSetMetaData();

            try {
               md.addAttribute(new AttributeMetaData(GenerateDataArima.this.getParameterAsString("name_of_new_time_series_attribute"), 4));
               md.setNumberOfExamples(GenerateDataArima.this.getParameterAsInt("length"));
            } catch (UndefinedParameterError var3) {
               var3.printStackTrace();
            }

            GenerateDataArima.this.exampleSetTimeSeriesHelper.getExampleSetOutputPort().deliverMD(md);
         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asOutputPortOperator("arima", "arima").build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      double[] arCoefficients = this.getCoefficientsArrayFromParameterEnumeration("coefficients_of_the_auto-regressive_terms");
      double[] maCoefficients = this.getCoefficientsArrayFromParameterEnumeration("coefficients_of_the_moving-average_terms");
      double constant = this.getParameterAsDouble("constant");
      double sigma = this.getParameterAsDouble("standard_deviation_of_the_innovations");
      int length = this.getParameterAsInt("length");
      int stepsInExampleSetCreation = length / 2000000 + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(2 * stepsInExampleSetCreation);
      RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this.getParameterAsBoolean("use_local_random_seed"), this.getParameterAsInt("local_random_seed"));
      int offset = Math.max(arCoefficients.length, maCoefficients.length) + 5;
      ValueSeries valueSeries = GenerateData.generateArimaSeries(arCoefficients.length, 0, maCoefficients.length, arCoefficients, maCoefficients, constant, sigma, length + offset, (Random)randomGenerator);
      this.getProgress().step(stepsInExampleSetCreation);
      List listOfAtts = new LinkedList();
      Attribute timeSeriesAttribute = AttributeFactory.createAttribute(this.getParameterAsString("name_of_new_time_series_attribute"), 4);
      listOfAtts.add(timeSeriesAttribute);
      ExampleSetBuilder builder = ExampleSets.from(listOfAtts);
      double[] values = valueSeries.getValues();

      for(int i = offset; i < values.length; ++i) {
         double[] row = new double[]{values[i]};
         builder.addRow(row);
         if ((i - offset + 1) % 2000000 == 0) {
            this.getProgress().step();
         }
      }

      this.exampleSetTimeSeriesHelper.getExampleSetOutputPort().deliver(builder.build());
      this.getProgress().complete();
   }

   private double[] getCoefficientsArrayFromParameterEnumeration(String parameterString) throws OperatorException {
      String[] coefficientList = ParameterTypeEnumeration.transformString2Enumeration(this.getParameterAsString(parameterString));
      double[] coefficients = new double[coefficientList.length];
      int i = 0;
      String[] var5 = coefficientList;
      int var6 = coefficientList.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String entry = var5[var7];

         try {
            coefficients[i] = Double.valueOf(entry);
         } catch (NumberFormatException var10) {
            throw new UserError(this, 211, new Object[]{parameterString, entry});
         }

         ++i;
      }

      return coefficients;
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(new ArrayList());
      types.add(new ParameterTypeEnumeration("coefficients_of_the_auto-regressive_terms", "The coefficients of the auto-regressive terms.", new ParameterTypeDouble("coefficient", "The Coefficient.", -10.0D, 10.0D), false));
      types.add(new ParameterTypeEnumeration("coefficients_of_the_moving-average_terms", "The coefficients of the moving-average terms.", new ParameterTypeDouble("coefficient", "The Coefficient.", -10.0D, 10.0D), false));
      types.add(new ParameterTypeDouble("constant", "This parameters sets a starting point for the ARIMA process.", -1.7976931348623157E308D, Double.MAX_VALUE, 0.0D));
      types.add(new ParameterTypeDouble("standard_deviation_of_the_innovations", "This parameter sets the standard deviation of the innovations (amount of variation for each new data point).", 0.0D, Double.MAX_VALUE, 1.0D));
      types.add(new ParameterTypeInt("length", "This parameter is the final length of the generated time series.", 1, Integer.MAX_VALUE, 1000, false));
      types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
      return types;
   }
}
