package org.server;

import org.logging.Logger;

import com.yourorg.app.App;

public class Server
{
  public static void main(String[] args) throws InterruptedException {
    Thread t = new Thread(new ServerThread());
    t.start();
    t.join();
  }  
  
  public static class ServerThread implements Runnable
  {
    public void run() {
      for (int ii = 0; ii < 100; ii++) {
        Request req = Request.getNextRequest();
        Logger.log("Time: " + System.currentTimeMillis() + ", pass req to app!");
        App.handleRequest(req);
      }
    }
  }
  
  public static class Request
  {
    public static Request getNextRequest() {
      return new Request();
    }   
  }
}
