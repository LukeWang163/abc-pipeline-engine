package base.operators.h2o;

import base.operators.Process;
import base.operators.ProcessStateListener;
import base.operators.RapidMiner;
import base.operators.operator.Operator;
import base.operators.tools.FileSystemService;
import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import water.Job;

public class H2OUtils {
    public static final int MAX_EX_MSG_LENGTH = 230;
    private static final Pattern versionRegexp = Pattern.compile("(?<h2obaseversion>\\d+\\.\\d+\\.\\d+\\.\\d+)\\.RAPIDMINER(|\\.(?<patchversion>\\d+))(|-.*)");

    public static void addProcessStopListener(Operator op, final AtomicBoolean stopRequest, final Job<?> job) {
        if (op != null) {
            op.getProcess().addProcessStateListener(new ProcessStateListener()
            {
                @Override
                public void stopped(Process process)
                {
                    stopRequest.set(true);
                    job.stop();
                }

                @Override
                public void started(Process process) {}

                @Override
                public void resumed(Process process) {}

                @Override
                public void paused(Process process) {}
            });
        }
    }

    public static String extractH2OBaseVersion(String version) {
        Matcher m = versionRegexp.matcher(version);
        if (!m.matches()) {
            return null;
        }
        return m.group("h2obaseversion");
    }

    public static String extractRMPatchVersion(String version) {
        Matcher m = versionRegexp.matcher(version);
        if (!m.matches()) {
            return null;
        }
        return m.group("patchversion");
    }

    public static boolean isVersionCompatible(String aVersion, String bVersion) {
        if (aVersion == null || bVersion == null) {
            return false;
        }

        String aH2OBaseVersion = extractH2OBaseVersion(aVersion);
        String bH2OBaseVersion = extractH2OBaseVersion(bVersion);

        return (aH2OBaseVersion != null && aH2OBaseVersion.equals(bH2OBaseVersion));
    }

    private static final Set<RapidMiner.ExecutionMode> SERVER_TYPE_EXECUTION_MODES = EnumSet.of(RapidMiner.ExecutionMode.APPSERVER, RapidMiner.ExecutionMode.JOB_CONTAINER);

    public static boolean isRunningOnServer() {
        return SERVER_TYPE_EXECUTION_MODES.contains(RapidMiner.getExecutionMode());
    }

    public static File getLogDir() {
        return (!isRunningOnServer() || System.getProperty("jboss.server.log.dir") == null) ?
                FileSystemService.getUserRapidMinerDir() : new File(System.getProperty("jboss.server.log.dir"));
    }

    public static String getLogDirPath() {
        return getLogDir().toString().replace(File.separatorChar, '/') + "/";
    }
}
