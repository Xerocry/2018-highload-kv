package ru.mail.polis.xerocry;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Store {
    private final File path;

    public Store(File dir) {
        path = dir;
    }

//    public byte[] get(String key) throws NoSuchElementException {
//        try {
//            final File file = new File(path, key);
//            if (file.exists()) {
//                try (final FileInputStream stream = new FileInputStream(file)) {
//                    return Util.getData(stream);
//                }
//            } else return null;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        throw new NoSuchElementException("No such element");
//    }

    void put(String key, @NotNull byte[] val) {
        try {
            Value value = new Value(val, System.currentTimeMillis(), false, false);
            final File file = new File(path, checkKey(key));
            final FileOutputStream stream = new FileOutputStream(file);
            byte[] tempAr = value.toBytes();
            stream.write(tempAr);
//            stream.write(value.toBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void delete(String key) throws IOException {
        Value value = new Value(new byte[0], System.currentTimeMillis(), false, true);
        final File file = new File(path, key);
        final FileOutputStream stream = new FileOutputStream(file);
        stream.write(value.toBytes());
        stream.close();
//        file.delete();
    }

    private String checkKey(String key) throws IOException {
        final Pattern pattern = Pattern.compile("\\w*");
        final Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) return key;
        else throw new IOException("Incorrect key: " + key);
    }

    boolean isDeleted(String key) throws IOException {
        final File file = new File(path, key);
        final FileInputStream stream = new FileInputStream(file);
        Value valueWrapped = Value.fromBytes(Util.getData(stream));
        return valueWrapped.isDeleted();
    }


    Value getAsValue(String key) throws NoSuchElementException {
        try {
            final File file = new File(path, key);
            if (file.exists()) {
                try (final FileInputStream stream = new FileInputStream(file)) {
                    Value value = Value.fromBytes(Util.getData(stream));
                    if (value.isDeleted()) {
                        throw new NoSuchElementException();
                    }
                    return value;
                }
            } else return null;

        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new NoSuchElementException("No such element");

    }

}
