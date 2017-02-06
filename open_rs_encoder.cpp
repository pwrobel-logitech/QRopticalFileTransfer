#include "open_rs_encoder.h"

///Encoded frame part
///
///

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

};

void OpenRSEncodedFrame::set_frame_number(uint32_t frame_number){

};

void OpenRSEncodedFrame::set_max_frames(uint32_t max_frames){

};


///////////Encoder part


OpenRSEncoder::OpenRSEncoder(){
    this->bytes_currently_read_from_file_ = 0;
    this->internal_memory_ = NULL;
    this->n_dataframe_processed_ = 0;
    this->n_header_frame_processed_ = 0;
    this->file_data_.clear();
};

OpenRSEncoder::~OpenRSEncoder(){

}

void OpenRSEncoder::set_filename(const char* name){
    this->filename_ = (const char*) name;
}

void OpenRSEncoder::set_filelength(uint32_t file_length){
    this->total_file_length_ = file_length;
};

void OpenRSEncoder::set_datafeed_callback(needDataCB cb){
    this->needData_ = cb;
};

void OpenRSEncoder::set_hashlength(uint16_t hash_length){

};

void OpenRSEncoder::set_nchannels_parallel(uint32_t nch){
    this->n_channels_ = nch;
}

void OpenRSEncoder::set_RS_nk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
    if(this->internal_memory_ == NULL)
        delete this->internal_memory_;
    this->internal_memory_ = new int[n*this->n_channels_];
};

uint8_t* OpenRSEncoder::compute_hash(){

};

Encoder::generated_frame_status OpenRSEncoder::produce_next_encoded_frame(EncodedFrame* frame){
    frame->set_frame_RSnk(this->RSn_, this->RSk_);
    //if(bytes_currently_processed_from_file_==bytes_currently_read_from_file_){
    uint32_t mem_to_read = this->RSk_ * this->n_channels_;
    FileChunk* chunk = new FileChunk();
    chunk->chunkdata = new char[mem_to_read];
    chunk->chunk_length = mem_to_read;
    chunk->chunk_fileoffset = bytes_currently_read_from_file_;
    this->file_data_.push_back(chunk);
    (this->needData_)(chunk);
    bytes_currently_read_from_file_+= mem_to_read;


    //}

};
