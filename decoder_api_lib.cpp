
#include "qr_frame_decoder.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>


QR_frame_decoder* framedecoder = NULL;


FILE *f = NULL;
char namebuf[200];

immediate_status last_status;


int initialize_decoder(){
    framedecoder = new QR_frame_decoder;
    return 0;
};


int set_decoded_file_path(const char* path){
    framedecoder->tell_file_generation_path(path);
    return 0;
};


immediate_status send_next_grayscale_buffer_to_decoder(
        const char* grayscale_qr_data,
        int image_width,
        int image_height){
    return framedecoder->send_next_grayscale_qr_frame(grayscale_qr_data, image_width, image_height);
};


immediate_status tell_decoder_no_more_qr(){
    return framedecoder->tell_no_more_qr();;
};


int get_total_frames_of_data_that_will_be_produced(){
    return framedecoder->get_total_frames_of_data_that_will_be_produced();
};


int get_last_number_of_frame_detected(){
    return framedecoder->get_last_number_of_frame_detected();
};

int get_last_number_of_header_frame_detected(){
    return framedecoder->get_last_number_of_header_frame_detected();
};


int deinitialize_decoder(){
    delete framedecoder;
    framedecoder = NULL;
    return 0;
};
