#include "qr_frame_producer.h"
#include <libqrencoder_wrapper.h>
#include "hash-library/sha256.h"
#include <iostream>
#include <math.h>
#include "globaldefs.h"
#include <libgen.h>

Qr_frame_producer::Qr_frame_producer()
{
    this->metadata_encoder_ = NULL;
    this->total_chars_per_QR_ = 31;
    this->is_header_frame_generating_ = true;
    this->setup_metadata_encoder();
    //this->setup_encoder();
    int max_target_width = 1300;
    init_libqrencoder(max_target_width*max_target_width*2);
    this->file_info_.filelength = 0;
    this->file_info_.filename = std::string("");
    this->file_info_.filepath = std::string("");
    this->file_info_.filename_without_any_path = std::string("");
    this->file_info_.fp = NULL;
    this->current_position_of_file_to_process_ = 0;
    this->last_frame_num_produced_by_encoder_ = 0;
    this->is_first_dataframe_number_offset_reconfigured_on_the_res_decoder_ = false;
    this->total_frame_numbers_that_will_be_produced_ = 0;
    this->nfr_done_ = 0;
    this->ndataframe_done_ = 0;
    this->is_header_frame_generating_switch_pending_ = false;
}

Qr_frame_producer::~Qr_frame_producer(){
    if(this->encoder_!=NULL)
        delete this->encoder_;
    if(this->metadata_encoder_!=NULL)
        delete this->metadata_encoder_;
    finish_libqrencoder();
    if(this->file_info_.fp != NULL){
        FileClose(this->file_info_.fp);
        this->file_info_.fp = NULL;
    }
}

void Qr_frame_producer::calculate_file_content_hash(int hash_chunk_size){
    if (this->file_info_.fp == NULL)
        return;
    int chunk_num = this->file_info_.filelength / hash_chunk_size;
    int bytes_remain = this->file_info_.filelength % hash_chunk_size;
    SHA256 sha256stream;
    uint32_t lastpos = 0;
    for(int i = 0; i < chunk_num; i++){
        if(read_file_fp(this->file_info_.fp, fixed_filehash_buff,
                        i*fixed_filehash_buff_size, fixed_filehash_buff_size) == -1){
                            DLOG("ERR, failed to read file !\n");
                            return;
                        }
        sha256stream.add(fixed_filehash_buff, fixed_filehash_buff_size); //hash
        lastpos += fixed_filehash_buff_size;
    }
    if (bytes_remain > 0){
        if(read_file_fp(this->file_info_.fp, fixed_filehash_buff,
                        lastpos, bytes_remain) == -1){
                            DLOG("ERR, failed to read file !\n");
                            return;
                        }
        sha256stream.add(fixed_filehash_buff, bytes_remain); //hash
    }
    std::string h_small = sha256stream.getHash();

    this->file_info_.hash.resize(8);
    std::string hs_low  = std::string(h_small.c_str(), 8);
    uint32_t hs_wlow = (uint32_t)strtoul(hs_low.c_str(), NULL, 16);
    *((uint32_t*)(&this->file_info_.hash[0])) = hs_wlow;
    std::string hs_high = std::string(h_small.c_str() + 8, 8);
    uint32_t hs_whigh = (uint32_t)strtoul(hs_high.c_str(), NULL, 16);
    *((uint32_t*)(&this->file_info_.hash[4])) = hs_whigh;
};

int Qr_frame_producer::set_external_file_info(const char* filename, const char* filepath, int suggested_qr_payload_length){
    this->total_chars_per_QR_ = suggested_qr_payload_length;
    this->setup_metadata_encoder();
    this->file_info_.filename = std::string(filename);
    this->file_info_.filename_without_any_path = std::string(basename((char*)filename));
    this->file_info_.filepath = std::string(filepath);
    if(this->file_info_.fp != NULL){
        FileClose(this->file_info_.fp);
        this->file_info_.fp = NULL;
    }
    std::string fullname;
    if(strlen(filepath) > 0)
        fullname = this->file_info_.filepath + "/" + this->file_info_.filename;
    else
        fullname = this->file_info_.filename;
    this->file_info_.fp = FileOpenToRead(fullname.c_str());
    if(this->file_info_.fp == NULL)
        return -1;

    uint32_t fsize = get_file_size_fp(this->file_info_.fp);
    if(fsize > 0) {
        this->file_info_.filelength = fsize;
        //hash file
        this->calculate_file_content_hash(fixed_filehash_buff_size);
    }
    this->produce_metadata();
    return 1;
};

