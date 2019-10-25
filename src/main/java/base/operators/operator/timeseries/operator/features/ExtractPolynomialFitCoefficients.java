package base.operators.operator.timeseries.operator.features;

import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilders;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.feature.PolynomialFitCoefficients;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.util.Pair;

public class ExtractPolynomialFitCoefficients extends AbstractFeaturesOperator {
   private OutputPort fittedOutputPort = (OutputPort)this.getOutputPorts().createPort("fitted");
   private PolynomialFitCoefficients defaultPolynomialFit = null;
   private PolynomialFitCoefficients realPolynomialFit = null;
   private PolynomialFitCoefficients timePolynomialFit = null;
   SeriesBuilder seriesBuilder = null;
   public static final String PARAMETER_DEGREE = "degree";
   public static final String PARAMETER_ADD_DISCREPANCY = "add_discrepancy";
   public static final String FITTED_PREFIX = "fitted";
   public static final String USER_ERROR_KEY_FIT_NOT_CONVERGED = "time_series_extension.parameter.polynomial_fit_not_converged";

   public ExtractPolynomialFitCoefficients(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            ExampleSetMetaData emd = new ExampleSetMetaData();

            try {
               AttributeMetaData indicesAttributeMetaData = ExtractPolynomialFitCoefficients.this.exampleSetTimeSeriesHelper.getIndicesAttributeMetaData(ExtractPolynomialFitCoefficients.this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
               if (indicesAttributeMetaData != null) {
                  emd.addAttribute(indicesAttributeMetaData.clone());
               }
            } catch (WrongConfiguredHelperException | IncompatibleMDClassException | UndefinedParameterError var6) {
               ExtractPolynomialFitCoefficients.this.getLogger().warning(var6.getMessage());
            }

            try {
               List selectedAttributes = ExtractPolynomialFitCoefficients.this.exampleSetTimeSeriesHelper.getSelectedTimeSeriesAttributesMetaData(ExtractPolynomialFitCoefficients.this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
               if (!selectedAttributes.isEmpty()) {
                  Iterator var3 = selectedAttributes.iterator();

                  while(var3.hasNext()) {
                     AttributeMetaData amd = (AttributeMetaData)var3.next();
                     emd.addAttribute(amd);
                     AttributeMetaData fittedAmd = amd.clone();
                     fittedAmd.setName("fitted " + fittedAmd.getName());
                     emd.addAttribute(fittedAmd);
                  }
               }
            } catch (WrongConfiguredHelperException | IncompatibleMDClassException | UndefinedParameterError var7) {
               ExtractPolynomialFitCoefficients.this.getLogger().warning(var7.getMessage());
            }

            ExtractPolynomialFitCoefficients.this.fittedOutputPort.deliverMD(emd);
         }
      });
   }

   protected void initFeatureCalculation(ISeries inputSeries) throws UserError {
      int degree = this.getParameterAsInt("degree");
      PolynomialFitCoefficients.PolynomialFitBuilder defaultBuilder = (new PolynomialFitCoefficients.PolynomialFitBuilder()).degree(degree);
      PolynomialFitCoefficients.PolynomialFitBuilder realBuilder = (new PolynomialFitCoefficients.PolynomialFitBuilder()).degree(degree);
      PolynomialFitCoefficients.PolynomialFitBuilder timeBuilder = (new PolynomialFitCoefficients.PolynomialFitBuilder()).degree(degree);
      if (this.getParameterAsBoolean("add_discrepancy")) {
         defaultBuilder = defaultBuilder.calculateDiscrepancy();
         realBuilder = realBuilder.calculateDiscrepancy();
         timeBuilder = timeBuilder.calculateDiscrepancy();
      }

      if (this.getParameterAsBoolean("ignore_invalid_values")) {
         defaultBuilder = defaultBuilder.skipInvalidValues();
         realBuilder = realBuilder.skipInvalidValues();
         timeBuilder = timeBuilder.skipInvalidValues();
      }

      this.defaultPolynomialFit = (PolynomialFitCoefficients)defaultBuilder.build();
      this.realPolynomialFit = (PolynomialFitCoefficients)realBuilder.build();
      this.timePolynomialFit = (PolynomialFitCoefficients)timeBuilder.build();
      if (this.fittedOutputPort.isConnected()) {
         this.seriesBuilder = SeriesBuilders.fromSeries(inputSeries).indexDimension(inputSeries.getIndexDimension());
      }

   }

   private List convertFeaturesToFeatureContainerList(List featureList) {
      List result = new ArrayList();
      Iterator var3 = featureList.iterator();

      while(var3.hasNext()) {
         Pair valuePair = (Pair)var3.next();
         FeatureContainer feature = (new FeatureContainer()).setName((String)valuePair.getFirst()).setValueType(4).setSeriesValuesType(SeriesBuilder.ValuesType.REAL).setValue(valuePair.getSecond()).setDoubleValue(valuePair.getSecond() == null ? Double.NaN : (Double)valuePair.getSecond());
         result.add(new Pair(valuePair.getFirst(), feature));
      }

      return result;
   }

   protected List getFeaturesDefaultReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      try {
         List featureList = this.defaultPolynomialFit.getComputedFeatures(indexDimension, seriesValues);
         if (this.fittedOutputPort.isConnected()) {
            this.seriesBuilder = this.seriesBuilder.addSeriesValues(seriesValues).addSeriesValues(this.defaultPolynomialFit.getSeriesFunction().evaluate(indexDimension, "fitted " + seriesValues.getName()));
         }

         return this.convertFeaturesToFeatureContainerList(featureList);
      } catch (ConvergenceException var4) {
         throw new UserError(this, "time_series_extension.parameter.polynomial_fit_not_converged", new Object[]{this.getParameterAsInt("degree"), seriesValues.getName()});
      }
   }

   protected List getFeaturesRealReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      try {
         List featureList = this.realPolynomialFit.getComputedFeatures(indexDimension, seriesValues);
         if (this.fittedOutputPort.isConnected()) {
            this.seriesBuilder = this.seriesBuilder.addSeriesValues(seriesValues).addSeriesValues(this.realPolynomialFit.getSeriesFunction().evaluate(indexDimension, "fitted " + seriesValues.getName()));
         }

         return this.convertFeaturesToFeatureContainerList(featureList);
      } catch (ConvergenceException var4) {
         throw new UserError(this, "time_series_extension.parameter.polynomial_fit_not_converged", new Object[]{this.getParameterAsInt("degree"), seriesValues.getName()});
      }
   }

   protected List getFeaturesTimeReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      try {
         List featureList = this.timePolynomialFit.getComputedFeatures(indexDimension, seriesValues);
         if (this.fittedOutputPort.isConnected()) {
            this.seriesBuilder = this.seriesBuilder.addSeriesValues(seriesValues).addSeriesValues(this.timePolynomialFit.getSeriesFunction().evaluate(indexDimension, "fitted " + seriesValues.getName()));
         }

         return this.convertFeaturesToFeatureContainerList(featureList);
      } catch (ConvergenceException var4) {
         throw new UserError(this, "time_series_extension.parameter.polynomial_fit_not_converged", new Object[]{this.getParameterAsInt("degree"), seriesValues.getName()});
      }
   }

   protected void finishFeatureCalculation(ISeries inputSeries) throws ProcessStoppedException {
      if (this.fittedOutputPort.isConnected()) {
         this.fittedOutputPort.deliver(this.exampleSetTimeSeriesHelper.convertISeriesToExampleSet(this.seriesBuilder.build()));
      }

   }

   protected List parameterTypesBefore() {
      List types = new ArrayList();
      types.add(new ParameterTypeInt("degree", "Degree of the fitted polynom.", 0, Integer.MAX_VALUE, 2, false));
      types.add(new ParameterTypeBoolean("add_discrepancy", "Select if discrepancy shall be added to the calculated features.", true, false));
      return types;
   }

   protected List parameterTypesAfter() {
      return new ArrayList();
   }

   protected String[] getFeatureNames() throws UndefinedParameterError {
      PolynomialFitCoefficients.PolynomialFitBuilder defaultBuilder = (new PolynomialFitCoefficients.PolynomialFitBuilder()).degree(this.getParameterAsInt("degree"));
      if (this.getParameterAsBoolean("add_discrepancy")) {
         defaultBuilder = defaultBuilder.calculateDiscrepancy();
      }

      return defaultBuilder.build().getFeatureNames();
   }

   protected ExampleSetTimeSeriesHelper.IndiceHandling getIndicesHandling() {
      return ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES;
   }

   protected boolean failOnInvalidValues() {
      return true;
   }

   protected int getDefaultValueType() {
      return 4;
   }

   protected SeriesBuilder.ValuesType getValuesType() {
      return SeriesBuilder.ValuesType.REAL;
   }

   protected boolean sortAttributes() {
      return false;
   }
}
