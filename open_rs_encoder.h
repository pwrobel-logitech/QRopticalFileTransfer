#include "encoder.h"

class OpenRSEncodedFrame : public EncodedFrame{
public:
    OpenRSEncodedFrame(int RSn, int RSk);
    OpenRSEncodedFrame();
    void set_frame_capacity(uint16_t capacity);
    bool is_header_frame();
    uint32_t get_frame_number();
    void set_frame_number(uint32_t frame_number);
    void set_max_frames(uint32_t max_frames);
    void set_frame_RSnk(uint16_t n, uint16_t k);
};



class OpenRSEncoder : public Encoder{

public:
    OpenRSEncoder();

    ~OpenRSEncoder();

    void set_filename(const char* filename);

    void set_filelength(uint32_t file_length);

    //encoder will call function passed as a callback here, to request for more file chunk
    void set_datafeed_callback(needDataCB cb);

    void set_hashlength(uint16_t hash_length);

    void set_RS_nk(uint16_t n, uint16_t k);

    void set_nchannels_parallel(uint32_t nch);

    void set_nbytes_data_per_generated_frame(uint16_t nbytes);

    uint8_t* compute_hash();

    generated_frame_status produce_next_encoded_frame(EncodedFrame* frame);

    static struct codeconst {
        int symsize;
        int genpoly;
        int fcs;
        int prim;
        int nroots;
        int ntrials;
      } RSfecCodeConsts[];

protected:
    // used to store symbols containing the data for the series of frames + the RS redundancy symbols
    uint32_t* internal_memory_;
    //number of the sections processed in parrarell - corresponds to the
    //number of data symbols held on, for example, single QR frame
    //not the same as bytes_per_generated_frame_ - because the character can be larger than 1byte
    uint32_t n_channels_;

    uint32_t n_header_frame_processed_;
    uint32_t n_dataframe_processed_;

    //RS encode
    bool apply_RS_code_to_internal_memory();
    uint32_t RSfecCodeConsts_index_; //which index is currently used.
    void* RSfecEnc;
    // RS decode
    bool apply_RS_decode_to_internal_memory();
    int* internal_RS_error_location_mem_;


    //to test if the created data matches the original
    bool recreate_original_arr(uint32_t* symbols_arr, char** data_produced, uint32_t* length_produced);
};

