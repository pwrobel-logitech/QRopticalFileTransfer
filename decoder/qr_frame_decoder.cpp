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
    int generated_datalength;
    char* generated_data;
    immediate_status ret_status =
            generate_data_from_qr_greyscalebuffer(&generated_datalength, &generated_data,
                                                  grayscale_qr_data,
                                                  image_width, image_height);
    ////////////////////////// process generated_data = extract frame number - fix the frame number adding in the
    /// encoder
    delete []generated_data;
    return ret_status;
}