void Qr_frame_producer::setup_metadata_encoder(){
    if(this->metadata_encoder_ != NULL)
        delete this->metadata_encoder_;
    this->metadata_.resize(fixed_metadata_arr_size);
    memset(&(this->metadata_[0]), 'a', fixed_metadata_arr_size * sizeof(char));
    this->metadata_encoder_ = new OpenRSEncoder();
    this->metadata_encoder_->set_filename("");
    this->metadata_encoder_->set_filelength(0);
    this->metadata_encoder_->set_datafeed_provider(this);
    this->metadata_encoder_->set_is_header_frame_generating(true);
    uint32_t n = fixed_N_metadata;


    int bestfit = utils::count_symbols_to_fit(n,
                                              256,
                                              this->total_chars_per_QR_ - 6)-1;
    this->metadata_encoder_->set_nchannels_parallel(bestfit);
    this->metadata_encoder_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 6);
    this->metadata_encoder_->set_RS_nk(n, fixed_K_metadata); //redundancy level
}

int Qr_frame_producer::estimate_capacity(int N, int K, int charperQR){
    int bytes = 0;
    int bestfit = utils::count_symbols_to_fit(N,
                                              256,
                                              charperQR - 4) - 1;
    bytes = K * bestfit * utils::nbits_forsymcombinationsnumber(N) / 8;
    return bytes;
}

//medatada DD pattern
// MMMMTThhh..hhHHH..HH|NNKKnnkkQQQQQLLhhhh..hhcc..ccc
// MM - 0xBAADA551 - magic byte seq
// 2byte length total, 1 byte length of hash, XB hash metadata, XB hash metadata + variable length content |
// 4B (N,K), 4B (n,k), 5Bfilelength(Q), 2B length fname, 1B hash length, XB file name, XB file hash content

