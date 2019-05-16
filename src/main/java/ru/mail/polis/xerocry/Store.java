package ru.mail.polis.xerocry;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class Store implements KVDao {

    private final DB db;

    public Store(File dir) throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        db = Iq80DBFactory.factory.open(dir, options);
    }

    @Override
    public void close() {
    }

    boolean isDeleted(String key) {
        Value value = Value.fromBytes(db.get(key.getBytes()));
        if (value.getVal() == null) {
            throw new NoSuchElementException();
        } else return value.isDeleted();
    }

    Value getAsValue(String key) throws NoSuchElementException {
        byte[] value = db.get(key.getBytes());
        if (value == null) {
            throw new NoSuchElementException("No such element");
        }
        return Value.fromBytes(value);
    }

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] key) throws NoSuchElementException {
        return getAsValue(new String(key, StandardCharsets.UTF_8)).getVal();
    }


    public void upsert(@NotNull byte[] key, @NotNull byte[] val) {
        Value value = new Value(val, System.currentTimeMillis(), false);
        db.put(key, value.toBytes());
    }

    @Override
    public void remove(@NotNull byte[] key) {
        Value value = new Value(new byte[0], System.currentTimeMillis(), true);
        db.put(key, value.toBytes());
    }
}
