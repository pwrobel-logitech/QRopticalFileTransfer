#include "encoder.h"
#include "libqrencoder_wrapper/libqrencoder_wrapper.h"

#ifdef DEBUG
#define DCHECK(a) if(!a)printf("Condition "#a" failed in line %d, file %s !\n", __LINE__, __FILE__);
#endif
// interface for the decoder class being able to decode the data from the delivered images
// containing the qr frames produced by the encoder class
class Decoder {
public:

    enum detector_status {
        STILL_OK, // not too much errors
        TOO_MUCH_ERRORS
    };

    // this will set the next encoded frame for the decoder
    // if we currently passing the header info - so that the actual (n,k)
    // of the RS code for the data is not yet known - pass the (0,0) pair
    // then this will get interpreted as the header metadata frame
    // and the special fixed short RS code will be used to pass the header metadata
    // itself - consistent across decoder and encoder
    // metadata contains the info such as RS code (n,k), filename, filesize, QRsize, filehash, etc.
    // we do not care if the frame has some errors - we set it as decoded from the qr image
    // then, each time, if too much errors are detected, and the file retrieval is not possible
    // any more - the status_ will tell us of TOO_MUCH_ERRORS by the get_detector_status() call
    // it is advisable to ask the status after sending RSn of frames - then the RS decoding will be done
    // and the status send to status_
    virtual void send_next_frame(EncodedFrame* frame) = 0;

    //this sets number of parralel channels processed by encoder -
    //number of data symbols per frame
    virtual void set_nchannels_parallel(uint32_t nch) = 0;

    // as explained above
    virtual detector_status get_detector_status() = 0;

    virtual void set_RS_nk(uint16_t n, uint16_t k) = 0;

    //number of the current frame index - 0 - first
    virtual uint32_t get_nframe() = 0;

    virtual uint16_t get_RSn() = 0;
    virtual uint16_t get_RSk() = 0;

protected:
    // this will keep the current status of the detector
    // if ever returns TOO_MUCH_ERRORS then the file is not
    // recoverable any more and the whole decoding process should be aborted
    detector_status status_;

    //RS code (n,k)
    uint16_t RSn_, RSk_;

};
