package base.operators.operator.timeseries.operator.validation;

import base.operators.Process;
import base.operators.example.*;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.*;
import base.operators.operator.concurrency.execution.BackgroundExecutionService;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperatorChain;
import base.operators.operator.timeseries.operator.forecast.AbstractForecastModel;
import base.operators.operator.timeseries.operator.forecast.ApplyForecastOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WindowingHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.UndefinedParameterError;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionService;
import base.operators.studio.concurrency.internal.util.ExampleSetAppender;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import base.operators.tools.OperatorService;
import base.operators.tools.RandomGenerator;
import base.operators.tools.container.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;


public class ForecastValidationOperator
        extends ExampleSetTimeSeriesOperatorChain
{
   private WindowingHelper<OperatorChain, ISeries<?, ?>> windowingHelper;

   private static class ForecastValidationResult
   {
      private Pair<List<PerformanceVector>, ExampleSet> partialResult;
      private IOObject modelResult;
      private boolean lastModel;

      public ForecastValidationResult(Pair<List<PerformanceVector>, ExampleSet> partialResult) {
         this.partialResult = partialResult;
         this.lastModel = false;
      }

      public ForecastValidationResult(Pair<List<PerformanceVector>, ExampleSet> partialResult, IOObject lastModel) {
         this.partialResult = partialResult;
         this.modelResult = lastModel;
         this.lastModel = true;
      }


      public IOObject getModelResult() { return this.modelResult; }



      public Pair<List<PerformanceVector>, ExampleSet> getPartialResult() { return this.partialResult; }



      public boolean isLastModel() { return this.lastModel; }
   }

   private final OutputPort testSetInnerOutput = (OutputPort)getSubprocess(1).getInnerSources().createPort("test set");


   private final InputPort modelInnerInput = getSubprocess(0).getInnerSinks().createPort("model", base.operators.operator.timeseries.operator.forecast.arima.ArimaModel.class);
   private final OutputPort modelOutput = (OutputPort)getOutputPorts().createPort("model");

   private final OutputPort trainingSetInnerOutput = (OutputPort)getSubprocess(0).getInnerSources().createPort("training set");
   private final InputPort testResultSetInnerInput = (InputPort)getSubprocess(1).getInnerSinks().createPort("test set results");
   private final OutputPort exampleSetOutput = (OutputPort)getOutputPorts().createPort("example set");
   private final OutputPort testResultSetOutput = (OutputPort)getOutputPorts().createPort("test result set");
   private final PortPairExtender performanceOutputPortExtender = new PortPairExtender("performance",
           getSubprocess(1).getInnerSinks(), getOutputPorts(), new MetaData(PerformanceVector.class));


   private final PortPairExtender throughExtender = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
           getSubprocess(1).getInnerSources());

   public ForecastValidationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description, new String[] { "Training", "Testing" });
      this.throughExtender.start();
      this.performanceOutputPortExtender.start();

      InputPort inputPort = ((PortPairExtender.PortPair)this.performanceOutputPortExtender.getManagedPairs().get(0)).getInputPort();
      inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(PerformanceVector.class)));


      this.windowingHelper.addWindowMetaDataRule(this.trainingSetInnerOutput);
      this.windowingHelper.addWindowParameterPreconditions();


      getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));


      addTestSetRule(this.testSetInnerOutput);


      getTransformer().addRule(this.throughExtender.makePassThroughRule());


      getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));


      getTransformer().addPassThroughRule(this.modelInnerInput, this.modelOutput);
      getTransformer().addRule(this.performanceOutputPortExtender.makePassThroughRule());
      getTransformer().addPassThroughRule(this.windowingHelper.getExampleSetInputPort(), this.exampleSetOutput);
      getTransformer().addPassThroughRule(this.testResultSetInnerInput, this.testResultSetOutput);
      this.testResultSetInnerInput.addPrecondition(new SimplePrecondition(this.testResultSetInnerInput, new ExampleSetMetaData())
      {
         protected boolean isMandatory()
         {
            return false;
         }
      });
   }



   protected ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder<OperatorChain> builder = new TimeSeriesHelperBuilder<OperatorChain>(this);

      builder = builder.asInputPortOperator("example set").setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).asWindowingHelper().withMandatoryHorizon().setHorizonAttributeSelection(WindowingHelper.HorizonAttributeSelection.SAME);
      this.windowingHelper = (WindowingHelper)builder.build();
      return this.windowingHelper;
   }

   private void addTestSetRule(final OutputPort outputPort) {
      getTransformer().addGenerationRule(outputPort, ExampleSet.class);
      getTransformer().addRule(new MDTransformationRule()
      {
         public void transformMD()
         {
            ExampleSetMetaData md = new ExampleSetMetaData();

            try {
               if (ForecastValidationOperator.this.windowingHelper.getExampleSetInputPort().isConnected()) {
                  ExampleSetMetaData inputMD = (ExampleSetMetaData)ForecastValidationOperator.this.windowingHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                  String horizonSeriesAttribute = ForecastValidationOperator.this.getParameterAsString("time_series_attribute");

                  md.addAttribute(new AttributeMetaData(horizonSeriesAttribute, 2, "label"));

                  md.addAttribute(new AttributeMetaData("forecast of " + horizonSeriesAttribute, 2, "prediction"));

                  md.addAttribute(new AttributeMetaData("forecast position", 3));
                  if (inputMD != null) {
                     md.addAllAttributes(ForecastValidationOperator.this.windowingHelper.createLastIndexInWindowAttributeMetaData(inputMD));
                  }
                  md.setNumberOfExamples(ForecastValidationOperator.this.getParameterAsInt("horizon_size"));
               }
            } catch (UndefinedParameterError|base.operators.operator.ports.IncompatibleMDClassException e) {

               e.printStackTrace();
            }
            outputPort.deliverMD(md);
         }
      });
   }


   public void doWork() throws OperatorException {
      this.windowingHelper.resetHelper();
      this.windowingHelper.checkWindowParameterSettings();
      List<ArrayIndicesWindow<?>> windows = this.windowingHelper.createWindowsFromInput();
      this.exampleSetOutput.deliver(this.windowingHelper.getInputExampleSet().copy());

      boolean executeParallely = checkParallelizability();


      int numberOfFolds = this.windowingHelper.getNumberOfTrainingWindows();

      boolean deliverTestSet = (this.testResultSetOutput.isConnected() && this.testResultSetInnerInput.isConnected());

      getProgress().setCheckForStop(false);
      getProgress().setTotal(numberOfFolds);


      List<Pair<List<PerformanceVector>, ExampleSet>> results = null;
      if (executeParallely) {
         results = performParallelValidation(numberOfFolds, windows);
      } else {
         results = performSynchronizedValidation(numberOfFolds, windows);
      }


      Pair<List<PerformanceVector>, ExampleSet> firstResult = (Pair)results.remove(numberOfFolds - 1);
      List<ExampleSet> resultSets = new LinkedList<ExampleSet>();
      if (firstResult.getSecond() != null) {
         resultSets.add(firstResult.getSecond());
      }
      List<PerformanceVector> vectors = (List)firstResult.getFirst();
      for (Pair<List<PerformanceVector>, ExampleSet> otherResult : results) {
         for (int i = 0; i < vectors.size(); i++) {
            PerformanceVector vector = (PerformanceVector)vectors.get(i);
            PerformanceVector otherVector = (PerformanceVector)((List)otherResult.getFirst()).get(i);
            vector.buildAverages(otherVector);
            if (otherResult.getSecond() != null) {
               resultSets.add(otherResult.getSecond());
            }
         }
      }


      int i = 0;
      for (PortPairExtender.PortPair pair : this.performanceOutputPortExtender.getManagedPairs()) {
         if (pair.getOutputPort().isConnected() && vectors.size() > i) {
            pair.getOutputPort().deliver((IOObject)vectors.get(i++));
         }
      }


      if (deliverTestSet) {
         this.testResultSetOutput.deliver(ExampleSetAppender.merge(this, resultSets));
      }
      getProgress().complete();
   }

   private List<Pair<List<PerformanceVector>, ExampleSet>> performSynchronizedValidation(int numberOfValidations, List<ArrayIndicesWindow<?>> windows) throws OperatorException {
      List<Pair<List<PerformanceVector>, ExampleSet>> results = new ArrayList<Pair<List<PerformanceVector>, ExampleSet>>(numberOfValidations);


      Process process = getProcess();
      RandomGenerator base = RandomGenerator.stash(process);
      boolean computeFullModel = this.modelOutput.isConnected();
      IOObject trainResults = null;
      for (int iteration = 0; iteration < numberOfValidations; iteration++) {

         RandomGenerator.init(process, Long.valueOf(base.nextLongInRange(1L, 2147483648L)));

         ArrayIndicesWindow trainingWindow = (ArrayIndicesWindow)windows.get(2 * iteration);
         Series windowedTrainingSeries = trainingWindow.getWindowedSeries(this.windowingHelper.getInputSeries());
         trainResults = train(windowedTrainingSeries);


         Series windowedTestSeries = ((ArrayIndicesWindow)windows.get(2 * iteration + 1)).getWindowedSeries(this.windowingHelper.getInputSeries());
         results.add(test(trainingWindow, windowedTestSeries, trainResults));
         getProgress().step();
      }

      RandomGenerator.restore(process);


      if (computeFullModel)
      {
         this.modelOutput.deliver(trainResults);
      }
      return results;
   }








   private List<Pair<List<PerformanceVector>, ExampleSet>> performParallelValidation(final int numberOfValidations, final List<ArrayIndicesWindow<?>> windows) throws OperatorException {
      final boolean computeFullModel = this.modelOutput.isConnected();
      List<ForecastValidationResult> resultSet = new ArrayList<ForecastValidationResult>(numberOfValidations + 1);


      int batchSize = ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize();
      List<Callable<ForecastValidationResult>> taskSet = new ArrayList<Callable<ForecastValidationResult>>(batchSize + 1);

      //手动设置service，不确定
      BackgroundExecutionService service = new BackgroundExecutionService();
      for (int iteration = 0; iteration < numberOfValidations; ) {

         batchSize = Math.min(numberOfValidations - iteration,
                 ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize());
         for (int j = 1; j <= batchSize; iteration++, j++) {
            final int currentIteration = iteration;


            final ForecastValidationOperator copy = (ForecastValidationOperator)ConcurrencyTools.clone(this);
            copy.setWindowingHelper(this.windowingHelper.clone(copy));



            Callable<ForecastValidationResult> singleTask = service.prepareOperatorTask(getProcess(), copy, iteration + 1, (iteration + 1 == numberOfValidations), new Callable<ForecastValidationResult>()
            {


               public ForecastValidationOperator.ForecastValidationResult call() throws Exception
               {
                  ArrayIndicesWindow trainingWindow = (ArrayIndicesWindow)windows.get(2 * currentIteration);

                  Series windowedTrainingSeries = trainingWindow.getWindowedSeries(ForecastValidationOperator.this.windowingHelper.getInputSeries());
                  IOObject trainResults = copy.train(windowedTrainingSeries);



                  Series windowedTestSeries = ((ArrayIndicesWindow)windows.get(2 * currentIteration + 1)).getWindowedSeries(ForecastValidationOperator.this.windowingHelper.getInputSeries());
                  Pair<List<PerformanceVector>, ExampleSet> result = copy.test(trainingWindow, windowedTestSeries, trainResults);


                  ForecastValidationOperator.this.getProgress().step();




                  if (currentIteration + 1 == numberOfValidations && computeFullModel) {
                     return new ForecastValidationOperator.ForecastValidationResult(result, trainResults);
                  }
                  return new ForecastValidationOperator.ForecastValidationResult(result);
               }
            });

            taskSet.add(singleTask);
         }
         resultSet.addAll(service.executeOperatorTasks(this, taskSet));
         taskSet.clear();
      }


      IOObject fullResult = null;
      List<Pair<List<PerformanceVector>, ExampleSet>> result = new ArrayList<Pair<List<PerformanceVector>, ExampleSet>>(numberOfValidations);
      for (ForecastValidationResult singleResult : resultSet) {
         result.add(singleResult.getPartialResult());
         if (singleResult.isLastModel()) {
            fullResult = singleResult.getModelResult();
         }
      }

      if (computeFullModel) {
         this.modelOutput.deliver(fullResult);
      }
      return result;
   }






   private IOObject train(Series windowedTrainingSeries) throws OperatorException {
      this.trainingSetInnerOutput.deliver(this.windowingHelper.convertSeriesToExampleSet(windowedTrainingSeries));
      getSubprocess(0).execute();
      return this.modelInnerInput.getData(IOObject.class);
   }








   private Pair<List<PerformanceVector>, ExampleSet> test(ArrayIndicesWindow trainingWindow, Series windowedTestSeries, IOObject model) throws OperatorException {
      ExampleSet testExampleSet = null;
      AbstractForecastModel forecastModel = (AbstractForecastModel)model;
      try {
         ApplyForecastOperator applyForecastOperator = (ApplyForecastOperator)OperatorService.createOperator(ApplyForecastOperator.class);
         applyForecastOperator.setParameter("add_original_time_series", "false");
         applyForecastOperator.setParameter("add_combined_time_series", "false");
         applyForecastOperator.setParameter("forecast_horizon",
                 String.valueOf(this.windowingHelper.getHorizonWidth()));
         testExampleSet = applyForecastOperator.performForecast(forecastModel);
      } catch (OperatorCreationException e) {

         e.printStackTrace();
      }
      Attribute labelAttribute = AttributeFactory.createAttribute(windowedTestSeries.getName(), 2);
      Attributes attributes = testExampleSet.getAttributes();
      labelAttribute.setTableIndex(attributes.size());
      testExampleSet.getExampleTable().addAttribute(labelAttribute);
      AttributeRole role = new AttributeRole(labelAttribute);
      role.setSpecial("label");
      attributes.add(role);
      Attribute forecastPositionAttribute = AttributeFactory.createAttribute("forecast position", 3);
      forecastPositionAttribute.setTableIndex(attributes.size());
      testExampleSet.getExampleTable().addAttribute(forecastPositionAttribute);
      attributes.addRegular(forecastPositionAttribute);
      Attribute indexAttribute = this.windowingHelper.createAndAddLastIndexAttribute(testExampleSet);
      double indexValue = 0.0D;
      if (indexAttribute != null) {
         indexValue = this.windowingHelper.getLastIndexValue(trainingWindow);
      }
      double[] values = windowedTestSeries.getValues();
      int i = 0;
      for (Example example : testExampleSet) {
         example.setValue(labelAttribute, values[i]);
         example.setValue(forecastPositionAttribute, (i + 1));
         if (indexAttribute != null) {
            example.setValue(indexAttribute, indexValue);
         }
         i++;
      }

      this.testSetInnerOutput.deliver(testExampleSet);
      this.throughExtender.passDataThrough();
      getSubprocess(1).execute();



      List<PerformanceVector> perfVectors = new ArrayList<PerformanceVector>(this.performanceOutputPortExtender.getManagedPairs().size());
      for (PortPairExtender.PortPair pair : this.performanceOutputPortExtender.getManagedPairs()) {
         if (pair.getInputPort().isConnected()) {
            perfVectors.add(pair.getInputPort().getData(PerformanceVector.class));
         }
      }
      return new Pair(perfVectors, this.testResultSetInnerInput.getDataOrNull(ExampleSet.class));
   }


   public List<ParameterType> getParameterTypes() {
      List<ParameterType> types = this.windowingHelper.getParameterTypes(new ArrayList());
      types.addAll(super.getParameterTypes());
      return types;
   }






   public void setWindowingHelper(WindowingHelper<OperatorChain, ISeries<?, ?>> windowingHelper) { this.windowingHelper = windowingHelper; }
}
