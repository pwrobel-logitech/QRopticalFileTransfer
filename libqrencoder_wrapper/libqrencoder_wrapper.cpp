
#include "libqrencoder_wrapper.h"
#include "qrencode.h"

#include <sys/time.h>
#include "bin_jpeg/jpeglib.h"


#include <iostream>
#include <stdlib.h>
#include <stdint.h>
#include <fstream>
#include <string>
#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/Exception.h>
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/DecodeHints.h>
#include "BufferBitmapSource.h"

double currmili(){
    struct timeval start;
    double mtime, seconds, useconds;
    gettimeofday(&start, NULL);
    seconds  = start.tv_sec;
    useconds = start.tv_usec;
    mtime = ((seconds) * 1000.0 + useconds/1000.0) + 0.5;
    return mtime;
}

char* raw_img_mem = NULL;

//JSAMPLE  image_buffer[] = {0x00,0x00,0x00, 0xff,0xff,0xff, 0xff,0xff,0xff, 0x00,0,0};	/* Points to large array of R,G,B-order data */
//int image_height=2;	/* Number of rows in image */
//int image_width=2;		/* Number of columns in image */

const int static_workbuffer_size = (int)1.5*1024*1024; //1.5MB
char static_workbuffer1[static_workbuffer_size]; //for processing jpeg - reduces number of allocations

size_t compressImage(char* buffer, size_t width, size_t height, unsigned char** outputBuffer, int quality){

  unsigned char* bits = (unsigned char*) buffer;
  unsigned char* outp = (unsigned char*) static_workbuffer1;

  struct jpeg_compress_struct cinfo;
  struct jpeg_error_mgr jerr;

  JSAMPROW row_pointer[1];  //Pointer to JSAMPLE row[s]

  int row_stride = width * 1; //Physical row width in image buffer
  cinfo.err      = jpeg_std_error(&jerr);

  jpeg_create_compress(&cinfo);

  cinfo.image_width        = width;
  cinfo.image_height       = height;
  cinfo.input_components   = 1;
  cinfo.in_color_space     = JCS_GRAYSCALE;
  size_t outlen            = 0;

  jpeg_mem_dest(&cinfo, &outp,(long unsigned int*) &outlen);

  jpeg_set_defaults(&cinfo);
  jpeg_set_quality(&cinfo, quality, (boolean)true);
  jpeg_start_compress(&cinfo, (boolean)true);

  while(cinfo.next_scanline < cinfo.image_height){
    row_pointer[0] = &bits[cinfo.next_scanline * row_stride];
    jpeg_write_scanlines(&cinfo, row_pointer, 1);
  }

  jpeg_finish_compress(&cinfo);
  jpeg_destroy_compress(&cinfo);

  *outputBuffer = outp;
  //memcpy(outputBuffer, outp, outlen);

  return outlen;
}


void init_libqrencoder(int size){
    //if(raw_img_mem == NULL){
    raw_img_mem = new char[size];
    //}
}

void finish_libqrencoder(){
    if(raw_img_mem != NULL)
        delete []raw_img_mem;
    raw_img_mem = NULL;
}

void generate_image_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size,
                         int *max_target_width){

  double t = currmili();
  QRcode *generatedQR = NULL;
  generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
  unsigned char* QR_pixeldata = NULL;
  int QR_pixeldata_size = -1;
  if (generatedQR != NULL){
      QR_pixeldata = generatedQR->data;
      QR_pixeldata_size = generatedQR->width * generatedQR->width;
  }

  int w = generatedQR->width;
  int i = 1;
  do {
      w = generatedQR->width * i;
      i++;
  }
  while (generatedQR->width * i<=*max_target_width);
  *max_target_width = w;
  int target_width = *max_target_width;


  char* out_target_rgb_image = raw_img_mem;//new char[target_width*target_width];
  //memset(out_target_rgb_image, 0, target_width*target_width);

  for (int i = 0; i<target_width; i++) {
      for (int j = 0; j<target_width; j++) {
          char val = ((QR_pixeldata[(int)(i*(((float)generatedQR->width)/target_width))*generatedQR->width+(int)(j*((((float)generatedQR->width)/target_width)))] & 1) ? 0 : 0xff);
          out_target_rgb_image[(i*target_width+j)] = val;
      }
  }

  unsigned char* out_jpeg_buff; //pointer to the generated data in the static buffer
  int jpegsize = compressImage(out_target_rgb_image, target_width, target_width, &out_jpeg_buff, 100);

  *out_image_data_size = jpegsize;
  *out_image_data = (char*)out_jpeg_buff;
  //delete []out_target_rgb_image;

  printf("MS genQR %f\n", currmili() - t);

}


