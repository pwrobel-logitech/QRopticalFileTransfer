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
    this->RSfecEnc = NULL;
    this->internal_RS_error_location_mem_ = NULL;
};

OpenRSEncoder::~OpenRSEncoder(){
    if(this->RSfecEnc)
        free_rs_int(this->RSfecEnc);
    if(this->internal_RS_error_location_mem_ != NULL)
        delete []this->internal_RS_error_location_mem_;
    if(this->internal_memory_ != NULL){
        delete this->internal_memory_;
        this->internal_memory_ = NULL;
    }
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

    int i = 0;
    while ((1<<OpenRSEncoder::RSfecCodeConsts[i].symsize) != n+1)
        i++;
    this->RSfecCodeConsts_index_ = i;
    this->RSfecEnc = init_rs_int(
            OpenRSEncoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].symsize,
            OpenRSEncoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].genpoly,
            OpenRSEncoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].fcs,
            OpenRSEncoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].prim,
            n - k,
            0);
    if(this->internal_RS_error_location_mem_ != NULL){
        delete []this->internal_RS_error_location_mem_;
        this->internal_RS_error_location_mem_ = new int[this->RSn_];
    }
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
                if(val>this->RSn_)
                    DLOG("ERROR - value bigger than allowed symbol value !!!!\n");
                this->internal_memory_[i+j*this->RSn_] = val;
            }
        }
        this->apply_RS_code_to_internal_memory();

        // apply few errors/erasures - without the RS decode, that would generate some errors, because the recreated array would
        // differ to the original
        this->internal_memory_[5]=0;
        this->internal_memory_[8]=0;
        this->internal_memory_[250]=0;
        this->internal_memory_[514]=0;

        this->apply_RS_decode_to_internal_memory();
        char* test_data;
        uint32_t length_of_test_data = 0;
        this->recreate_original_arr(this->internal_memory_, &test_data, &length_of_test_data);
        for (uint32_t k = 0; k < length_of_test_data; k++){
            if (test_data[k] != file_read_start[k])
                DLOG("Error - difference of processed data on the %d position\n", k);
        }

    }
    this->byte_of_file_currently_processed_to_frames_ += this->bytes_per_generated_frame_;


};


bool OpenRSEncoder::recreate_original_arr(uint32_t *symbols_arr, char **data_produced, uint32_t* length_produced){
    *length_produced = this->bytes_per_generated_frame_ * this->RSk_;
    *data_produced = new char[*length_produced];
    memset(*data_produced, 0, *length_produced);
    for (uint32_t j = 0; j<this->n_channels_; j++)
        for (uint32_t i = 0; i<this->RSk_; i++){
            utils::set_data((void*)*data_produced, (j * this->RSk_+ i)*utils::nbits_forsymcombinationsnumber(this->RSn_), symbols_arr[i+j*this->RSn_]);
        }
    int k=0;
    return true;
}


bool OpenRSEncoder::apply_RS_code_to_internal_memory(){
    for (uint32_t j = 0; j < this->n_channels_; j++){
        encode_rs_int(this->RSfecEnc, j*this->RSn_ + (int*)this->internal_memory_,
                      j*this->RSn_ + (int*) &this->internal_memory_[this->RSk_]);
    }
    return true;
}

bool OpenRSEncoder::apply_RS_decode_to_internal_memory(){
    for (uint32_t j = 0; j < this->n_channels_; j++){
        int num_of_errors = decode_rs_int(this->RSfecEnc, j*this->RSn_ + (int*)this->internal_memory_,
                                         this->internal_RS_error_location_mem_, 0);
    }
};



OpenRSEncoder::codeconst OpenRSEncoder::RSfecCodeConsts[] = {
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
