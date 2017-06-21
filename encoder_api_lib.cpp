#include <qr_frame_producer.h>
#include "public_encoder_api.h"

Qr_frame_producer* internal_frame_producer = NULL;


int init_and_set_external_file_info(const char* filename,
                                    const char* filepath,
                                    int suggested_qr_payload_length){
    if (internal_frame_producer == NULL){
        internal_frame_producer = new Qr_frame_producer;
        internal_frame_producer->set_external_file_info(filename, filepath, suggested_qr_payload_length);
    }
    return 0;
};



int produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width){
    if (internal_frame_producer == NULL)
        return -2;
    return internal_frame_producer->produce_next_qr_grayscale_image_to_mem(produced_image, produced_width);
};



int tell_no_more_generating_header(){
    if (internal_frame_producer == NULL){
        return -2;
    }
    return internal_frame_producer->tell_no_more_generating_header();
};

int tell_how_much_frames_will_be_generated(){
    if (internal_frame_producer == NULL)
        return -1;
    return internal_frame_producer->tell_how_much_frames_will_be_generated();
};

int destroy_current_encoder(){
    if (internal_frame_producer != NULL){
        delete internal_frame_producer;
        internal_frame_producer = NULL;
    }
};
