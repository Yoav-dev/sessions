package infra;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;

import jobs.Job;

public class SessionsManager {
	private static final int MAX_SESSIONS = 10;
	private static final String SESSION_API_FORMAT = "http://%s:%s/reservation-0.0.1-SNAPSHOT/Sessions";
	
	private ConcurrentLinkedQueue<Long> freeSessions = new ConcurrentLinkedQueue<Long>();
	private ConcurrentLinkedQueue<Long> usedSessions = new ConcurrentLinkedQueue<Long>();
	
	private String fullAPI;
	private String apiKey;
	
	SessionsManager(String ip, int port, String apiKey) {
		this.apiKey = apiKey;
		fullAPI = String.format(SESSION_API_FORMAT, ip, port);
	}

	private LinkedBlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
	private ThreadPoolExecutor executor = new ThreadPoolExecutor(11, 20, 20, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
	
	void run() throws ClientProtocolException, IOException {
		while (true) {
			final Job task = jobs.poll();
			
			executor.execute(new Runnable() {				
				public void run() {
					HttpPost httpRequest = new HttpPost(fullAPI);
					CloseableHttpClient httpClient = HttpClients.createDefault();
					httpRequest.addHeader("action", task.action());
					httpRequest.addHeader("apiKey", apiKey);
					Long session = freeSessions.poll();
					
					if (session == null) {
						if (usedSessions.size() == MAX_SESSIONS) {
							synchronized (usedSessions) {
								try {
									usedSessions.wait();
									session = freeSessions.poll();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						} else {
							httpRequest.setHeader("action", "createSession");
							
							try {
								session = Long.valueOf(httpClient.execute(httpRequest).getFirstHeader("sessionId").getValue());
							} catch (ClientProtocolException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							httpRequest.setHeader("action", task.action());
						}
					}
					
					httpRequest.addHeader("session", String.valueOf(session));
					usedSessions.add(session);

					try {
						httpClient.execute(httpRequest);
					} catch (ClientProtocolException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} finally {
						usedSessions.remove(session);
						freeSessions.add(session);
						
						synchronized (usedSessions) {
							usedSessions.notify();
						}
					}
				}
			});
		}
	}
}