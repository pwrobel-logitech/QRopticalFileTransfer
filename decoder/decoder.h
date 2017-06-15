#include "encoder.h"
#include "libqrencoder_wrapper/libqrencoder_wrapper.h"
#include <pthread.h>

//#ifdef DEBUG
//#define DCHECK(a) if(!(a))printf("Condition "#a" failed in line %d, file %s !\n", __LINE__, __FILE__);
//#endif

class ChunkListener;
class Decoder;

//define the data passed to another thread in order to complete the async task involving the RS decode
struct AsyncInfo{
    uint32_t* internal_mem;
    char* recreated_data;
    int* internal_RS_successfull_indexes_per_chunk_;
    int* internal_RS_erasure_location_mem_;
    int next_erasure_successful_num_position_;
    int RSn_;
    int RSk_;
    void* RSfecDec;
    bool is_header_frame_generating_;
    uint32_t n_channels_;
    bool is_switched_to_residual_data_decoder_;

    Decoder* current_decoder;

    bool async_completed;
    immediate_status completion_status; // so far, used to communicate failure in some RS decode
    ChunkListener* chlistener;

    pthread_t async_thr_id_;
    pthread_mutex_t async_mutex_; //mutex for protecting the concurrent con
    pthread_cond_t async_condvar_; //async thread waits on it to start processing - async_thread_waiting_
    pthread_cond_t async_main_wait_; //main is waiting for the async to complete - async_main_is_waiting_for_thread_to_complete_
    bool async_thread_alive_; // async thr alive
    bool async_thread_waiting_; //is thread waiting to do some processing
    bool async_main_is_waiting_for_thread_to_complete_;
};


//interface for the class listening for the ready chunk and appending to the chunk pool
class ChunkListener{
public:
    // context : 0 - file, 1 - metadata, 2 - trailing chunk
    // returns additional status info
    virtual int notifyNewChunk(int chunklength, const char* chunkdata, int context) = 0;
    virtual bool getIsSwitchedToResidualDataDecoder() = 0;
    virtual AsyncInfo* getAsyncInfo() = 0;
};



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
    virtual detector_status send_next_frame(EncodedFrame* frame) = 0;

    // we already know there will be no more frames send - tell decoder to finish all the decoding -
    // try to decode what we alread have  - and unpack bits to get the original array
    virtual detector_status tell_no_more_qr() = 0;


    //this sets number of parralel channels processed by encoder -
    //number of data symbols per frame - does not include 4 first bytes
    virtual void set_nchannels_parallel(uint32_t nch) = 0;

    // as explained above
    virtual detector_status get_detector_status() = 0;

    virtual void set_RS_nk(uint16_t n, uint16_t k) = 0;

    //number of the current frame index - 0 - first
    virtual uint32_t get_nframe() = 0;

    virtual uint16_t get_RSn() = 0;
    virtual uint16_t get_RSk() = 0;

    // this recreates the original data of the chunk from the internal_memory_
    // that contains the int symbols - usually 9bits [0..511] - by allocating new array
    // and informing about its length - caller of that function is responsible for
    // deallocating this array
    virtual bool recreate_original_arr(/*internal_memory*/uint32_t *symbols_arr,
                               char **data_produced, uint32_t* length_produced) = 0;

    virtual void set_bytes_per_generated_frame(uint32_t num_bytes) = 0;

    virtual void set_header_frame_generating(bool isheader) = 0;

    virtual void set_configured(bool configured) = 0;
    virtual bool get_configured() = 0;

    virtual void set_chunk_listener(ChunkListener* l) = 0;

    //the first number of frame this decoder should use - serves as an offset
    //note this is not necessarily the first received frame number - because the first frame that
    //should have been received may have been not passed - so needed to tell the decoder manually
    virtual void fist_proper_framedata_number_for_this_decoder(uint32_t first) = 0;

    //bool is_residual_; //for testing purposes only
    //bool processed_once_;//for testing

    virtual void execute_RS_async_action() = 0;

protected:
    // this will keep the current status of the detector
    // if ever returns TOO_MUCH_ERRORS then the file is not
    // recoverable any more and the whole decoding process should be aborted
    detector_status status_;

    //RS code (n,k)
    uint16_t RSn_, RSk_;

    //max number of bytes for the data payload of the each encoded frame produced - does not include
    //first 4 bytes designed for holding the frame number
    uint16_t bytes_per_generated_frame_;

    bool is_header_frame_generating_;

    bool configured_;

    //once we done with the decoding,
    ChunkListener* chunk_listener_;

};
