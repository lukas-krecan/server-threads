package net.javacrumbs.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/sync")
public class ThreadServlet extends AbstractServlet {

    private static final long serialVersionUID = 7770323867448369047L;

    private HttpClient client;

    @Override
    public void init() throws ServletException {
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(0xFFFF);
        connectionManager.setMaxTotal(0xFFFF);
        client = new DefaultHttpClient(connectionManager);
    }

    @Override
    public void destroy() {
        try {
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int number = getCurrentNumber(req);

        if (number > getMax(req)) {
            resp.setContentType(CONTENT_TYPE);
            resp.getWriter().write("OK: " + number);
            return;
        }

        log("Servlet no. {} called.", number);
        try {
            callServer(number, req, resp);
            log("Servlet no. {} returning.", number);
        } catch (Throwable e) {
            logger.error("Reached {} of connections", number, e);
            resp.getWriter().write("Reached " + number + " of connections.");
        }
    }


    private void callServer(final int number, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        HttpGet get = new HttpGet("http://localhost:8080/" + req.getServletContext().getContextPath() + "/sync?max=" + getMax(req));
        get.addHeader("number", Integer.toString(number + 1));
        client.execute(get, new ResponseHandler<Void>() {
            @Override
            public Void handleResponse(HttpResponse response) throws IOException {
                resp.setContentType(CONTENT_TYPE);
                final ServletOutputStream outputStream = resp.getOutputStream();
                response.getEntity().writeTo(outputStream);
                log("Servlet no. {} processed.", number);
                return null;
            }
        });
    }
}
