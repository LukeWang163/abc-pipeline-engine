package base.operators.operator.timeseries.timeseriesanalysis.demo;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.gson.Gson;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfDouble;
import java.util.stream.DoubleStream;

public abstract class SeriesIO {
   public static final String US_ASCII_STRING = "US_ASCII";

   public static ValueSeries readValueSeriesFromJSON(String filePath) throws IOException {
      String jsonString = readFile(filePath, StandardCharsets.UTF_8);
      Gson gson = new Gson();
      return (ValueSeries)gson.fromJson(jsonString, ValueSeries.class);
   }

   public static TimeSeries readTimeSeriesFromJSON(String filePath) throws IOException {
      String jsonString = readFile(filePath, Charset.forName("US_ASCII"));
      Gson gson = new Gson();
      return (TimeSeries)gson.fromJson(jsonString, TimeSeries.class);
   }

   public static void writeValueSeriesToJSON(String filePath, ValueSeries valueSeries) throws IOException {
      Gson gson = new Gson();
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.forName("US_ASCII"));
      Throwable var4 = null;

      try {
         String line = gson.toJson((Object)valueSeries);
         writer.write(line, 0, line.length());
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (writer != null) {
            if (var4 != null) {
               try {
                  writer.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               writer.close();
            }
         }

      }

   }

   public static void writeTimeSeriesToJSON(String filePath, TimeSeries timeSeries) throws IOException {
      Gson gson = new Gson();
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.forName("US_ASCII"));
      Throwable var4 = null;

      try {
         String line = gson.toJson((Object)timeSeries);
         writer.write(line, 0, line.length());
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (writer != null) {
            if (var4 != null) {
               try {
                  writer.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               writer.close();
            }
         }

      }

   }

   public static ValueSeries readValueSeriesFromCSV(String filePath, char separator, int skipFirstNRows) throws IOException {
      if (!(new File(filePath)).isAbsolute()) {
         filePath = ClassLoader.getSystemResource(filePath).getPath();
      }

      CSVReader csvReader = new CSVReader(new FileReader(filePath), separator);
      Throwable var4 = null;

      ValueSeries var23;
      try {
         List rows = csvReader.readAll();
         if (rows == null) {
            throw new IOException("The provided csv file is empty");
         }

         if (skipFirstNRows >= rows.size()) {
            throw new IOException("The number of skipped rows is greater or equal the total number of rows.");
         }

         for(int i = 0; i < skipFirstNRows; ++i) {
            rows.remove(0);
         }

         double[] indices = null;
         if (((String[])rows.get(0)).length > 1) {
            indices = new double[rows.size()];
         }

         double[] values = new double[rows.size()];

         try {
            for(int i = 0; i < rows.size(); ++i) {
               if (indices != null) {
                  indices[i] = Double.parseDouble(((String[])rows.get(i))[0]);
                  values[i] = Double.parseDouble(((String[])rows.get(i))[1]);
               } else {
                  values[i] = Double.parseDouble(((String[])rows.get(i))[0]);
               }
            }
         } catch (NumberFormatException var19) {
            throw new IOException("The provided csv file contains values that can not be parsed to double.");
         }

         if (indices != null) {
            var23 = ValueSeries.create(indices, values);
            return var23;
         }

         var23 = ValueSeries.create(values);
      } catch (Throwable var20) {
         var4 = var20;
         throw var20;
      } finally {
         if (csvReader != null) {
            if (var4 != null) {
               try {
                  csvReader.close();
               } catch (Throwable var18) {
                  var4.addSuppressed(var18);
               }
            } else {
               csvReader.close();
            }
         }

      }

      return var23;
   }

   public static void writeValueSeriesToCSV(ValueSeries valueSeries, String filePath, char separator) throws IOException {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         CSVWriter writer = new CSVWriter(new FileWriter(filePath), separator, '\u0000', '\u0000', System.getProperty("line.separator"));
         OfDouble indicesStream = DoubleStream.of(valueSeries.getIndices()).iterator();
         OfDouble valuesStream = DoubleStream.of(valueSeries.getValues()).iterator();
         String[] row = new String[2];

         while(indicesStream.hasNext() && valuesStream.hasNext()) {
            row[0] = String.valueOf(indicesStream.next());
            row[1] = String.valueOf(valuesStream.next());
            writer.writeNext(row);
         }

         writer.close();
      }
   }

   public static TimeSeries readTimeSeriesFromCSV(String filePath, char separator, int skipFirstNRows, DateTimeFormatter dateTimeFormatter) throws IOException {
      if (!(new File(filePath)).isAbsolute()) {
         filePath = ClassLoader.getSystemResource(filePath).getPath();
      }

      CSVReader csvReader = new CSVReader(new FileReader(filePath), separator);
      Throwable var5 = null;

      TimeSeries var23;
      try {
         List rows = csvReader.readAll();
         if (rows == null) {
            throw new IOException("The provided csv file is empty");
         }

         if (skipFirstNRows >= rows.size()) {
            throw new IOException("The number of skipped rows is greater or equal the total number of rows.");
         }

         for(int i = 0; i < skipFirstNRows; ++i) {
            rows.remove(0);
         }

         if (((String[])rows.get(0)).length <= 1) {
            throw new IOException("The provided csv file contains only one column, whereas one time and one value colum are required");
         }

         ArrayList indices = new ArrayList();
         double[] values = new double[rows.size()];

         try {
            for(int i = 0; i < rows.size(); ++i) {
               LocalDate tempDate = LocalDate.parse(((String[])rows.get(i))[0], dateTimeFormatter);
               indices.add(tempDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
               values[i] = Double.parseDouble(((String[])rows.get(i))[1]);
            }
         } catch (NumberFormatException | DateTimeParseException var19) {
            throw new IOException("The provided csv file contains values that can not be parsed to Instant or double.");
         }

         var23 = TimeSeries.create(indices, values);
      } catch (Throwable var20) {
         var5 = var20;
         throw var20;
      } finally {
         if (csvReader != null) {
            if (var5 != null) {
               try {
                  csvReader.close();
               } catch (Throwable var18) {
                  var5.addSuppressed(var18);
               }
            } else {
               csvReader.close();
            }
         }

      }

      return var23;
   }

   public static void writeTimeSeriesToCSV(TimeSeries timeSeries, String filePath, char separator, DateTimeFormatter dateTimeFormatter) throws IOException {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         CSVWriter writer = new CSVWriter(new FileWriter(filePath), separator, '\u0000', '\u0000', System.getProperty("line.separator"));
         Iterator indicesStream = timeSeries.getIndices().iterator();
         OfDouble valuesStream = DoubleStream.of(timeSeries.getValues()).iterator();
         String[] row = new String[2];

         while(indicesStream.hasNext() && valuesStream.hasNext()) {
            row[0] = dateTimeFormatter.withZone(ZoneId.systemDefault()).format((TemporalAccessor)indicesStream.next());
            row[1] = String.valueOf(valuesStream.next());
            writer.writeNext(row);
         }

         writer.close();
      }
   }

   private static String readFile(String path, Charset encoding) throws IOException {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
   }
}
