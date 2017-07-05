
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
bool mydecoder_initialized = false;


int initialize_decoder(){
    if (mydecoder_initialized){
        if (framedecoder != NULL){
            immediate_status stat = framedecoder->destroy_and_get_filetransfer_status();
            delete framedecoder;
        }
    }
    framedecoder = new QR_frame_decoder;
    mydecoder_initialized = true;
    return 0;
};


int set_decoded_file_path(const char* path){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        framedecoder->tell_file_generation_path(path);
    else
        return (int)API_NOT_INITIALIZED;
};


immediate_status send_next_grayscale_buffer_to_decoder(
        const char* grayscale_qr_data,
        int image_width,
        int image_height){
    if (!mydecoder_initialized)
        return API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        return framedecoder->send_next_grayscale_qr_frame(grayscale_qr_data, image_width, image_height);
    else
        return API_NOT_INITIALIZED;
};


immediate_status tell_decoder_no_more_qr(){
    if (!mydecoder_initialized)
        return API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        return framedecoder->tell_no_more_qr();
    else
        return API_NOT_INITIALIZED;
};


int get_total_frames_of_data_that_will_be_produced(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        return framedecoder->get_total_frames_of_data_that_will_be_produced();
    else
        return (int)API_NOT_INITIALIZED;
};


int get_last_number_of_frame_detected(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        return framedecoder->get_last_number_of_frame_detected();
    else
        return (int)API_NOT_INITIALIZED;
};

int get_last_number_of_header_frame_detected(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    if(framedecoder != NULL)
        return framedecoder->get_last_number_of_header_frame_detected();
    else
        return (int)API_NOT_INITIALIZED;
};


char filename_array[4096];
const char* get_last_recognized_file_name_str(){
    if (!mydecoder_initialized)
        return NULL;
    if (framedecoder != NULL)
        framedecoder->copy_recognized_filename_to_provided_memory(filename_array, sizeof(filename_array));
    return (const char*)filename_array;
};

int get_main_RSN(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    return framedecoder->get_main_RSN();
};

int get_main_RSK(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    return framedecoder->get_main_RSK();
};

int get_residual_RSN(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    return framedecoder->get_residual_RSN();
};

int get_residual_RSK(){
    if (!mydecoder_initialized)
        return (int)API_NOT_INITIALIZED;
    return framedecoder->get_residual_RSK();
};


immediate_status deinitialize_decoder(){
    if (!mydecoder_initialized)
        return API_NOT_INITIALIZED;
    immediate_status stat = framedecoder->destroy_and_get_filetransfer_status();
    if (framedecoder != NULL)
        delete framedecoder;
    framedecoder = NULL;
    mydecoder_initialized = false;
    return stat;
};