void Qr_frame_producer::produce_metadata(){
    int spos = 0;
    bool cont = true;
    int optimal_rsn = 127;
    int optimal_rsk = 63;
    // estimate remain (n,k)
    int nch = utils::count_symbols_to_fit(optimal_rsn, 256, this->total_chars_per_QR_ - 4) - 1;
    int datalength_per_chunk = optimal_rsk * nch * utils::nbits_forsymcombinationsnumber(optimal_rsn) / 8;
    int remain_length = this->file_info_.filelength % datalength_per_chunk;
    int chunk_length = this->file_info_.filelength / datalength_per_chunk;
    this->datalength_per_chunk_ = datalength_per_chunk;
    this->remain_length_ = remain_length;
    this->chunk_length_ = chunk_length;
    int curr_size = 0;
    int curr_power = 2;
    do{
        curr_size = this->estimate_capacity((1 << curr_power) - 1, (1 << curr_power) / 2, this->total_chars_per_QR_);
        curr_power += 1;
    }while(curr_size<remain_length);
    int remN = (1 << curr_power) - 1;
    int remK = remN - ((1 << curr_power) / 2);
    if (remN > optimal_rsn)
        remN = optimal_rsn;
    if (remK > optimal_rsk)
        remK = optimal_rsk;
    this->setup_encoder(optimal_rsn, optimal_rsk, remN, remK);
    // fill metadata array
    while (cont){
        char* start = &this->metadata_[spos];
        ///first run
        int pos = 0;
        *((uint32_t*)start) = 0xBAADA551; // magic bytes
        pos+=4; // skip magic bytes
        pos+=2; //skip total length
        char* header_hash = start + pos;
        pos += 8;//skip over header hash
        pos += 8;//skip over header hash with variable length content
        char* NKpos = start + pos;
        *((uint16_t*) NKpos) = optimal_rsn;
        *((uint16_t*) (NKpos+2)) = optimal_rsk;
        pos += 4; //skip over main (N,K) field
        *((uint16_t*) (pos+start)) = remN;
        *((uint16_t*) (pos+2+start)) = remK;
        pos += 4; //skip over remain (N,K) field
        *((uint32_t*)(start+pos)) = this->file_info_.filelength;
        *((uint8_t*)(start+pos+4)) = 0; //remain
        pos += 5; //skip over the filelength field
        *((uint16_t*) (pos+start)) = this->file_info_.filename_without_any_path.length();
        pos += 2; //skip over filenamelength field
        *((uint32_t*)(start+pos)) = *((uint32_t*)(&this->file_info_.hash[0]));
        *((uint32_t*)(start+pos+4)) = *((uint32_t*)(&this->file_info_.hash[4]));
        pos += 8;//skip over filedata hash
        memcpy(start+pos, this->file_info_.filename_without_any_path.c_str(),
               this->file_info_.filename_without_any_path.length());
        pos += this->file_info_.filename_without_any_path.length(); // skip all filename characters for now

        *((uint16_t*) (start+4)) = pos;//save total length

        //hash small hash - not the filename content and not the hashes itself
        SHA256 sha256stream;
        sha256stream.add(start, 6); //hash
        sha256stream.add(start + 22, 23);
        std::string h_small = sha256stream.getHash();

        std::string hs_low  = std::string(h_small.c_str(), 8);
        uint32_t hs_wlow = (uint32_t)strtoul(hs_low.c_str(), NULL, 16);
        *((uint32_t*)(start + 6)) = hs_wlow;
        std::string hs_high = std::string(h_small.c_str() + 8, 8);
        uint32_t hs_whigh = (uint32_t)strtoul(hs_high.c_str(), NULL, 16);
        *((uint32_t*)(start + 10)) = hs_whigh;

        //hash big - file name text as well
        SHA256 sha256streamB;
        sha256streamB.add(start, 6); //hash
        sha256streamB.add(start + 22, 23);
        sha256streamB.add(start + 45, this->file_info_.filename_without_any_path.length());
        std::string h_big = sha256streamB.getHash();

        std::string hb_low  = std::string(h_big.c_str(), 8);
        uint32_t hb_wlow = (uint32_t)strtoul(hb_low.c_str(), NULL, 16);
        *((uint32_t*)(start + 6 + 8)) = hb_wlow;
        std::string hb_high = std::string(h_big.c_str() + 8, 8);
        uint32_t hb_whigh = (uint32_t)strtoul(hb_high.c_str(), NULL, 16);
        *((uint32_t*)(start + 10 + 8)) = hb_whigh;

        spos += pos;
        ///
        if(spos > fixed_metadata_arr_size - 512)
            cont = false;
    }

}

void Qr_frame_producer::setup_encoder(uint32_t N, uint32_t K, uint32_t rN, uint32_t rK){

    this->file_info_.RSn = N;
    this->file_info_.RSk = K;
    this->file_info_.RSn_residual = rN;
    this->file_info_.RSk_residual = rK;

    this->encoder_ = new OpenRSEncoder();
    this->encoder_->set_filename(this->filename_.c_str());
    this->encoder_->set_filelength(this->file_info_.filelength);
    this->encoder_->set_fileread_start_offset(0);
    this->encoder_->set_datafeed_provider(this);
    this->encoder_->set_is_header_frame_generating(false);
    this->encoder_->set_is_header_frame_generating(false);

    //256 - combination, not the redundancy level below!
    //if more than 256, then the nchannels < total_chars_per_QR_
    this->encoder_->set_nchannels_parallel(utils::count_symbols_to_fit(N,
                                                                       256,
                                                                       this->total_chars_per_QR_ - 4)-1);
    this->encoder_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 4);
    this->encoder_->set_RS_nk(N, K); //redundancy level


    this->encoder_res_ = new OpenRSEncoder();
    this->encoder_res_->set_filename(this->filename_.c_str());
    this->encoder_res_->set_filelength(this->file_info_.filelength);
    this->encoder_res_->set_fileread_start_offset(this->datalength_per_chunk_ * this->chunk_length_);
    this->encoder_res_->set_datafeed_provider(this);
    this->encoder_res_->set_is_header_frame_generating(false);
    this->encoder_res_->set_is_header_frame_generating(false);


   //256 - combination, not the redundancy level below!
   //if more than 256, then the nchannels < total_chars_per_QR_
    this->encoder_res_->set_nchannels_parallel(utils::count_symbols_to_fit(rN,
                                                                           256,
                                                                           this->total_chars_per_QR_ - 4)-1);
    this->encoder_res_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 4);
    this->encoder_res_->set_RS_nk(rN, rK); //redundancy level

    this->total_frame_numbers_that_will_be_produced_ = this->chunk_length_ * N + rN;
}

