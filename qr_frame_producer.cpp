#include "qr_frame_producer.h"
#include <libqrencoder_wrapper.h>

const int fixed_metadata_arr_size = 16*4096;

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
    if(fsize > 0)
        this->file_info_.filelength = fsize;
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

void Qr_frame_producer::produce_metadata(){
    int pos = 0;
    bool cont = true;
    int optimal_rsn = 511;
    int optimal_rsk = 256;
    while (cont){
        char* start = &this->metadata_[pos];
        pos++;
        if(pos > fixed_metadata_arr_size - 512)
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

int Qr_frame_producer::produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width){
    DLOG("Producing image to mem..\n");
    OpenRSEncodedFrame *frame = new OpenRSEncodedFrame();
    this->encoder_->produce_next_encoded_frame(frame);
    DLOG("Frame number mem : %d\n", (int)frame->get_frame_number());
    int resulting_width;
    char* generated_grayscale_data;
    generate_qr_greyscale_bitmap_data(&frame->framedata_[0],
                                           frame->framedata_.size(),
                                           &generated_grayscale_data,
                                           &resulting_width,
                                           1);
    *produced_image = generated_grayscale_data;
    *produced_width = resulting_width;
    delete []frame;
    return 0;
};

int Qr_frame_producer::produce_next_qr_image_to_file(const char* imagename){

    DLOG("Producing image..\n");
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
    char namebuf[60];
    snprintf(namebuf, sizeof(namebuf), imagename, frame->get_frame_number());
    FILE *f = fopen(namebuf, "wb");
    fwrite(generated_grayscale_data, resulting_width*resulting_width, 1, f);
    fclose(f);
    delete frame;
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