void generate_small_image_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size,
                         int *target_width, int width_multiplier){

  double t = currmili();
  QRcode *generatedQR = NULL;
  generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
  unsigned char* QR_pixeldata = NULL;
  int QR_pixeldata_size = -1;
  if (generatedQR != NULL){
      QR_pixeldata = generatedQR->data;
      QR_pixeldata_size = generatedQR->width * generatedQR->width;
  }

  *target_width = generatedQR->width * width_multiplier;
  int w = *target_width;

  char* out_target_rgb_image = raw_img_mem;//new char[target_width*target_width];
  //memset(out_target_rgb_image, 0, target_width*target_width);

  for (int i = 0; i<w; i++) {
      for (int j = 0; j<w; j++) {
          char val = ((QR_pixeldata[(int)(i*(((float)generatedQR->width)/w))*generatedQR->width+(int)(j*((((float)generatedQR->width)/w)))] & 1) ? 0 : 0xff);
          out_target_rgb_image[(i*w+j)] = val;
      }
  }

  unsigned char* out_jpeg_buff; //pointer to the generated data in the static buffer
  int jpegsize = compressImage(out_target_rgb_image, w, w, &out_jpeg_buff, 100);

  *out_image_data_size = jpegsize;
  *out_image_data = (char*)out_jpeg_buff;
  //delete []out_target_rgb_image;

  printf("MS genQR small mult %d - %f ms\n", width_multiplier, currmili() - t);

}


void generate_small_image_data_margin(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size,
                         int *target_width, int width_multiplier, int margin){

  double t = currmili();
  QRcode *generatedQR = NULL;
  generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
  unsigned char* QR_pixeldata = NULL;
  int QR_pixeldata_size = -1;
  if (generatedQR != NULL){
      QR_pixeldata = generatedQR->data;
      QR_pixeldata_size = generatedQR->width * generatedQR->width;
  }

  *target_width = (generatedQR->width + 2*margin) * width_multiplier;
  int w = *target_width;

  char* out_target_rgb_image = raw_img_mem;//new char[target_width*target_width];
  //memset(out_target_rgb_image, 0xff, w*w);

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

  unsigned char* out_jpeg_buff; //pointer to the generated data in the static buffer
  int jpegsize = compressImage(out_target_rgb_image, w, w, &out_jpeg_buff, 100);

  *out_image_data_size = jpegsize;
  *out_image_data = (char*)out_jpeg_buff;
  //delete []out_target_rgb_image;

  printf("MS genQR small mult %d, margin %d - %f ms\n", width_multiplier, margin, currmili() - t);

}

// generate qr grayscale bitmap
void generate_qr_greyscale_bitmap_data(const unsigned char* input_data, int input_length,
                                       char** out_image_data, int *out_image_data_width,
                                       int margin){


    //double t = currmili();
    QRcode *generatedQR = NULL;
    generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
    unsigned char* QR_pixeldata = NULL;
    int QR_pixeldata_size = -1;
    if (generatedQR != NULL){
        QR_pixeldata = generatedQR->data;
        QR_pixeldata_size = generatedQR->width * generatedQR->width;
    }


    int w =  (generatedQR->width + 2*margin);
    *out_image_data_width = w;
    const int width_multiplier = 1;
    char* out_target_rgb_image = raw_img_mem;//new char[target_width*target_width];
    //memset(out_target_rgb_image, 0xff, w*w);

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
    //delete []out_target_rgb_image;


};



using namespace std;
using namespace zxing;
using namespace zxing::qrcode;
using namespace qrviddec;

void generate_data_from_qr_greyscalebuffer(int* generated_datalength, char** generated_data, char* input_greyscale_buffer,
                                           int width){



    // A buffer containing an image. In your code, this would be an image from your camera. In this
    // example, it's just an array containing the code for "Hello!".
    char *buffer = input_greyscale_buffer;
    int height = width;

    // Convert the buffer to something that the library understands.
    Ref<LuminanceSource> source (new BufferBitmapSource(width, height, (unsigned char*)buffer));

    // Turn it into a binary image.
    Ref<Binarizer> binarizer (new GlobalHistogramBinarizer(source));
    Ref<BinaryBitmap> image(new BinaryBitmap(binarizer));

    // Tell the decoder to try as hard as possible.
    DecodeHints hints(DecodeHints::DEFAULT_HINT);
    hints.setTryHarder(true);

    // Perform the decoding.
    QRCodeReader reader;
    std::vector<char> bitres;
    Ref<Result> result(reader.decode(image, hints, bitres));


    *generated_datalength = bitres.size();
    *generated_data = new char [*generated_datalength];

    //http://stackoverflow.com/questions/17973641/how-to-decode-data-using-zxing-c

    for (int i = 0; i < *generated_datalength; i++)
      (*generated_data)[i] = bitres[i];
    // Output the result.
    //cout << "zxing res : " << result->getText()->getText() << endl;
    printf ("TXT %c\n", result->getText()->getText().c_str()[0]);

}
