package org.yourorg;

import org.logging.Logger;
import org.server.Server.Request;

public class AppCode
{
  public static void handleRequest(Request req) {
    Logger.log("Time: " + System.currentTimeMillis() + ", got request!");
    // App specific code here
  }
}
