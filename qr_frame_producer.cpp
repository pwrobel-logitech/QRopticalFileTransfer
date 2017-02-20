#include "qr_frame_producer.h"


Qr_frame_producer::Qr_frame_producer(const char* file)
{
    this->filename_ = file;
    this->setup_encoder();
}

Qr_frame_producer::~Qr_frame_producer(){
    if(this->encoder_!=NULL)
        delete this->encoder_;
}

void Qr_frame_producer::setup_encoder(){
    uint32_t filesize = get_file_size(this->filename_.c_str());
    this->encoder_ = new OpenRSEncoder();
    this->encoder_->set_filename(this->filename_.c_str());
    this->encoder_->set_filelength(filesize);
    this->encoder_->set_datafeed_callback(Qr_frame_producer::needData);
    ///print some test data
    DLOG("size of the file %d \n", filesize);
    //print 8 bytes at offset 4
    char textfrag[8];
    memset(textfrag, 0, sizeof(char));
    read_file(this->filename_.c_str(), textfrag, 4, 8);
    DLOG("Eight bytes of text at offset 4: %s\n", textfrag);


    if(filesize > 2000){
        uint32_t n = 511;
        this->total_chars_per_QR_ = 32;
        //256 - combination, not the redundancy level below!
        //if more than 256, then the nchannels < total_chars_per_QR_
        this->encoder_->set_nchannels_parallel(utils::count_symbols_to_fit(n,
                                                                           256,
                                                                           this->total_chars_per_QR_ - 4));
        this->encoder_->set_nbytes_data_per_generated_frame(this->total_chars_per_QR_ - 4);
        this->encoder_->set_RS_nk(n, 256); //redundancy level
    }
}


int Qr_frame_producer::produce_next_qr_image_to_file(const char* imagepath){
    DLOG("Producing image..\n");
    OpenRSEncodedFrame *frame = new OpenRSEncodedFrame();
    this->encoder_->produce_next_encoded_frame(frame);
    DLOG("Frame number : %d\n",frame->get_frame_number());
    delete frame;
    return 0;
};


int Qr_frame_producer::needData(FileChunk *chunk){
    Qr_frame_producer* producer = Qr_frame_producer::getQr_frame_producer();
    read_file(producer->filename_.c_str(),
              chunk->chunkdata,
              chunk->chunk_fileoffset,
              chunk->chunk_length);
}
