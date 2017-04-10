#include "qr_frame_producer.h"
#include <libqrencoder_wrapper.h>
#include "hash-library/sha256.h"
#include <iostream>



Qr_frame_producer::Qr_frame_producer()
{
    this->total_chars_per_QR_ = 31;
    this->is_header_frame_generating_ = true;
    this->setup_metadata_encoder();
    this->setup_encoder();
    int max_target_width = 1300;
    init_libqrencoder(max_target_width*max_target_width*2);
    this->file_info_.filelength = 0;
    this->file_info_.filename = std::string("");
    this->file_info_.filepath = std::string("");
    this->file_info_.fp = NULL;
    this->iframe_counter_ = 0;
}

Qr_frame_producer::~Qr_frame_producer(){
    if(this->encoder_!=NULL)
        delete this->encoder_;
    if(this->metadata_encoder_!=NULL)
        delete this->metadata_encoder_;
    finish_libqrencoder();
    if(this->file_info_.fp != NULL)
        FileClose(this->file_info_.fp);
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
    uint32_t hs_wlow = (uint32_t)strtol(hs_low.c_str(), NULL, 16);
    *((uint32_t*)(&this->file_info_.hash[0])) = hs_wlow;
    std::string hs_high = std::string(h_small.c_str() + 8, 8);
    uint32_t hs_whigh = (uint32_t)strtol(hs_high.c_str(), NULL, 16);
    *((uint32_t*)(&this->file_info_.hash[4])) = hs_whigh;
};

int Qr_frame_producer::set_external_file_info(const char* filename, const char* filepath, int suggested_qr_payload_length){
    this->total_chars_per_QR_ = suggested_qr_payload_length;
    this->file_info_.filename = std::string(filename);
    this->file_info_.filepath = std::string(filepath);
    if(this->file_info_.fp != NULL)
        FileClose(this->file_info_.fp);
    std::string fullname = this->file_info_.filepath + this->file_info_.filename;
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
    this->metadata_.resize(fixed_metadata_arr_size);
    memset(&(this->metadata_[0]), 'a', fixed_metadata_arr_size * sizeof(char));
    this->metadata_encoder_ = new OpenRSEncoder();
    this->metadata_encoder_->set_filename("");
    this->metadata_encoder_->set_filelength(0);
    this->metadata_encoder_->set_datafeed_provider(this);
    this->metadata_encoder_->set_is_header_frame_generating(true);
    uint32_t n = 7;


    int bestfit = utils::count_symbols_to_fit(n,
                                              256,
                                              this->total_chars_per_QR_ - 6)-1;
    this->metadata_encoder_->set_nchannels_parallel(bestfit);
    this->metadata_encoder_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 6);
    this->metadata_encoder_->set_RS_nk(n, 3); //redundancy level
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
    int optimal_rsn = 511;
    int optimal_rsk = 256;
    // estimate remain (n,k)
    int nch = utils::count_symbols_to_fit(optimal_rsn, 256, this->total_chars_per_QR_ - 4) - 1;
    int datalength_per_chunk = optimal_rsk * nch * utils::nbits_forsymcombinationsnumber(optimal_rsn) / 8;
    int remain_length = this->file_info_.filelength % datalength_per_chunk;
    int chunk_length = this->file_info_.filelength / datalength_per_chunk;
    int curr_size = 0;
    int curr_power = 2;
    do{
        curr_size = this->estimate_capacity((1 << curr_power) - 1, (1 << curr_power) / 2, this->total_chars_per_QR_);
        curr_power += 1;
    }while(curr_size<remain_length);
    int remN = (1 << curr_power) - 1;
    int remK = (1 << curr_power) / 2;
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
        *((uint16_t*) (pos+start)) = this->file_info_.filename.length();
        pos += 2; //skip over filenamelength field
        *((uint32_t*)(start+pos)) = *((uint32_t*)(&this->file_info_.hash[0]));
        *((uint32_t*)(start+pos+4)) = *((uint32_t*)(&this->file_info_.hash[4]));
        pos += 8;//skip over filedata hash
        memcpy(start+pos, this->file_info_.filename.c_str(), this->file_info_.filename.length());
        pos += this->file_info_.filename.length(); // skip all filename characters for now

        *((uint16_t*) (start+4)) = pos;//save total length

        //hash small hash - not the filename content and not the hashes itself
        SHA256 sha256stream;
        sha256stream.add(start, 6); //hash
        sha256stream.add(start + 22, 23);
        std::string h_small = sha256stream.getHash();

        std::string hs_low  = std::string(h_small.c_str(), 8);
        uint32_t hs_wlow = (uint32_t)strtol(hs_low.c_str(), NULL, 16);
        *((uint32_t*)(start + 6)) = hs_wlow;
        std::string hs_high = std::string(h_small.c_str() + 8, 8);
        uint32_t hs_whigh = (uint32_t)strtol(hs_high.c_str(), NULL, 16);
        *((uint32_t*)(start + 10)) = hs_whigh;

        //hash big - file name text as well
        SHA256 sha256streamB;
        sha256streamB.add(start, 6); //hash
        sha256streamB.add(start + 22, 23);
        sha256streamB.add(start + 45, this->file_info_.filename.length());
        std::string h_big = sha256streamB.getHash();

        std::string hb_low  = std::string(h_big.c_str(), 8);
        uint32_t hb_wlow = (uint32_t)strtol(hb_low.c_str(), NULL, 16);
        *((uint32_t*)(start + 6 + 8)) = hb_wlow;
        std::string hb_high = std::string(h_big.c_str() + 8, 8);
        uint32_t hb_whigh = (uint32_t)strtol(hb_high.c_str(), NULL, 16);
        *((uint32_t*)(start + 10 + 8)) = hb_whigh;

        spos += pos;
        ///
        if(spos > fixed_metadata_arr_size - 512)
            cont = false;
    }

}

