
#include "libqrencoder_wrapper.h"


JSAMPLE  image_buffer[] = {0x00,0x00,0x00, 0xff,0xff,0xff, 0xff,0xff,0xff, 0x00,0,0};	/* Points to large array of R,G,B-order data */
int image_height=2;	/* Number of rows in image */
int image_width=2;		/* Number of columns in image */

const int static_workbuffer_size = (int)1.5*1024*1024; //1.5MB
char static_workbuffer1[static_workbuffer_size]; //for processing jpeg - reduces number of allocations

size_t compressImage(char* buffer, size_t width, size_t height, unsigned char** outputBuffer, int quality){

  unsigned char* bits = (unsigned char*) buffer;
  unsigned char* outp = (unsigned char*) static_workbuffer1;

  struct jpeg_compress_struct cinfo;
  struct jpeg_error_mgr jerr;

  JSAMPROW row_pointer[1];  //Pointer to JSAMPLE row[s]

  int row_stride = width * 3; //Physical row width in image buffer
  cinfo.err      = jpeg_std_error(&jerr);

  jpeg_create_compress(&cinfo);

  cinfo.image_width        = width;
  cinfo.image_height       = height;
  cinfo.input_components   = 3;
  cinfo.in_color_space     = JCS_RGB;
  size_t outlen            = 0;

  jpeg_mem_dest(&cinfo, &outp, &outlen);

  jpeg_set_defaults(&cinfo);
  jpeg_set_quality(&cinfo, quality, true);
  jpeg_start_compress(&cinfo, true);

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

}

void finish_libqrencoder(){

}

void generate_image_data(const unsigned char* input_data, int input_length, char** out_image_data, int *out_image_data_size,
                         int *max_target_width){

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


  //*out_image_data_size = target_width * target_width * 3; // RGB buffer size
  //*out_image_data = new char[*out_image_data_size]; //RGB buffer as an input for jpeg creation


  char* out_target_rgb_image = new char[target_width*target_width*3];
  memset(out_target_rgb_image, 0, target_width*target_width*3);

  for (int i = 0; i<target_width; i++) {
      for (int j = 0; j<target_width; j++) {
          char val = ((QR_pixeldata[(int)(i*(((float)generatedQR->width)/target_width))*generatedQR->width+(int)(j*((((float)generatedQR->width)/target_width)))] & 1) ? 0 : 0xff);
          out_target_rgb_image[3*(i*target_width+j)+0] = val;
          out_target_rgb_image[3*(i*target_width+j)+1] = val;
          out_target_rgb_image[3*(i*target_width+j)+2] = val;
          if(i==0)printf(" %02x", val);
      }
  }

  /*
  printf("\n");
  for (int i = 0; i<generatedQR->width; i++) {
      for (int j = 0; j<generatedQR->width; j++) {
          printf("%02x ", QR_pixeldata[i*generatedQR->width+j]);
      }
      printf("\n");
  }
  //memcpy(*out_image_data, image_buffer, 12);
  */
  unsigned char* out_jpeg_buff; //pointer to the generated data in the static buffer
  int jpegsize = compressImage(out_target_rgb_image, target_width, target_width, &out_jpeg_buff, 100);

  delete []out_target_rgb_image;

  FILE *my_file = fopen("dump11.jpg", "wb");
  fwrite(out_jpeg_buff, jpegsize , 1, my_file);
  fclose(my_file);
}



