package base.operators.operator.tools;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.tools.FontTools;
import base.operators.tools.container.ValueAndCount;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JPopupMenu;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;

public class ChartTools {
    private static final int MAXIMUM_DRAW_HEIGHT = 2000;
    private static final int MAXIMUM_DRAW_WIDTH = 3000;
    private static final int MAX_NUMBER_OF_BINS = 10;
//    public static final Font CHART_FONT = FontTools.getFont("Dialog", 0, 11);
//    public static final Font CHART_FONT_BOLD = FontTools.getFont("Dialog", 1, 11);
//    public static final Font CHART_FONT_LARGE_BOLD = FontTools.getFont("Dialog", 1, 13);
    public static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);
    public static final Color NORMAL_COLOR = new Color(20, 194, 204);
    public static final Color NORMAL_DIMMED_COLOR = new Color(53, 173, 180);
    public static final Color NORMAL_LIGHT_COLOR = new Color(200, 234, 244);
    public static final Color HIGHLIGHT_COLOR = new Color(255, 130, 40);
    public static final Color HIGHLIGHT_LIGHT_COLOR = new Color(255, 225, 204);
    public static final Color POSITIVE_COLOR = new Color(0, 178, 86);
    public static final Color HIGHLIGHTED_BACKGROUND_COLOR = new Color(69, 115, 23);
    public static final Color HIGHLIGHTED_BACKGROUND_LIGHT_COLOR = new Color(245, 255, 235);
    public static final Color NEGATIVE_COLOR = new Color(215, 35, 15);
    public static final Color NEGATIVE_COLOR_LIGHT = new Color(255, 236, 234);
    public static final Color WARNING_COLOR = new Color(215, 35, 15);
    public static final Color WARNING_COLOR_LIGHT = new Color(255, 195, 175);
    public static final Color NEUTRAL_COLOR = new Color(60, 60, 60);
    public static final Color NEUTRAL2_COLOR = new Color(140, 140, 140);
    public static final Color NEUTRAL3_COLOR = new Color(100, 100, 100);
    public static final Color NEUTRAL_LIGHT_COLOR = new Color(200, 200, 200);
    public static final Color NEUTRAL_LIGHTEST_COLOR = new Color(215, 215, 215);
    public static final Color NEUTRAL_LIGHTEST2_COLOR = new Color(230, 230, 230);
    public static final Color CONFIDENCE_COLOR = new Color(202, 229, 202);
    public static final Color RED_COLOR;
    public static final Color YELLOW_COLOR;
    public static final Color YELLOW_COLOR_LIGHT;
    public static final Color GREEN_COLOR;
    public static final Color TABLE_HEADER_COLOR;
    public static final Color TABLE_HEADER_DISTRIBUTION_COLOR;
    public static final Color TABLE_ALTERNATIVE1_COLOR;
    public static final Color TABLE_ALTERNATIVE1_COLOR_LIGHT;
    public static final Color TABLE_ALTERNATIVE2_COLOR;
    public static final Color TABLE_ALTERNATIVE2_COLOR_LIGHT;
    public static final Color TABLE_ALTERNATIVE3_COLOR;
    public static final Color TABLE_ALTERNATIVE3_COLOR_LIGHT;
    public static final String HIGHLIGHT_COLOR_HEX;
    public static final String NEUTRAL_COLOR_HEX;
    public static final String NEUTRAL2_COLOR_HEX;
    public static final String NEUTRAL3_COLOR_HEX;
    public static final String[] CHART_COLOR_PALETTE;
//    private static ColorProvider RM_COLOR_PROVIDER;
    public static final RenderingHints RENDERING_HINTS;

    public ChartTools() {
    }

    public static String toHex(Color color) {
        StringBuilder hex = new StringBuilder(Integer.toHexString(color.getRGB() & 16777215));

        while(hex.length() < 6) {
            hex.insert(0, "0");
        }

        return "#" + hex;
    }

