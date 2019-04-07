package ru.mail.polis.xerocry;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Util {

    private final static int BUF_SIZE = 1024;


    public static byte[] getData(final InputStream is) {
        final byte buf[] = new byte[BUF_SIZE];

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()){
            int j;

            while ((j = is.read(buf)) >= 0) {
                os.write(buf, 0, j);
            }

            return os.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }
}