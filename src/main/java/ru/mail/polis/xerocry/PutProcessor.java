package ru.mail.polis.xerocry;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PutProcessor extends RequestProcessor {

    PutProcessor(Store store, Map<String, HttpClient> replicas, String[] topology, String myReplica) {
        super(store, replicas, topology, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) {

        String id = ackParms.getId();
        byte[] value = request.getBody();
        AtomicInteger ack = new AtomicInteger();

        if (!ackParms.isNeedRepl()) {
            store.put(id, value);
            return new Response(Response.CREATED, Response.EMPTY);
        }
        List<String> replicas = getNodes(id, ackParms.getFrom());

        for (String replica : replicas) {
            try {
                if (myReplica.equals(replica)) {
                    store.put(id, value);
                    ack.getAndIncrement();
                } else if (this.replicas.get(replica).put("/v0/entity?id=" + id, value, NEED_REPL_HEADER + ": 1").getStatus() == 201) {
                    ack.getAndIncrement();
                }
            } catch (IOException | InterruptedException | HttpException | PoolException e) {
                e.printStackTrace();
            }

        }

        return ack.get() >= ackParms.getAck()
                ? new Response(Response.CREATED, Response.EMPTY)
                : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

}