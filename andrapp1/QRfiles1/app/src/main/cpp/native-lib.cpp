#include <jni.h>
#include <string>

#include "public_decoder_api.h"
#include "public_encoder_api.h"

#include <android/log.h>

//encoder

long upper_power_of_two(long v)
{
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;
    return v;
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_QRSurface_tell_1how_1much_1frames_1will_1be_1generated(
        JNIEnv *env, jclass type) {
    return (jint)tell_how_much_frames_will_be_generated();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_QRSurface_tell_1no_1more_1generating_1header(JNIEnv *env,
                                                                                jclass type) {
    return (jint)tell_no_more_generating_header();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_QRSurface_destroy_1current_1encoder(JNIEnv *env, jclass type) {
    return destroy_current_encoder();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_QRSurface_produce_1next_1qr_1grayscale_1image_1to_1mem(
        JNIEnv *env, jclass type, jobject produced_image, jobject produced_width) {
    char* bbufmem = (char*)((env)->GetDirectBufferAddress(produced_image));
    char* obtained_mem;
    int numproduced_width = 0;
    int stat = produce_next_qr_grayscale_image_to_mem(&obtained_mem, &numproduced_width);
#ifdef ANDROID
    //__android_log_print(ANDROID_LOG_INFO, "QNAT", "prodw %d", numproduced_width);
#endif
    int* intmem = (int*)((env)->GetDirectBufferAddress(produced_width));
    memset(intmem, 0, sizeof(int));
    *intmem = numproduced_width;

    int upperpower = (int)upper_power_of_two((int)numproduced_width);
    for (int i = 0; i < numproduced_width; i++)
        for (int j = 0; j < numproduced_width; j++){
            bbufmem[j*upperpower+i] = obtained_mem[i*numproduced_width+j];
        }

    //memcpy(bbufmem, obtained_mem, numproduced_width * numproduced_width);
    return (jint)stat;
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_QRSurface_init_1and_1set_1external_1file_1info(JNIEnv *env,
                                                                                  jclass type,
                                                                                  jstring filename_,
                                                                                  jstring filepath_,
                                                                                  jint suggested_qr_payload_length,
                                                                                  jdouble suggested_err_fraction,
                                                                                  jint suggested_N) {
    const char *filename = env->GetStringUTFChars(filename_, 0);
    const char *filepath = env->GetStringUTFChars(filepath_, 0);
    jint stat = init_and_set_external_file_info(filename, filepath, suggested_qr_payload_length, suggested_err_fraction, suggested_N);
    env->ReleaseStringUTFChars(filename_, filename);
    env->ReleaseStringUTFChars(filepath_, filepath);
    return stat;
}

///decoder

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1main_1RSN(JNIEnv *env, jclass type) {
    return (jint)get_main_RSN();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1main_1RSK(JNIEnv *env, jclass type) {
    return (jint)get_main_RSK();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1residual_1RSN(JNIEnv *env, jclass type) {
    return (jint)get_residual_RSN();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1residual_1RSK(JNIEnv *env, jclass type) {
    return (jint)get_residual_RSK();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1total_1frames_1of_1data_1that_1will_1be_1produced(
        JNIEnv *env, jclass type) {
    return (jint)get_total_frames_of_data_that_will_be_produced();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1last_1number_1of_1frame_1detected(JNIEnv *env,
                                                                                       jclass type) {
    return (jint)get_last_number_of_frame_detected();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1last_1number_1of_1header_1frame_1detected(
        JNIEnv *env, jclass type) {
    return (jint)get_last_number_of_header_frame_detected();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1last_1recognized_1file_1name_1str(JNIEnv *env,
                                                                                       jclass type) {
    std::string str(get_last_recognized_file_name_str());
    return env->NewStringUTF(str.c_str());
}


extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_get_1last_1recognized_1file_1size(JNIEnv *env,
                                                                                   jclass type) {
    return (jint)get_last_recognized_file_size();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_initialize_1decoder(JNIEnv *env, jclass type) {
#ifdef ANDROID
    //__android_log_print(ANDROID_LOG_INFO, "QDEC", "init decoder ");
#endif
    return initialize_decoder();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_set_1decoded_1file_1path(JNIEnv *env, jclass type,
                                                                         jstring path_) {
#ifdef ANDROID
    //__android_log_print(ANDROID_LOG_INFO, "QDEC", "set filepath ");
#endif
    const char *path = env->GetStringUTFChars(path_, 0);
    int res = set_decoded_file_path(path);
    env->ReleaseStringUTFChars(path_, path);
    return res;
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_send_1next_1grayscale_1buffer_1to_1decoder(
        JNIEnv *env, jclass type, jbyteArray grayscale_qr_data_, jint image_width,
        jint image_height) {
    jbyte *grayscale_qr_data = env->GetByteArrayElements(grayscale_qr_data_, NULL);
    jint status = send_next_grayscale_buffer_to_decoder((const char*)grayscale_qr_data, image_width, image_height);
    env->ReleaseByteArrayElements(grayscale_qr_data_, grayscale_qr_data, 0);
    return status;
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_tell_1decoder_1no_1more_1qr(JNIEnv *env,
                                                                            jclass type) {
#ifdef ANDROID
    //__android_log_print(ANDROID_LOG_INFO, "QDEC", "nomoreqr ");
#endif
    return tell_decoder_no_more_qr();
}

extern "C"
JNIEXPORT jint JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_deinitialize_1decoder(JNIEnv *env, jclass type) {
#ifdef ANDROID
    //__android_log_print(ANDROID_LOG_INFO, "QDEC", "deinit decoder ");
#endif
    return deinitialize_decoder();
}

//////////////////////////////////////////////////////////////////////////////////////////
/*
extern "C"
JNIEXPORT void JNICALL
Java_pl_pwrobel_opticalfiletransfer_CameraWorker_applygrayscalenative(JNIEnv *env, jclass type,
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
*/
extern "C"
JNIEXPORT jstring JNICALL
Java_pl_pwrobel_opticalfiletransfer_Qrfiles_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

