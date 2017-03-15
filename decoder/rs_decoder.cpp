#include "rs_decoder.h"
#include "fec.h"



OpenRSEncodedFrame::OpenRSEncodedFrame(int RSn, int RSk) :
    EncodedFrame::EncodedFrame(RSn, RSk){}

OpenRSEncodedFrame::OpenRSEncodedFrame() :
    EncodedFrame::EncodedFrame(){}

void OpenRSEncodedFrame::set_frame_RSnk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
};

void OpenRSEncodedFrame::set_frame_capacity(uint16_t capacity){

};

bool OpenRSEncodedFrame::is_header_frame(){

};

uint32_t OpenRSEncodedFrame::get_frame_number(){
    return this->n_frame_;
};

void OpenRSEncodedFrame::set_frame_number(uint32_t frame_number){
    this->n_frame_ = frame_number;
};

void OpenRSEncodedFrame::set_max_frames(uint32_t max_frames){

};



RS_decoder::RS_decoder(){
    //this->bytes_currently_read_from_file_ = 0;
    this->internal_memory_ = NULL;
    this->n_dataframe_processed_ = 0;
    this->n_header_frame_processed_ = 0;
    //this->byte_of_file_currently_processed_to_frames_ = 0;
    this->file_data_.clear();
    this->RSfecDec = NULL;
    this->internal_RS_error_location_mem_ = NULL;
}

RS_decoder::~RS_decoder(){
    if(this->RSfecDec)
        free_rs_int(this->RSfecDec);
    if(this->internal_RS_error_location_mem_ != NULL)
        delete []this->internal_RS_error_location_mem_;
    if(this->internal_memory_ != NULL){
        delete []this->internal_memory_;
        this->internal_memory_ = NULL;
    }
}

void RS_decoder::send_next_frame(EncodedFrame* frame){

    int ipos = (frame->get_frame_number()) % this->RSn_;

    int nbits = utils::nbits_forsymcombinationsnumber(this->RSn_);

    int32_t numsym = utils::count_symbols_to_fit(this->RSn_, 256, frame->framedata_.size());

    for (uint32_t j = 0; j < numsym; j++){ //iterate over symbols within a frame
        uint32_t val = utils::get_data(&(frame->framedata_[4]), (j * this->RSk_+ ipos)*nbits, nbits);
        if(val>this->RSn_)
            DLOG("ERROR decoder - value bigger than allowed symbol value !!!!\n");
        this->internal_memory_[ipos+j*this->RSn_] = val;
    }

};


Decoder::detector_status RS_decoder::get_detector_status(){

};

void RS_decoder::set_RS_nk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
    if(this->internal_memory_ != NULL){
        delete this->internal_memory_;
        this->internal_memory_ = NULL;
    }
    this->internal_memory_ = new uint32_t[n*this->n_channels_];
    memset(this->internal_memory_,0 , n*this->n_channels_*sizeof(uint32_t));

    int i = 0;
    while ((1<<RS_decoder::RSfecCodeConsts[i].symsize) != n+1)
        i++;
    this->RSfecCodeConsts_index_ = i;
    this->RSfecDec = init_rs_int(
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].symsize,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].genpoly,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].fcs,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].prim,
            n - k,
            0);
    if(this->internal_RS_error_location_mem_ != NULL){
        delete []this->internal_RS_error_location_mem_;
        this->internal_RS_error_location_mem_ = new int[this->RSn_];
    }
};


RS_decoder::codeconst RS_decoder::RSfecCodeConsts[] = {
 {2, 0x7,     1,   1, 1, 10 },
 {3, 0xb,     1,   1, 2, 10 },
 {4, 0x13,    1,   1, 4, 10 },
 {5, 0x25,    1,   1, 6, 10 },
 {6, 0x43,    1,   1, 8, 10 },
 {7, 0x89,    1,   1, 10, 10 },
 {8, 0x11d,   1,   1, 32, 10 },
 {9, 0x211,   1,   1, 32, 10 },
 {10,0x409,   1,   1, 32, 10 },
 {11,0x805,   1,   1, 32, 10 },
 {12,0x1053,  1,   1, 32, 5 },
 {13,0x201b,  1,   1, 32, 2 },
 {14,0x4443,  1,   1, 32, 1 },
 {15,0x8003,  1,   1, 32, 1 },
 {16,0x1100b, 1,   1, 32, 1 },
};
