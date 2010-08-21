#include "stdlib.h"

#include "agent_util.h"
#include "java_crw_demo.h"

#include "jni.h"
#include "jvmti.h"

#include "objectSizer.h"

#define PROFILER_CONTROL_class   org/inmemprofiler/runtime/Profiler       /* Name of control class */
#define PROFILER_class           org/inmemprofiler/runtime/ObjectProfiler /* Name of class we are using */
#define PROFILER_newobj          newObject      /* Name of java init method */

/* C macros to create strings from tokens */
#define _STRING(s) #s
#define STRING(s) _STRING(s)

/* Global agent data structure */
typedef struct {
    /* JVMTI Environment */
    jvmtiEnv      *jvmti;
	
    /* State of the VM flags */
    jboolean       vmStarted;	
	
	/* Startup options */
	char*          options;
	jboolean       trackObj;
	jboolean       trackArr;
} GlobalAgentData;

static GlobalAgentData *gdata;

/* Callback for JVMTI_EVENT_VM_START */
static void JNICALL
cbVMStart(jvmtiEnv *jvmti, JNIEnv *env)
{    
	/* Indicate VM has started */
	gdata->vmStarted = JNI_TRUE;    
}

/* Callback for JVMTI_EVENT_VM_INIT */
static void JNICALL
cbVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread)
{    
	/* Begin Profiling */
	jclass cls = (*env)->FindClass(env, STRING(PROFILER_CONTROL_class));
	if (cls == NULL)
	{
	  fatal_error("Failed to find Profiler control class\n");
	}
	
	jmethodID mid = (*env)->GetStaticMethodID(env, cls, "beginProfiling", "(Ljava/lang/String;)V");
	if (mid == NULL)
	{
	  fatal_error("Failed to find Profiler control beginProfiling method\n");
	}
	
	jstring joptions = NULL;
	char* options = gdata->options;	
	if (options != NULL)
	{
		joptions = (*env)->NewStringUTF(env, options);
		if (joptions == NULL)
		{
		  fatal_error("Failed to find Profiler control beginProfiling method\n");
		}
	}
	
	(*env)->CallStaticVoidMethod(env, cls, mid, joptions);
}

/* Callback for JVMTI_EVENT_CLASS_FILE_LOAD_HOOK */
static void JNICALL
cbClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* env,
                jclass class_being_redefined, jobject loader,
                const char* name, jobject protection_domain,
                jint class_data_len, const unsigned char* class_data,
                jint* new_class_data_len, unsigned char** new_class_data)
{
	const char * classname;

	/* Name can be NULL, make sure we avoid SEGV's */
	if ( name == NULL ) {
		classname = java_crw_demo_classname(class_data, class_data_len,
						NULL);
		if ( classname == NULL ) {
			fatal_error("ERROR: No classname in classfile\n");
		}
	} else {
		classname = strdup(name);
		if ( classname == NULL ) {
			fatal_error("ERROR: Ran out of malloc() space\n");
		}
	}

	*new_class_data_len = 0;
	*new_class_data     = NULL;

	/* The tracker class itself? */
	if ( strcmp(classname, STRING(PROFILER_class)) != 0 ) {
		jint           cnum;
		int            systemClass;
		unsigned char *newImage;
		long           newLength;

		/* Is it a system class? If the class load is before VmStart
		 *   then we will consider it a system class that should
		 *   be treated carefully. (See java_crw_demo)
		 */
		systemClass = 0;
		if ( !gdata->vmStarted ) {
			systemClass = 1;
		}

		newImage = NULL;
		newLength = 0;

		/* Call the class file reader/write demo code */		
		java_crw_demo(cnum,
			classname,
			class_data,
			class_data_len,
			systemClass,
			STRING(PROFILER_class),
			"L" STRING(PROFILER_class) ";",
			NULL, NULL,
			NULL, NULL,
			(gdata->trackObj == JNI_TRUE ? STRING(PROFILER_newobj) : NULL), 
			(gdata->trackObj == JNI_TRUE ? "(Ljava/lang/Object;)V" : NULL),
			(gdata->trackArr == JNI_TRUE ? STRING(PROFILER_newobj) : NULL), 
			(gdata->trackArr == JNI_TRUE ? "(Ljava/lang/Object;)V" : NULL),
			&newImage,
			&newLength,
			NULL,
			NULL);

		/* If we got back a new class image, return it back as "the"
		 *   new class image. This must be JVMTI Allocate space.
		 */
		if ( newLength > 0 ) {
			unsigned char *jvmti_space;

			jvmti_space = (unsigned char *)allocate(jvmti, (jint)newLength);
			(void)memcpy((void*)jvmti_space, (void*)newImage, (int)newLength);
			*new_class_data_len = (jint)newLength;
			*new_class_data     = jvmti_space; /* VM will deallocate */
		}

		/* Always free up the space we get from java_crw_demo() */
		if ( newImage != NULL ) {
			(void)free((void*)newImage); /* Free malloc() space with free() */
		}
	}

	(void)free((void*)classname);
}

/* Agent_OnLoad: This is called immediately after the shared library is 
 *   loaded. This is the first code executed.
 */
JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    static GlobalAgentData data;
    jvmtiEnv              *jvmti;
    jvmtiError             error;
    jint                   res;
    jvmtiCapabilities      capabilities;
    jvmtiEventCallbacks    callbacks;	
    
    /* Setup initial global agent data area 
     *   Use of static/extern data should be handled carefully here.
     *   We need to make sure that we are able to cleanup after ourselves
     *     so anything allocated in this library needs to be freed in
     *     the Agent_OnUnload() function.
     */
    (void)memset((void*)&data, 0, sizeof(data));
    gdata = &data;
   
    /* First thing we need to do is get the jvmtiEnv* or JVMTI environment */
    res = (*vm)->GetEnv(vm, (void **)&jvmti, JVMTI_VERSION_1);
    if (res != JNI_OK) {
        /* This means that the VM was unable to obtain this version of the
         *   JVMTI interface, this is a fatal error.
         */
        fatal_error("ERROR: Unable to access JVMTI Version 1 (0x%x),"
                " is your JDK a 5.0 or newer version?"
                " JNIEnv's GetEnv() returned %d\n",
               JVMTI_VERSION_1, res);
    }

    /* Here we save the jvmtiEnv* for Agent_OnUnload(). */
    gdata->jvmti = jvmti;


    /* Immediately after getting the jvmtiEnv* we need to ask for the
     *   capabilities this agent will need. 
     */
    (void)memset(&capabilities,0, sizeof(capabilities));
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_get_source_file_name  = 1;
    capabilities.can_get_line_numbers  = 1;
    error = (*jvmti)->AddCapabilities(jvmti, &capabilities);
    check_jvmti_error(jvmti, error, "Unable to get necessary JVMTI capabilities.");
    
    /* Next we need to provide the pointers to the callback functions to
     *   to this jvmtiEnv*
     */
    (void)memset(&callbacks,0, sizeof(callbacks));
    /* JVMTI_EVENT_VM_START */
    callbacks.VMStart           = &cbVMStart;       
    /* JVMTI_EVENT_VM_INIT */
    callbacks.VMInit            = &cbVMInit;      	
    /* JVMTI_EVENT_CLASS_FILE_LOAD_HOOK */
    callbacks.ClassFileLoadHook = &cbClassFileLoadHook; 
    error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(callbacks));
    check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");
   
    /* At first the only initial events we are interested in are VM
     *   initialization, VM death, and Class File Loads. 
     *   Once the VM is initialized we will request more events.
     */
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, 
                          JVMTI_EVENT_VM_START, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, 
                          JVMTI_EVENT_VM_INIT, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");	
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, 
                          JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
   
    /* Save off options for later */    	   
	jboolean instrumentObjAllocs = JNI_TRUE;
	jboolean instrumentArrAllocs = JNI_TRUE;
	
	if (options == NULL)
	{
	  gdata->options = NULL;
	}
	else
	{
	  int optionsLen = strlen(options) + 1;
	  if (optionsLen == 0)
	  {
	    gdata->options = NULL;
	  }
	  else
	  {
	    char* storedOptions = malloc(optionsLen * sizeof(char));
	    if (storedOptions == NULL)
	    {
  	      fatal_error("Failed to save options\n");
	    }
	    
	    strncpy(storedOptions, options, (size_t)optionsLen);
	    storedOptions[optionsLen - 1] = '\0';
	    
	    char* includeOption = strstr(storedOptions, "#include");
	    if (includeOption != NULL)
	    {	      
	      char* endIncludeOption = strstr((includeOption + 1), "#");
	      
	      if (endIncludeOption == NULL)
	      {
	        endIncludeOption = storedOptions + optionsLen - 1;
	      }
	    
	      instrumentObjAllocs = JNI_FALSE;
	      instrumentArrAllocs = JNI_FALSE;
	      
	      // Look for [ within the include string
	      char* arrayCharIndex = strstr((includeOption + 1), "[");
	      if ((arrayCharIndex != NULL) &&
	          (arrayCharIndex < endIncludeOption))
	      {
	        instrumentArrAllocs = JNI_TRUE;	        	     
	      }
	      
	      // Look for a non [ within the include string
	      // can take the form comma then non array char or
	      // dash then non array char
	      char* dashIndex = strstr((includeOption + 1), "-");
	      if ((dashIndex != NULL) &&
	          (dashIndex < endIncludeOption) &&
	          (dashIndex[1] != '['))
	      {
	        instrumentObjAllocs = JNI_TRUE;
	      }
	      else
	      {
	        char* commaIndex = strstr((includeOption + 1), ",");
	        while ((commaIndex != NULL) &&
	               (commaIndex < endIncludeOption))
	        {
	          if (commaIndex[1] != '[')
	          {
	            instrumentObjAllocs = JNI_TRUE;
	            break;
	          }
	        
	          commaIndex = strstr((commaIndex + 1), ",");
	        }
	      }	      
	    }
	    	    
	    gdata->options = storedOptions;
	  }
	}
	gdata->trackArr = instrumentArrAllocs;
	gdata->trackObj = instrumentObjAllocs;
	
    /* We return JNI_OK to signify success */
    return JNI_OK;
}

/* Agent_OnUnload: This is called immediately before the shared library is 
 *   unloaded. This is the last code executed.
 */
JNIEXPORT void JNICALL 
Agent_OnUnload(JavaVM *vm)
{
    /* Skip any cleanup, VM is about to die anyway */
}

JNIEXPORT jlong JNICALL Java_org_inmemprofiler_runtime_ObjectSizer_getObjectSize
  (JNIEnv * jenv,
   jclass klass,
   jobject xiObj)
{
  jlong      xoSize; 
  jvmtiError error;
  
  error = (*(gdata->jvmti))->GetObjectSize(gdata->jvmti,
                                           xiObj,
                                           &xoSize);                
  check_jvmti_error(gdata->jvmti, error, "Cannot get size of object");
  
  return xoSize;
}
