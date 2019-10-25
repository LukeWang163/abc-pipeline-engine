package base.operators.operator.timeseries.timeseriesanalysis.demo;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.awt.Dimension;
import java.time.Instant;
import java.util.ArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.SeriesException;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class SeriesChart extends ApplicationFrame {
   private static final long serialVersionUID = 8600867453126620614L;

   private SeriesChart(String heading, String title, String xlabel, String ylabel, Number[] x, Number[] y, boolean timeSeries) {
      super(heading);
      XYDataset dataset = this.createDataset(x, y);
      JFreeChart chart;
      if (timeSeries) {
         chart = this.createTimeSeriesChart(dataset, title, xlabel, ylabel);
      } else {
         chart = this.createValueSeriesChart(dataset, title, xlabel, ylabel);
      }

      ChartPanel panel = new ChartPanel(chart);
      panel.setPreferredSize(new Dimension(600, 400));
      panel.setMouseZoomable(true, false);
      this.setContentPane(panel);
   }

   public static SeriesChart create(String heading, String title, String xlabel, String ylabel, ValueSeries valueSeries) {
      if (heading != null && title != null && xlabel != null && ylabel != null) {
         if (valueSeries == null) {
            throw new ArgumentIsNullException("value series");
         } else {
            Number[] x = new Double[valueSeries.getLength()];
            Number[] y = new Double[valueSeries.getLength()];
            double[] indices = valueSeries.getIndices();
            double[] values = valueSeries.getValues();

            for(int i = 0; i < valueSeries.getLength(); ++i) {
               x[i] = indices[i];
               y[i] = values[i];
            }

            return new SeriesChart(heading, title, xlabel, ylabel, x, y, false);
         }
      } else {
         throw new ArgumentIsNullException("label (one of heading, title, xlabel, ylabel)");
      }
   }

   public static SeriesChart create(String heading, String title, String xlabel, String ylabel, TimeSeries timeSeries) {
      if (heading != null && title != null && xlabel != null && ylabel != null) {
         if (timeSeries == null) {
            throw new ArgumentIsNullException("time series");
         } else {
            Number[] x = new Long[timeSeries.getLength()];
            Number[] y = new Double[timeSeries.getLength()];
            ArrayList indices = timeSeries.getIndices();
            double[] values = timeSeries.getValues();

            for(int i = 0; i < timeSeries.getLength(); ++i) {
               x[i] = ((Instant)indices.get(i)).toEpochMilli();
               y[i] = values[i];
            }

            return new SeriesChart(heading, title, xlabel, ylabel, x, y, true);
         }
      } else {
         throw new ArgumentIsNullException("label (one of heading, title, xlabel, ylabel)");
      }
   }

   public void plot() {
      this.pack();
      RefineryUtilities.positionFrameRandomly(this);
      this.setVisible(true);
   }

   private XYDataset createDataset(Number[] x, Number[] y) {
      XYSeries series = new XYSeries("Time Series Data");

      for(int i = 0; i < x.length; ++i) {
         try {
            series.add(x[i], y[i]);
         } catch (SeriesException var6) {
            System.err.println("Error adding to series");
            var6.printStackTrace();
         }
      }

      return new XYSeriesCollection(series);
   }

   private JFreeChart createTimeSeriesChart(XYDataset dataset, String title, String xlabel, String ylabel) {
      return ChartFactory.createTimeSeriesChart(title, xlabel, ylabel, dataset, false, false, false);
   }

   private JFreeChart createValueSeriesChart(XYDataset dataset, String title, String xlabel, String ylabel) {
      return ChartFactory.createXYLineChart(title, xlabel, ylabel, dataset, PlotOrientation.VERTICAL, false, false, false);
   }
}
