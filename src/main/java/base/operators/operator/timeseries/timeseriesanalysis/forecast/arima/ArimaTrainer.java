package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima;

import com.github.lbfgs4j.LbfgsMinimizer;
import com.github.lbfgs4j.liblbfgs.Function;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.TimeSeriesForecast;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.TimeSeriesForecastTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ValueSeriesForecast;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ValueSeriesForecastTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArmaLogLikelihood;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.HannanRissanen;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.YuleWalker;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation.AkaikesInformationCriterion;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation.BayesianInformationCriterion;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation.CorrectedAkaikesInformationCriterion;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.Differentiation;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.Pair;

public class ArimaTrainer implements TimeSeriesForecastTrainer, ValueSeriesForecastTrainer {
   private int p;
   private int d;
   private int q;
   private boolean estimateConstant;
   private boolean transformParams;
   private ArimaUtils.TrainingAlgorithm trainingAlgorithm;
   private ArimaUtils.OptimizationMethod optimizationMethod;
   private int maxNumberOfIterations;
   private boolean useRegressionForBOBYQAParameters;
   private double[] parametersForOptimization;
   private double[] initialParameters;
   private boolean calculateStartParameters;
   private double[] finalParameters;
   private double finalLogLikelihood;
   private double finalAicValue;
   private double finalBicValue;
   private double finalCorrectedAicValue;

