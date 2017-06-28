#include <sys/time.h>
//#include "bin_jpeg/jpeglib.h"


#include <iostream>
#include <stdlib.h>
#include <stdint.h>
#include <fstream>
#include <string>

#include "../public_decoder_api.h"
#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/Exception.h>
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/DecodeHints.h>
#include "BufferBitmapSource.h"

#include "decoder_wrapper.h"
double currmili(){
    struct timeval start;
    double mtime, seconds, useconds;
    gettimeofday(&start, NULL);
    seconds  = start.tv_sec;
    useconds = start.tv_usec;
    mtime = ((seconds) * 1000.0 + useconds/1000.0) + 0.5;
    return mtime;
}

size_t compressImage(char* buffer, size_t width, size_t height, unsigned char** outputBuffer, int quality){
/*
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
  jpeg_set_quality(&cinfo, quality, (boolean)1);
  jpeg_start_compress(&cinfo, (boolean)1);

  while(cinfo.next_scanline < cinfo.image_height){
    row_pointer[0] = &bits[cinfo.next_scanline * row_stride];
    jpeg_write_scanlines(&cinfo, row_pointer, 1);
  }

  jpeg_finish_compress(&cinfo);
  jpeg_destroy_compress(&cinfo);

  *outputBuffer = outp;
  //memcpy(outputBuffer, outp, outlen);

  return outlen;*/
    return 0;
}


using namespace std;
using namespace zxing;
using namespace zxing::qrcode;
using namespace qrviddec;

const uint32_t generated_data_mempool_size = 512 * 1024;
char generated_data_mempool[generated_data_mempool_size];

immediate_status generate_data_from_qr_greyscalebuffer(int* generated_datalength, char** generated_data, const char* input_greyscale_buffer,
                                           int width, int height){
//GLOBAL DEFAULT   20 zxing::qrcode::QRCodeReader::decode(zxing::Ref<zxing::BinaryBitmap>, zxing::DecodeHints)
    immediate_status ret = RECOGNIZED;
    double t = currmili();
    try{
    // A buffer containing an image. In your code, this would be an image from your camera. In this
    // example, it's just an array containing the code for "Hello!".
    char *buffer = (char*)input_greyscale_buffer;

    // Convert the buffer to something that the library understands.
    Ref<LuminanceSource> source (new BufferBitmapSource(width, height, (unsigned char*)buffer));

    // Turn it into a binary image.
    Ref<Binarizer> binarizer (new GlobalHistogramBinarizer(source));
    Ref<BinaryBitmap> image(new BinaryBitmap(binarizer));

    // Tell the decoder not to try as hard as possible.
    DecodeHints hints;
    hints.clear();
    hints.setTryHarder(false);
    hints.addFormat(BarcodeFormat::QR_CODE);


    // Perform the decoding.
    QRCodeReader reader;
    std::vector<char> bitres;
    Ref<Result> result(reader.decode(image, hints, bitres));


    *generated_datalength = bitres.size();
    *generated_data = generated_data_mempool;//new char [*generated_datalength];

    //http://stackoverflow.com/questions/17973641/how-to-decode-data-using-zxing-c

    for (int i = 0; i < *generated_datalength; i++)
      (*generated_data)[i] = bitres[i];
    // Output the result.
    //cout << "zxing res : " << result->getText()->getText() << endl;
    printf ("TXTDONE dt %f ms\n",currmili()-t/*, result->getText()->getText().c_str()+4*/);
    }
    catch (zxing::Exception& e)
    {
#ifdef ANDROID

#endif
        cerr << "Error zxing, not recognized qr code : " << e.what() << endl;
        ret = NOT_RECOGNIZED;
        //if ((*generated_data) != NULL){
        //    delete [](*generated_data);
        //}
        *generated_data = NULL;
    }
    return ret;

}
