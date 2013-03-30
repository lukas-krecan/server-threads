package net.javacrumbs.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.impl.nio.conn.PoolingClientAsyncConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/*", asyncSupported = true)
public class ThreadsServlet extends HttpServlet {
    private static final String CONTENT_TYPE = "text/plain";

    private static final long serialVersionUID = 7770323867448369047L;
    public static final String MAX = "max";

    private HttpAsyncClient client;

    private final Logger logger = LoggerFactory.getLogger(ThreadsServlet.class);

    @SuppressWarnings("deprecation")
    @Override
    public void init() throws ServletException {
        super.init();
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

        logger.info("Servlet no. {} called.", number);
        try {
            callServer(number, req, resp);
            logger.info("Servlet no. {} returning.", number);
        } catch (Throwable e) {
            logger.error("Reached {} of connections", number, e);
            resp.getWriter().write("Reached " + number + " of connections.");
        }
    }

    private int getMax(HttpServletRequest req) {
        if (req.getParameter(MAX) != null) {
            return Integer.valueOf(req.getParameter(MAX));
        } else {
            return 5000;
        }
    }


    private int getCurrentNumber(HttpServletRequest req) {
        int number;
        String header = req.getHeader("number");
        if (header != null) {
            number = Integer.valueOf(header);
        } else {
            number = 1;
        }
        return number;
    }

    private void callServer(final int number, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpGet get = new HttpGet("http://localhost:8080/" + req.getServletContext().getContextPath() + "/async/req?max=" + getMax(req));
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
                    outputStream.print("OK ");
                    outputStream.print(number);
                    outputStream.print("\n");
                    result.getEntity().writeTo(outputStream);
                    logger.info("Servlet no. {} processed {}.", number, asyncContext);
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
