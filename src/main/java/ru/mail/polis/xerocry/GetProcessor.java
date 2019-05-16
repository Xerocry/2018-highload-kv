package ru.mail.polis.xerocry;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.HttpClient;
import one.nio.http.Request;
import one.nio.http.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GetProcessor extends RequestProcessor {

    GetProcessor(Store store, Map<String, HttpClient> replicas, String myReplica) {
        super(store, replicas, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) throws IOException {
        String id = ackParms.getId();
        AtomicInteger ackFound = new AtomicInteger();
        AtomicInteger ackNotFound = new AtomicInteger();
        AtomicInteger ackDeleted = new AtomicInteger();

        Map<Long, byte[]> timestampToValue = new HashMap<>();

        if (!ackParms.isNeedRepl()) {
            try {
                return new Response(Response.OK, store.getAsValue(id).toBytes());
            } catch (NoSuchElementException e) {
                if (store.isDeleted(id)) {
                    return new Response(Response.FORBIDDEN, Response.EMPTY);
                } else return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }

        List<String> replicas = getNodes(id, ackParms.getFrom());

        for (String replica : replicas) {
            try {
                if (replica.equals(myReplica)) {
                    try {
                        timestampToValue.put(store.getAsValue(id).getTimestamp(), store.get(id.getBytes()));
                        ackFound.incrementAndGet();
                    } catch (NoSuchElementException e) {
                        try {
                            if (store.isDeleted(id)) {
                                ackDeleted.incrementAndGet();
                            } else {
                                ackNotFound.incrementAndGet();
                            }
                        } catch (NoSuchElementException e1) {
                            ackNotFound.incrementAndGet();
                        }
                        ackFound.incrementAndGet();
                    }
                } else {
                    Response response = getRequest(this.replicas.get(replica), id);
                    switch (response.getStatus()) {
                        case 200:
                            Value value = Value.fromBytes(response.getBody());
                            timestampToValue.put(value.getTimestamp(), value.getVal());
                            break;
                        case 404:
                            ackNotFound.incrementAndGet();
                            break;
                        case 403:
                            ackDeleted.getAndIncrement();
                            break;
                        default:
                            log.info("GET def" + ":" + response.getStatus());
                            break;
                    }
                    ackFound.incrementAndGet();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (ackFound.get() >= ackParms.getAck()) {
            if (ackNotFound.get() == ackFound.get() || ackDeleted.get() > 0) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } else if (timestampToValue.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getKey)).get().getValue() != null) {
                return Response.ok(timestampToValue.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getKey)).get().getValue());
            } else {
                return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
            }
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    private Response getRequest(HttpClient client, String key) throws IOException {
        try {
            return client.get("/v0/entity?id=" + key, NEED_REPL_HEADER + ": 1");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
