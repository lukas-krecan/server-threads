package net.javacrumbs.test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class ThreadsServlet extends HttpServlet {
    private static final String CONTENT_TYPE = "text/plain";

    private static final long serialVersionUID = 7770323867448369047L;

    private HttpClient client;
    private MultiThreadedHttpConnectionManager connectionManager;

    @SuppressWarnings("deprecation")
    @Override
    public void init() throws ServletException {
        super.init();

        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxTotalConnections(32000);
        connectionManager.setMaxConnectionsPerHost(32000);
        client = new HttpClient(connectionManager);
    }

    @Override
    public void destroy() {
        connectionManager.shutdown();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int number = getCurrentNumber(req);

        if (number > 5000) {
            resp.setContentType(CONTENT_TYPE);
            resp.getWriter().write("OK: " + number);
            return;
        }

        logEntry(number);
        GetMethod get = null;
        try {
            get = callServer(number);
            logReturn(number);
            resp.setContentType(CONTENT_TYPE);
            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.print("OK ");
            outputStream.print(number);
            outputStream.print("\n");
            copy(get.getResponseBodyAsStream(), outputStream);
        } catch (Throwable e) {
            String message = "Reached " + number + " of connections";
            System.out.println(message);
            System.out.println(e);
            resp.getWriter().write(message);
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
    }

    private void logEntry(int number) {
        System.out.print("Servlet no. ");
        System.out.print(number);
        System.out.println(" called.");
    }

    private void logReturn(int number) {
        System.out.print("Servlet no.");
        System.out.print(number);
        System.out.println(" returning.");
    }

    private void copy(InputStream responseBodyAsStream, ServletOutputStream outputStream) throws IOException {
        int b;
        while ((b = responseBodyAsStream.read()) != -1) {
            outputStream.write(b);
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

    private GetMethod callServer(int number) throws IOException {
        GetMethod get;
        get = new GetMethod("http://localhost:8080/threads/req");
        get.setRequestHeader("number", Integer.toString(number + 1));
        get.getParams().setIntParameter("http.socket.sendbuffer", 1024);
        get.getParams().setIntParameter("http.socket.receivebuffer", 1024);
        client.executeMethod(get);
        return get;
    }
}
