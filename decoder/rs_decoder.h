#include "decoder.h"




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




class RS_decoder : public Decoder {
public:

    RS_decoder();
    ~RS_decoder();

    detector_status send_next_frame(EncodedFrame* frame);

    detector_status tell_no_more_qr();

    void set_nchannels_parallel(uint32_t nch);

    // as explained above
    detector_status get_detector_status();


    bool is_header_parsing(){return mode_header_parsing_;}

    void set_RS_nk(uint16_t n, uint16_t k);

    static struct codeconst {
        int symsize;
        int genpoly;
        int fcs;
        int prim;
        int nroots;
        int ntrials;
      } RSfecCodeConsts[];

    uint32_t get_nframe() {return n_dataframe_processed_;}

    uint16_t get_RSn(){return RSn_;}
    uint16_t get_RSk(){return RSk_;}

    bool recreate_original_arr(/*internal_memory*/uint32_t *symbols_arr,
                                   char **data_produced, uint32_t* length_produced);

    void set_bytes_per_generated_frame(uint32_t nb);

    void set_header_frame_generating(bool isheader);

    void set_configured(bool configured);
    bool get_configured();

    void set_chunk_listener(ChunkListener* l);

protected:

    bool mode_header_parsing_;

    // used to store symbols containing the data for the series of frames + the RS redundancy symbols
    uint32_t* internal_memory_;
    //this is for the libfec
    int* internal_RS_error_location_mem_;
    void* RSfecDec;
    uint32_t RSfecCodeConsts_index_; //which index is currently used.



    //number of the sections processed in parrarell - corresponds to the
    //number of data symbols held on, for example, single QR frame
    //not the same as bytes_per_generated_frame_ - because the character can be larger than 1byte
    uint32_t n_channels_;


    uint32_t n_header_frame_processed_;
    uint32_t n_dataframe_processed_;

    //raw binary file data in terms of array of file chunks
    std::vector<FileChunk*> file_data_;



    // each chunk fits into the whole internal memory of the detector - what was the last chunk
    // used to measure if the chunk has increased - then it's time to do the decoding
    uint32_t old_chunk_number_;

    //This pair encode the redundancy of the QR frames.
    //It is the classical Reed-Solomon code of the (n,k)
    //Typical code to correct up to 2 bits of errors would stand as (255,251)
    //It would correspond that each bit of the series of 255 frames encodes only
    //the 251 bits of real data - 4 frames would constitue the overhead.
    uint16_t RSn_;
    uint16_t RSk_;

    //this will apply RS code to each of the n-length section of the internal_memory
    //it's what makes possible to lose some frame, while still making the decoding possible
    //returns number of errors detected - if bigger than (n-k)/2 - the recovery of the original
    //data is not possible, and further processing by the decoder should be immediately stopped
    //telling the reason of why it's so
    uint32_t apply_RS_decode_to_internal_memory();

    void internal_getdata_from_internal_memory();
};
