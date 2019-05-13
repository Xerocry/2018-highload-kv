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
public class GetProcessor extends RequestProcessor {

    private final static String TIMESTAMP_HEADER = "X-Timestamp";

    GetProcessor(Store store, Map<String, HttpClient> replicas, String[] topology, String myReplica) {
        super(store, replicas, topology, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) throws InterruptedException, IOException, HttpException, PoolException {
        String id = ackParms.getId();
        AtomicInteger ackFound = new AtomicInteger();
        AtomicInteger ackNotFound = new AtomicInteger();
        AtomicInteger ackDeleted = new AtomicInteger();

        Map<Long, byte[]> timestampToValue = new HashMap<>();

        if (!ackParms.isNeedRepl()) {
            try {
                Value node = store.getAsValue(id);
//                if (node.isMilestone()) {
//                    Response response = new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
//                    response.addHeader(TIMESTAMP_HEADER + ": " + node.getTimestamp());
//                    return response;
//                } else {
                    Response response = new Response(Response.OK, node.toBytes());
                    response.addHeader(TIMESTAMP_HEADER + ": " + node.getTimestamp());
                    return response;
//                }
            } catch (NoSuchElementException e) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }

        List<String> replicas = getNodes(id, ackParms.getFrom());

        for (String replica : replicas) {
            if (replica.equals(myReplica)) {
                try {
                    timestampToValue.put(store.getAsValue(id).getTimestamp(), store.getAsValue(id).getVal());
                    ackFound.getAndIncrement();
                } catch (NoSuchElementException e) {
                    try {
                        if (store.isDeleted(id)) {
                            ackDeleted.getAndIncrement();
                        } else {
                            ackNotFound.getAndIncrement();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                Response response = this.replicas.get(replica).get("/v0/entity?id=" + id, NEED_REPL_HEADER + ": 1");
                switch (response.getStatus()) {
                    case 200:
                        Value value = Value.fromBytes(response.getBody());
                        timestampToValue.put(value.getTimestamp(), value.getVal());
                        ackFound.getAndIncrement();
                        break;
                    case 404:
                        if (store.isDeleted(id)) {
                            ackDeleted.getAndIncrement();
                        } else {
                            ackNotFound.getAndIncrement();
                        }
                        break;
                }
            }
        }

        if (ackNotFound.get() + ackDeleted.get() + ackFound.get() < ackParms.getAck()) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        } else if (ackFound.get() >= ackParms.getAck() && ackDeleted.get() == 0) {
            byte[] maxTimestampValue = timestampToValue.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getKey)).get().getValue();
            return Response.ok(maxTimestampValue);
        } else {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

}
