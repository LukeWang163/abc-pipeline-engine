package base.operators.operator.generator.generators;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.generator.generators.DataGenerators.GeneratorType;
import base.operators.operator.Operator;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDateFormat;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeTupel;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Tools;
import base.operators.tools.math.container.Range;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class DateSeriesDataGenerator extends AbstractDataGenerator {
    private static final String PARAMETER_DATE_SERIES_CONFIGURATION_NUMBER = "date_series_configuration";
    private static final String PARAMETER_DATE_SERIES_CONFIGURATION_INTERVAL = "date_series_configuration (interval)";
    private static final String PARAMETER_DATE_SERIES_DATE_FORMAT = "date_format";
    private static final String PARAMETER_TIME_ZONE = "time_zone";

    DateSeriesDataGenerator(Operator parent) {
        super(parent);
    }

    @Override
    public ExampleSetMetaData generateExampleSetMetaData() throws UserError {
        DateSeriesDataGenerator.Settings settings = new DateSeriesDataGenerator.Settings();
        ExampleSetMetaData md = new ExampleSetMetaData();
        md.setNumberOfExamples(settings.numberOfExamples);

        for(int i = 0; i < settings.attributeNames.length; ++i) {
            Range range = this.getRange(settings.startTimes[i], settings.stopTimes[i]);
            AttributeMetaData newAttribute = new AttributeMetaData(settings.attributeNames[i], 9);
            newAttribute.setValueRange(range, SetRelation.EQUAL);
            md.addAttribute(newAttribute);
        }

        return md;
    }

    private Range getRange(ZonedDateTime startTime, ZonedDateTime stopTime) {
        return stopTime.isBefore(startTime) ? new Range((double)stopTime.toInstant().toEpochMilli(), (double)startTime.toInstant().toEpochMilli()) : new Range((double)startTime.toInstant().toEpochMilli(), (double)stopTime.toInstant().toEpochMilli());
    }

    @Override
    public ExampleSet generateExampleSet() throws UserError, ProcessStoppedException {
        DateSeriesDataGenerator.Settings settings = new DateSeriesDataGenerator.Settings();
        int numberOfAttributes = settings.attributeNames.length;
        this.getParent().getProgress().setTotal(settings.numberOfExamples + numberOfAttributes);
        DateSeriesDataGenerator.SingleDateSeriesGenerator[] dataGenerators = new DateSeriesDataGenerator.SingleDateSeriesGenerator[numberOfAttributes];
        Attribute[] attributes = new Attribute[numberOfAttributes];

        for(int i = 0; i < numberOfAttributes; ++i) {
            dataGenerators[i] = new DateSeriesDataGenerator.SingleDateSeriesGenerator(settings.startTimes[i], settings.stopTimes[i], settings.stepSizes[i], settings.intervalTypes[i]);
            attributes[i] = AttributeFactory.createAttribute(settings.attributeNames[i], 9);
            this.getParent().getProgress().step();
        }

        ExampleSetBuilder builder = ExampleSets.from(attributes);

        for(int example = 0; example < settings.numberOfExamples; ++example) {
            double[] row = new double[numberOfAttributes];

            for(int attr = 0; attr < numberOfAttributes; ++attr) {
                row[attr] = (double)dataGenerators[attr].getNext().toInstant().toEpochMilli();
            }

            builder.addRow(row);
            this.getParent().getProgress().step();
        }

        this.getParent().getProgress().complete();
        return builder.build();
    }

    @Override
    protected List<ParameterType> getParameterTypesInternal() {
        List<ParameterType> types = new ArrayList();
        String[] dateIntervalTypesNames = (String[])Arrays.stream(DateSeriesDataGenerator.DateTimeInterval.values()).map(DateSeriesDataGenerator.DateTimeInterval::getName).toArray((x$0) -> {
            return new String[x$0];
        });
        ParameterType type = new ParameterTypeList("date_series_configuration", "Date series list to generate.", new ParameterTypeString("attribute_name", "Attribute name"), new ParameterTypeTupel("series_settings (start_date ; end_date)", "Date series settings.", new ParameterType[]{new ParameterTypeString("start_date", "start date", true, false), new ParameterTypeString("end_date", "end date", true, false)}), false);
        type.setOptional(false);
        type.setPrimary(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this.getParent(), "use_stepsize", true, false));
        types.add(type);
        type = new ParameterTypeList("date_series_configuration (interval)", "Date series list to generate.", new ParameterTypeString("attribute_name", "Attribute name"), new ParameterTypeTupel("series_settings (start_date ; stepsize ; interval_type)", "Date series settings.", new ParameterType[]{new ParameterTypeString("start_date", "start date", true, false), new ParameterTypeInt("stepsize", "", -2147483648, 2147483647, 1), new ParameterTypeCategory("interval_type", "", dateIntervalTypesNames, 0)}), false);
        type.setOptional(false);
        type.setPrimary(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this.getParent(), "use_stepsize", true, true));
        types.add(type);
        type = new ParameterTypeDateFormat("date_format", "Date format used for 'start date' and 'end date' parameters.", "yyyy-MM-dd HH:mm:ss", false);
        type.setOptional(false);
        types.add(type);
        type = new ParameterTypeCategory("time_zone", "The time zone used for the date objects if not specified in the date string itself.", Tools.getAllTimeZones(), Tools.getPreferredTimeZoneIndex());
        types.add(type);
        return types;
    }

    @Override
    protected GeneratorType getGeneratorType() {
        return GeneratorType.DATE_SERIES;
    }

    private static class SingleDateSeriesGenerator {
        private final long stepSize;
        private final ZonedDateTime lowerBound;
        private final ZonedDateTime upperBound;
        private final TemporalUnit dateIntervalType;
        private ZonedDateTime nextValue;

        SingleDateSeriesGenerator(ZonedDateTime start, ZonedDateTime stop, long stepSize, TemporalUnit dateIntervalType) {
            if (start.isAfter(stop)) {
                this.lowerBound = stop;
                this.upperBound = start;
            } else {
                this.lowerBound = start;
                this.upperBound = stop;
            }

            this.stepSize = stepSize;
            this.dateIntervalType = dateIntervalType;
            this.nextValue = start;
        }

        ZonedDateTime getNext() {
            ZonedDateTime currentValue = this.nextValue;
            ZonedDateTime next = currentValue.plus(this.stepSize, this.dateIntervalType);
            if (next.isAfter(this.upperBound)) {
                next = this.upperBound;
            } else if (next.isBefore(this.lowerBound)) {
                next = this.lowerBound;
            }
            this.nextValue = next;
            return currentValue;
        }
    }

    private static enum DateTimeInterval implements TemporalUnit {
        YEAR(ChronoUnit.YEARS),
        MONTH(ChronoUnit.MONTHS),
        WEEK(ChronoUnit.WEEKS),
        DAY(ChronoUnit.DAYS),
        HOUR(ChronoUnit.HOURS),
        MINUTE(ChronoUnit.MINUTES),
        SECOND(ChronoUnit.SECONDS),
        MILLISECOND(ChronoUnit.MILLIS);

        private final transient ChronoUnit unit;

        private DateTimeInterval(ChronoUnit unit) {
            this.unit = unit;
        }

        @Override
        public Duration getDuration() {
            return this.unit.getDuration();
        }

        @Override
        public boolean isDurationEstimated() {
            return this.unit.isDurationEstimated();
        }

        @Override
        public boolean isDateBased() {
            return this.unit.isDateBased();
        }

        @Override
        public boolean isTimeBased() {
            return this.unit.isTimeBased();
        }

        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return this.unit.addTo(temporal, amount);
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return this.unit.between(temporal1Inclusive, temporal2Exclusive);
        }

        public static DateSeriesDataGenerator.DateTimeInterval byName(String name) {
            return valueOf(name.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
        }
    }

    private final class Settings {
        private final int numberOfExamples;
        private final ZonedDateTime[] startTimes;
        private final ZonedDateTime[] stopTimes;
        private final long[] stepSizes;
        private TemporalUnit[] intervalTypes;
        private final String[] attributeNames;

        private Settings() throws UserError {
            ParameterHandler parent = DateSeriesDataGenerator.this.getParent();
            this.numberOfExamples = parent.getParameterAsInt("number_of_examples");
            boolean useStepSize = parent.getParameterAsBoolean("use_stepsize");
            TimeZone timeZone = Tools.getTimeZone(parent.getParameterAsInt("time_zone"));
            ZoneId zone = timeZone.toZoneId();
            SimpleDateFormat format = new SimpleDateFormat(parent.getParameterAsString("date_format"));
            format.setTimeZone(timeZone);
            String dateSeriesConfigurationKey = useStepSize ? "date_series_configuration (interval)" : "date_series_configuration";
            List<String[]> dateSeriesConfiguration = parent.getParameterList(dateSeriesConfigurationKey);
            int size = dateSeriesConfiguration.size();
            this.stopTimes = new ZonedDateTime[size];
            this.stepSizes = new long[size];
            this.intervalTypes = new TemporalUnit[size];
            this.startTimes = new ZonedDateTime[size];
            this.attributeNames = new String[size];
            int pos = 0;

            for(Iterator var11 = dateSeriesConfiguration.iterator(); var11.hasNext(); ++pos) {
                String[] pair = (String[])var11.next();
                this.attributeNames[pos] = pair[0];
                String[] settings = ParameterTypeTupel.transformString2Tupel(pair[1]);

                try {
                    this.startTimes[pos] = format.parse(settings[0]).toInstant().atZone(zone);
                    if (useStepSize) {
                        this.stepSizes[pos] = Long.parseLong(settings[1]);
                        this.intervalTypes[pos] = DateSeriesDataGenerator.DateTimeInterval.byName(settings[2]);
                        this.stopTimes[pos] = this.startTimes[pos].plus((long)this.numberOfExamples * this.stepSizes[pos], this.intervalTypes[pos]);
                    } else {
                        this.stopTimes[pos] = format.parse(settings[1]).toInstant().atZone(zone);
                        this.intervalTypes[pos] = ChronoUnit.MICROS;
                        long diff = this.intervalTypes[pos].between(this.startTimes[pos], this.stopTimes[pos]);
                        long steps = (long)Math.max(this.numberOfExamples - 1, 1);
                        long sign = (long)Long.signum(diff);
                        this.stepSizes[pos] = (diff - sign + steps * sign) / steps;
                    }
                } catch (ParseException var20) {
                    throw new UserError((Operator)null, "wrong_date_format", new Object[]{settings[0], dateSeriesConfigurationKey.replace('_', ' ')});
                }
            }
        }
    }
}

