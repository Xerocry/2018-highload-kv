package ru.mail.polis.xerocry;

import lombok.Data;
import one.nio.http.Request;

@Data
class AcknowledgeRequest {
    private final static String NEED_REPL_HEADER = "X-Need-Repl";
    private static Boolean needRepl;
    private final int ack;
    private final int from;
    private final String id;

    private AcknowledgeRequest(String id, int ack, int from, boolean needRepl) {
        this.ack = ack;
        this.from = from;
        this.id = id;
        this.needRepl = needRepl;
    }

    static AcknowledgeRequest fromRequest(Request request, int count, String id, String replicas) throws IllegalArgumentException {
        String needReplHeader = request.getHeader(NEED_REPL_HEADER);
        needRepl = needReplHeader == null;
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException();
        } else if (replicas != null) {
            String[] replicasParts = replicas.split("/");
            if (replicasParts.length == 2) {
                try {
                    int ack = Integer.valueOf(replicasParts[0]);
                    int from = Integer.valueOf(replicasParts[1]);

                    if (from > count || ack < 1 || ack > from) {
                        throw new IllegalArgumentException();
                    }

                    return new AcknowledgeRequest(id, ack, from, needRepl);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            return new AcknowledgeRequest(id, count / 2 + 1, count, needRepl);
        }
    }

    static Boolean isNeedRepl() {
        return needRepl;
    }
}
