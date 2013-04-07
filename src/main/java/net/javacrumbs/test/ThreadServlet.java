package net.javacrumbs.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/sync", name = "sync")
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


    protected void doProcess(final int number, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        log("Servlet no. {} called.", number);
        HttpGet backendRequest = createBackendRequest(req, number);
        HttpResponse result = client.execute(backendRequest);
        copyResultToResponse(result, resp);
        log("Servlet no. {} processed.", number);
        log("Servlet no. {} returning.", number);
    }
}