   private ArimaTrainer(int p, int d, int q, boolean estimateConstant, boolean transformParams, ArimaUtils.TrainingAlgorithm trainingAlgorithm, ArimaUtils.OptimizationMethod optimizationMethod, int maxNumberOfIterations, boolean useRegressionForBOBYQAParameters) {
      if (p < 0) {
         throw new IllegalIndexArgumentException("p", p, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (d < 0) {
         throw new IllegalIndexArgumentException("d", d, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (q < 0) {
         throw new IllegalIndexArgumentException("q", q, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (maxNumberOfIterations <= 0) {
         throw new IllegalIndexArgumentException("max number of iterations", maxNumberOfIterations, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (p + q == 0) {
         throw new IllegalArgumentException("At least one AR or MA term has to provided.");
      } else {
         this.p = p;
         this.d = d;
         this.q = q;
         this.estimateConstant = estimateConstant;
         this.transformParams = transformParams;
         this.trainingAlgorithm = trainingAlgorithm;
         this.maxNumberOfIterations = maxNumberOfIterations;
         this.optimizationMethod = optimizationMethod;
         this.useRegressionForBOBYQAParameters = useRegressionForBOBYQAParameters;
         this.checkAndSetOptimizationParameters(getDefaultOptimizationParameters(optimizationMethod));
         this.calculateStartParameters = true;
         this.initialParameters = null;
      }
   }

   public static ArimaTrainer create(int p, int d, int q) {
      return new ArimaTrainer(p, d, q, true, true, ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD, ArimaUtils.OptimizationMethod.LBFGS, 1000, false);
   }

   public static ArimaTrainer create(int p, int d, int q, boolean estimateConstant, Boolean transformParameters, ArimaUtils.TrainingAlgorithm trainingAlgorithm, ArimaUtils.OptimizationMethod optimizationMethod, int maxNumberOfIterations, boolean useRegressionForBOBYQAParameters) {
      return new ArimaTrainer(p, d, q, estimateConstant, transformParameters, trainingAlgorithm, optimizationMethod, maxNumberOfIterations, useRegressionForBOBYQAParameters);
   }

   public ValueSeriesForecast trainForecast(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return this.trainArima(valueSeries.getValues());
      }
   }

   public TimeSeriesForecast trainForecast(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return this.trainArima(timeSeries.getValues());
      }
   }

   public Arima trainArima(double[] tempValues) {
      this.validateAllowedParameters(tempValues.length);
      if (SeriesUtils.hasNaNValues(tempValues)) {
         throw new SeriesContainsInvalidValuesException("", SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (SeriesUtils.hasInfiniteValues(tempValues)) {
         throw new SeriesContainsInvalidValuesException("", SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         double[] values = Differentiation.diff(tempValues, this.d);
         HannanRissanen hannanRissanenTrainer;
         if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.HANNAN_RISSANEN && this.d == 0) {
            hannanRissanenTrainer = new HannanRissanen(this.p, this.q, this.estimateConstant, values, this.maxNumberOfIterations, 15);
            Arima trainedArima = hannanRissanenTrainer.trainArima();
            this.finalParameters = hannanRissanenTrainer.getParameters(false);
            return trainedArima;
         } else if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.HANNAN_RISSANEN && this.d != 0) {
            throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.HANNAN_RISSANEN_ILLEGAL_D);
         } else {
            if (this.calculateStartParameters) {
               YuleWalker yuleWalker;
               if (this.p == 0 && this.q > 0) {
                  yuleWalker = YuleWalker.create(this.q, values, this.estimateConstant);
                  this.initialParameters = yuleWalker.computeCoefficients();
               } else if (this.p > 0 && this.q == 0) {
                  yuleWalker = YuleWalker.create(this.p, values, this.estimateConstant);
                  this.initialParameters = yuleWalker.computeCoefficients();
               } else {
                  hannanRissanenTrainer = new HannanRissanen(this.p, this.q, this.estimateConstant, values);
                  hannanRissanenTrainer.trainArima();
                  if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.EXACT_MAX_LOGLIKELIHOOD) {
                     this.initialParameters = hannanRissanenTrainer.getParameters(true);
                  } else {
                     this.initialParameters = hannanRissanenTrainer.getParameters(false);
                  }
               }
            }

            double[] startParameters;
            double[] parameters;
            ArmaLogLikelihood conditionalLogLikelihood;
            if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD || this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD) {
               conditionalLogLikelihood = new ArmaLogLikelihood(this.p, this.q, this.estimateConstant, this.transformParams, ArimaUtils.ArimaLogLikelihoodType.CONDITIONAL, values);
               startParameters = this.initialParameters;
               if (this.transformParams) {
                  startParameters = ArimaUtils.inverseTransformParams(startParameters, this.p, this.q, this.estimateConstant);
               }

               parameters = this.performOptimization(values, conditionalLogLikelihood, false, startParameters);
               if (this.transformParams) {
                  parameters = ArimaUtils.transformParams(parameters, this.p, this.q, this.estimateConstant);
               }

               if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD) {
                  conditionalLogLikelihood.setTransParams(false);
                  return this.prepareFinalArima(parameters, conditionalLogLikelihood, values.length);
               }

               if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD) {
                  this.initialParameters = new double[parameters.length + 1];

                  int i;
                  for(i = 0; i < parameters.length; ++i) {
                     this.initialParameters[i] = parameters[i];
                  }

                  this.initialParameters[i] = conditionalLogLikelihood.getSigmaSquare();
               }
            }

            if (this.trainingAlgorithm != ArimaUtils.TrainingAlgorithm.EXACT_MAX_LOGLIKELIHOOD && this.trainingAlgorithm != ArimaUtils.TrainingAlgorithm.CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD) {
               throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.NOT_SUPPORTED_TRAINING_ALGORITHM);
            } else {
               conditionalLogLikelihood = new ArmaLogLikelihood(this.p, this.q, this.estimateConstant, this.transformParams, ArimaUtils.ArimaLogLikelihoodType.EXACT, values);
               startParameters = this.initialParameters;
               if (this.transformParams) {
                  startParameters = ArimaUtils.inverseTransformParams(startParameters, this.p, this.q, this.estimateConstant);
               }

               parameters = this.performOptimization(values, conditionalLogLikelihood, true, startParameters);
               if (this.transformParams) {
                  parameters = ArimaUtils.transformParams(parameters, this.p, this.q, this.estimateConstant);
               }

               conditionalLogLikelihood.setTransParams(false);
               return this.prepareFinalArima(parameters, conditionalLogLikelihood, values.length);
            }
         }
      }
   }

   public Pair validateNumberOfParameters(int length) {
      String message = "";
      int numberOfParameters = this.p + this.d + this.q;
      if (this.estimateConstant) {
         ++numberOfParameters;
      }

      if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.EXACT_MAX_LOGLIKELIHOOD) {
         ++numberOfParameters;
      }

      if (numberOfParameters > length - 3) {
         message = "The number of parameters exceeds the allowed number for this Series. Number of parameters: " + numberOfParameters + ",\t allowed number of parameters (series.length - 3): " + (length - 3);
         return new Pair(false, message);
      } else {
         return new Pair(true, message);
      }
   }

