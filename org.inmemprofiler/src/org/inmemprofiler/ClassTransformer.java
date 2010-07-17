package org.inmemprofiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;

public class ClassTransformer implements ClassFileTransformer
{

  private final Instrumentation inst;
  public ClassTransformer(Instrumentation inst)
  {
    this.inst = inst;
  }

  @Override
  public byte[] transform(ClassLoader loader, 
                          String internalClassName,
                          Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] originalClassfile)
                          throws IllegalClassFormatException
  {
    //System.out.println("Consider modify:\t " + internalClassName);
    if (internalClassName.startsWith("dcl"))
    {
      System.out.println("Actually modify: " + internalClassName);
      
      byte[] retVal = getInstrumentedClassBytes(internalClassName, originalClassfile);
      return retVal;
    }
    else
    {
      return null;
    }
  }

  private byte[] getInstrumentedClassBytes(String name, byte[] classfileBuffer)
  {
    try
    {
      ClassReader cr = new ClassReader(classfileBuffer);

      ProfiledClassWriter writer = new ProfiledClassWriter(cr);
      cr.accept(writer, 0);

      return writer.toByteArray();
    }
    catch (Throwable th)
    {
      System.err.println("Caught Throwable when trying to instrument: " + name);
      th.printStackTrace();
      return null;
    }
  }
  
  public void instrumentObject()
  {
    try
    {
      Class<?>[] loadedClasses = inst.getAllLoadedClasses();
      for (Class<?> loadedClass : loadedClasses)
      {
        if (!loadedClass.isAnnotation() &&
            !loadedClass.isSynthetic() && 
            inst.isModifiableClass(loadedClass))
        {          
          //System.out.println("Attempt to modify:\t " + loadedClass.getName());
          
          inst.retransformClasses(loadedClass);
        }
      }
    }
    catch (Throwable ex)
    {
      ex.printStackTrace();
    }
  }
}
