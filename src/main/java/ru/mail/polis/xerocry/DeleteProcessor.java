package ru.mail.polis.xerocry;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DeleteProcessor extends RequestProcessor{

    DeleteProcessor(Store store, Map<String, HttpClient> replicas, String[] topology, String myReplica) {
        super(store, replicas, topology, myReplica);
    }

    @Override
    protected Response processRequest(AcknowledgeRequest ackParms, Request request) throws IOException, InterruptedException, HttpException, PoolException {
        String id = ackParms.getId();
        int ack = 0;

        if (!ackParms.isNeedRepl()) {
            try {
                store.delete(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }

        List<String> replicas = getNodes(id, ackParms.getAck());

        for (String replica : replicas) {
                if (myReplica.equals(replica)) {
                    store.delete(id);
                    ack++;
                } else if (this.replicas.get(replica).delete("/v0/entity?id=" + id,  NEED_REPL_HEADER + ": 1").getStatus() == 202) {
                    ack++;
                }
        }

        return ack >= ackParms.getAck()
                ? new Response(Response.ACCEPTED, Response.EMPTY)
                : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

}
