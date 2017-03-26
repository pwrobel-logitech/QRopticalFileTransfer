#include "rs_decoder.h"


class QR_frame_decoder {
public:

    QR_frame_decoder();
    ~QR_frame_decoder();

    // deliver next qr image data
    // detector should itself recognize which frame number does it corresponds to
    immediate_status send_next_grayscale_qr_frame(const char* grayscale_qr_data,
                                                  int image_width, int image_height);

    immediate_status tell_no_more_qr();


protected:
    uint32_t RSn_, RSk_;
    Decoder* decoder_;
    Decoder* header_decoder_;
    void reconfigure_qr_size(int qrlen); // only header frame, starting with the 0xffffffff can trigger the reconfiguration
    bool is_header_generating_;
};
