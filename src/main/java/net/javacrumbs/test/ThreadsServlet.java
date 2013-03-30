package net.javacrumbs.test;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.util.FileCopyUtils;

public class ThreadsServlet extends HttpServlet {
	private static final String CONTENT_TYPE = "text/plain";

	private static final long serialVersionUID = 7770323867448369047L;

	private HttpClient client;
	@SuppressWarnings("deprecation")
	@Override
	public void init() throws ServletException {
		super.init();
		
	  	MultiThreadedHttpConnectionManager connectionManager =	new MultiThreadedHttpConnectionManager();
	  	connectionManager.setMaxTotalConnections(32000);
	  	connectionManager.setMaxConnectionsPerHost(32000);
	  	client = new HttpClient(connectionManager);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int number;
		String header = req.getHeader("number");
		if (header!=null)
		{
			 number = Integer.valueOf(header);
		}
		else
		{
			number = 1;
		}
		
		if (number>10000)
		{
			resp.setContentType(CONTENT_TYPE);
			resp.getWriter().write("OK: "+number);
			return;
		}
		
		System.out.println("Servlet no. "+number+" called.");
		GetMethod get = null;
		try {
			get = callServer(number);
			System.out.println("Servlet no."+number+" returning.");
			resp.setContentType(CONTENT_TYPE);
			resp.getWriter().write("OK "+number+"\n");
			FileCopyUtils.copy(new InputStreamReader(get.getResponseBodyAsStream()), resp.getWriter());
		} catch (Throwable e) {
			String message = "Reached "+number+" of connections";
			System.out.println(message);
			System.out.println(e);
			resp.getWriter().write(message);
		}
		finally
		{
			if (get!=null)
			{
				get.releaseConnection();
			}
		}
	}

	private GetMethod callServer(int number) throws IOException {
		GetMethod get;
		get = new GetMethod("http://localhost:8080/threads/req");
		get.setRequestHeader("number", Integer.toString(number+1));
		get.getParams().setIntParameter("http.socket.sendbuffer", 1024);
		get.getParams().setIntParameter("http.socket.receivebuffer", 1024);
		client.executeMethod(get);
		return get;
	}
}
