package ru.mail.polis.xerocry;

import one.nio.http.HttpClient;
import one.nio.http.Request;
import one.nio.http.Response;

import java.io.IOException;
import java.util.*;

abstract public class RequestProcessor {
    final static String NEED_REPL_HEADER = "X-Need-Repl";

    final Store store;
    Map<String, HttpClient> replicas;
    final String myReplica;

    Response process(AcknowledgeRequest ackParms, Request request) throws IOException {
        return processRequest(ackParms, request);
    }

    RequestProcessor(Store store, Map<String, HttpClient> replicas, String myReplica) {
        this.store = store;
        this.replicas = replicas;
        this.myReplica = myReplica;
    }

    List<String> getNodes(String key, int length) {
        List<String> clients = new ArrayList<>();
        String[] hosts = replicas.keySet().toArray(new String[replicas.size()]);
        int firstNodeId = (key.hashCode() & Integer.MAX_VALUE) % replicas.size();
        for (int i = 0; i < length; i++) {
            clients.add(hosts[(firstNodeId + i) % replicas.size()]);
        }
        return clients;
    }

    abstract protected Response processRequest(AcknowledgeRequest queryParams, Request request) throws IOException;
}
