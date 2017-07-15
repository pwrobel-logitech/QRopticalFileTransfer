

//this initializes the encoder for the one particular file
//gives standalone file name and path to that file we wish to encode
//suggested_qr_payload_length = byte capacity of the given QR frame. Above 585 is problem with
//fast decoding so far
//err_fraction - ratio = (N-K)/N - this implies which K set for given N
//suggested_N = N value of the RS code, default 511, for small chunk 255, for large 1023
extern "C" __attribute__ ((visibility ("default"))) int init_and_set_external_file_info(const char* filename,
                                    const char* filepath,
                                    int suggested_qr_payload_length,
                                    double err_fraction,
                                    int suggested_N);


//we wish to get the next greyscale square memory buffer of size
//obtained in the produced_width*produced_width
//call it periodically, and after few seconds tell no more header to get some data
// -2 if internally encoder is not initialized yet
extern "C" __attribute__ ((visibility ("default"))) int produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width);



//tells that we wish stop obtaining header frames, and wish to get the data frames instead
// will generate header frame no more, -1 if the not enough header generated
// -2 if internally encoder is not initialized yet
extern "C" __attribute__ ((visibility ("default"))) int tell_no_more_generating_header();

//after frame generation is done, this will tell in advance how much data frames will be generated
extern "C" __attribute__ ((visibility ("default"))) int tell_how_much_frames_will_be_generated();

//0 if destroyed successfully, -1 if it was destroyed internally before
extern "C" __attribute__ ((visibility ("default"))) int destroy_current_encoder();




