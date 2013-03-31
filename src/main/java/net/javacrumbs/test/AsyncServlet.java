package net.javacrumbs.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.impl.nio.conn.PoolingClientAsyncConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/async", asyncSupported = true)
public class AsyncServlet extends AbstractServlet {

    private static final long serialVersionUID = 7770323867448369047L;

    private HttpAsyncClient client;

    @Override
    public void init() throws ServletException {
        try {
            PoolingClientAsyncConnectionManager connectionManager = new PoolingClientAsyncConnectionManager(new DefaultConnectingIOReactor());
            connectionManager.setDefaultMaxPerRoute(0xFFFF);
            connectionManager.setMaxTotal(0xFFFF);
            client = new DefaultHttpAsyncClient(connectionManager);
            client.start();
        } catch (IOReactorException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            client.shutdown();
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

    private void callServer(final int number, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpGet get = new HttpGet("http://localhost:8080/" + req.getServletContext().getContextPath() + "/async?max=" + getMax(req));
        get.addHeader("number", Integer.toString(number + 1));
        final AsyncContext asyncContext = req.startAsync(req, resp);
        asyncContext.setTimeout(600_000);
        client.execute(get, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                try {
                    ServletResponse response = asyncContext.getResponse();
                    response.setContentType(CONTENT_TYPE);
                    final ServletOutputStream outputStream = response.getOutputStream();
                    result.getEntity().writeTo(outputStream);
                    log("Servlet no. {} processed.", number);
                    asyncContext.complete();
                } catch (Exception e) {
                    logger.error("IOException {}:", asyncContext, e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void failed(Exception ex) {
                logger.error("Failed {}:", asyncContext, ex);
                asyncContext.complete();
            }

            @Override
            public void cancelled() {
                logger.error("Canceled {}", asyncContext);
                asyncContext.complete();
            }
        });
    }
}
