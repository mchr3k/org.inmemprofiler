package org.server;

import org.logging.Logger;
import org.yourorg.AppCode;

public class Server
{
  public static void main(String[] args) {
    ServerThread.handleRequests();
  }  
  
  public static class ServerThread
  {
    public static void handleRequests() {
      while (true) {
        Request req = Request.getNextRequest();
        Logger.log("Time: " + System.currentTimeMillis() + ", pass req to app!");
        AppCode.handleRequest(req);
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
