#include "qr_frame_decoder.h"

QR_frame_decoder::QR_frame_decoder(){
    this->decoder_ = new RS_decoder();
}

QR_frame_decoder::~QR_frame_decoder(){
    if (this->decoder_ != NULL)
        delete this->decoder_;
}

immediate_status QR_frame_decoder::send_next_grayscale_qr_frame(const char *grayscale_qr_data,
                                                                int image_width, int image_height){
    immediate_status ret_status = RECOGNIZED;

    return ret_status;
}
