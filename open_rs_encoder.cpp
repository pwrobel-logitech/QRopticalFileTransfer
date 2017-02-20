#include "open_rs_encoder.h"
#include <memory.h>


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
    this->byte_of_file_currently_processed_to_frames_ = 0;
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

void OpenRSEncoder::set_nbytes_data_per_generated_frame(uint16_t nbytes){
    this->bytes_per_generated_frame_ = nbytes;
};

void OpenRSEncoder::set_RS_nk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
    if(this->internal_memory_ != NULL){
        delete this->internal_memory_;
        this->internal_memory_ = NULL;
    }
    this->internal_memory_ = new uint32_t[n*this->n_channels_];
    memset(this->internal_memory_,0 , n*this->n_channels_*sizeof(uint32_t));
};

uint8_t* OpenRSEncoder::compute_hash(){

};

Encoder::generated_frame_status OpenRSEncoder::produce_next_encoded_frame(EncodedFrame* frame){
    frame->set_frame_RSnk(this->RSn_, this->RSk_);
    if (this->byte_of_file_currently_processed_to_frames_ >= this->bytes_currently_read_from_file_){
        uint32_t mem_to_read = (this->RSk_) * this->bytes_per_generated_frame_;
        FileChunk* chunk = new FileChunk();
        chunk->chunkdata = new char[mem_to_read];
        chunk->chunk_length = mem_to_read;
        chunk->chunk_fileoffset = bytes_currently_read_from_file_;
        this->file_data_.push_back(chunk);
        (this->needData_)(chunk);
        bytes_currently_read_from_file_+= mem_to_read;


        char* file_read_start = (this->file_data_.back())->chunkdata +
            this->byte_of_file_currently_processed_to_frames_;

        for (uint32_t j = 0; j<this->n_channels_; j++){ //iterate over symbols within a frame
            for (uint32_t i = 0; i<this->RSk_; i++){ //iterate over frame numbers with data
            //utils::set_data(begin, i*utils::nbits_forsymcombinationsnumber(this->RSn_),
             //               file_read_start);
                uint32_t val = utils::get_data(file_read_start,
                                               (j * this->RSk_+ i)*utils::nbits_forsymcombinationsnumber(this->RSn_),
                                                utils::nbits_forsymcombinationsnumber(this->RSn_));
                this->internal_memory_[i+j*this->RSn_] = val;
            }
        }
    }
    this->byte_of_file_currently_processed_to_frames_ += this->bytes_per_generated_frame_;


};
