package base.operators.operator.timeseries.timeseriesanalysis.window.factory;

public class WindowFactory {
   public static final String NUMBER_OF_WINDOWS_DESCRIPTOR = "number of windows";
   public static final EquidistantWindowFactory equidistant = new EquidistantWindowFactory();
   public static final SlidingWindowFactory slidingWindow = new SlidingWindowFactory();
}
