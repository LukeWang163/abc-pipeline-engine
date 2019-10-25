package base.operators.h2o;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import base.operators.RapidMiner;
import base.operators.core.license.ProductConstraintManager;
import base.operators.operator.tools.ThreadParameterProvider;
import base.operators.license.LicenseEvent;
import base.operators.license.LicenseManagerListener;
import base.operators.license.LicenseManagerRegistry;
import base.operators.operator.OperatorException;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import base.operators.tools.parameter.ParameterChangeListener;
import hex.ModelMetrics;
import hex.tree.CompressedTree;
import hex.tree.SharedTreeModel;
import hex.tree.TreeVisitor;
import hex.tree.gbm.GBMModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import jsr166y.ForkJoinPool;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import water.AutoBuffer;
import water.DKV;
import water.H2O;
import water.Key;
import water.KeySnapshot;
import water.Keyed;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.util.IcedBitSet;
import water.util.SB;

public class ClusterManager {
    private static final String clusterName = generateClusterName();
    private static final int clusterStartupTimeout = 30000;
    private static final Gson gson = (new GsonBuilder()).serializeNulls().registerTypeAdapter(Key.class, new KeyTypeAdapter()).registerTypeAdapter(ModelMetrics.class, new GenericTypeAdapter()).setPrettyPrinting().serializeSpecialFloatingPointValues().create();
    private static final Object clusterLock = new Object();
    private static volatile boolean isClusterStarted = false;
    public static final boolean DEBUG_ENABLED = false;
    private static volatile Boolean flowEnabled = null;

    public ClusterManager() {
    }

    public static Logger getH2OLogger() {
        return Logger.getLogger("water.default");
    }

    public static void startCluster() {
        synchronized(clusterLock) {
            if (!isClusterStarted) {
//                ParameterService.registerParameterChangeListener(new ParameterChangeListener() {
//                    @Override
//                    public void informParameterSaved() {
//                    }
//
//                    @Override
//                    public void informParameterChanged(String key, String value) {
//                        if ("rapidminer.general.number_of_threads".equals(key)) {
//                            int threads = ThreadParameterProvider.getNumberOfThreads();
//                            H2O.setNThreads(threads);
//                            LogService.getRoot().log(Level.INFO, "Changeing number of threads in embdedded H2O cluster  " + ClusterManager.clusterName + " to " + threads);
//                        }
//
//                    }
//                });
//                int threads = 1;
//                if (LicenseManagerRegistry.INSTANCE.get() != null) {
//                    ProductConstraintManager.INSTANCE.registerLicenseManagerListener(new LicenseManagerListener() {
//                        @Override
//                        public <S, C> void handleLicenseEvent(LicenseEvent<S, C> event) {
//                            if (event.getType() == LicenseEvent.LicenseEventType.ACTIVE_LICENSE_CHANGED || event.getType() == LicenseEvent.LicenseEventType.LICENSE_EXPIRED) {
//                                int threads = ThreadParameterProvider.getNumberOfThreads();
//                                H2O.setNThreads(threads);
//                                LogService.getRoot().log(Level.INFO, "Changeing number of threads in embdedded H2O cluster  " + ClusterManager.clusterName + " to " + threads);
//                            }
//
//                        }
//                    });
//                    threads = ThreadParameterProvider.getNumberOfThreads();
//                }
//
//                H2O.setNThreads(threads);
//                LogService.getRoot().log(Level.INFO, "Starting up embdedded H2O cluster " + clusterName + " with " + threads + " threads.");
                PrintWriter os = null;

                try {
                    File f = File.createTempFile("h2o_log4j", ".properties");
                    f.deleteOnExit();
                    os = new PrintWriter(f, "UTF-8");
                    constructLog4JProperties(os);
                    System.setProperty("h2o.log4j.configuration", f.getAbsolutePath());
                } catch (IOException var14) {
                    LogService.getRoot().warning("Could not initialize H2O logging: " + var14.toString());
                } finally {
                    if (os != null) {
                        os.close();
                    }

                }

                List<String> clusterOptions = new ArrayList();
                String username = System.getProperty("user.name");
                if (username == null) {
                    username = "";
                }

                String u2 = username.replaceAll(" ", "_");
                if (u2.length() == 0) {
                    u2 = "unknown";
                }

                String iceRootDir = H2OUtils.getLogDirPath() + "H2O/";

                try {
                    Path tempDir = Files.createTempDirectory("h2o-" + u2);
                    tempDir.toFile().deleteOnExit();
                    iceRootDir = tempDir.toAbsolutePath().toString();
                } catch (IOException var13) {
                    var13.printStackTrace();
                }

                clusterOptions.addAll(Arrays.asList("-name", clusterName, "-quiet", "-ga_opt_out", "yes", "-md5skip", "-ip", "127.0.0.1", "-web_ip", "127.0.0.1", "-ice_root", iceRootDir));
                if (RapidMiner.getExecutionMode().isHeadless() || !flowEnabled()) {
                    clusterOptions.add("-disable_web");
                }

                H2O.main((String[])clusterOptions.toArray(new String[0]));
                H2O.waitForCloudSize(1, 30000L);
                H2O.registerRestApis(".");
                H2O.finalizeRegistration();
                isClusterStarted = true;
            }

        }
    }

