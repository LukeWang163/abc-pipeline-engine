package base.operators.gui.viewer.metadata.model;

import java.awt.Color;
import java.util.Arrays;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.tools.Ontology;


/**
 * Model for {@link AttributeStatisticsPanel}s which are backed by a numerical {@link Attribute}.
 *
 * @author Marco Boeck
 *
 */
public class NumericalAttributeStatisticsModel extends AbstractAttributeStatisticsModel {

    /** the index for the histogram chart */
    private static final int INDEX_HISTOGRAM_CHART = 0;

    /** the max number of bins */
    private static final int MAX_BINS_HISTOGRAM = 10;

    /** used to color the chart background invisible */
    private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

    /** the average of the numerical values */
    private double average;

    /** the standard deviation of the numerical values */
    private double deviation;

    /** the minimum of the numerical values */
    private double minimum;

    /** the maximum of the numerical values */
    private double maximum;

    /** the minimum of the numerical values */
    private double lowerQuartile;

    /** the maximum of the numerical values */
    private double higherQuartile;

    /** array of charts for this model */
    private JFreeChart[] chartsArray;

    /**
     * Creates a new {@link NumericalAttributeStatisticsModel}.
     *
     * @param exampleSet
     * @param attribute
     */
    public NumericalAttributeStatisticsModel(ExampleSet exampleSet, Attribute attribute) {
        super(exampleSet, attribute);

        chartsArray = new JFreeChart[1];
    }

    @Override
    public void updateStatistics(ExampleSet exampleSet) {
        average = exampleSet.getStatistics(getAttribute(), Statistics.AVERAGE);
        deviation = Math.sqrt(exampleSet.getStatistics(getAttribute(), Statistics.VARIANCE));
        minimum = exampleSet.getStatistics(getAttribute(), Statistics.MINIMUM);
        maximum = exampleSet.getStatistics(getAttribute(), Statistics.MAXIMUM);
        missing = exampleSet.getStatistics(getAttribute(), Statistics.UNKNOWN);
        lowerQuartile = exampleSet.getStatistics(getAttribute(), Statistics.LOWER_QUARTILE);
        higherQuartile = exampleSet.getStatistics(getAttribute(), Statistics.HIGHER_QUARTILE);
//        fireStatisticsChangedEvent();
    }

    /**
     * Gets the average of the numerical values.
     *
     * @return
     */
    public double getAverage() {
        return average;
    }

    /**
     * Gets the standard deviation of the numerical values.
     *
     * @return
     */
    public double getDeviation() {
        return deviation;
    }

    /**
     * Gets the minimum of the numerical values.
     *
     * @return
     */
    public double getMinimum() {
        return minimum;
    }

    /**
     * Gets the maximum of the numerical values.
     *
     * @return
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * Gets the minimum of the numerical values.
     *
     * @return
     */
    public double getLowerQuartile() {
        return lowerQuartile;
    }

    /**
     * Gets the maximum of the numerical values.
     *
     * @return
     */
    public double getHigherQuartile() {
        return higherQuartile;
    }


//    @Override
//    public JFreeChart getChartOrNull(int index) {
//        prepareCharts();
//        if (index == INDEX_HISTOGRAM_CHART) {
//            return chartsArray[index];
//        }
//
//        return null;
//    }

    /**
     * Creates a {@link HistogramDataset} for this {@link Attribute}.
     *
     * @param exampleSet
     * @return
     */
    private HistogramDataset createHistogramDataset(ExampleSet exampleSet) {
        HistogramDataset dataset = new HistogramDataset();

        double[] array = new double[exampleSet.size()];
        int count = 0;

        for (Example example : exampleSet) {
            double value = example.getDataRow().get(getAttribute());
            // don't use missing values because otherwise JFreeChart tries to plot them too which
            // can lead to false histograms
            if (!Double.isNaN(value)) {
                array[count++] = value;
            }
        }

        // add points to data set (if any)
        if (count > 0) {
            // truncate array if necessary
            if (count < array.length) {
                array = Arrays.copyOf(array, count);
            }
            dataset.addSeries(getAttribute().getName(), array, Math.min(array.length, MAX_BINS_HISTOGRAM));
        }

        return dataset;
    }

    /**
     * Creates the histogram chart.
     *
     * @param exampleSet
     * @return
     */
//    private JFreeChart createHistogramChart(ExampleSet exampleSet) {
//        JFreeChart chart = ChartFactory.createHistogram(null, null, null, createHistogramDataset(exampleSet),
//                PlotOrientation.VERTICAL, false, false, false);
//        AbstractAttributeStatisticsModel.setDefaultChartFonts(chart);
//        chart.setBackgroundPaint(null);
//        chart.setBackgroundImageAlpha(0.0f);
//
//        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setRangeGridlinesVisible(false);
//        plot.setDomainGridlinesVisible(false);
//        plot.setOutlineVisible(false);
//        plot.setRangeZeroBaselineVisible(false);
//        plot.setDomainZeroBaselineVisible(false);
//        plot.setBackgroundPaint(COLOR_INVISIBLE);
//        plot.setBackgroundImageAlpha(0.0f);
//
//        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
//        renderer.setSeriesPaint(0, AttributeGuiTools.getColorForValueType(Ontology.NUMERICAL));
//        renderer.setBarPainter(new StandardXYBarPainter());
//        renderer.setDrawBarOutline(true);
//        renderer.setShadowVisible(false);
//
//        return chart;
//    }
//
//    @Override
//    public void prepareCharts() {
//        if (chartsArray[INDEX_HISTOGRAM_CHART] == null && getExampleSetOrNull() != null) {
//            chartsArray[INDEX_HISTOGRAM_CHART] = createHistogramChart(getExampleSetOrNull());
//        }
//    }

}