   public Pair validateAllowedParametersForHannanRissanen(int length) {
      String message = "";
      if (this.calculateStartParameters) {
         int maxOrderOfInitialARProcess = (int)Math.round(12.0D * Math.pow((double)((float)length / 100.0F), 0.25D));
         int ystart = Math.max(maxOrderOfInitialARProcess + this.q, this.p);
         if (ystart >= length - this.d) {
            message = "The given parameters p,d,q are not valid for the given series to apply HannanRissanen (HR) to estimate Start Parameters.\nthe condition ystart &lt; (length - d) is not fulfilled:\nlength: " + length + ",\t d: " + this.d + ",\t q: " + this.q + ",\t p: " + this.p + "\n ystart = Math.max(maxOrderOfInitialARProcess + q, p) = " + ystart + "\t, maxOrderOfInitialARProcess of HR =  " + maxOrderOfInitialARProcess;
            return new Pair(false, message);
         }
      }

      return new Pair(true, message);
   }

   public void validateAllowedParameters(int length) {
      Pair validation = this.validateNumberOfParameters(length);
      if (!(Boolean)validation.getFirst()) {
         throw new IllegalArgumentException((String)validation.getSecond());
      } else {
         validation = this.validateAllowedParametersForHannanRissanen(length);
         if (!(Boolean)validation.getFirst()) {
            throw new IllegalArgumentException((String)validation.getSecond());
         }
      }
   }

   private Arima prepareFinalArima(double[] parameters, ArmaLogLikelihood logLikelihood, int n) {
      AkaikesInformationCriterion akaikesInformationCriterion = new AkaikesInformationCriterion();
      BayesianInformationCriterion bayesianInformationCriterion = new BayesianInformationCriterion();
      CorrectedAkaikesInformationCriterion correctedAkaikesInformationCriterion = new CorrectedAkaikesInformationCriterion();
      this.finalParameters = parameters;
      this.finalLogLikelihood = logLikelihood.value(parameters);
      double[] arCoefficients = logLikelihood.getArCoefficients();
      double[] maCoefficients = logLikelihood.getMaCoefficients();
      double constant = 0.0D;
      if (this.estimateConstant) {
         constant = logLikelihood.getConstant();
      }

      double[] allResiduals = logLikelihood.getResiduals();
      double[] lastResiduals = new double[this.q];

      for(int i = 0; i < this.q; ++i) {
         lastResiduals[i] = allResiduals[allResiduals.length - this.q + i];
      }

      this.finalAicValue = akaikesInformationCriterion.compute(this.finalLogLikelihood, logLikelihood.getNumberOfParameters(), n);
      this.finalBicValue = bayesianInformationCriterion.compute(this.finalLogLikelihood, logLikelihood.getNumberOfParameters(), n);
      this.finalCorrectedAicValue = correctedAkaikesInformationCriterion.compute(this.finalLogLikelihood, logLikelihood.getNumberOfParameters(), n);
      return Arima.create(this.p, this.d, this.q, arCoefficients, maCoefficients, constant, lastResiduals);
   }

