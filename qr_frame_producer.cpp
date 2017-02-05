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
    printf("size %d \n", get_file_size(this->filename_.c_str()));
    this->encoder_ = new OpenRSEncoder();
}


int Qr_frame_producer::produce_next_qr_image_to_file(const char* imagepath){
    printf("Producing image..\n");
    return 0;
};