int Qr_frame_producer::tell_no_more_generating_header(){
    this->is_header_frame_generating_switch_pending_ = true;
    return 0;
}

int Qr_frame_producer::produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width){
    DLOG("Producing image to mem..\n");

    //check if there is no header->data switch pending, and apply it in the right moment
    if(( (this->nfr_done_+0) % fixed_N_metadata == 0) && this->is_header_frame_generating_switch_pending_){
        this->is_header_frame_generating_ = false;
        this->is_header_frame_generating_switch_pending_ = false;
    }

    OpenRSEncodedFrame *frame = new OpenRSEncodedFrame();
    if(this->is_header_frame_generating_){
        this->metadata_encoder_->set_is_header_frame_generating(true);
        this->metadata_encoder_->produce_next_encoded_frame(frame);
    }else{
        Encoder* current_encoder;
        uint32_t will_frame = 1 + this->encoder_->get_last_produced_dataframe_number();

        if(will_frame < this->chunk_length_ * this->file_info_.RSn){
            current_encoder = this->encoder_;
            //this->last_frame_num_produced_by_encoder_ = this->encoder_->get_last_produced_dataframe_number();
        }
        else{
            current_encoder = this->encoder_res_;
            if(this->chunk_length_ == 0)
                will_frame = 0;
            if(this->encoder_res_->get_last_produced_dataframe_number() == 0 &&
               !this->is_first_dataframe_number_offset_reconfigured_on_the_res_decoder_){
                this->encoder_res_->set_first_dataframe_number_offset(will_frame);
                this->is_first_dataframe_number_offset_reconfigured_on_the_res_decoder_ = true;
            }
        }
        current_encoder->set_is_header_frame_generating(false);
        current_encoder->produce_next_encoded_frame(frame);
    }
    DLOG("Frame number : %d\n", (int)frame->get_frame_number());
    int resulting_width;
    char* generated_grayscale_data;
    int size = frame->framedata_.size()-end_corruption_overhead;
    int margin = 3;
    generate_qr_greyscale_bitmap_data(&frame->framedata_[0],
                                           size,
                                           &generated_grayscale_data,
                                           &resulting_width,
                                           margin);
    *produced_image = generated_grayscale_data;
    *produced_width = resulting_width;
    delete frame;
    int status;

    this->nfr_done_++;
    if(!this->is_header_frame_generating_)
        this->ndataframe_done_++;

    if(this->is_header_frame_generating_)
        status = 0;
    else
        status = (this->ndataframe_done_ > this->total_frame_numbers_that_will_be_produced_);

    return status;
};

int Qr_frame_producer::produce_next_qr_image_to_file(const char* imagename){
    DLOG("Producing image..\n");
    int resw;
    char* res_graybuf;
    int status = this->produce_next_qr_grayscale_image_to_mem(&res_graybuf, &resw);
    FILE *f = fopen(imagename, "wb");
    fwrite(res_graybuf, resw*resw, 1, f);
    fclose(f);
    return status;
};


char* Qr_frame_producer::file;


int Qr_frame_producer::getFileData(FileChunk *chunk){
    if(chunk->reason == 0){
        uint32_t nread =
            read_file_fp(this->file_info_.fp,
                         chunk->chunkdata,
                         chunk->chunk_fileoffset,
                         chunk->chunk_length);
        this->current_position_of_file_to_process_ += chunk->chunk_length;
        return nread;
    }else if (chunk->reason == 1){
        char* metadata = &(this->metadata_[chunk->chunk_fileoffset]);
        if (chunk->chunkdata != NULL)
            memcpy(chunk->chunkdata, metadata, chunk->chunk_length);
        else
            chunk->chunkdata = metadata;
    }
}
