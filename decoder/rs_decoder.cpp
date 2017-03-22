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
    this->old_chunk_number_ = 0;
    this->status_ = RS_decoder::STILL_OK;
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

void RS_decoder::internal_getdata_from_internal_memory(){

    for(int k = 0; k<this->RSn_*this->n_channels_; k++)
        printf("ind %d, val %d\n",k, this->internal_memory_[k]);
    //

    char* data;
    uint32_t length=0;
    double t = utils::currmili();
    uint32_t nerr = this->apply_RS_decode_to_internal_memory();
    printf("Decode time %f\n", utils::currmili()-t);

    //for(int k = 0; k<this->RSn_*this->n_channels_; k++)
    //    printf("indadec %d, val %d\n",k, this->internal_memory_[k]);
    //

    if(nerr>0)
        DLOG("Warning, nerr = %d\n", nerr);
    if (nerr>(this->get_RSn()-this->get_RSk())/2)
        status_ = RS_decoder::TOO_MUCH_ERRORS;

    this->recreate_original_arr(this->internal_memory_, &data, &length);
    int k;
    printf("Trying to printf chunk: \n", data);
    for(int q=0;q<length;q++)printf("%c",data[q]);
    printf("\n");
    if(length > 0)
        delete []data;
}

RS_decoder::detector_status RS_decoder::tell_no_more_qr(){
    this->internal_getdata_from_internal_memory();
    return status_;
};

RS_decoder::detector_status RS_decoder::send_next_frame(EncodedFrame* frame){
    /////////////////// first action to recover previous chunk from the internal memory
    int ipos = (frame->get_frame_number()) % this->RSn_;

    uint32_t curr_chunk = (frame->get_frame_number()) / this->RSn_;

    if (curr_chunk > this->old_chunk_number_){ // time to decode the internal_memory_ + pack bits back to the original array
        this->internal_getdata_from_internal_memory();
    }
    /////////////////////////// action for the new frame that was actally send
    int nbits = utils::nbits_forsymcombinationsnumber(this->RSn_);

    int32_t numsym = this->n_channels_;

    //DCHECK(numsym==this->n_channels_);

    for (uint32_t j = 0; j < numsym; j++){ //iterate over symbols within a frame
        uint32_t val = utils::get_data(&(frame->framedata_[4]), j*nbits, nbits);
        if(val>this->RSn_)
            DLOG("ERROR decoder - value bigger than allowed symbol value !!!!\n");
        DCHECK(ipos+j*this->RSn_<this->RSn_*this->n_channels_);
        this->internal_memory_[ipos+j*this->RSn_] = val;
    }

    this->old_chunk_number_ = curr_chunk;

};

void RS_decoder::set_nchannels_parallel(uint32_t nch){
    this->n_channels_ = nch;
};

void RS_decoder::set_bytes_per_generated_frame(uint32_t nb){
    this->bytes_per_generated_frame_ = nb;
}

Decoder::detector_status RS_decoder::get_detector_status(){

};

void RS_decoder::set_RS_nk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
    this->status_ = RS_decoder::STILL_OK;
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
    }
    this->internal_RS_error_location_mem_ = new int[this->RSn_];
};


bool RS_decoder::recreate_original_arr(/*internal_memory*/uint32_t *symbols_arr,
                                       char **data_produced, uint32_t* length_produced){
    *length_produced = this->bytes_per_generated_frame_ * this->RSk_;
    *data_produced = new char[*length_produced];
    if(*data_produced == NULL)
        return false;
    memset(*data_produced, 0, *length_produced);
    for (uint32_t j = 0; j<this->n_channels_; j++)
        for (uint32_t i = 0; i<this->RSk_; i++){
            utils::set_data((void*)*data_produced, (j * this->RSk_+ i)*utils::nbits_forsymcombinationsnumber(this->RSn_), symbols_arr[i+j*this->RSn_]);
        }
    return true;
}

uint32_t RS_decoder::apply_RS_decode_to_internal_memory(){
    uint32_t nerr = 0;
    printf("\n");
    for (uint32_t j = 0; j < this->n_channels_; j++){
        uint32_t e = decode_rs_int(this->RSfecDec, j*this->RSn_ + (int*)this->internal_memory_,
                             NULL, 0);
        printf("%d ",e);
        if (e > nerr)
            nerr = e;
    }
    printf("\n");
    return nerr;
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