   private double[] performOptimization(double[] values, MultivariateFunction goalFunction, boolean addSigmaSquare, double[] startParameters) {
      PointValuePair result = null;
      InitialGuess initialGuess = new InitialGuess(startParameters);
      SimpleBounds simpleBounds = this.createSimpleBounds(addSigmaSquare);
      if (this.parametersForOptimization == null) {
         throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.NO_OPTIMIZATION_PARAMETERS_PROVIDED);
      } else {
         switch(this.optimizationMethod) {
         case BOBYQA:
            double factorInterpolationPoints = this.parametersForOptimization[0];
            double initialTrustRegionRadius = this.parametersForOptimization[1];
            double stoppingTrustRegionRadius = this.parametersForOptimization[2];
            if (this.useRegressionForBOBYQAParameters) {
               factorInterpolationPoints = this.calculateParameterForBOBYQAbyRegression(values.length, this.p, this.q, 0);
               initialTrustRegionRadius = this.calculateParameterForBOBYQAbyRegression(values.length, this.p, this.q, 1);
               stoppingTrustRegionRadius = this.calculateParameterForBOBYQAbyRegression(values.length, this.p, this.q, 2);
            }

            BOBYQAOptimizer bobyqaOptimizer = new BOBYQAOptimizer((int)(factorInterpolationPoints * (double)(this.p + this.q + 3)), initialTrustRegionRadius, stoppingTrustRegionRadius);
            result = bobyqaOptimizer.optimize(new OptimizationData[]{new MaxEval(this.maxNumberOfIterations), GoalType.MAXIMIZE, new ObjectiveFunction(goalFunction), initialGuess, simpleBounds});
            return result.getPoint();
         case CMAES:
            double standardSigma = this.parametersForOptimization[0];
            double populationSizeOffset = this.parametersForOptimization[1];
            double populationSizeFactor = this.parametersForOptimization[2];
            double cmaesStopFitness = this.parametersForOptimization[3];
            double cmaesDiagonalOnly = this.parametersForOptimization[4];
            double cmaesCheckFeasableCount = this.parametersForOptimization[5];
            double pointCheckerRelativeThreshold = this.parametersForOptimization[6];
            double pointCheckerAbsolutThreshold = this.parametersForOptimization[7];
            double[] sigmas = new double[this.p + this.q + 1];

            for(int i = 0; i < sigmas.length; ++i) {
               sigmas[i] = standardSigma;
            }

            CMAESOptimizer.Sigma sigma = new CMAESOptimizer.Sigma(sigmas);
            CMAESOptimizer.PopulationSize populationSize = new CMAESOptimizer.PopulationSize(Math.round((float)(populationSizeOffset + populationSizeFactor * Math.log((double)(this.p + this.q) + 1.0D))));
            CMAESOptimizer cmaesOptimizer = new CMAESOptimizer(this.maxNumberOfIterations, cmaesStopFitness, true, (int)cmaesDiagonalOnly, (int)cmaesCheckFeasableCount, new MersenneTwister(), true, new SimplePointChecker(pointCheckerRelativeThreshold, pointCheckerAbsolutThreshold));
            result = cmaesOptimizer.optimize(new MaxEval(this.maxNumberOfIterations), GoalType.MAXIMIZE, new ObjectiveFunction(goalFunction), initialGuess, simpleBounds, sigma, populationSize);
            return result.getPoint();
         case NELDERMEAD:
            double simplexpointCheckerRelativeThreshold = this.parametersForOptimization[0];
            double simplexPointCheckerAbsolutThreshold = this.parametersForOptimization[1];
            NelderMeadSimplex nelderMeadSimplex = new NelderMeadSimplex(this.p + this.q + 1);
            SimplexOptimizer simplexOptimizer = new SimplexOptimizer(new SimplePointChecker(simplexpointCheckerRelativeThreshold, simplexPointCheckerAbsolutThreshold));
            result = simplexOptimizer.optimize(new MaxEval(this.maxNumberOfIterations), GoalType.MAXIMIZE, new ObjectiveFunction(goalFunction), initialGuess, nelderMeadSimplex);
            return result.getPoint();
         case POWELL:
            double powellRelativeThreshold = this.parametersForOptimization[0];
            double powellAbsolutThreshold = this.parametersForOptimization[1];
            MultivariateFunctionMappingAdapter functionAdapter = new MultivariateFunctionMappingAdapter(goalFunction, simpleBounds.getLower(), simpleBounds.getUpper());
            PowellOptimizer powellOptimizer = new PowellOptimizer(powellRelativeThreshold, powellAbsolutThreshold);
            result = powellOptimizer.optimize(new OptimizationData[]{new MaxEval(this.maxNumberOfIterations), GoalType.MAXIMIZE, new ObjectiveFunction(functionAdapter), initialGuess});
            return functionAdapter.unboundedToBounded(result.getPoint());
         case LBFGS:
            LbfgsMinimizer minimizer = new LbfgsMinimizer(false);
            return minimizer.minimize((Function)goalFunction, startParameters);
         default:
            throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.NOT_SUPPORTED_OPTIMIZATION_METHOD);
         }
      }
   }

   private SimpleBounds createSimpleBounds(boolean addSigmaSquare) {
      int numberOfParameters = this.p + this.q;
      if (this.estimateConstant) {
         ++numberOfParameters;
      }

      if (addSigmaSquare) {
         ++numberOfParameters;
      }

      double[][] bounds = new double[2][numberOfParameters];

      int i;
      for(i = 0; i < this.p + this.q; ++i) {
         bounds[0][i] = -10.0D + Math.ulp(-1.0D);
         bounds[1][i] = 10.0D - Math.ulp(1.0D);
      }

      if (this.estimateConstant) {
         bounds[0][i] = Double.NEGATIVE_INFINITY;
         bounds[1][i] = Double.POSITIVE_INFINITY;
         ++i;
      }

      if (addSigmaSquare) {
         bounds[0][i] = 0.0D + Math.ulp(0.0D);
         bounds[1][i] = Double.POSITIVE_INFINITY;
      }

      return new SimpleBounds(bounds[0], bounds[1]);
   }

   private double calculateParameterForBOBYQAbyRegression(int n, int p, int q, int i) {
      double[][] regressionCoefficients = new double[3][4];
      regressionCoefficients[0] = new double[]{1.2360566632877907D, 9.228543571328063E-7D, 0.015222052447694447D, 0.01020758884697153D};
      regressionCoefficients[1] = new double[]{0.22780848175157706D, -7.553414912441716E-6D, -0.013150511834201704D, -0.014741139928110577D};
      regressionCoefficients[2] = new double[]{0.0880210759444603D, -8.51594906210044E-6D, -0.007564360060476942D, -0.0022898447216298965D};
      return regressionCoefficients[i][0] + regressionCoefficients[i][1] * (double)n + regressionCoefficients[i][2] * (double)p + regressionCoefficients[i][3] * (double)q;
   }

   private static double[] getDefaultOptimizationParameters(ArimaUtils.OptimizationMethod optimizationMethod) {
      double[] optimizationParameters = null;
      switch(optimizationMethod) {
      case BOBYQA:
         optimizationParameters = new double[]{1.5D, 0.01D, 1.0E-4D};
         break;
      case CMAES:
         optimizationParameters = new double[]{0.1D, 500.0D, 3.0D, -1000.0D, 1000.0D, 0.0D, 0.01D, -1.0D};
         break;
      case NELDERMEAD:
         optimizationParameters = new double[]{1.0E-4D, -1.0D};
         break;
      case POWELL:
         optimizationParameters = new double[]{1.0E-8D, 1.0E-9D};
         break;
      case LBFGS:
         optimizationParameters = new double[0];
      }

      return optimizationParameters;
   }

   public double[] getFinalParameters() {
      return this.finalParameters;
   }

   public double getFinalLogLikelihood() {
      return this.finalLogLikelihood;
   }

   public double getFinalAicValue() {
      return this.finalAicValue;
   }

   public double getFinalBicValue() {
      return this.finalBicValue;
   }

   public double getFinalCorrectedAicValue() {
      return this.finalCorrectedAicValue;
   }

   public void setOptimizationParameters(double[] optimizationParameters) {
      if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.HANNAN_RISSANEN) {
         throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.HANNAN_RISSANEN_OPTIMIZATION_PARAM);
      } else {
         this.checkAndSetOptimizationParameters(optimizationParameters);
      }
   }

   private void checkAndSetOptimizationParameters(double[] optimizationParameters) {
      if (optimizationParameters == null) {
         throw new ArgumentIsNullException("optimization parameters array");
      } else {
         int necessaryLengthOfParametersArray = 0;
         switch(this.optimizationMethod) {
         case BOBYQA:
            necessaryLengthOfParametersArray = 3;
            break;
         case CMAES:
            necessaryLengthOfParametersArray = 8;
            break;
         case NELDERMEAD:
            necessaryLengthOfParametersArray = 2;
            break;
         case POWELL:
            necessaryLengthOfParametersArray = 2;
            break;
         case LBFGS:
            necessaryLengthOfParametersArray = 0;
         }

         if (optimizationParameters.length != necessaryLengthOfParametersArray) {
            throw new IllegalArgumentException("Length of provided optimization parameters array (" + optimizationParameters.length + ") is not equal to the necessary length (" + necessaryLengthOfParametersArray + ") for the used optimization method (" + this.optimizationMethod.toString() + ").");
         } else {
            this.parametersForOptimization = optimizationParameters;
         }
      }
   }

   public void setInitialParameters(double[] initialParameters) {
      if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.HANNAN_RISSANEN) {
         throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.HANNAN_RISSANEN_INITIAL_PARAM);
      } else {
         int necessaryLength = this.p + this.q;
         String necessaryParameters = "[ p arCoefficients, q maCoefficients";
         if (this.estimateConstant) {
            ++necessaryLength;
            necessaryParameters = necessaryParameters + ", constant";
         }

         if (this.trainingAlgorithm == ArimaUtils.TrainingAlgorithm.EXACT_MAX_LOGLIKELIHOOD) {
            ++necessaryLength;
            necessaryParameters = necessaryParameters + ", sigmaSquare";
         }

         if (initialParameters.length != necessaryLength) {
            throw new IllegalArgumentException("Length of provided initialParametersArray (" + initialParameters.length + ") is not equal to the necessary length (" + necessaryLength + "). Please provide the following parameters as initial Parameters: " + necessaryParameters + " ].");
         } else {
            this.initialParameters = initialParameters;
            this.calculateStartParameters = false;
         }
      }
   }
}
