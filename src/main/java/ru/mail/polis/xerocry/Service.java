package ru.mail.polis.xerocry;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Service extends HttpServer implements KVService {
    private String[] topology;

    private final PutProcessor putProcessor;
    private final DeleteProcessor deleteProcessor;
    private final GetProcessor getProcessor;

    private final static Pattern ID = Pattern.compile(".*id=([\\w]*)(.*)?");
    private final static Pattern REPLICAS = Pattern.compile(".*replicas=([\\w]*/[\\w]*)");

    public Service(HttpServerConfig config, @NotNull Store store, Set<String> topology) throws IOException {
        super(config);
        this.topology = topology.toArray(new String[0]);
        Map<String, HttpClient> cluster = new HashMap<>();
        for (String str : topology) {
                cluster.put(str, new HttpClient(new ConnectionString(str)));
        }
        String replica = topology.stream().filter(r -> r.indexOf(":" + port) > 0).findFirst().get();
        putProcessor = new PutProcessor(store, cluster, this.topology, replica);
        getProcessor = new GetProcessor(store, cluster, this.topology, replica);
        deleteProcessor = new DeleteProcessor(store, cluster, this.topology, replica);
    }

    @Path("/v0/status")
    public void status(Request request, HttpSession session) throws IOException {
        if (request.getMethod() == Request.METHOD_GET) {
            session.sendResponse(Response.ok(Response.EMPTY));
        } else {
            session.sendError(Response.METHOD_NOT_ALLOWED, null);
        }
    }

    @Path("/v0/entity")
    public void entity(Request request, HttpSession session) throws IOException {
        final String id = getID(request.getURI());
        final String replicas = getReplicas(request.getURI());
        log.debug("Got entity message with ID:" + id + " and replica: " + replicas);
        AcknowledgeRequest ackParms = AcknowledgeRequest.fromRequest(request, topology.length, id, replicas);

        try {
//            if (replicas == null || replicas.isEmpty()) {
//                session.sendError(Response.BAD_REQUEST, null);
//                return;
//            }
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    try {
                        log.debug("GET Message!");
                        session.sendResponse(getProcessor.process(ackParms, request));
                    } catch (NoSuchElementException e) {
                        session.sendError(Response.BAD_REQUEST, null);
                    }
                    break;
                case Request.METHOD_PUT:
                    log.debug("PUT Message!");
                    session.sendResponse(putProcessor.process(ackParms, request));
                    break;
                case Request.METHOD_DELETE:
                    log.debug("DELETE Message!");
                    session.sendResponse(deleteProcessor.process(ackParms, request));
                    break;
                default:
                    log.error("Not-defined message type!");
                    session.sendError(Response.METHOD_NOT_ALLOWED, "Unsupported method");
            }
        } catch (NoSuchElementException nSEE) {
            nSEE.printStackTrace();
            session.sendError(Response.NOT_FOUND, null);
        } catch (Exception e) {
            e.printStackTrace();
            session.sendError(Response.INTERNAL_ERROR, null);
        }
    }

    private static String getID(String uri) {
        final Matcher matcher = ID.matcher(uri);
        if (matcher.matches()) {
            return matcher.group(1);
        } else return null;
    }

    private static String getReplicas(String uri) {
        final Matcher matcher = REPLICAS.matcher(uri);

        if (matcher.matches()) {
            return matcher.group(1);
        } else return null;
    }

    @Override
    public void start() {
        log.info("Starting Server!");
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
