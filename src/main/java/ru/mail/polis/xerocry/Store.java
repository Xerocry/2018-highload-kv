package ru.mail.polis.xerocry;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Store {
    private final File path;

    public Store(File dir){
        path = dir;
    }

    public byte[] get(String key) throws NoSuchElementException {
        try {
            final File file = new File(path, key);
            if (file.exists()) {
                try (final FileInputStream stream = new FileInputStream(file)) {
                    return Util.getData(stream);
                }
            } else return null;

        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new NoSuchElementException("No such element");
    }

    public void put(String key, byte[] value) {
        try {
            final File file = new File(path, checkKey(key));
            final FileOutputStream stream = new FileOutputStream(file);
            stream.write(value);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(String key) {
        final File file = new File(path, key);
        file.delete();
    }

    private String checkKey(String key) throws IOException {
        final Pattern pattern = Pattern.compile("\\w*");
        final Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) return key;
        else throw new IOException("Incorrect key: " + key);
    }
}
