package ru.mail.polis.xerocry;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
class Value {
    private final byte[] val;
    private long timestamp;
    private boolean milestone;
    private final boolean deleted;

    Value(byte[] val, long timestamp, boolean milestone, boolean deleted) {
        this.val = val;
        this.timestamp = timestamp;
        this.milestone = milestone;
        this.deleted = deleted;
    }

    static Value fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long timestamp = buffer.getLong();
        boolean deleted = buffer.get() > 0;
        byte[] value = new byte[data.length - Long.BYTES - Byte.BYTES];
        buffer.get(value);
        return new Value(value, timestamp, false, deleted);
    }

    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + Byte.BYTES + val.length);
        buffer.putLong(timestamp);
        buffer.put((byte) (deleted ? 1 : 0));
        buffer.put(val);
        return buffer.array();
    }
}
