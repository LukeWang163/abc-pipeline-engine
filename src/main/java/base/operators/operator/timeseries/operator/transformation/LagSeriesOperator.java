package base.operators.operator.timeseries.operator.transformation;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.*;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.ports.quickfix.AttributeSelectionQuickFix;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.parameter.*;

import java.util.*;
import java.util.Map.Entry;

public class LagSeriesOperator extends AbstractExampleSetProcessing {
   public static final String PARAMETER_ATTRIBUTES = "attributes";
   public static final String PARAMETER_LAG = "lag";
   public static final String PARAMETER_OVERWRITE_ATTRIBUTES = "overwrite_attributes";
   public static final String PARAMETER_EXTEND_EXAMPLESET = "extend_exampleset";

   public LagSeriesOperator(OperatorDescription description) {
      super(description);
      this.getExampleSetInputPort().addPrecondition(new ExampleSetPrecondition(this.getExampleSetInputPort()) {
         public void makeAdditionalChecks(ExampleSetMetaData emd) {
            try {
               final List<String[]> list = LagSeriesOperator.this.getParameterList("attributes");
               List<String> selectedAttributes = new ArrayList<String>();
               boolean overwriteAttributes = LagSeriesOperator.this.getParameterAsBoolean("overwrite_attributes");
               for (String[] pair : list) {

                  MetaDataInfo attInfo = emd.containsAttributeName(pair[0]);
                  if (attInfo == MetaDataInfo.NO) {
                     createError(ProcessSetupError.Severity.ERROR, Collections.singletonList(new AttributeSelectionQuickFix(emd, "attributes", LagSeriesOperator.this, pair[0])
                     {

                        public void insertChosenOption(String chosenOption)
                        {
                           pair[0] = chosenOption;
                           LagSeriesOperator.this.setListParameter("attributes", list);
                        }
                     }), "missing_attribute", new Object[] { pair[0] });
                  }
                  if (overwriteAttributes && selectedAttributes.contains(pair[0])) {
                     this.createError(Severity.ERROR, Collections.singletonList(new ParameterSettingQuickFix(LagSeriesOperator.this, "overwrite_attributes", "false")), "time_series_extension.parameters.lag_series.attribute_selected_twice", new Object[]{pair[0]});
                  }
               }
            } catch (UndefinedParameterError var8) {
               LagSeriesOperator.this.getLogger().warning(var8.getMessage());
            }

         }
      });
   }

   protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
      try {
         boolean overwriteAttributes = this.getParameterAsBoolean("overwrite_attributes");
         boolean extendExampleSet = this.getParameterAsBoolean("extend_exampleset");
         int maxLag = 0;
         List list = this.getParameterList("attributes");
         Iterator var6 = list.iterator();

         while(var6.hasNext()) {
            String[] pair = (String[])var6.next();
            AttributeMetaData targetAttribute = metaData.getAttributeByName(pair[0]);
            if (targetAttribute != null) {
               int lag = Integer.parseInt(pair[1]);
               if (lag > maxLag) {
                  maxLag = lag;
               }

               AttributeMetaData attribute = targetAttribute;
               if (!overwriteAttributes) {
                  attribute = targetAttribute.clone();
                  attribute.setName(pair[0] + "-" + lag);
                  metaData.addAttribute(attribute);
               }

               if (!extendExampleSet) {
                  attribute.getNumberOfMissingValues().add(new MDInteger(lag));
               }
            }
         }

         if (extendExampleSet) {
            metaData.getNumberOfExamples().add(new MDInteger(maxLag));
            var6 = metaData.getAllAttributes().iterator();

            while(var6.hasNext()) {
               AttributeMetaData amd = (AttributeMetaData)var6.next();
               amd.getNumberOfMissingValues().add(new MDInteger(maxLag));
            }
         }
      } catch (NumberFormatException var11) {
         ;
      }

