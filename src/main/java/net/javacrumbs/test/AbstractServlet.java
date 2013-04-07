/*
 * Copyright (c) 2013 Jadler contributors
 * This program is made available under the terms of the MIT License.
 */
package net.javacrumbs.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractServlet extends HttpServlet {
    public static final String MAX = "max";
    protected static final String CONTENT_TYPE = "text/plain";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected int getMax(HttpServletRequest req) {
        if (req.getParameter(MAX) != null) {
            return Integer.valueOf(req.getParameter(MAX));
        } else {
            return 5000;
        }
    }

    protected int getCurrentNumber(HttpServletRequest req) {
        int number;
        String header = req.getHeader("number");
        if (header != null) {
            number = Integer.valueOf(header);
        } else {
            number = 1;
        }
        return number;
    }

    protected HttpGet createBackendRequest(HttpServletRequest req, int number) {
        HttpGet get = new HttpGet("http://localhost:8080/" + req.getServletContext().getContextPath() + "/" + getServletName() + "?max=" + getMax(req));
        get.addHeader("number", Integer.toString(number + 1));
        return get;
    }

    /**
     * Copies backend response to servlet response.
     *
     * @param response
     * @param resp
     * @throws IOException
     */
    protected void copyResultToResponse(HttpResponse response, ServletResponse resp) throws IOException {
        resp.setContentType(CONTENT_TYPE);
        final ServletOutputStream outputStream = resp.getOutputStream();
        response.getEntity().writeTo(outputStream);
    }


    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int number = getCurrentNumber(req);

        if (number >= getMax(req)) {
            resp.setContentType(CONTENT_TYPE);
            resp.getWriter().write("OK: " + number);
            return;
        }
        try {
            doProcess(number, req, resp);
        } catch (Throwable e) {
            logger.error("Reached {} of connections", number, e);
            resp.getWriter().write("Reached " + number + " of connections.");
        }
    }

    /**
     * Does the processing. Overriden in the subclasses.
     *
     * @param number
     * @param req
     * @param resp
     */
    protected abstract void doProcess(int number, HttpServletRequest req, HttpServletResponse resp) throws Exception;


    protected final void log(String message, int number) {
        logger.info(message, number);
    }
}
