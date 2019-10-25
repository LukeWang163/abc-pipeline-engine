package base.operators.operator.timeseries.operator.decomposition;

import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition.ClassicDecomposition;
import base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition.MultivariateSeriesDecomposition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassicDecompositionOperator extends AbstractDecompositionOperator {
   public static final String PARAMETER_DECOMPOSITION_MODE = "decomposition_mode";

   public ClassicDecompositionOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeInt("seasonality", "This parameter defines the seasonality for the decomposition.", 1, Integer.MAX_VALUE, 12, false));
      return types;
   }

   protected MultivariateSeriesDecomposition createDecomposition(int seasonality) throws UndefinedParameterError {
      return ClassicDecomposition.create(ClassicDecomposition.DecompositionMode.valueOf(this.getParameterAsString("decomposition_mode").toUpperCase()), seasonality);
   }

   protected List parameterTypesBeforeSeasonality() {
      List types = new ArrayList();
      types.add(new ParameterTypeCategory("decomposition_mode", "With this parameter the used decomposition mode can be selected.", (String[])Arrays.stream(ClassicDecomposition.DecompositionMode.class.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      return types;
   }

   protected List parameterTypesAfterSeasonality() {
      return new ArrayList();
   }
}
