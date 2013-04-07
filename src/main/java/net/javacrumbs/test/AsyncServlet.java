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
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/async", asyncSupported = true, name = "async")
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


    protected void doProcess(final int number, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        log("Servlet no. {} called.", number);
        HttpGet backendRequest = createBackendRequest(req, number);
        //start async processing
        final AsyncContext asyncContext = req.startAsync(req, resp);
        asyncContext.setTimeout(600_000);
        client.execute(backendRequest, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                try {
                    ServletResponse response = asyncContext.getResponse();
                    copyResultToResponse(result, response);
                    log("Servlet no. {} processed.", number);
                    //really finish the processing
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
        // leaving the servlet, processing not finished yet.
        log("Servlet no. {} returning.", number);
    }
}
