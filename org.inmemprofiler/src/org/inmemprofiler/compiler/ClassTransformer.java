package org.inmemprofiler.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;

/**
 * ClassTransformer which invokes a standard transformer on the java.lang.Object class. 
 */
public class ClassTransformer implements ClassFileTransformer
{
  private final Instrumentation inst;

  public ClassTransformer(Instrumentation inst)
  {
    this.inst = inst;
  }

  public void instrumentObject()
  {
    try
    {
      Class<?>[] loadedClasses = inst.getAllLoadedClasses();
      for (Class<?> loadedClass : loadedClasses)
      {
        if (!loadedClass.isAnnotation() && !loadedClass.isSynthetic()
            && inst.isModifiableClass(loadedClass))
        {
          inst.retransformClasses(loadedClass);
        }
      }
    }
    catch (Throwable ex)
    {
      ex.printStackTrace();
    }
  }

  @Override
  public byte[] transform(ClassLoader loader, String internalClassName,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] originalClassfile) throws IllegalClassFormatException
  {
    if (internalClassName.equals("java/lang/Object"))
    {
      System.out.println("Modifying: " + internalClassName);

      byte[] retVal = getInstrumentedClassBytes(internalClassName,
          originalClassfile);
      File instrumentedObject = writeClassBytes(retVal, internalClassName
          + ".class");

      System.out.println("Save modified: " + internalClassName + " to "
          + instrumentedObject.getAbsolutePath());

      return null;
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

      ObjectClassWriter writer = new ObjectClassWriter(cr);
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

  private File writeClassBytes(byte[] newBytes, String className)
  {
    File classOut = new File("./build/genclasses/" + className);
    File parentDir = classOut.getParentFile();
    boolean dirExists = parentDir.exists();
    if (!dirExists)
    {
      dirExists = parentDir.mkdirs();
    }
    if (dirExists)
    {
      try
      {
        OutputStream out = new FileOutputStream(classOut);
        try
        {
          out.write(newBytes);
          out.flush();
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
        finally
        {
          try
          {
            out.close();
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
          }
        }
      }
      catch (FileNotFoundException ex)
      {
        ex.printStackTrace();
      }
    }
    else
    {
      System.out.println("Can't create directory " + parentDir
          + " for saving traced classfiles.");
    }

    return classOut;
  }
}
