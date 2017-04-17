

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>

#include <string.h>


//returned status after we pass the qr image data
enum immediate_status {
    NOT_RECOGNIZED, // on the passed image, there were no detected qr image
    AMBUGUOUS, // more than one qr image detected
    ERRONEUS_HASH_WRONG,
    ERRONEUS, // detected, but the frame number is wrong or missed too many frames
              // if received only once, the whole detection is revoked and resources must be released
    ERR_DATAFRAME_TOO_EARLY, // got data frame, without recognizing the header first from previous frames
    RECOGNIZED, // some qr code has been recognized for sure
    ALREADY_CORRECTLY_TRANSFERRED
};

size_t compressImage(char* buffer, size_t width, size_t height, unsigned char** outputBuffer, int quality);

extern "C" {

// init libqrencoder - prepare to use with data of given fixed size
void init_libqrencoder(int size);

// deallocate all the resources needed for the libqrencoder module
void finish_libqrencoder();

// for given data of input length, allocate and generate the data of the image
// responsibility to release allocated memory for the generated image data lies
// on whoever called that function
// pass max target width - function will write the actual choosen width for the image
void generate_image_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size, int *max_target_width);

// the same as above - but only it creates smallest possible jpeg, and return size to target_width
// it also supports width multiplier - for smallest image it will be 1
void generate_small_image_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size, int *target_width,
                               int width_multiplier);

// same as above - but adds some white margin of size 'margin' - in units of generated smallest qr image pixel (get multiplied by the multiplier as well)
void generate_small_image_data_margin(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size, int *target_width,
                               int width_multiplier, int margin);



/////////// qr recognition part
///
/// so far implemented with the usage of the CPP port of the xzing lib
/// under the 	https://github.com/embarkmobile/zxing-cpp
/// the caller of that function must take care of deleting the buffer
immediate_status generate_data_from_qr_greyscalebuffer(int* generated_datalength, char** generated_data, const char* input_greyscale_buffer,
                                           int width, int height);

// compatible decoder to the encoder described above
void generate_qr_greyscale_bitmap_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_width,
                                       int margin);

}
