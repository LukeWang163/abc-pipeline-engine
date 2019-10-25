package base.operators.operator.learner.associations.fpgrowth;

import base.operators.belt.column.Column;
import base.operators.belt.reader.ObjectReader;
import base.operators.belt.reader.Readers;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.learner.associations.FrequentItemSet;
import base.operators.operator.learner.associations.FrequentItemSets;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.ParameterConditionedPrecondition;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeChar;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.tools.Ontology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeltFPGrowth
        extends Operator
{
    static final String PARAMETER_INPUT_FORMAT = "input_format";
    private static final String[] INPUT_FORMATS = { "item list in a column", "items in separate columns", "items in dummy coded columns" };

    static final int INPUT_FORMATS_ITEM_LIST_IN_A_COLUMN = 0;

    static final int INPUT_FORMATS_ITEMS_IN_SEPARATE_COLUMNS = 1;

    static final int INPUT_FORMATS_ITEMS_IN_DUMMY_CODED_COLUMNS = 2;

    static final String PARAMETER_COLUMN_SEPARATORS = "item_separators";

    static final String PARAMETER_USE_QUOTES = "use_quotes";

    static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";
    static final String PARAMETER_ESCAPE_CHARACTER = "escape_character";
    static final String PARAMETER_TRIM_ITEM_NAMES = "trim_item_names";
    static final String PARAMETER_POSITIVE_VALUE = "positive_value";
    private static final String PARAMETER_MIN_REQUIREMENT = "min_requirement";
    private static final String[] MIN_REQUIREMENTS = { "support", "frequency" };

    private static final int MIN_REQUIREMENT_SUPPORT = 0;

    private static final int MIN_REQUIREMENT_FREQUENCY = 1;

    private static final String PARAMETER_MIN_SUPPORT = "min_support";

    private static final String PARAMETER_MIN_FREQUENCY = "min_frequency";

    private static final String PARAMETER_MIN_NUMBER_OF_ITEMS_IN_AN_ITEMSET = "min_items_per_itemset";

    private static final String PARAMETER_MAX_NUMBER_OF_ITEMS_IN_AN_ITEMSET = "max_items_per_itemset";
    private static final String PARAMETER_MAX_NUMBER_OF_ITEMSETS = "max_number_of_itemsets";
    private static final String PARAMETER_FIND_MIN_NUMBER_OF_ITEMSETS = "find_min_number_of_itemsets";
    private static final String PARAMETER_MIN_NUMBER_OF_ITEMSETS = "min_number_of_itemsets";
    private static final String PARAMETER_MAX_REDUCTION_STEPS = "max_number_of_retries";
    private static final String PARAMETER_REQUIREMENT_DECREASE_FACTOR = "requirement_decrease_factor";
    private static final String PARAMETER_MUST_CONTAIN_LIST = "must_contain_list";
    private static final String PARAMETER_MUST_CONTAIN_ITEM = "must_contain_item";
    static final String PARAMETER_MUST_CONTAIN_REGEXP = "must_contain_regexp";
    private static final int CONVERSION_STEPS = 500;
    private static final int FREQUENCY_COUNTING_STEPS = 1000;
    private static final int TREE_BUILDING_STEPS = 1000;
    static final int TREE_MINING_STEPS = 9000;
    private static final int TREE_TOTAL_STEPS = 10000;
    private final InputPort exampleSetInput = (InputPort)getInputPorts().createPort("example set");
    private final OutputPort exampleSetOutput = (OutputPort)getOutputPorts().createPort("example set");
    private final OutputPort frequentSetsOutput = (OutputPort)getOutputPorts().createPort("frequent sets");

    public BeltFPGrowth(OperatorDescription description) {
        super(description);
        this.exampleSetInput.addPrecondition(new ExampleSetPrecondition(this.exampleSetInput, 1, new String[0]));

        this.exampleSetInput.addPrecondition(new ParameterConditionedPrecondition(this.exampleSetInput, new ExampleSetPrecondition(this.exampleSetInput)
        {
            @Override
            public void check(MetaData metaData)
            {
                if (metaData instanceof ExampleSetMetaData && !BeltFPGrowth.this.checkBinominal((ExampleSetMetaData)metaData)) {
                    createError(ProcessSetupError.Severity.WARNING, BeltFPGrowth.this
                            .getOtherInputFormats(2), "fp_growth_input.dummy_not_binominal", new Object[0]);
                }
            }
        }, this, "input_format", INPUT_FORMATS[2]));

        this.exampleSetInput.addPrecondition(new ParameterConditionedPrecondition(this.exampleSetInput, new ExampleSetPrecondition(this.exampleSetInput)
        {

            @Override
            public void check(MetaData metaData)
            {
                if (metaData instanceof ExampleSetMetaData &&
                        !BeltFPGrowth.this.checkSingleNominal((ExampleSetMetaData)metaData)) {
                    createError(ProcessSetupError.Severity.WARNING, BeltFPGrowth.this
                            .getOtherInputFormats(0), "fp_growth_input.list_more_than_one", new Object[0]);
                }
            }
        }, this, "input_format", INPUT_FORMATS[0]));

        getTransformer().addGenerationRule(this.frequentSetsOutput, FrequentItemSets.class);
        getTransformer().addPassThroughRule(this.exampleSetInput, this.exampleSetOutput);
    }

    private boolean checkSingleNominal(ExampleSetMetaData emd) {
        int counter = 0;
        for (AttributeMetaData amd : emd.getAllAttributes()) {

            counter++;
            if (!amd.isSpecial() && counter > 1)
            {
                return false;
            }
        }

        return true;
    }

    private boolean checkBinominal(ExampleSetMetaData emd) {
        for (AttributeMetaData amd : emd.getAllAttributes()) {
            if (!amd.isSpecial() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), 6))
            {

                return false;
            }
        }
        return true;
    }

    private List<QuickFix> getOtherInputFormats(int currentInputFormat) {
        List<QuickFix> qfixes = new ArrayList<QuickFix>();
        for (int i = 0; i < INPUT_FORMATS.length; i++) {
            if (i != currentInputFormat) {
                qfixes.add(new ParameterSettingQuickFix(this, "input_format", INPUT_FORMATS[i]));
            }
        }
        return qfixes;
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet)this.exampleSetInput.getData(ExampleSet.class);

        Tools.onlyNominalAttributes(exampleSet, "FPGrowth");

        if (exampleSet.getAttributes().size() > 0) {

            boolean shouldFindMinimumNumber = getParameterAsBoolean("find_min_number_of_itemsets");
            int maxRetries = 0;
            if (shouldFindMinimumNumber) {
                maxRetries = getParameterAsInt("max_number_of_retries");
            }

            if (shouldFindMinimumNumber) {
                getProgress().setTotal(1000 + (maxRetries + 1) * 10000);
            } else {
                getProgress().setTotal(11000);
            }

            Set<String> mandatoryItems = new HashSet<String>();

            Column column = (new ToColumnConverter(this)).convert(exampleSet, mandatoryItems);
            getProgress().setCompleted(500);


            addMandatoryListItems(mandatoryItems);

            Map<NominalItem, Integer> itemFrequencies = new IdentityHashMap<NominalItem, Integer>();
            List<NominalItem> mandatoryList = new ArrayList<NominalItem>();
            int nTransactions = countItemFrequencies(column, mandatoryItems, itemFrequencies, mandatoryList);

            getProgress().setCompleted(1000);

            FrequentItemSets sets = calculateFrequentItemSets(shouldFindMinimumNumber, maxRetries, mandatoryList, itemFrequencies, nTransactions, column);

            this.frequentSetsOutput.deliver(sets);
        } else {

            this.frequentSetsOutput.deliver(new FrequentItemSets(0));
        }

        this.exampleSetOutput.deliver(exampleSet);
    }

    private void addMandatoryListItems(Set<String> mandatoryItems) throws UndefinedParameterError {
        if (isParameterSet("must_contain_list")) {
            String[] mustContainListItems = ParameterTypeEnumeration.transformString2Enumeration(
                    getParameterAsString("must_contain_list"));
            mandatoryItems.addAll(Arrays.asList(mustContainListItems));
        }
    }

    private int countItemFrequencies(Column column, Set<String> mandatoryItems, Map<NominalItem, Integer> itemsWithFrequencies, List<NominalItem> mandatoryList) {
        int nTransactions = 0;
        Map<String, NominalItem> itemMap = new HashMap<String, NominalItem>();
        ObjectReader<ItemSet> reader = Readers.objectReader(column, ItemSet.class);
        while (reader.hasRemaining()) {
            ItemSet itemSet = (ItemSet)reader.read();

            boolean allMandatoryItemsPresent = processItemSet(itemSet, itemsWithFrequencies, itemMap, mandatoryItems);
            if (allMandatoryItemsPresent) {
                nTransactions++;
            }
        }

        if (!mandatoryItems.isEmpty()) {
            for (String mandatoryItem : mandatoryItems) {
                NominalItem item = (NominalItem)itemMap.remove(mandatoryItem);
                if (item != null) {
                    mandatoryList.add(item);
                    itemsWithFrequencies.remove(item);
                }
            }
        }

        return nTransactions;
    }

    private static boolean processItemSet(ItemSet itemSet, Map<NominalItem, Integer> itemsWithFrequencies, Map<String, NominalItem> itemMap, Set<String> mandatoryItems) {
        boolean allMandatoryItemsPresent = true;
        if (!mandatoryItems.isEmpty()) {
            for (String mandatoryItem : mandatoryItems) {
                if (!itemSet.contains(mandatoryItem)) {
                    allMandatoryItemsPresent = false;
                }
            }
        }

        for (String item : itemSet) {
            NominalItem nominalItem = (NominalItem)itemMap.get(item);
            if (nominalItem == null) {
                nominalItem = new NominalItem(item);
                itemMap.put(item, nominalItem);
            }
            nominalItem.increaseFrequency();
            if (allMandatoryItemsPresent) {
                itemsWithFrequencies.merge(nominalItem, Integer.valueOf(1), Integer::sum);
            }
        }
        return allMandatoryItemsPresent;
    }

    private FrequentItemSets calculateFrequentItemSets(boolean shouldFindMinimumNumber, int maxRetries, List<NominalItem> mandatoryItems, Map<NominalItem, Integer> itemFrequencies, int nTransactions, Column column) throws OperatorException {
        List<FrequentItemSet> acc;
        int maxNumberOfSets = getParameterAsInt("max_number_of_itemsets");


        if (shouldFindMinimumNumber) {
            acc = findMinimumNumber(itemFrequencies, mandatoryItems, nTransactions, column, maxNumberOfSets, maxRetries);
        } else {
            acc = runOnce(itemFrequencies, mandatoryItems, nTransactions, column, maxNumberOfSets);
        }
        if (maxNumberOfSets != 0 && acc.size() > maxNumberOfSets) {
            acc = acc.subList(0, maxNumberOfSets);
        }

        FrequentItemSets sets = new FrequentItemSets(column.size());
        for (FrequentItemSet frequentItemSet : acc) {
            sets.addFrequentSet(frequentItemSet);
        }
        return sets;
    }

    private List<FrequentItemSet> runOnce(Map<NominalItem, Integer> itemFrequencies, List<NominalItem> mandatoryItems, int nTransactions, Column column, int maxNumberOfSets) throws OperatorException {
        int minFrequency, minRequirement = getParameterAsInt("min_requirement");

        if (minRequirement == 0) {
            double minSupport = getParameterAsDouble("min_support");
            minFrequency = (int)Math.ceil(minSupport * column.size());
        } else {
            minFrequency = getParameterAsInt("min_frequency");
        }

        List<FrequentItemSet> acc = new ArrayList<FrequentItemSet>();
        BeltFPTreeMiner miner = getTreeMiner();
        fpGrowth(miner, acc, itemFrequencies, mandatoryItems, nTransactions, column, minFrequency, maxNumberOfSets, true);

        return acc;
    }

    private List<FrequentItemSet> findMinimumNumber(Map<NominalItem, Integer> itemFrequencies, List<NominalItem> mandatoryItems, int nTransactions, Column column, int maxNumberOfSets, int maxRetries) throws OperatorException {
        int minRequirement = getParameterAsInt("min_requirement");
        double minSupport = 0.0D;
        int minFrequency = 0;
        if (minRequirement == 0) {
            minSupport = getParameterAsDouble("min_support");
        } else {
            minFrequency = getParameterAsInt("min_frequency");
        }

        int minNumberOfSets = getParameterAsInt("min_number_of_itemsets");
        double decreaseFactor = getParameterAsDouble("requirement_decrease_factor");
        checkNumberOfSets(minNumberOfSets, maxNumberOfSets);

        BeltFPTreeMiner miner = getTreeMiner();
        List<FrequentItemSet> acc = new ArrayList<FrequentItemSet>();
        int nRetries = 0;
        do {
            if (minRequirement == 0) {
                minFrequency = (int)Math.ceil(minSupport * column.size());
            }

            acc.clear();
            fpGrowth(miner, acc, itemFrequencies, mandatoryItems, nTransactions, column, minFrequency, maxNumberOfSets, false);


            if (acc.size() >= minNumberOfSets) {
                break;
            }

            if (minRequirement == 0) {
                minSupport *= decreaseFactor;
            } else {
                minFrequency = (int)Math.round(decreaseFactor * minFrequency);
            }
            nRetries++;

            getProgress().setCompleted(1000 + nRetries * 10000);
        } while (nRetries <= maxRetries);

        getProgress().setCompleted(1000 + (maxRetries + 1) * 10000);

        return acc;
    }

    private BeltFPTreeMiner getTreeMiner() throws UserError {
        int minItemSetSize = getParameterAsInt("min_items_per_itemset");
        int maxItemSetSize = getParameterAsInt("max_items_per_itemset");
        checkSetSizes(minItemSetSize, maxItemSetSize);
        return new BeltFPTreeMiner(this, minItemSetSize, maxItemSetSize);
    }

    private void checkSetSizes(int minItemSetSize, int maxItemSetSize) throws UserError {
        if (maxItemSetSize != 0 && minItemSetSize > maxItemSetSize) {
            throw new UserError(this, "fp_growth.max_items_per_set_too_small");
        }
    }

    private void checkNumberOfSets(int minNumberOfSets, int maxNumberOfSets) throws UserError {
        if (maxNumberOfSets != 0 && minNumberOfSets > maxNumberOfSets) {
            throw new UserError(this, "fp_growth.max_number_sets_too_small");
        }
    }

    private void fpGrowth(BeltFPTreeMiner miner, List<FrequentItemSet> accumulator, Map<NominalItem, Integer> itemFrequencies, List<NominalItem> mandatoryItems, int nTransactions, Column column, int minFrequency, int maxItemSets, boolean showMiningProgress) throws OperatorException {
        if (nTransactions >= minFrequency) {
            FrequentItemSet mandatoryItemSet;
            if (!mandatoryItems.isEmpty()) {
                mandatoryItemSet = new FrequentItemSet(new ArrayList(mandatoryItems), nTransactions);
                accumulator.add(mandatoryItemSet);
                if (maxItemSets != 0) {
                    maxItemSets--;
                }
            } else {
                mandatoryItemSet = new FrequentItemSet();
            }

            BeltFPTree tree = BeltFPTreeBuilder.buildFromItemsColumns(column, mandatoryItems, itemFrequencies, minFrequency);

            getProgress().step(1000);
            int desiredItemSets = (maxItemSets != 0) ? maxItemSets : Integer.MAX_VALUE;
            miner.mine(tree, mandatoryItemSet, minFrequency, accumulator, desiredItemSets, showMiningProgress);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        ParameterTypeCategory parameterTypeCategory2 = new ParameterTypeCategory("input_format", "Specifies the input format of the data.", INPUT_FORMATS, 2);

        parameterTypeCategory2.setExpert(false);
        types.add(parameterTypeCategory2);

        ParameterTypeString parameterTypeString3 = new ParameterTypeString("item_separators", "Item separator (regular expression)", "|", false);

        parameterTypeString3.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, true, new int[] { 0 }));

        types.add(parameterTypeString3);
        ParameterTypeBoolean parameterTypeBoolean3 = new ParameterTypeBoolean("use_quotes", "Indicates if quotes should be regarded.", false, false);

        parameterTypeBoolean3.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, false, new int[] { 0 }));

        types.add(parameterTypeBoolean3);
        ParameterTypeChar parameterTypeChar = new ParameterTypeChar("quotes_character", "The quotes character.", '"', false);

        parameterTypeChar.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, true, new int[] { 0 }));

        parameterTypeChar.registerDependencyCondition(new BooleanParameterCondition(this, "use_quotes", false, true));
        types.add(parameterTypeChar);
        parameterTypeChar = new ParameterTypeChar("escape_character", "The character that is used to escape quotes and item seperators", '\\', true);

        parameterTypeChar.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, true, new int[] { 0 }));

        types.add(parameterTypeChar);

        ParameterTypeBoolean parameterTypeBoolean2 = new ParameterTypeBoolean("trim_item_names", "Indicates if item names should be trimmed.", true, false);

        parameterTypeBoolean2.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, false, new int[] { 0, 1 }));

        types.add(parameterTypeBoolean2);

        ParameterTypeString parameterTypeString2 = new ParameterTypeString("positive_value", "This parameter determines, which value of the binominal attributes is treated as positive. Attributes with that value are considered as part of a transaction. If left blank, the example set determines, which is value is used.", true);

        parameterTypeString2.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, false, new int[] { 2 }));

        types.add(parameterTypeString2);

        ParameterTypeCategory parameterTypeCategory1 = new ParameterTypeCategory("min_requirement", "Specifies the minimum support requirement.", MIN_REQUIREMENTS, 0);

        parameterTypeCategory1.setExpert(false);
        types.add(parameterTypeCategory1);

        ParameterTypeDouble parameterTypeDouble2 = new ParameterTypeDouble("min_support", "The minimum support is defined as a percentage to consider an itemset to be frequent.", 0.0D, 1.0D, 0.95D);

        parameterTypeDouble2.registerDependencyCondition(new EqualTypeCondition(this, "min_requirement", MIN_REQUIREMENTS, true, new int[] {0}));

        parameterTypeDouble2.setExpert(false);
        types.add(parameterTypeDouble2);
        ParameterTypeInt parameterTypeInt2 = new ParameterTypeInt("min_frequency", "The minimum support is defined as a number of occurrences to consider an itemset to be frequent.", 0, 2147483647, 100);

        parameterTypeInt2.registerDependencyCondition(new EqualTypeCondition(this, "min_requirement", MIN_REQUIREMENTS, true, new int[] {1}));

        parameterTypeInt2.setExpert(false);
        types.add(parameterTypeInt2);

        parameterTypeInt2 = new ParameterTypeInt("min_items_per_itemset", "The lower bound for the size of the itemsets.", 1, 2147483647, 1);

        parameterTypeInt2.setExpert(false);
        types.add(parameterTypeInt2);
        parameterTypeInt2 = new ParameterTypeInt("max_items_per_itemset", "The upper bound for the size of the itemsets (0: no upper bound)", 0, 2147483647, 0);

        parameterTypeInt2.setExpert(false);
        types.add(parameterTypeInt2);
        parameterTypeInt2 = new ParameterTypeInt("max_number_of_itemsets", "The upper bound for the number of itemsets (0: no upper bound)", 0, 2147483647, 1000000);

        parameterTypeInt2.setExpert(false);
        types.add(parameterTypeInt2);

        ParameterTypeBoolean parameterTypeBoolean1 = new ParameterTypeBoolean("find_min_number_of_itemsets", "Indicates if the minimal support should be decreased automatically until the specified minimum number of frequent itemsets are found.", true);


        parameterTypeBoolean1.setExpert(false);
        types.add(parameterTypeBoolean1);
        ParameterTypeInt parameterTypeInt1 = new ParameterTypeInt("min_number_of_itemsets", "Indicates the minimum number of itemsets to find.", 1, 2147483647, 100);

        parameterTypeInt1.registerDependencyCondition(new BooleanParameterCondition(this, "find_min_number_of_itemsets", true, true));

        parameterTypeInt1.setExpert(false);
        types.add(parameterTypeInt1);
        parameterTypeInt1 = new ParameterTypeInt("max_number_of_retries", "This determines how many times the operator lowers the minimum support to find the minimal number of itemsets.", 1, 2147483647, 15);


        parameterTypeInt1.registerDependencyCondition(new BooleanParameterCondition(this, "find_min_number_of_itemsets", false, true));

        types.add(parameterTypeInt1);
        ParameterTypeDouble parameterTypeDouble1 = new ParameterTypeDouble("requirement_decrease_factor", "The factor by the minimum requirement decreases in each iteration.", 0.0D, 1.0D, 0.9D, true);

        parameterTypeDouble1.registerDependencyCondition(new BooleanParameterCondition(this, "find_min_number_of_itemsets", false, true));

        types.add(parameterTypeDouble1);

        ParameterTypeEnumeration parameterTypeEnumeration = new ParameterTypeEnumeration("must_contain_list", "The items that all itemset should contain.", new ParameterTypeString("must_contain_item", "The name of the item"), false);

        types.add(parameterTypeEnumeration);
        ParameterTypeString parameterTypeString1 = new ParameterTypeString("must_contain_regexp", "The items that all itemset should contain (regular expression, empty means none).");

        parameterTypeString1.registerDependencyCondition(new EqualTypeCondition(this, "input_format", INPUT_FORMATS, false, new int[] {2}));

        types.add(parameterTypeString1);

        return types;
    }
}
