package org.inmemprofiler.runtime.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;

public class FileOutput
{
  public static BufferedWriter writer;
  
  public static synchronized void writeOutput(String output)
  {
    if (writer != null)
    {
      try
      {
        writer.write("===\n");
        writer.write(new Date().toString() + ":\n");
        writer.write(output + "\n");
        writer.flush();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }
}
