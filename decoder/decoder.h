#include "encoder.h"
#include "libqrencoder_wrapper/libqrencoder_wrapper.h"

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
    virtual void send_next_frame(const EncodedFrame* frame) = 0;

    // as explained above
    virtual detector_status get_detector_status() = 0;

protected:
    // this will keep the current status of the detector
    // if ever returns TOO_MUCH_ERRORS then the file is not
    // recoverable any more and the whole decoding process should be aborted
    detector_status status_;
};
