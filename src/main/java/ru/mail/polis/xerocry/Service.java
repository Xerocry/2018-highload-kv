package ru.mail.polis.xerocry;

import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Service extends HttpServer implements KVService {
    private final static Pattern ID = Pattern.compile(".*id=([\\w]*)");
    private final Store store;

    public Service(HttpServerConfig config, @NotNull Store store) throws IOException {
        super(config);
        this.store = store;
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
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    try {
                        final byte value[] = store.get(id);
                        if (value != null) {
                            session.sendResponse(new Response(Response.OK, value));
                        } else {
                            session.sendError(Response.BAD_REQUEST, null);
                        }
                    } catch (NoSuchElementException e) {
                        session.sendError(Response.BAD_REQUEST, null);
                    }
                    break;
                case Request.METHOD_PUT:
                    final byte value[] = request.getBody();
                    store.put(id, value);
                    session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                    break;
                case Request.METHOD_DELETE:
                    store.delete(id);
                    session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                    break;
                default:
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

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
