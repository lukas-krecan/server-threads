/*
 * Copyright (c) 2013 Jadler contributors
 * This program is made available under the terms of the MIT License.
 */
package net.javacrumbs.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class AbstractServlet extends HttpServlet {
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


    protected final void log(String message, int number) {
        logger.info(message, number);
    }
}
