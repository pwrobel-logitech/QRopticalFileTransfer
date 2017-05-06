#include <jni.h>
#include <string>

extern "C"
JNIEXPORT void JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_applygrayscalenative(JNIEnv *env, jclass type,
                                                                     jbyteArray pixels_,
                                                                     jbyteArray data_, jint width,
                                                                     jint height) {
    jbyte *pixels = env->GetByteArrayElements(pixels_, NULL);
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    char p;
    int size = width*height;
    for(int i = 0; i < size; i++) {
        p = (char)(data[i] & 0xFF);
        pixels[i] = p;
    }


    env->ReleaseByteArrayElements(pixels_, pixels, 0);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_qrfiles_pwrobel_myapplication_Qrfiles_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

