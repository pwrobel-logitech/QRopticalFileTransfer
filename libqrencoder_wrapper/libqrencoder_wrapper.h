

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>

#include <string.h>


size_t compressImage(char* buffer, size_t width, size_t height, unsigned char** outputBuffer, int quality);

extern "C" {

enum image_type{
    IMAGE_TYPE_PNG,
    IMAGE_TYPE_JPEG,
    IMAGE_TYPE_BMP,
    IMAGE_TYPE_RAW,
};

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
///
/// TODO  : make it return the status to tell if the decoding was successfull or not
void generate_data_from_qr_greyscalebuffer(int* generated_datalength, char** generated_data, char* input_greyscale_buffer,
                                           int width);

// compatible decoder to the encoder described above
void generate_qr_greyscale_bitmap_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_width,
                                       int margin);

}