//    public static Color getClusterColor(int clusterIndex, int numberOfClusters) {
//        double normalized = (double)clusterIndex / (double)(numberOfClusters - 1);
//        if (RM_COLOR_PROVIDER == null) {
//            RM_COLOR_PROVIDER = new ColorProvider(false);
//        }
//
//        return reduceColorBrightness(RM_COLOR_PROVIDER.getPointColor(normalized, 255));
//    }
//
//    public static Color getCategoryColor(int category, int numberOfCategories) {
//        if (RM_COLOR_PROVIDER == null) {
//            RM_COLOR_PROVIDER = new ColorProvider(false);
//        }
//
//        return numberOfCategories <= 1 ? reduceColorBrightness(RM_COLOR_PROVIDER.getPointColor(1.0D, 255)) : reduceColorBrightness(RM_COLOR_PROVIDER.getPointColor((double)category / (double)(numberOfCategories - 1), 255));
//    }

    private static Color reduceColorBrightness(Color color) {
        float[] hsb = new float[3];
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        Color.RGBtoHSB(r, g, b, hsb);
        hsb[2] *= 0.75F;
        hsb[1] *= 0.95F;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    public static ChartPanel createChartPanel(JFreeChart chart) {
        return createChartPanel(chart, Color.WHITE, false, false);
    }

    public static ChartPanel createChartPanel(JFreeChart chart, Color background) {
        return createChartPanel(chart, background, false, false);
    }

    public static ChartPanel createChartPanel(JFreeChart chart, Color background, boolean zoomable, boolean toolTips) {
        chart.setBackgroundPaint(background);
        chart.setBackgroundImageAlpha(0.0F);
        ChartPanel chartPanel = new ChartPanel(chart);
        if (!zoomable) {
            chartPanel.setPopupMenu((JPopupMenu)null);
        }

        chartPanel.setDomainZoomable(zoomable);
        chartPanel.setRangeZoomable(zoomable);
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMaximumDrawWidth(3000);
        chartPanel.setBackground(background);
        if (!toolTips) {
            chartPanel.getChartRenderingInfo().setEntityCollection((EntityCollection)null);
        }

        return chartPanel;
    }

    public static CategoryDataset createBarDataset(List<ValueAndCount> nominalValues, int maxCount) {
        List<ValueAndCount> listOfBarValues = (List)nominalValues.stream().limit((long)maxCount).collect(Collectors.toList());
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Iterator var4 = listOfBarValues.iterator();

        while(var4.hasNext()) {
            ValueAndCount value = (ValueAndCount)var4.next();
            dataset.addValue((double)value.getCount(), "", value.getValue());
        }

        return dataset;
    }

//    public static CategoryDataset createDoubleBarDataset(List<KeyAndValue> valuePairs, int maxCount) {
//        List<KeyAndValue> listOfBarValues = (List)valuePairs.stream().limit((long)maxCount).collect(Collectors.toList());
//        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
//        Iterator var4 = listOfBarValues.iterator();
//
//        while(var4.hasNext()) {
//            KeyAndValue pair = (KeyAndValue)var4.next();
//            dataset.addValue(pair.getValue(), "", pair.getKey());
//        }
//
//        return dataset;
//    }

    public static HistogramDataset createHistogramDataset(ExampleSet exampleSet, Attribute attribute) {
        HistogramDataset dataset = new HistogramDataset();
        double[] array = new double[exampleSet.size()];
        int count = 0;
        Iterator var5 = exampleSet.iterator();

        while(var5.hasNext()) {
            Example example = (Example)var5.next();
            double value = example.getValue(attribute);
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                array[count++] = value;
            }
        }

        if (count > 0) {
            if (count < array.length) {
                array = Arrays.copyOf(array, count);
            }

            dataset.addSeries(attribute.getName(), array, Math.min(array.length, 10));
        }

        return dataset;
    }

    public static HistogramDataset createHistogramDataset(ExampleSet exampleSet, Attribute attribute, boolean[] sampleSelection) {
        HistogramDataset dataset = new HistogramDataset();
        double[] array = new double[exampleSet.size()];
        int count = 0;
        int exampleCounter = 0;
        Iterator var7 = exampleSet.iterator();

        while(var7.hasNext()) {
            Example example = (Example)var7.next();
            if (sampleSelection[exampleCounter++]) {
                double value = example.getValue(attribute);
                if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                    array[count++] = value;
                }
            }
        }

        if (count > 0) {
            if (count < array.length) {
                array = Arrays.copyOf(array, count);
            }

            dataset.addSeries(attribute.getName(), array, Math.min(array.length, 10));
        }

        return dataset;
    }

