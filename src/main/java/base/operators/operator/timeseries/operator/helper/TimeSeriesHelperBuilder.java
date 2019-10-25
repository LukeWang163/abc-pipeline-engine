package base.operators.operator.timeseries.operator.helper;

import base.operators.operator.Operator;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;

public class TimeSeriesHelperBuilder<T extends Operator>
        extends Object
{
   private T operator;
   private boolean isInputPortOperator;
   private String inputPortName;
   private boolean multivariateInput;
   private boolean includeSpecialAttributes;
   private ExampleSetTimeSeriesHelper.IndiceHandling indiceHandling;
   private boolean isOutputPortOperator;
   private String outputPortName;
   private boolean createPorts;
   private boolean addOverwriteOption;
   private String defaultNewAttributeNameOrPostfix;
   private boolean changeOutputAttributesToReal;
   private boolean isWindowingHelper;
   private boolean mandatoryHorizon;
   private WindowingHelper.HorizonAttributeSelection horizonAttributeSelection;
   private SeriesBuilder.ValuesType valuesType;
   private boolean useISeries;

   public TimeSeriesHelperBuilder(T operator) {
      this.isInputPortOperator = false;
      this.inputPortName = "";

      this.multivariateInput = false;
      this.includeSpecialAttributes = false;

      this.indiceHandling = ExampleSetTimeSeriesHelper.IndiceHandling.NO_INDICES;

      this.isOutputPortOperator = false;
      this.outputPortName = "";

      this.createPorts = true;

      this.addOverwriteOption = false;
      this.defaultNewAttributeNameOrPostfix = "";

      this.changeOutputAttributesToReal = false;

      this.isWindowingHelper = false;

      this.mandatoryHorizon = false;
      this.horizonAttributeSelection = WindowingHelper.HorizonAttributeSelection.SAME;

      this.valuesType = SeriesBuilder.ValuesType.REAL;

      this.useISeries = false;

      this.operator = operator;
   }

   public ExampleSetTimeSeriesHelper<T, ISeries<?, ?>> build() {
      if (this.isWindowingHelper) {
         return new WindowingHelper(this.operator, this.isInputPortOperator, this.inputPortName, this.isOutputPortOperator, this.outputPortName, this.multivariateInput, this.includeSpecialAttributes, this.indiceHandling, this.createPorts, this.addOverwriteOption, this.defaultNewAttributeNameOrPostfix, this.changeOutputAttributesToReal, this.valuesType, this.useISeries, this.mandatoryHorizon, this.horizonAttributeSelection);
      }



      return new ExampleSetTimeSeriesHelper(this.operator, this.isInputPortOperator, this.inputPortName, this.isOutputPortOperator, this.outputPortName, this.multivariateInput, this.includeSpecialAttributes, this.indiceHandling, this.createPorts, this.addOverwriteOption, this.defaultNewAttributeNameOrPostfix, this.changeOutputAttributesToReal, this.valuesType, this.useISeries);
   }

   public TimeSeriesHelperBuilder<T> asInputPortOperator(String portName) {
      this.isInputPortOperator = true;
      this.inputPortName = portName;
      return this;
   }

   public TimeSeriesHelperBuilder<T> enableMultivariateInput() throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("enableMultivariateInput()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      this.multivariateInput = true;
      return this;
   }

   public TimeSeriesHelperBuilder<T> includeSpecialAttributes() throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("includeSpecialAttributes()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      if (!this.multivariateInput) {
         throw new WrongConfiguredHelperException("includeSpecialAttributes()", "not initialized to have multivariate input", "enableMultivariateInput()");
      }

      this.includeSpecialAttributes = true;
      return this;
   }



   public TimeSeriesHelperBuilder<T> setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling handling) throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("setIndiceHandling()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      this.indiceHandling = handling;
      return this;
   }

   public TimeSeriesHelperBuilder<T> setValuesType(SeriesBuilder.ValuesType type) throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("setValuesType()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      this.valuesType = type;
      return this;
   }

   public TimeSeriesHelperBuilder<T> useISeries() throws WrongConfiguredHelperException {
      this.useISeries = true;
      return this;
   }


   public TimeSeriesHelperBuilder<T> asOutputPortOperator(String portName, String defaultNewAttributeNameOrPostfix) {
      this.isOutputPortOperator = true;
      this.outputPortName = portName;
      this.defaultNewAttributeNameOrPostfix = defaultNewAttributeNameOrPostfix;
      return this;
   }



   public TimeSeriesHelperBuilder<T> addOverwriteOption() throws WrongConfiguredHelperException {
      if (!this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("addOverwriteOption()", "not initialized as ouputPortOperator", "asOutputPortOperator()");
      }

      this.addOverwriteOption = true;
      return this;
   }

   public TimeSeriesHelperBuilder<T> changeOutputAttributesToReal() throws WrongConfiguredHelperException {
      if (!this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("changeOutputAttributesToReal()", "not initialized as ouputPortOperator", "asOutputPortOperator()");
      }

      this.changeOutputAttributesToReal = true;
      return this;
   }



   public TimeSeriesHelperBuilder<T> disablePortCreation() throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator && !this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("disablePortCreation()", "not initialized as input- and outputPortOperator", "asInputPortOperator(),asOutputPortOperator()");
      }

      this.createPorts = false;
      return this;
   }



   public TimeSeriesHelperBuilder<T> asWindowingHelper() throws WrongConfiguredHelperException {
      this.isWindowingHelper = true;
      return this;
   }


   public TimeSeriesHelperBuilder<T> withMandatoryHorizon() throws WrongConfiguredHelperException {
      if (!this.isWindowingHelper) {
         throw new WrongConfiguredHelperException("withMandatoryHorizon()", "not initialized as a Windowing Helper", "asWindowingHelper()");
      }

      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("withMandatoryHorizon()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      this.mandatoryHorizon = true;
      return this;
   }


   public TimeSeriesHelperBuilder<T> setHorizonAttributeSelection(WindowingHelper.HorizonAttributeSelection selection) throws WrongConfiguredHelperException {
      if (!this.isWindowingHelper) {
         throw new WrongConfiguredHelperException("setHorizonAttributeSelection()", "not initialized as a Windowing Helper", "asWindowingHelper()");
      }

      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("setHorizonAttributeSelection()", "not initialized as inputPortOperator", "asInputPortOperator()");
      }

      this.horizonAttributeSelection = selection;
      return this;
   }
}
