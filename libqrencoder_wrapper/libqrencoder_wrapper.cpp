
#include "libqrencoder_wrapper.h"
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <jpeglib.h>
 #include <string.h>
JSAMPLE  image_buffer[] = {0x00,0x00,0x00, 0xff,0xff,0xff, 0x00,0xff,0xff, 0x00,0,0};	/* Points to large array of R,G,B-order data */
int image_height=2;	/* Number of rows in image */
int image_width=2;		/* Number of columns in image */

void
write_JPEG_to_mem (char** created_jpeg, int* created_jpeg_length, int input_w, int input_h, const char* inputRGB, int quality)
{
  /* This struct contains the JPEG compression parameters and pointers to
   * working space (which is allocated as needed by the JPEG library).
   * It is possible to have several such structures, representing multiple
   * compression/decompression processes, in existence at once.  We refer
   * to any one struct (and its associated working data) as a "JPEG object".
   */
  struct jpeg_compress_struct cinfo;
  /* This struct represents a JPEG error handler.  It is declared separately
   * because applications often want to supply a specialized error handler
   * (see the second half of this file for an example).  But here we just
   * take the easy way out and use the standard error handler, which will
   * print a message on stderr and call exit() if compression fails.
   * Note that this struct must live as long as the main JPEG parameter
   * struct, to avoid dangling-pointer problems.
   */
  struct jpeg_error_mgr jerr;
  /* More stuff */
  //FILE * outfile;		/* target file */
  JSAMPROW row_pointer[1];	/* pointer to JSAMPLE row[s] */
  int row_stride;		/* physical row width in image buffer */

  /* Step 1: allocate and initialize JPEG compression object */

  /* We have to set up the error handler first, in case the initialization
   * step fails.  (Unlikely, but it could happen if you are out of memory.)
   * This routine fills in the contents of struct jerr, and returns jerr's
   * address which we place into the link field in cinfo.
   */
  //cinfo.dest->term_destination
  cinfo.err = jpeg_std_error(&jerr);
  /* Now we can initialize the JPEG compression object. */
  jpeg_create_compress(&cinfo);

  /* Step 2: specify data destination (eg, a file) */
  /* Note: steps 2 and 3 can be done in either order. */

  /* Here we use the library-supplied code to send compressed data to a
   * stdio stream.  You can also write your own code to do something else.
   * VERY IMPORTANT: use "b" option to fopen() if you are on a machine that
   * requires it in order to write binary files.
   */

  //jpeg_destination_mgr* dest;
  //dest = (jpeg_destination_mgr) cinfo.dest;
  jpeg_mem_dest(&cinfo, (unsigned char**)created_jpeg, (long unsigned int*)created_jpeg_length);

  /* Step 3: set parameters for compression */

  /* First we supply a description of the input image.
   * Four fields of the cinfo struct must be filled in:
   */
  cinfo.image_width = input_w; 	/* image width and height, in pixels */
  cinfo.image_height = input_h;
  cinfo.input_components = 3;		/* # of color components per pixel */
  cinfo.in_color_space = JCS_RGB; 	/* colorspace of input image */
  /* Now use the library's routine to set default compression parameters.
   * (You must set at least cinfo.in_color_space before calling this,
   * since the defaults depend on the source color space.)
   */
  jpeg_set_defaults(&cinfo);
  /* Now you can set any non-default parameters you wish to.
   * Here we just illustrate the use of quality (quantization table) scaling:
   */
  jpeg_set_quality(&cinfo, quality, TRUE /* limit to baseline-JPEG values */);

  /* Step 4: Start compressor */

  /* TRUE ensures that we will write a complete interchange-JPEG file.
   * Pass TRUE unless you are very sure of what you're doing.
   */
  jpeg_start_compress(&cinfo, TRUE);

  /* Step 5: while (scan lines remain to be written) */
  /*           jpeg_write_scanlines(...); */

  /* Here we use the library's state variable cinfo.next_scanline as the
   * loop counter, so that we don't have to keep track ourselves.
   * To keep things simple, we pass one scanline per call; you can pass
   * more if you wish, though.
   */
  row_stride = image_width * 3;	/* JSAMPLEs per row in image_buffer */

  while (cinfo.next_scanline < cinfo.image_height) {
    /* jpeg_write_scanlines expects an array of pointers to scanlines.
     * Here the array is only one element long, but you could pass
     * more than one scanline at a time if that's more convenient.
     */
    row_pointer[0] =(JSAMPROW) & inputRGB[cinfo.next_scanline * row_stride];
    (void) jpeg_write_scanlines(&cinfo, row_pointer, 1);
  }

  /* Step 6: Finish compression */

  jpeg_finish_compress(&cinfo);
  //created_jpeg = cinfo->dest->dest;
printf("QQ %d %d %d\n", *created_jpeg_length, created_jpeg, *created_jpeg);
  /* Step 7: release JPEG compression object */

  /* This is an important step since it will release a good deal of memory. */
  jpeg_destroy_compress(&cinfo);

  /* And we're done! */
}



size_t compressImage(char* buffer, size_t width, size_t height, unsigned char* outputBuffer, int quality){

  unsigned char* bits = (unsigned char*) buffer;
  unsigned char* outp = new unsigned char[60000];
  memset(outp, '0', (size_t)(60000));

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

  memcpy(outputBuffer, outp, outlen);

  return outlen;
}


void init_libqrencoder(int size){

}

void finish_libqrencoder(){

}

void generate_image_data(const unsigned char* input_data, int input_length, unsigned char** out_image_data, int *out_image_data_size){
  QRcode *generatedQR = NULL;
  generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
  if (generatedQR != NULL){
      *out_image_data = generatedQR->data;
      *out_image_data_size = generatedQR->width * generatedQR->width;
  }
  FILE *my_file = fopen("dump11.jpg", "wb");
  char out_jpeg[6000];
  char out_b[6000];
  int outsize = 0;
  int* out_jpeg_size = &outsize;
  //write_JPEG_to_mem ((char**)&out_jpeg, out_jpeg_size, image_width, image_height, (const char*)image_buffer, 100);

compressImage(out_jpeg, image_width,image_height,(unsigned char*) out_b, 100);

  fwrite(out_b, 743, 1, my_file);
  fclose(my_file);
}



