#include "open_rs_encoder.h"

///Encoded frame part

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

};

OpenRSEncoder::~OpenRSEncoder(){

}

void OpenRSEncoder::set_filename(char* name){
    this->filename_ = (const char*) name;
}

void OpenRSEncoder::set_filelength(uint32_t file_length){

};

void OpenRSEncoder::set_datafeed_callback(needDataCB cb){

};

void OpenRSEncoder::set_hashlength(uint16_t hash_length){

};

void OpenRSEncoder::set_RS_nk(uint16_t n, uint16_t k){

};

uint8_t* OpenRSEncoder::compute_hash(){

};

Encoder::generated_frame_status OpenRSEncoder::produce_next_encoded_frame(EncodedFrame* frame){

};
