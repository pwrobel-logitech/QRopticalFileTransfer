#include "rs_decoder.h"


class QR_frame_decoder {
public:

    QR_frame_decoder();
    ~QR_frame_decoder();

    // deliver next qr image data
    // detector should itself recognize which frame number does it corresponds to
    immediate_status send_next_grayscale_qr_frame(const char* grayscale_qr_data,
                                                  int image_width, int image_height);

protected:
    RS_decoder* decoder_;
};
