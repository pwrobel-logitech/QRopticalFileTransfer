#include "libqrencoder_wrapper.h"
#include "qrencode.h"
#include <iostream>
#include <stdlib.h>
#include <stdint.h>
#include <fstream>
#include <string>

char* raw_img_mem = NULL;

void init_libqrencoder(int size){
    if(raw_img_mem != NULL)
        delete []raw_img_mem;
    raw_img_mem = new char[size];
}

void finish_libqrencoder(){
    if(raw_img_mem != NULL)
        delete []raw_img_mem;
    raw_img_mem = NULL;
}

// generate qr grayscale bitmap
void generate_qr_greyscale_bitmap_data(const unsigned char* input_data, int input_length,
                                       char** out_image_data, int *out_image_data_width,
                                       int margin){
    QRcode *generatedQR = NULL;
    generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_L);
    unsigned char* QR_pixeldata = NULL;
    int QR_pixeldata_size = -1;
    if (generatedQR != NULL){
        QR_pixeldata = generatedQR->data;
        QR_pixeldata_size = generatedQR->width * generatedQR->width;
    }

    int w =  (generatedQR->width + 2*margin);
    *out_image_data_width = w;
    const int width_multiplier = 1;
    char* out_target_rgb_image = raw_img_mem;

    // fill white margin 4 times

    for (int i = 0; i < w; i++) {
        for (int j = 0; j < width_multiplier * margin; j++)
            out_target_rgb_image[(i*w+j)] = 0xff;
    }

    for (int i = 0; i < w; i++) {
        for (int j = w - margin * width_multiplier; j < w; j++)
            out_target_rgb_image[(i*w+j)] = 0xff;
    }


    for (int j = 0; j < w; j++) {
        for (int i = w - margin * width_multiplier; i < w; i++)
            out_target_rgb_image[(i*w+j)] = 0xff;
    }

    for (int j = 0; j < w; j++) {
        for (int i = 0; i < margin * width_multiplier; i++)
            out_target_rgb_image[(i*w+j)] = 0xff;
    }

    // main qr image
    for (int i = width_multiplier*margin; i<w-width_multiplier*margin; i++) {
        for (int j = width_multiplier*margin; j<w-width_multiplier*margin; j++) {
            char val = ((QR_pixeldata[(int)((i-width_multiplier*margin)*(((float)generatedQR->width)/(w-2*width_multiplier*margin)))*generatedQR->width+(int)((j-width_multiplier*margin)*((((float)generatedQR->width)/(w-2*width_multiplier*margin))))] & 1) ? 0 : 0xff);
            out_target_rgb_image[(i*w+j)] = val;
        }
    }

    *out_image_data = (char*)out_target_rgb_image;
    QRcode_free(generatedQR);

};


