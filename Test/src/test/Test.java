package test;

import java.util.ArrayList;
import java.util.List;

import other.test.TestObject;

public class Test
{
  /**
   * @param args
   * @throws InterruptedException
   */
  public static void main(String[] args) throws Exception
  {
    // Example args: 
    // -Xbootclasspath/p:./inmemprofiler-runtime.jar 
    // -agentpath:./objectSizer.dll=[bucket-5,15,25,35,45,55[gc-1
    
    // Allocate 5 objects
    List<TestObject> round1 = allocateObjects(5);
    Thread.sleep(10 * 1000);
    
    // Allocate 5 objects
    List<TestObject> round2 = allocateObjects(5);
    Thread.sleep(10 * 1000);
    
    // Allocate 5 objects
    List<TestObject> round3 = allocateObjects(5);
    Thread.sleep(10 * 1000);
    
    // Allocate 5 objects, and discard 5 which are 30 seconds old
    round1 = allocateObjects(5);
    System.out.println(round1);
    
    // Discard 5 which are 0 seconds old
    round1 = null;
    System.out.println(round2);
    
    // Discard 5 which are 20 seconds old
    round2 = null;
    System.out.println(round3);
    
    // Discard 5 which are 10 seconds old
    round3 = null;
    Thread.sleep(6 * 1000);
    System.out.println("===");
  }

  private static List<TestObject> allocateObjects(int len)
  {
    List<TestObject> list = new ArrayList<TestObject>();
    for (int ii = 0; ii < len; ii++)
    {
      list.add(new TestObject());
    }
    return list;
  }
}
