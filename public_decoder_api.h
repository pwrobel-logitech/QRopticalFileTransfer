

//returned status after we pass the qr image data
enum immediate_status {
    API_NOT_INITIALIZED = -1, // you must call the initialization function after release of resource
    NOT_RECOGNIZED = 0, // on the passed image, there were no detected qr image
    AMBUGUOUS = 1, // more than one qr image detected
    HEADER_ALREADY_DETECTED = 2,
    ERRONEUS_HASH_WRONG = 3,
    ERRONEUS = 4, // detected, but the frame number is wrong or missed too many frames
              // if received only once, the whole detection is revoked and resources must be released
              //usually delivered asynchronously on some other frame status, when the previous RS decode has failed
    ERR_DATAFRAME_TOO_EARLY = 5, // got data frame, without recognizing the header first from previous frames
    RECOGNIZED = 6, // some qr code has been recognized for sure
    ALREADY_CORRECTLY_TRANSFERRED = 7,
    NOT_INITIALIZED = 8, // told to finish qr generation, but not even initialized before with the first frame
    LENGTH_QR_CHANGED = 9, //new QR frame has the new length - migh want to kill current transfer and start the new one
    NEW_HEADER_FRAME_IN_THE_MIDDLE_OF_DATA_DETECTION = 10, //when receiving this, it is advisable to tell no more qr frames and immediately reset decoder
    NO_ASYNC_RESULT_KNOWN_YET = 11 //so far no info about the async failure in the middle of the detection has been delivered
};

#define EXPRTa __attribute__((visibility ("default")))

//
extern "C"{

// call this at the beginning of the usage
EXPRTa int initialize_decoder();

//set the path that will be used to flush the successfully decoded files to
EXPRTa int set_decoded_file_path(const char* path);

// send the next grayscale 8bit buffer with the QR image to the initialized decoder
// returned status indicates the information about what to do -
// for example when the ERRONEUS is received we must suspend any further decoding and deinitialize
// telling that the decoding of that pariticular file has failed
EXPRTa immediate_status send_next_grayscale_buffer_to_decoder(
        const char* grayscale_qr_data,
        int image_width,
        int image_height);


// execute this to inform the decoder that no further frames will be delivered for that particular file
// for sure
EXPRTa immediate_status tell_decoder_no_more_qr();


//-1 if header frame is still being generated by the decoder
// otherwise, this will return the total number of frames
// that the decoder will produce. This might be used to estimate the
// moment when the production of the frames has stopped
// (in case the very last frame has been omitted)
EXPRTa int get_total_frames_of_data_that_will_be_produced();

// those two can be used to estimate which frames are missing
// this can be used to create statistic about the current progress
// and the efficiency of the frame recognition
// -1 means no frame of given type has been processed so far
EXPRTa int get_last_number_of_frame_detected();
EXPRTa int get_last_number_of_header_frame_detected();


//returns pointer to already preallocated array, where the null-terminated string with the
//file name recognized from the header is present. Gives only reasonable result if the
//file has already been detected from the stream - so in other words when
//get_total_frames_of_data_that_will_be_produced() > 0 or /*when status is 2*/
EXPRTa const char* get_last_recognized_file_name_str();


//this tells the size of the last recoginzed file in bytes. -1 if not known yet
EXPRTa int get_last_recognized_file_size();

//this get the RS correction codes settings for the residual and the main decoder
//in case they are not known - for example, when the header has not been recognized yet - they are -1
EXPRTa int get_main_RSN();
EXPRTa int get_main_RSK();
EXPRTa int get_residual_RSN();
EXPRTa int get_residual_RSK();


// call this at the end of the usage of the decoder
//at the end of the destruction we join with the RS processing thread and know for sure the actual
//file transfer status including checking the file hash
//delivers ALREADY_CORRECTLY_TRANSFERRED if file is checked for consistency successfully and saved
//or ERRONEUS_HASH_WRONG if the transmited temporary file has the wrong hash
EXPRTa immediate_status deinitialize_decoder();

//get native arch string
EXPRTa const char *get_qrdecoderpwrobelARCH_string();

}
