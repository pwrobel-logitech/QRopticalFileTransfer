#include "qr_frame_decoder.h"

QR_frame_decoder::QR_frame_decoder(){
    this->decoder_ = new RS_decoder();
    this->RSn_ = 511;
    this->RSk_ = 256;
    this->decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(this->RSn_, 256, 32-4));
    this->decoder_->set_RS_nk(this->RSn_, this->RSk_);
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
    ////////////////////////// process generated_data = extract frame number
    uint32_t nfr = *((uint32_t*)generated_data);
    if(nfr < this->decoder_->get_nframe()){ //impossible - we got less than previously received
        ret_status = ERRONEUS;
        return ret_status;
    }

    if(nfr > this->decoder_->get_nframe() + this->RSn_){//we missed too many frames - recovery will be impossible for sure
        ret_status = ERRONEUS;
        return ret_status;
    }
    int ipos = nfr % this->RSn_;

    EncodedFrame* fr = new OpenRSEncodedFrame();
    fr->set_frame_number(nfr);
    fr->framedata_.resize(generated_datalength);
    memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
    fr->set_frame_RSnk(this->decoder_->get_RSn(), this->decoder_->get_RSk());

    this->decoder_->send_next_frame(fr);

    delete []generated_data;
    return ret_status;
}