    private static void constructLog4JProperties(PrintWriter os) throws FileNotFoundException {
        String level = "INFO";
        os.println("log4j.logger.water.default=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.water.api.RequestServer=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.org.apache.http=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.com.amazonaws=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.org.apache.hadoop=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.org.jets3t.service=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.org.reflections.Reflections=" + level + ", H2OLog4jAppender");
        os.println("log4j.logger.com.brsanthu.googleanalytics=" + level + ", H2OLog4jAppender");
        os.println("log4j.appender.H2OLog4jAppender=org.apache.log4j.RollingFileAppender");
        os.println("log4j.appender.H2OLog4jAppender.File=" + H2OUtils.getLogDirPath() + "h2o.log");
        os.println("log4j.appender.H2OLog4jAppender.MaxFileSize=15MB");
        os.println("log4j.appender.H2OLog4jAppender.MaxBackupIndex=1");
        os.println("log4j.appender.H2OLog4jAppender.layout=org.apache.log4j.PatternLayout");
        os.println("log4j.appender.H2OLog4jAppender.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n\n");
    }

    public static String generateClusterName() {
        return "h2o-cluster-" + (int)(ThreadLocalRandom.current().nextDouble() * 1000000.0D);
    }

    public static String generateFrameName(String opName) {
        String transformedOpName = opName.replaceAll("\\s", "_").toLowerCase();
        return "h2o-frame-" + transformedOpName + "-" + (int)(ThreadLocalRandom.current().nextDouble() * 1000000.0D);
    }

    public static String generateModelName(String opName) {
        String transformedOpName = opName.replaceAll("\\s", "_").toLowerCase();
        return "h2o-model-" + transformedOpName + "-" + (int)(ThreadLocalRandom.current().nextDouble() * 1000000.0D);
    }

    public static String getClusterName() {
        return clusterName;
    }

    public static boolean isClusterStarted() {
        return isClusterStarted;
    }

    public static void cleanUpCluster() {
        Key[] var0 = KeySnapshot.globalSnapshot().keys();
        int var1 = var0.length;

        for(int var2 = 0; var2 < var1; ++var2) {
            Key<?> key = var0[var2];
            if (!H2ONativeObject.isReferenced(key)) {
                synchronized(clusterLock) {
                    key.remove();
                }
            }
        }

    }

    public static boolean isInCluster(Keyed<?> h2oObject) {
        synchronized(clusterLock) {
            return isClusterStarted && DKV.get(h2oObject._key) != null;
        }
    }

    public static Keyed<?> deserializeIntoClusterFromBinary(byte[] binary) {
        synchronized(clusterLock) {
            startCluster();
            return Keyed.readAll(new AutoBuffer(new ByteArrayInputStream(binary)));
        }
    }

    public static Keyed<?> deserializeIntoClusterFromJson(List<byte[]> compressedJsons, List<String> jsonClasses) throws OperatorException {
        synchronized(clusterLock) {
            startCluster();
            Keyed<?> h2oObject = null;

            for(int i = 0; i < compressedJsons.size(); ++i) {
                byte[] compressedJsonPiece = (byte[])compressedJsons.get(i);
                String jsonClass = (String)jsonClasses.get(i);

                try {
                    Keyed<?> keyed = (Keyed)gson.fromJson(decompress(compressedJsonPiece), Class.forName(jsonClass));
                    DKV.put(keyed);
                    if (h2oObject == null) {
                        h2oObject = keyed;
                    }
                } catch (Exception var9) {
                    throw new OperatorException("Cannot deserialize JSON into the H2O cluster", var9);
                }
            }

            return h2oObject;
        }
    }

    public static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(data.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        byte[] compressedData = out.toByteArray();
        out.close();
        return compressedData;
    }

