#include "stdlib.h"

#include "objectSizer.h"

#include "jni.h"
#include "jvmti.h"

/* Global agent data structure */
typedef struct {
    /* JVMTI Environment */
    jvmtiEnv      *jvmti;
} GlobalAgentData;

static GlobalAgentData *gdata;

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

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
void
check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str)
{
    if ( errnum != JVMTI_ERROR_NONE ) {
	char       *errnum_str;
	
	errnum_str = NULL;
	(void)(*jvmti)->GetErrorName(jvmti, errnum, &errnum_str);
	
	fatal_error("ERROR: JVMTI: %d(%s): %s\n", errnum, 
		(errnum_str==NULL?"Unknown":errnum_str),
		(str==NULL?"":str));
    }
}

/* Send message to stderr or whatever the error output location is and exit  */
void
fatal_error(const char * format, ...)
{
    va_list ap;

    va_start(ap, format);
    (void)vfprintf(stderr, format, ap);
    (void)fflush(stderr);
    va_end(ap);
    exit(3);
}