void Qr_frame_producer::setup_encoder(){

    this->encoder_ = new OpenRSEncoder();
    this->encoder_->set_filename(this->filename_.c_str());
    this->encoder_->set_filelength(this->file_info_.filelength);
    this->encoder_->set_datafeed_provider(this);
    this->encoder_->set_is_header_frame_generating(false);
    ///print some test data
    DLOG("size of the file %d \n", this->file_info_.filelength);
    //print 8 bytes at offset 4
    char textfrag[8];
    memset(textfrag, 0, sizeof(char));
    read_file(this->filename_.c_str(), textfrag, 4, 8);
    DLOG("Eight bytes of text at offset 4: %s\n", textfrag);

    this->encoder_->set_is_header_frame_generating(false);

    //if(filesize > 2000){
        uint32_t n = 511;

        //256 - combination, not the redundancy level below!
        //if more than 256, then the nchannels < total_chars_per_QR_
        this->encoder_->set_nchannels_parallel(utils::count_symbols_to_fit(n,
                                                                           256,
                                                                           this->total_chars_per_QR_ - 4)-1);
        this->encoder_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 4);
        this->encoder_->set_RS_nk(n, 256); //redundancy level
    //}
}

int Qr_frame_producer::tell_no_more_generating_header(){
    this->is_header_frame_generating_ = false;
    return 0;
}

int Qr_frame_producer::produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width){
    DLOG("Producing image to mem..\n");
    OpenRSEncodedFrame *frame = new OpenRSEncodedFrame();
    if(this->is_header_frame_generating_){
        this->metadata_encoder_->set_is_header_frame_generating(true);
        this->metadata_encoder_->produce_next_encoded_frame(frame);
    }else{
        this->encoder_->set_is_header_frame_generating(false);
        this->encoder_->produce_next_encoded_frame(frame);
    }
    DLOG("Frame number : %d\n", (int)frame->get_frame_number());
    int resulting_width;
    char* generated_grayscale_data;
    generate_qr_greyscale_bitmap_data(&frame->framedata_[0],
                                           frame->framedata_.size(),
                                           &generated_grayscale_data,
                                           &resulting_width,
                                          1);
    *produced_image = generated_grayscale_data;
    *produced_width = resulting_width;
    delete frame;
    this->iframe_counter_++;
    return 0;
};

int Qr_frame_producer::produce_next_qr_image_to_file(const char* imagename){
    DLOG("Producing image..\n");
    char namebuf[60];
    snprintf(namebuf, sizeof(namebuf), imagename, this->iframe_counter_);
    int resw;
    char* res_graybuf;
    this->produce_next_qr_grayscale_image_to_mem(&res_graybuf, &resw);
    FILE *f = fopen(namebuf, "wb");
    fwrite(res_graybuf, resw*resw, 1, f);
    fclose(f);
    return 0;
};


char* Qr_frame_producer::file;


int Qr_frame_producer::getFileData(FileChunk *chunk){
    if(chunk->reason == 0){
        return read_file_fp(this->file_info_.fp,
                  chunk->chunkdata,
                  chunk->chunk_fileoffset,
                  chunk->chunk_length);
    }else if (chunk->reason == 1){
        char* metadata = &(this->metadata_[chunk->chunk_fileoffset]);
        chunk->chunkdata = metadata;
    }
}
