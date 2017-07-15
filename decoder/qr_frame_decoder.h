
#include "rs_decoder.h"

#include "../common_decoder_encoder.h"

class __attribute__ ((visibility ("default"))) QR_frame_decoder : public ChunkListener {
public:

    QR_frame_decoder();
    ~QR_frame_decoder();
    ///////////API
    // deliver next qr image data
    // detector should itself recognize which frame number does it corresponds to
    immediate_status send_next_grayscale_qr_frame(const char* grayscale_qr_data,
                                                  int image_width, int image_height);

    immediate_status tell_no_more_qr();
    void print_current_header();
    void print_current_maindata();

    void tell_file_generation_path(const char* filepath);
    int get_total_frames_of_data_that_will_be_produced();

    int get_last_number_of_frame_detected();

    int get_last_number_of_header_frame_detected();

    //in case the header generation is not done, return -1
    int get_main_RSN();
    int get_main_RSK();
    int get_residual_RSN();
    int get_residual_RSK();

    //is called right before destructor, and does the actual destruction with returning the async status
    //at the end of the destruction we join with the RS processing thread and know for sure the actual
    //file transfer status including checking the file hash
    //delivers ALREADY_CORRECTLY_TRANSFERRED if file is checked for consistency successfully and saved
    //or ERRONEUS_HASH_WRONG if the transmited temporary file has the wrong hash
    immediate_status destroy_and_get_filetransfer_status();


    //once the header detection has been done, copy the file name to the provided memory
    void copy_recognized_filename_to_provided_memory(char* str, int maxlen);

    //-1 if it is not known yet
    int get_last_recognized_file_size();

    ////////////End API

    int notifyNewChunk(int chunklength, const char* chunkdata, int context);
    bool getIsSwitchedToResidualDataDecoder() {return this->is_switched_to_residual_data_decoder_;}


    //async info
    AsyncInfo* getAsyncInfo() {return &(this->async_info_);};

protected:

    uint32_t qr_byte_length; // how many bytes one single qr frame stores
                             // must be uniform across the different frames

    uint32_t RSn_, RSk_;
    uint32_t RSn_rem_, RSk_rem_;
    uint32_t decoder_bytes_len_;
    uint32_t decoder_res_bytes_len_;

    //what the res decoder should use as a first framenumber
    uint32_t first_proper_framenumber_into_res_decoder_;

    Decoder* decoder_;
    Decoder* res_decoder_; //residual decoder for the last (RSn_rem_, RSk_rem_) pair
    Decoder* header_decoder_;
    void reconfigure_qr_size(int qrlen); // only header frame, starting with the 0xffffffff can trigger the reconfiguration
    bool is_header_generating_;

    bool is_switched_to_residual_data_decoder_;

    std::vector<char> header_data_;
    std::vector<char> header_data_tmp_;

    std::vector<char> main_chunk_data_;
    std::vector<char> main_chunk_data_tmp_;

    // 1 - detected correct metadata - can stop any further header data processing
    // 0 - not yet processed the metadata, continue as normal
    // -1 - error
    int analyze_header();
    int last_analyzed_header_pos_; // to speed up the detection process
    bool header_detection_done_; // once the header has been checked to be processed
                                 // and all the hashes match, finish the detection
    void setup_detector_after_header_recognized(); // when correct header is detected
                                                   // do the RK setup
    FileInfo file_info_; //file info the info from the header is being saved to

    std::string api_told_filepath_;


    //flush it to the file detected in the fileinfo
    void flush_data_to_file(const char* data, uint32_t datalen);

    //flush according to this current offset
    uint32_t position_in_file_to_flush_;

    // check the hash of the written file to the one we were told in the header. True if the hash is OK
    bool is_file_hash_correct();


    //false when producing data. True, when all the file was flushed - no matter if it was confirmed to be correct or not
    bool is_all_file_processing_done_;
    //set to true only when the previous processing has been done, and the hash has been confirmed to be correct
    bool is_hash_of_flushed_file_correct_;

    //what was the last frame number that was successfully recognized
    //-1 means, that nothing has been processed yet
    int last_frame_number_processed_;
    int last_header_frame_number_processed_;


    ///Async part
    AsyncInfo async_info_;

    uint8_t encoder_version_;

    static void* thrfunc(void*);

};
