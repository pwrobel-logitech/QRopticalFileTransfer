#include "decoder.h"

class RS_decoder : public Decoder {
public:

    void send_next_frame(const EncodedFrame* frame);

    // as explained above
    detector_status get_detector_status();


    bool is_header_parsing(){return mode_header_parsing_;}

protected:

    bool mode_header_parsing_;

    // used to store symbols containing the data for the series of frames + the RS redundancy symbols
    uint32_t* internal_memory_;
    //number of the sections processed in parrarell - corresponds to the
    //number of data symbols held on, for example, single QR frame
    //not the same as bytes_per_generated_frame_ - because the character can be larger than 1byte
    uint32_t n_channels_;


    uint32_t n_header_frame_processed_;
    uint32_t n_dataframe_processed_;

    //raw binary file data in terms of array of file chunks
    std::vector<FileChunk*> file_data_;


    //This pair encode the redundancy of the QR frames.
    //It is the classical Reed-Solomon code of the (n,k)
    //Typical code to correct up to 2 bits of errors would stand as (255,251)
    //It would correspond that each bit of the series of 255 frames encodes only
    //the 251 bits of real data - 4 frames would constitue the overhead.
    uint16_t RSn_;
    uint16_t RSk_;
};
