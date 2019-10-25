package base.operators.operator.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InputStreamLogger {
    private InputStreamLogger() {
    }

    public static void log(InputStream stream, Logger logger) {
        Runnable outputReader = () -> {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                Throwable var4 = null;

                try {
                    String line;
                    try {
                        while((line = bufferedReader.readLine()) != null) {
                            logger.log(Level.INFO, line);
                        }
                    } catch (Throwable var14) {
                        var4 = var14;
                        throw var14;
                    }
                } finally {
                    if (bufferedReader != null) {
                        if (var4 != null) {
                            try {
                                bufferedReader.close();
                            } catch (Throwable var13) {
                                var4.addSuppressed(var13);
                            }
                        } else {
                            bufferedReader.close();
                        }
                    }

                }
            } catch (IOException var16) {
            }

        };
        Thread outputReaderThread = new Thread(outputReader);
        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }
}
