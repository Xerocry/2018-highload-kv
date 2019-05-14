package ru.mail.polis.xerocry;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Store implements KVDao {

    private final File path;

    public Store(File dir) {
        path = dir;
    }

    @Override
    public void close() {
    }

    private String checkKey(String key) throws IOException {
        final Pattern pattern = Pattern.compile("\\w*", Pattern.UNICODE_CHARACTER_CLASS);
        final Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) return key;
        else throw new IOException("Incorrect key: " + key);
    }

    boolean isDeleted(String key) throws IOException {
        try {
            final File file = new File(path, key);
            Files.readAllBytes(file.toPath());
            final FileInputStream stream = new FileInputStream(file);
            Value valueWrapped = Value.fromBytes(Util.getData(stream));
            stream.close();
            return valueWrapped.isDeleted();
        } catch (NoSuchFileException e) {
            return false;
        }
    }

    Value getAsValue(String key) throws NoSuchElementException {
        try {
            final File file = new File(path, checkKey(key));
            if (file.exists()) {
                try (final FileInputStream stream = new FileInputStream(file)) {
                    Value value = Value.fromBytes(Util.getData(stream));
                    stream.close();
                    if (value.isDeleted()) {
                        throw new NoSuchElementException();
                    }
                    return value;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new NoSuchElementException("No such element");
    }

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] key) throws NoSuchElementException, IOException {
        return getAsValue(new String(key, StandardCharsets.UTF_8)).getVal();
    }


    public void upsert(@NotNull byte[] key, @NotNull byte[] val) throws IOException {
        try {
            Value value = new Value(val, System.currentTimeMillis(), false);
            final File file = new File(path, checkKey(new String(key, StandardCharsets.UTF_8)));
            final FileOutputStream stream = new FileOutputStream(file);
            stream.write(value.toBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(@NotNull byte[] key) throws IOException {
        try {
        Value value = new Value(new byte[0], System.currentTimeMillis(), true);
        final File file = new File(path, checkKey(new String(key, StandardCharsets.UTF_8)));
        final FileOutputStream stream = new FileOutputStream(file);
        stream.write(value.toBytes());
        stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
