package base.operators.operator.scripting.python;

import base.operators.operator.nio.file.BufferedFileObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PythonNativeObject extends BufferedFileObject {
    public static final long MAX_FILE_SIZE = 2147483639L;

    public PythonNativeObject(File file) throws IOException {
        super(fileToBuffer(file));
    }

    private static byte[] fileToBuffer(File file) throws IOException {
        Path path = file.toPath();
        long fileSize = Files.size(path);
        if (fileSize > 2147483639L) {
            throw new IOException("The file is too big. You are only allowed to buffer maximal 2147483639 bytes. Your object has a size of " + fileSize + " bytes.");
        } else {
            return Files.readAllBytes(path);
        }
    }
}

