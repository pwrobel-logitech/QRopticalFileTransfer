
#include "qrencode.h"

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
void generate_image_data(const unsigned char* input_data, int input_length, unsigned char** out_image_data, int *out_image_data_size);


}
