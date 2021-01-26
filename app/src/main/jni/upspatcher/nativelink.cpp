/* 
	Native JNI linkage
						*/
#include "libups.hpp"
#include <jni.h>
#include <string.h>

using namespace nall;

extern "C" {

	JNIEXPORT int JNICALL Java_fr_mother3vf_mother3vf_MainActivity_upsPatchRom ( 
		 JNIEnv *env,        /* interface pointer */ 
		 jobject obj,        /* "this" pointer */ 
		 jstring romPath,
		 jstring patchPath,
		 jstring outputFile,
		 jint jignoreChecksum)
	{ 
		const char *str1 = env->GetStringUTFChars(romPath, 0); 
		const char *str2 = env->GetStringUTFChars(patchPath, 0); 
		const char *str3 = env->GetStringUTFChars(outputFile, 0); 
		UPS ups;
		int e = ups.apply(str1, str3, str2, (int)jignoreChecksum);
		env->ReleaseStringUTFChars(romPath, str1); 
		env->ReleaseStringUTFChars(patchPath, str2); 
		env->ReleaseStringUTFChars(outputFile, str3); 
		return e;
	}

	jstring JNICALL Java_fr_mother3vf_mother3vf_MainActivity_getKey ( 
		 JNIEnv *env,
		 jobject obj)
	{ 
		return env->NewStringUTF("aEnQagzZjoxIrA7");
	} 

}