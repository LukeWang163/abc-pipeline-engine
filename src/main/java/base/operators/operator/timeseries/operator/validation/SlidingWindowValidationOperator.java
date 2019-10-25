package base.operators.operator.timeseries.operator.validation;

import base.operators.Process;
import base.operators.example.ExampleSet;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperatorChain;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WindowingHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.IOObject;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.PortPairExtender.PortPair;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionService;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionServiceProvider;
import base.operators.studio.concurrency.internal.util.ExampleSetAppender;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import base.operators.tools.RandomGenerator;
import base.operators.tools.container.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class SlidingWindowValidationOperator extends ExampleSetTimeSeriesOperatorChain {
   private WindowingHelper windowingHelper;
   private final OutputPort testSetInnerOutput = (OutputPort)this.getSubprocess(1).getInnerSources().createPort("test set");
   private final InputPort modelInnerInput = this.getSubprocess(0).getInnerSinks().createPort("model", Model.class);
   private final OutputPort modelInnerOutput = (OutputPort)this.getSubprocess(1).getInnerSources().createPort("model");
   private final OutputPort modelOutput = (OutputPort)this.getOutputPorts().createPort("model");
   private final PortPairExtender performanceOutputPortExtender = new PortPairExtender("performance", this.getSubprocess(1).getInnerSinks(), this.getOutputPorts(), new MetaData(PerformanceVector.class));
   private final OutputPort trainingSetInnerOutput = (OutputPort)this.getSubprocess(0).getInnerSources().createPort("training set");
   private final InputPort testResultSetInnerInput = (InputPort)this.getSubprocess(1).getInnerSinks().createPort("test set results");
   private final OutputPort exampleSetOutput = (OutputPort)this.getOutputPorts().createPort("example set");
   private final OutputPort testResultSetOutput = (OutputPort)this.getOutputPorts().createPort("test result set");
   private final PortPairExtender throughExtender = new PortPairExtender("through", this.getSubprocess(0).getInnerSinks(), this.getSubprocess(1).getInnerSources());

   public SlidingWindowValidationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description, "Training", "Testing");
      this.throughExtender.start();
      this.performanceOutputPortExtender.start();
      InputPort inputPort = ((PortPair)this.performanceOutputPortExtender.getManagedPairs().get(0)).getInputPort();
      inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(PerformanceVector.class)));
      this.windowingHelper.addWindowMetaDataRule(this.trainingSetInnerOutput);
      this.windowingHelper.addWindowMetaDataRule(this.testSetInnerOutput);
      this.getTransformer().addRule(new SubprocessTransformRule(this.getSubprocess(0)));
      this.getTransformer().addPassThroughRule(this.modelInnerInput, this.modelInnerOutput);
      this.getTransformer().addRule(this.throughExtender.makePassThroughRule());
      this.getTransformer().addRule(new SubprocessTransformRule(this.getSubprocess(1)));
      this.getTransformer().addPassThroughRule(this.modelInnerInput, this.modelOutput);
      this.getTransformer().addRule(this.performanceOutputPortExtender.makePassThroughRule());
      this.getTransformer().addPassThroughRule(this.windowingHelper.getExampleSetInputPort(), this.exampleSetOutput);
      this.getTransformer().addPassThroughRule(this.testResultSetInnerInput, this.testResultSetOutput);
      this.testResultSetInnerInput.addPrecondition(new SimplePrecondition(this.testResultSetInnerInput, new ExampleSetMetaData()) {
         protected boolean isMandatory() {
            return false;
         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      builder = builder.asInputPortOperator("example set").setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).enableMultivariateInput().asWindowingHelper().withMandatoryHorizon().setHorizonAttributeSelection(WindowingHelper.HorizonAttributeSelection.SAME);
      this.windowingHelper = (WindowingHelper)builder.build();
      return this.windowingHelper;
   }

   public void doWork() throws OperatorException {
      this.windowingHelper.resetHelper();
      List windows = this.windowingHelper.createWindowsFromInput();
      boolean executeParallely = this.checkParallelizability();
      int numberOfFolds = this.windowingHelper.getNumberOfTrainingWindows();
      boolean deliverTestSet = this.testResultSetOutput.isConnected() && this.testResultSetInnerInput.isConnected();
      List results = null;
      if (executeParallely) {
         results = this.performParallelValidation(numberOfFolds, windows);
      } else {
         results = this.performSynchronizedValidation(numberOfFolds, windows);
      }

      Pair firstResult = (Pair)results.remove(numberOfFolds - 1);
      List resultSets = new LinkedList();
      if (firstResult.getSecond() != null) {
         resultSets.add(firstResult.getSecond());
      }

      List vectors = (List)firstResult.getFirst();
      Iterator var9 = results.iterator();

      while(var9.hasNext()) {
         Pair otherResult = (Pair)var9.next();

         for(int i = 0; i < vectors.size(); ++i) {
            PerformanceVector vector = (PerformanceVector)vectors.get(i);
            PerformanceVector otherVector = (PerformanceVector)((List)otherResult.getFirst()).get(i);
            vector.buildAverages(otherVector);
            if (otherResult.getSecond() != null) {
               resultSets.add(otherResult.getSecond());
            }
         }
      }

      int i = 0;
      Iterator var15 = this.performanceOutputPortExtender.getManagedPairs().iterator();

      while(var15.hasNext()) {
         PortPair pair = (PortPair)var15.next();
         if (pair.getOutputPort().isConnected() && vectors.size() > i) {
            pair.getOutputPort().deliver((IOObject)vectors.get(i++));
         }
      }

      if (deliverTestSet) {
         this.testResultSetOutput.deliver(ExampleSetAppender.merge(this, resultSets));
      }

      this.getProgress().complete();
      this.exampleSetOutput.deliver(this.windowingHelper.getInputExampleSet());
   }

   private List performSynchronizedValidation(int numberOfValidations, List windows) throws OperatorException {
      List results = new ArrayList(numberOfValidations);
      Process process = this.getProcess();
      RandomGenerator base = RandomGenerator.stash(process);
      boolean computeFullModel = this.modelOutput.isConnected();
      IOObject trainResults = null;

      for(int iteration = 0; iteration < numberOfValidations; ++iteration) {
         RandomGenerator.init(process, base.nextLongInRange(1L, 2147483648L));
         ArrayIndicesWindow trainingWindow = (ArrayIndicesWindow)windows.get(2 * iteration);
         MultivariateSeries windowedTrainingSeries = trainingWindow.getWindowedSeries(this.windowingHelper.getInputMultivariateSeries());
         trainResults = this.train(windowedTrainingSeries);
         MultivariateSeries windowedTestSeries = ((ArrayIndicesWindow)windows.get(2 * iteration + 1)).getWindowedSeries(this.windowingHelper.getInputMultivariateSeries());
         results.add(this.test(windowedTestSeries, trainResults));
      }

      RandomGenerator.restore(process);
      if (computeFullModel) {
         this.modelOutput.deliver(trainResults);
      }

      return results;
   }

   private List<Pair<List<PerformanceVector>, ExampleSet>> performParallelValidation(final int numberOfValidations, final List<ArrayIndicesWindow> windows) throws OperatorException {
      final boolean computeFullModel = this.modelOutput.isConnected();
      List<SlidingWindowValidationResult> resultSet = new ArrayList<SlidingWindowValidationResult>(numberOfValidations + 1);


      int batchSize = ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize();
      List<Callable<SlidingWindowValidationResult>> taskSet = new ArrayList<Callable<SlidingWindowValidationResult>>(batchSize + 1);


      for (int iteration = 0; iteration < numberOfValidations; ) {

         batchSize = Math.min(numberOfValidations - iteration,
                 ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize());
         for (int j = 1; j <= batchSize; iteration++, j++) {
            final int currentIteration = iteration;


            final SlidingWindowValidationOperator copy = (SlidingWindowValidationOperator)ConcurrencyTools.clone(this);
            copy.setWindowingHelper(this.windowingHelper.clone(copy));



            Callable<SlidingWindowValidationResult> singleTask = ConcurrencyExecutionServiceProvider.INSTANCE.getService().prepareOperatorTask(getProcess(), copy, iteration + 1, (iteration + 1 == numberOfValidations), new Callable<SlidingWindowValidationResult>()
            {


               public SlidingWindowValidationOperator.SlidingWindowValidationResult call() throws Exception
               {
                  ArrayIndicesWindow trainingWindow = (ArrayIndicesWindow)windows.get(2 * currentIteration);

                  MultivariateSeries windowedTrainingSeries = trainingWindow.getWindowedSeries(SlidingWindowValidationOperator.this.windowingHelper.getInputMultivariateSeries());
                  IOObject trainResults = copy.train(windowedTrainingSeries);



                  MultivariateSeries windowedTestSeries = ((ArrayIndicesWindow)windows.get(2 * currentIteration + 1)).getWindowedSeries(SlidingWindowValidationOperator.this.windowingHelper.getInputMultivariateSeries());
                  Pair<List<PerformanceVector>, ExampleSet> result = copy.test(windowedTestSeries, trainResults);





                  if (currentIteration + 1 == numberOfValidations && computeFullModel) {
                     return new SlidingWindowValidationOperator.SlidingWindowValidationResult(result, trainResults);
                  }
                  return new SlidingWindowValidationOperator.SlidingWindowValidationResult(result);
               }
            });

            taskSet.add(singleTask);
         }
         resultSet.addAll(ConcurrencyExecutionServiceProvider.INSTANCE.getService().executeOperatorTasks(this, taskSet));
         taskSet.clear();
      }


      IOObject fullResult = null;
      List<Pair<List<PerformanceVector>, ExampleSet>> result = new ArrayList<Pair<List<PerformanceVector>, ExampleSet>>(numberOfValidations);
      for (SlidingWindowValidationResult singleResult : resultSet) {
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

   private IOObject train(MultivariateSeries windowedTrainingSeries) throws OperatorException {
      this.trainingSetInnerOutput.deliver(this.windowingHelper.convertMultivariateSeriesToExampleSet(windowedTrainingSeries));
      this.getSubprocess(0).execute();
      return this.modelInnerInput.getData(IOObject.class);
   }

   private Pair test(MultivariateSeries windowedTestSeries, IOObject model) throws OperatorException {
      this.testSetInnerOutput.deliver(this.windowingHelper.convertMultivariateSeriesToExampleSet(windowedTestSeries));
      this.throughExtender.passDataThrough();
      this.modelInnerOutput.deliver(model);
      this.getSubprocess(1).execute();
      List perfVectors = new ArrayList(this.performanceOutputPortExtender.getManagedPairs().size());
      Iterator var4 = this.performanceOutputPortExtender.getManagedPairs().iterator();

      while(var4.hasNext()) {
         PortPair pair = (PortPair)var4.next();
         if (pair.getInputPort().isConnected()) {
            perfVectors.add(pair.getInputPort().getData(PerformanceVector.class));
         }
      }

      return new Pair(perfVectors, this.testResultSetInnerInput.getDataOrNull(ExampleSet.class));
   }

   public List getParameterTypes() {
      List types = this.windowingHelper.getParameterTypes(new ArrayList());
      types.addAll(super.getParameterTypes());
      return types;
   }

   public void setWindowingHelper(WindowingHelper windowingHelper) {
      this.windowingHelper = windowingHelper;
   }

   private static class SlidingWindowValidationResult {
      private Pair partialResult;
      private IOObject modelResult;
      private boolean lastModel;

      public SlidingWindowValidationResult(Pair partialResult) {
         this.partialResult = partialResult;
         this.lastModel = false;
      }

      public SlidingWindowValidationResult(Pair partialResult, IOObject lastModel) {
         this.partialResult = partialResult;
         this.modelResult = lastModel;
         this.lastModel = true;
      }

      public IOObject getModelResult() {
         return this.modelResult;
      }

      public Pair getPartialResult() {
         return this.partialResult;
      }

      public boolean isLastModel() {
         return this.lastModel;
      }
   }
}
