#include "com_frostwire_android_python_PythonBridge.h"
#include <Python.h>

JNIEXPORT jint JNICALL Java_com_frostwire_android_python_PythonBridge_square(JNIEnv *jniEnv, jclass clazz, jint n) {
  Py_SetProgramName("frostwire python bridge");  /* optional but recommended */
  Py_Initialize();
  PyRun_SimpleString("from time import time,ctime\n"
                     "print 'Today is',ctime(time())\n");
  Py_Finalize();
  return n*n;
}
