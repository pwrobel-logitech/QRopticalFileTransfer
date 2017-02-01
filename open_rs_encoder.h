#include "encoder.h"

class OpenRSEncodedFrame : public EncodedFrame{
    void set_frame_capacity(uint16_t capacity);
    bool is_header_frame();
    uint32_t get_frame_number();
    void set_frame_number(uint32_t frame_number);
    void set_max_frames(uint32_t max_frames);
};



class OpenRSEncoder : public Encoder{

    void set_filename(char* filename);

    void set_filelength(uint32_t file_length);

    //encoder will call function passed as a callback here, to request for more file chunk
    void set_datafeed_callback(needDataCB cb);

    void set_hashlength(uint16_t hash_length);

    uint8_t* compute_hash();

    generated_frame_status produce_next_encoded_frame(EncodedFrame* frame);
};

