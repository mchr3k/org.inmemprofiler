package com.yourorg.app;

import org.logging.Logger;
import org.server.Server.Request;

public class App
{
  public static void handleRequest(Request req) {
    Logger.log("Time: " + System.currentTimeMillis() + ", got request!");
    // App logic here
    Storage.store(System.currentTimeMillis());
  }
  
  private static class Storage
  {
    public static void store(long currentTimeMillis) {
      Logger.log("Time: " + System.currentTimeMillis() + ", got request!");
      // Storage logic here
    }
  }
}