    public static String decompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        GZIPInputStream gis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(gis, bos);
        gis.close();
        bis.close();
        String data = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        bos.close();
        return data;
    }

    public static void removeFromCluster(Keyed<?>... h2oObjects) {
        Keyed[] var1 = h2oObjects;
        int var2 = h2oObjects.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Keyed<?> h2oObject = var1[var3];
            if (h2oObject != null) {
                synchronized(clusterLock) {
                    if (isClusterStarted) {
                        h2oObject.remove();
                    }
                }
            }
        }

    }

    public static int getNumberOfThreads() {
        return H2O.getNThreads();
    }

    public static void setNumberOfThreads(int n) {
        H2O.setNThreads(n);
    }

    public static void dkvStat() {
        System.out.println("DKV global: " + Arrays.toString(KeySnapshot.globalSnapshot().keys()));
        System.out.println("DKV local: " + Arrays.toString(KeySnapshot.localSnapshot().keys()));
    }

    public static void keyStat(Key<?> key) {
        System.out.println(key + ", user_allowed: " + key.user_allowed() + ", type: " + key.type());
    }

    public static void memStatter() {
        (new Thread() {
            public void run() {
                while(true) {
                    ClusterManager.memStat();

                    try {
                        Thread.sleep(10000L);
                    } catch (InterruptedException var2) {
                    }
                }
            }
        }).start();
    }

    public static void memStat() {
        System.gc();

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException var1) {
        }

        System.out.println("Memory: " + Runtime.getRuntime().freeMemory() / 1048576L + " MB / " + Runtime.getRuntime().totalMemory() / 1048576L + " MB / " + Runtime.getRuntime().maxMemory() / 1048576L + " MB");
    }

    public static void frameStat(Frame frame) {
        System.out.println("Frame: " + frame._key);
        System.out.println("Home node: " + frame._key.home_node());
        System.out.println("Compressed size: " + frame.byteSize());
        System.out.println("Vectors: " + frame.numCols());

        for(int vi = 0; vi < frame.numCols(); ++vi) {
            Vec v = frame.vec(vi);
            System.out.println("\t" + vi + ":");
            System.out.println("\tName: " + frame.name(vi));
            vecStat("\t", v);
        }

        System.out.println("Data: ");
        System.out.println(frame.toString());
    }

    public static void vecStat(String prefix, Vec v) {
        System.out.println(prefix + "Type: " + v.get_type_str());
        System.out.println(prefix + "Key: " + v._key);
        System.out.println(prefix + "Home node: " + v._key.home_node());
        System.out.println(prefix + "Compressed size: " + v.byteSize());
        System.out.println(prefix + "Group: " + v.group());
        System.out.println(prefix + "Chunks: " + v.nChunks());

        for(int ci = 0; ci < v.nChunks(); ++ci) {
            Chunk c = v.chunkForChunkIdx(ci);
            System.out.println(prefix + "\t" + ci + ":");
            chunkStat(prefix + "\t", c);
        }

    }

    public static void chunkStat(String prefix, Chunk c) {
        System.out.println(prefix + "Type: " + c.toString());
        System.out.println(prefix + "Start: " + c.start());
    }

    public static void gbmModelStat(GBMModel gbmModel) {
        System.out.println("Java:");
        System.out.println(gbmModel.toJava(false, true));
        System.out.println("Trees:");

        for(int tnum = 0; tnum < ((GBMModel.GBMOutput)gbmModel._output)._ntrees; ++tnum) {
            for(int knum = 0; knum < ((GBMModel.GBMOutput)gbmModel._output).nclasses(); ++knum) {
                System.out.println("tnum: " + tnum);
                System.out.println("knum: " + knum);
                if (((GBMModel.GBMOutput)gbmModel._output)._treeKeys[tnum][knum] != null) {
                    System.out.println(printTree(((GBMModel.GBMOutput)gbmModel._output).ctree(tnum, knum), (SharedTreeModel.SharedTreeOutput)gbmModel._output));
                }
            }
        }

    }

    public static String printTree(CompressedTree ct, SharedTreeModel.SharedTreeOutput tm) {
        final String[] names = tm._names;
        final SB sb = new SB();
        (new TreeVisitor<RuntimeException>(ct) {
            @Override
            protected void pre(int col, float fcmp, IcedBitSet gcmp, int equal) {
                if (equal == 1) {
                    sb.p("!Double.isNaN(" + sb.i().p(names[col]).p(") && "));
                }

                sb.i().p(names[col]).p(' ');
                if (equal == 0) {
                    sb.p("< ").p(fcmp);
                } else if (equal == 1) {
                    sb.p("!=").p(fcmp);
                } else {
                    sb.p("in ").p(gcmp);
                }

                sb.ii(1).nl();
            }

            @Override
            protected void post(int col, float fcmp, int equal) {
                sb.di(1);
            }

            @Override
            protected void leaf(float pred) {
                sb.i().p("return ").p(pred).nl();
            }
        }).visit();
        return sb.toString();
    }

    public static void poolStat() {
        try {
            Field fjpsField = H2O.class.getDeclaredField("FJPS");
            fjpsField.setAccessible(true);
            Object[] fjps = (Object[])((Object[])fjpsField.get((Object)null));

            for(int i = 0; i < fjps.length; ++i) {
                if (fjps[i] != null) {
                    System.out.println("Priority: " + i);
                    ForkJoinPool fjp = (ForkJoinPool)fjps[i];
                    System.out.println("\tParalelism: " + fjp.getParallelism());
                    System.out.println("\tActiveThreadCount: " + fjp.getActiveThreadCount());
                    System.out.println("\tPoolSize: " + fjp.getPoolSize());
                    System.out.println("\tQueuedTaskCount: " + fjp.getQueuedTaskCount());
                    System.out.println("\tQueuedSubmissionCount: " + fjp.getQueuedSubmissionCount());
                    System.out.println("\tQuiescent: " + fjp.isQuiescent());
                }
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException var4) {
            var4.printStackTrace();
        }

    }

    public static final boolean flowEnabled() {
        if (flowEnabled == null) {
            flowEnabled = "true".equals(System.getProperty("com.rapidminer.h2o.flow"));
        }

        return flowEnabled;
    }

    public static final void disableFlow() {
        flowEnabled = false;
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
