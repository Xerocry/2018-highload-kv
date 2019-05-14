package ru.mail.polis.xerocry;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.HttpClient;
import one.nio.http.Request;
import one.nio.http.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PutProcessor extends RequestProcessor {

    PutProcessor(Store store, Map<String, HttpClient> replicas, String myReplica) {
        super(store, replicas, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) throws IOException {

        String id = ackParms.getId();
        byte[] value = request.getBody();
        AtomicInteger ack = new AtomicInteger();

        if (!ackParms.isNeedRepl()) {
            store.upsert(id.getBytes(), value);
            return new Response(Response.CREATED, Response.EMPTY);
        }
        List<String> replicas = getNodes(id, ackParms.getFrom());

        for (String replica : replicas) {
            try {
                if (myReplica.equals(replica)) {
                    store.upsert(id.getBytes(), value);
                    ack.getAndIncrement();
                } else if (putRequest(this.replicas.get(replica), value, id).getStatus() == 201) {
                    ack.getAndIncrement();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return ack.get() >= ackParms.getAck()
                ? new Response(Response.CREATED, Response.EMPTY)
                : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    private Response putRequest(HttpClient client, byte[] value, String key) throws IOException {
        try {
            return client.put("/v0/entity?id=" + key, value,NEED_REPL_HEADER + ": 1");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

}