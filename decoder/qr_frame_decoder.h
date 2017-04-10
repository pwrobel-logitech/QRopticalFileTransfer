#include "../common_decoder_encoder.h"
#include "rs_decoder.h"


class QR_frame_decoder : public ChunkListener {
public:

    QR_frame_decoder();
    ~QR_frame_decoder();

    // deliver next qr image data
    // detector should itself recognize which frame number does it corresponds to
    immediate_status send_next_grayscale_qr_frame(const char* grayscale_qr_data,
                                                  int image_width, int image_height);

    immediate_status tell_no_more_qr();
    void print_current_header();
    void print_current_maindata();

    int notifyNewChunk(int chunklength, const char* chunkdata, int context);

protected:

    uint32_t qr_byte_length; // how many bytes one single qr frame stores
                             // must be uniform across the different frames

    uint32_t RSn_, RSk_;
    uint32_t RSn_rem_, RSk_rem_;
    Decoder* decoder_;
    Decoder* res_decoder_; //residual decoder for the last (RSn_rem_, RSk_rem_) pair
    Decoder* header_decoder_;
    void reconfigure_qr_size(int qrlen); // only header frame, starting with the 0xffffffff can trigger the reconfiguration
    bool is_header_generating_;

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
};
