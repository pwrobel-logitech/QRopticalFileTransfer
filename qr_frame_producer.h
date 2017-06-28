#include "common_decoder_encoder.h"
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include "open_rs_encoder.h"


class Qr_frame_producer : public FileDataProvider
{
public:
    Qr_frame_producer();
    ~Qr_frame_producer();
    //-1 = error, 0 - ok, 1 - finished
    // image template - "dump_%d" = will generate dump_1, dump_2, dump_3.. files
    int produce_next_qr_image_to_file(const char* imagetemplate);
    int produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width);
    int tell_no_more_generating_header(); // will generate header frame no more, -1 if the not enough header generated

    //returns the amount of frames that will be produced by the encoder.
    int tell_how_much_frames_will_be_generated();

    //static int needMetaData(FileChunk*);
    //static int needData(FileChunk*);
    static char* file;

    std::vector<char> metadata_; //metadata for the file is held here

    int set_external_file_info(const char* filename, const char* filepath, int suggested_qr_payload_length,
                               double suggested_errfraction, int suggested_N);

private:

    double suggested_err_ratio;
    double suggested_N;

    int getFileData(FileChunk* chunk);
    void setup_metadata_encoder();
    void setup_encoder(uint32_t N, uint32_t K, uint32_t rN, uint32_t rK);
    ///we will pass it for the encoder as the data feeding callback

    std::string filename_;
    Encoder* encoder_;
    Encoder* encoder_res_;

    bool is_first_dataframe_number_offset_reconfigured_on_the_res_decoder_;

    // metadata encoder on its own is a decoder with a fixed RS()
    Encoder* metadata_encoder_;//this stores filename, length, hash, main RS(n,k) and remainded RS(n,k) -
                               //for the shorter file end
            


    // describes whether we are generating the header frame now.
    bool is_header_frame_generating_;

    // decribes if there is any header/data mode switch pending
    bool is_header_frame_generating_switch_pending_;


    //default qr data length size
    uint16_t total_chars_per_QR_;

    FileInfo file_info_;

    //data used to split the file into the main part handled by the main decoder and the residual part
    //handled by the residual decoder with padding of the data
    //big decoder handles this much bytes per chunk
    uint32_t datalength_per_chunk_;
    //this is the number of bytes the small decoder will receive in its one chunk
    //Pad with 0s if it requests for more bytes
    uint32_t remain_length_;
    //number of chunks in the big decoder
    uint32_t chunk_length_;


    //keeps the current position within the file the encoders are so far processed
    uint32_t current_position_of_file_to_process_;

    //last framenumber produced by the main encoder or the res encoder if used
    uint32_t last_frame_num_produced_by_encoder_;

    void produce_metadata();
    void calculate_file_content_hash(int hash_chunk_size);

    //in bytes
    int estimate_capacity(int N, int K, int charperQR);

    //used to estimate if we are done with the frame generation
    uint32_t total_frame_numbers_that_will_be_produced_;
    uint32_t nfr_done_; //how many we have processed so far
    uint32_t ndataframe_done_;
};