      return metaData;
   }

   public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
      boolean overwriteAttributes = this.getParameterAsBoolean("overwrite_attributes");
      boolean extendExampleSet = this.getParameterAsBoolean("extend_exampleset");
      List list = this.getParameterList("attributes");
      String[] attributeNames = new String[list.size()];
      Attribute[] origAttributes = new Attribute[list.size()];
      Attribute[] laggedAttributes = new Attribute[list.size()];
      double[][] valueBuffer = new double[list.size()][];
      int[] valueBufferIndex = new int[list.size()];
      int[] lags = new int[list.size()];
      int maxLag = 0;

      int exampleCounter;
      for(int i = 0; i < list.size(); ++i) {
         String[] pair = (String[])list.get(i);
         attributeNames[i] = pair[0];
         if (overwriteAttributes) {
            for(exampleCounter = i - 1; exampleCounter >= 0; --exampleCounter) {
               if (attributeNames[exampleCounter].equals(pair[0])) {
                  throw new UserError(this, "time_series_extension.parameter.lag_series.attribute_selected_twice", new Object[]{pair[0]});
               }
            }
         }

         lags[i] = Integer.parseInt(pair[1]) - 1;
         if (lags[i] + 1 > maxLag) {
            maxLag = lags[i] + 1;
         }

         valueBuffer[i] = new double[lags[i] + 1];
         valueBufferIndex[i] = 0;

         for(exampleCounter = 0; exampleCounter < valueBuffer[i].length; ++exampleCounter) {
            valueBuffer[i][exampleCounter] = Double.NaN;
         }
      }

      Map newToOldAttributeMap = new LinkedHashMap();
      ExampleSet result = exampleSet;
      if (extendExampleSet) {
         List newAttList = new ArrayList();
         Map specialRoleMap = new LinkedHashMap();

         AttributeRole role;
         Attribute newAttribute;
         for(Iterator attributeRoleIterator = exampleSet.getAttributes().allAttributeRoles(); attributeRoleIterator.hasNext(); newToOldAttributeMap.put(newAttribute, role.getAttribute())) {
            role = (AttributeRole)attributeRoleIterator.next();
            newAttribute = (Attribute)role.getAttribute().clone();
            newAttList.add(newAttribute);
            if (role.isSpecial()) {
               specialRoleMap.put(newAttribute, role.getSpecialName());
            }
         }

         result = ExampleSets.from(newAttList).withRoles(specialRoleMap).withBlankSize(exampleSet.size() + maxLag).build();
      }

      for(exampleCounter = 0; exampleCounter < attributeNames.length; ++exampleCounter) {
         origAttributes[exampleCounter] = result.getAttributes().get(attributeNames[exampleCounter]);
         if (origAttributes[exampleCounter] == null) {
            throw new UserError(this, 111, new Object[]{attributeNames[exampleCounter]});
         }

         if (overwriteAttributes) {
            laggedAttributes[exampleCounter] = origAttributes[exampleCounter];
         } else {
            laggedAttributes[exampleCounter] = AttributeFactory.createAttribute(origAttributes[exampleCounter].getName() + "-" + (lags[exampleCounter] + 1), origAttributes[exampleCounter].getValueType());
            if (origAttributes[exampleCounter].isNominal()) {
               laggedAttributes[exampleCounter].setMapping((NominalMapping)origAttributes[exampleCounter].getMapping().clone());
            }

            result.getExampleTable().addAttribute(laggedAttributes[exampleCounter]);
            result.getAttributes().addRegular(laggedAttributes[exampleCounter]);
         }
      }

      exampleCounter = 0;

      for(Iterator var23 = result.iterator(); var23.hasNext(); ++exampleCounter) {
         Example example = (Example)var23.next();
         if (extendExampleSet) {
            if (exampleCounter < exampleSet.size()) {
               Example originalExample = exampleSet.getExample(exampleCounter);
               Iterator var29 = newToOldAttributeMap.entrySet().iterator();

               while(var29.hasNext()) {
                  Entry entry = (Entry)var29.next();
                  example.setValue((Attribute)entry.getKey(), originalExample.getValue((Attribute)entry.getValue()));
               }
            } else {
               Iterator var25 = newToOldAttributeMap.entrySet().iterator();

               while(var25.hasNext()) {
                  Entry entry = (Entry)var25.next();
                  example.setValue((Attribute)entry.getKey(), Double.NaN);
               }
            }
         }

         for(int j = 0; j < laggedAttributes.length; ++j) {
            double currentValue = example.getValue(origAttributes[j]);
            example.setValue(laggedAttributes[j], valueBuffer[j][valueBufferIndex[j]]);
            valueBuffer[j][valueBufferIndex[j]] = currentValue;
            valueBufferIndex[j] = valueBufferIndex[j] == lags[j] ? 0 : valueBufferIndex[j] + 1;
         }
      }

      return result;
   }

   public boolean writesIntoExistingData() {
      return this.getParameterAsBoolean("overwrite_attributes");
   }

   public List getParameterTypes() {
      List types = super.getParameterTypes();
      types.add(new ParameterTypeList("attributes", "The attributes which should be lagged.", new ParameterTypeAttribute("attribute", "This attribute will be lagged.", this.getExampleSetInputPort()), new ParameterTypeInt("lag", "The lag for this attribute.", 1, Integer.MAX_VALUE), false));
      types.add(new ParameterTypeBoolean("overwrite_attributes", "", false, false));
      types.add(new ParameterTypeBoolean("extend_exampleset", "", false, false));
      return types;
   }
}
