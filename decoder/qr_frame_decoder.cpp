#include "qr_frame_decoder.h"

QR_frame_decoder::QR_frame_decoder(){
    this->decoder_ = new RS_decoder();
    this->RSn_ = 511;
    this->RSk_ = 256;
    this->decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(this->RSn_, 256, 33-4)-1);
    this->decoder_->set_RS_nk(this->RSn_, this->RSk_);
    this->decoder_->set_bytes_per_generated_frame(33-4);

    //
    this->header_decoder_ = new RS_decoder();
}

void QR_frame_decoder::reconfigure_qr_size(int qrlen){

    int headerRSn = 7;
    int headerRSk = 3;

    this->header_decoder_->set_header_frame_generating(true);
    this->header_decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(headerRSn, 256, qrlen-6)-1);
    this->header_decoder_->set_bytes_per_generated_frame(qrlen-6);
    this->header_decoder_->set_RS_nk(headerRSn, headerRSk);


    this->decoder_->set_header_frame_generating(false);
    this->decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(this->RSn_, 256, qrlen-4)-1);
    this->decoder_->set_bytes_per_generated_frame(qrlen-4);
    this->decoder_->set_RS_nk(this->RSn_, this->RSk_);
    this->decoder_->set_configured(true);
}

QR_frame_decoder::~QR_frame_decoder(){
    if (this->decoder_ != NULL)
        delete this->decoder_;
}

immediate_status QR_frame_decoder::tell_no_more_qr(){
    immediate_status stat = RECOGNIZED;
    if (this->decoder_){
        this->decoder_->tell_no_more_qr();
    }
    return stat;
};

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

    if(nfr==0xffffffff && (this->header_decoder_->get_configured()==false)){
        this->is_header_generating_ = true;
        DCHECK(generated_datalength > 8);
        this->reconfigure_qr_size(generated_datalength);
    }


    if(this->is_header_generating_){
        printf("qqr %d, ",nfr);
        for(int k = 0; k<generated_datalength-6;k++)printf("0x%02hhx ", (generated_data+6)[k]);
        printf("\n");
    }else{
        printf("qqr %d, ",nfr);
        for(int k = 0; k<generated_datalength-4;k++)printf("0x%02hhx ", (generated_data+4)[k]);
        printf("\n");
    }
    /*
    if(nfr < this->decoder_->get_nframe()){ //impossible - we got less than previously received
        ret_status = ERRONEUS;
        return ret_status;
    }

    if(nfr > this->decoder_->get_nframe() + this->RSn_){//we missed too many frames - recovery will be impossible for sure
        ret_status = ERRONEUS;
        return ret_status;
    }
    */
    int ipos = nfr % this->RSn_;

    uint32_t nhfr = *((uint16_t*)(generated_data+4));

    EncodedFrame* fr = new OpenRSEncodedFrame();

    if(this->is_header_generating_){
        fr->set_frame_number(nhfr);
        fr->framedata_.resize(generated_datalength);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(this->header_decoder_->get_RSn(), this->header_decoder_->get_RSk());

        printf("header frame set %d\n",nfr);
        RS_decoder::detector_status dec_status = this->header_decoder_->send_next_frame(fr);
        if (dec_status == Decoder::TOO_MUCH_ERRORS)
            ret_status = ERRONEUS;
    }else{

        fr->set_frame_number(nfr);
        fr->framedata_.resize(generated_datalength);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(this->decoder_->get_RSn(), this->decoder_->get_RSk());

        printf("frame set %d\n",nfr);
        RS_decoder::detector_status dec_status = this->decoder_->send_next_frame(fr);
        if (dec_status == Decoder::TOO_MUCH_ERRORS)
            ret_status = ERRONEUS;
    }
    delete []generated_data;
    return ret_status;
}
