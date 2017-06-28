//#include "../public_decoder_api.h"
//#include "../common_decoder_encoder.h"


enum immediate_status;

extern "C" {

/////////// qr recognition part
///
/// so far implemented with the usage of the CPP port of the xzing lib
/// under the 	https://github.com/embarkmobile/zxing-cpp
/// the caller of that function must take care of deleting the buffer
immediate_status generate_data_from_qr_greyscalebuffer(int* generated_datalength, char** generated_data, const char* input_greyscale_buffer,
                                           int width, int height);


}
