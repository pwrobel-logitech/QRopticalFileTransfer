
#include "qr_frame_decoder.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

#ifdef ANDROID
#include <android/log.h>
#endif


QR_frame_decoder* framedecoder = NULL;


FILE *f = NULL;
char namebuf[200];

immediate_status last_status;


int initialize_decoder(){
    framedecoder = new QR_frame_decoder;
    return 0;
};


int set_decoded_file_path(const char* path){
    if(framedecoder != NULL)
        framedecoder->tell_file_generation_path(path);
    else
        return (int)API_NOT_INITIALIZED;
};


immediate_status send_next_grayscale_buffer_to_decoder(
        const char* grayscale_qr_data,
        int image_width,
        int image_height){
    if(framedecoder != NULL)
        return framedecoder->send_next_grayscale_qr_frame(grayscale_qr_data, image_width, image_height);
    else
        return API_NOT_INITIALIZED;
};


immediate_status tell_decoder_no_more_qr(){
    if(framedecoder != NULL)
        return framedecoder->tell_no_more_qr();
    else
        return API_NOT_INITIALIZED;
};


int get_total_frames_of_data_that_will_be_produced(){
    if(framedecoder != NULL)
        return framedecoder->get_total_frames_of_data_that_will_be_produced();
    else
        return (int)API_NOT_INITIALIZED;
};


int get_last_number_of_frame_detected(){
    if(framedecoder != NULL)
        return framedecoder->get_last_number_of_frame_detected();
    else
        return (int)API_NOT_INITIALIZED;
};

int get_last_number_of_header_frame_detected(){
    if(framedecoder != NULL)
        return framedecoder->get_last_number_of_header_frame_detected();
    else
        return (int)API_NOT_INITIALIZED;
};


int get_main_RSN(){
    return framedecoder->get_main_RSN();
};

int get_main_RSK(){
    return framedecoder->get_main_RSK();
};

int get_residual_RSN(){
    return framedecoder->get_residual_RSN();
};

int get_residual_RSK(){
    return framedecoder->get_residual_RSK();
};


int deinitialize_decoder(){
    if (framedecoder != NULL)
        delete framedecoder;
    framedecoder = NULL;
    return 0;
};
