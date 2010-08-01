#ifndef OBJECT_SIZER_H
#define OBJECT_SIZER_H

/* Standard C functions used throughout. */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdarg.h>

/* General JVM/Java functions, types and macros. */

#include <sys/types.h>
#include "jni.h"
#include "jvmti.h"

/* Agent library externals to export. */

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved);
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm);

JNIEXPORT jlong JNICALL Java_org_inmemprofiler_runtime_ObjectSizer_getObjectSize(JNIEnv * jenv,
                                                                                 jclass klass,
                                                                                 jobject xiObj);
                                                                                 
/* Utility functions */
void  fatal_error(const char * format, ...);
void  check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str);

#endif

