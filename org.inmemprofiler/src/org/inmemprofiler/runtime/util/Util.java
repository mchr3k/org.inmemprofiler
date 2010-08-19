package org.inmemprofiler.runtime.util;

public class Util
{
  public static void indent(StringBuilder str, int indent)
  {
    for (int ii = 0; ii < indent; ii++)
    {
      str.append("  ");
    }
  }
}