//    public static JEditorPane createWarningText(JComponent owner, String text) {
//        JEditorPane warningText = new JEditorPane((new ExtendedHTMLEditorKit()).getContentType(), text);
//        warningText.setEditable(false);
//        warningText.setBackground(owner.getBackground());
//        warningText.setBorder((Border)null);
//        return warningText;
//    }

    public static void setDefaultChartFonts(JFreeChart chart) {
        ChartTheme chartTheme = StandardChartTheme.createJFreeTheme();
        if (StandardChartTheme.class.isAssignableFrom(chartTheme.getClass())) {
            StandardChartTheme standardTheme = (StandardChartTheme)chartTheme;
//            standardTheme.setExtraLargeFont(CHART_FONT);
//            standardTheme.setLargeFont(CHART_FONT);
//            standardTheme.setRegularFont(CHART_FONT);
//            standardTheme.setSmallFont(CHART_FONT);
            standardTheme.apply(chart);
        }

    }

    /** @deprecated */
    @Deprecated
    private static Date transformDateToUTC(Date date) {
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(date.toInstant());
        return new Date(date.getTime() + (long)(zoneOffset.getTotalSeconds() * 1000));
    }

    static {
        RED_COLOR = NEGATIVE_COLOR;
        YELLOW_COLOR = new Color(255, 194, 30);
        YELLOW_COLOR_LIGHT = new Color(255, 247, 225);
        GREEN_COLOR = POSITIVE_COLOR;
        TABLE_HEADER_COLOR = new Color(74, 106, 149);
        TABLE_HEADER_DISTRIBUTION_COLOR = new Color(225, 230, 236);
        TABLE_ALTERNATIVE1_COLOR = new Color(83, 117, 163);
        TABLE_ALTERNATIVE1_COLOR_LIGHT = new Color(239, 242, 253);
        TABLE_ALTERNATIVE2_COLOR = new Color(47, 155, 150);
        TABLE_ALTERNATIVE2_COLOR_LIGHT = new Color(228, 244, 242);
        TABLE_ALTERNATIVE3_COLOR = new Color(148, 107, 155);
        TABLE_ALTERNATIVE3_COLOR_LIGHT = new Color(251, 241, 253);
        HIGHLIGHT_COLOR_HEX = toHex(HIGHLIGHT_COLOR);
        NEUTRAL_COLOR_HEX = toHex(NEUTRAL_COLOR);
        NEUTRAL2_COLOR_HEX = toHex(NEUTRAL2_COLOR);
        NEUTRAL3_COLOR_HEX = toHex(NEUTRAL3_COLOR);
        CHART_COLOR_PALETTE = new String[]{toHex(new Color(NORMAL_COLOR.getRed(), NORMAL_COLOR.getGreen(), NORMAL_COLOR.getBlue(), 180)), toHex(new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), 180)), toHex(new Color(TABLE_ALTERNATIVE3_COLOR.getRed(), TABLE_ALTERNATIVE3_COLOR.getGreen(), TABLE_ALTERNATIVE3_COLOR.getBlue(), 180)), toHex(new Color(TABLE_ALTERNATIVE2_COLOR.getRed(), TABLE_ALTERNATIVE2_COLOR.getGreen(), TABLE_ALTERNATIVE2_COLOR.getBlue(), 180)), toHex(new Color(TABLE_ALTERNATIVE1_COLOR.getRed(), TABLE_ALTERNATIVE1_COLOR.getGreen(), TABLE_ALTERNATIVE1_COLOR.getBlue(), 180)), toHex(new Color(POSITIVE_COLOR.getRed(), POSITIVE_COLOR.getGreen(), POSITIVE_COLOR.getBlue(), 180)), toHex(new Color(NEGATIVE_COLOR.getRed(), NEGATIVE_COLOR.getGreen(), NEGATIVE_COLOR.getBlue(), 180)), toHex(new Color(YELLOW_COLOR.getRed(), YELLOW_COLOR.getGreen(), YELLOW_COLOR.getBlue(), 180)), toHex(new Color(NORMAL_LIGHT_COLOR.getRed(), NORMAL_LIGHT_COLOR.getGreen(), NORMAL_LIGHT_COLOR.getBlue(), 180)), toHex(new Color(NORMAL_DIMMED_COLOR.getRed(), NORMAL_DIMMED_COLOR.getGreen(), NORMAL_DIMMED_COLOR.getBlue(), 180))};
        RENDERING_HINTS = new RenderingHints((Map)null);
        RENDERING_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RENDERING_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
