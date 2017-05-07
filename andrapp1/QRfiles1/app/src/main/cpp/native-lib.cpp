#include <jni.h>
#include <string>

#include "public_decoder_api.h"

extern "C"
JNIEXPORT jint JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_initialize_1decoder(JNIEnv *env, jclass type) {
    return initialize_decoder();
}

extern "C"
JNIEXPORT jint JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_set_1decoded_1file_1path(JNIEnv *env, jclass type,
                                                                         jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    int res = set_decoded_file_path(path);
    env->ReleaseStringUTFChars(path_, path);
    return res;
}

extern "C"
JNIEXPORT jint JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_send_1next_1grayscale_1buffer_1to_1decoder(
        JNIEnv *env, jclass type, jbyteArray grayscale_qr_data_, jint image_width,
        jint image_height) {
    jbyte *grayscale_qr_data = env->GetByteArrayElements(grayscale_qr_data_, NULL);
    jint status = send_next_grayscale_buffer_to_decoder((const char*)grayscale_qr_data, image_width, image_height);
    env->ReleaseByteArrayElements(grayscale_qr_data_, grayscale_qr_data, 0);
    return status;
}

extern "C"
JNIEXPORT jint JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_tell_1decoder_1no_1more_1qr(JNIEnv *env,
                                                                            jclass type) {
    return tell_decoder_no_more_qr();
}

extern "C"
JNIEXPORT jint JNICALL
Java_qrfiles_pwrobel_myapplication_CameraWorker_deinitialize_1decoder(JNIEnv *env, jclass type) {
    return deinitialize_decoder();
}

//////////////////////////////////////////////////////////////////////////////////////////

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

