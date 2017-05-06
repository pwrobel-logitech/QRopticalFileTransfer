

//returned status after we pass the qr image data
enum immediate_status {
    NOT_RECOGNIZED, // on the passed image, there were no detected qr image
    AMBUGUOUS, // more than one qr image detected
    HEADER_ALREADY_DETECTED,
    ERRONEUS_HASH_WRONG,
    ERRONEUS, // detected, but the frame number is wrong or missed too many frames
              // if received only once, the whole detection is revoked and resources must be released
    ERR_DATAFRAME_TOO_EARLY, // got data frame, without recognizing the header first from previous frames
    RECOGNIZED, // some qr code has been recognized for sure
    ALREADY_CORRECTLY_TRANSFERRED
};


//
extern "C"{

// call this at the beginning of the usage
int initialize_encoder();

//set the path that will be used to flush the successfully decoded files to
int set_decoded_file_path(const char* path);

// send the next grayscale 8bit buffer with the QR image to the initialized decoder
// returned status indicates the information about what to do -
// for example when the ERRONEUS is received we must suspend any further decoding and deinitialize
// telling that the decoding of that pariticular file has failed
immediate_status send_next_grayscale_buffer_to_decoder(
        const char* grayscale_qr_data,
        int image_width,
        int image_height);


// execute this to inform the decoder that no further frames will be delivered for that particular file
// for sure
immediate_status tell_decoder_no_more_qr();

// call this at the end of the usage
int deinitialize_encoder();

}
