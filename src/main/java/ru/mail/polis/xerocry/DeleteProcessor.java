package ru.mail.polis.xerocry;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DeleteProcessor extends RequestProcessor {

    DeleteProcessor(Store store, Map<String, HttpClient> replicas, String myReplica) {
        super(store, replicas, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) throws IOException {
        String id = ackParms.getId();
        int ack = 0;

        if (!ackParms.isNeedRepl()) {
            try {
                store.remove(id.getBytes());
            } catch (Exception e) {
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }

        List<String> replicas = getNodes(id, ackParms.getAck());

        for (String replica : replicas) {
            try {
                if (myReplica.equals(replica)) {
                    store.remove(id.getBytes());
                    ack++;
                } else if (deleteRequest(this.replicas.get(replica), id).getStatus() == 202) {
                    ack++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ack >= ackParms.getAck()
                ? new Response(Response.ACCEPTED, Response.EMPTY)
                : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    private Response deleteRequest(HttpClient client, String key) throws IOException {
        try {
            return client.delete("/v0/entity?id=" + key, NEED_REPL_HEADER + ": 1");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

}
