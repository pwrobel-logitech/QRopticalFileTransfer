
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <string.h>

#include "qr_frame_producer.h"


void test_some_utils(){
    //test bits get
    char data[] = { 0b00101100, 0b10110100, 0b11101111, 0b00011000,
                    0b10101010, 0b01010101, 0b01001011, 0b00001001,
                    0b00000001, 0b11110011, 0b10010101, 0b10101000 };

    char *data_writable = new char[sizeof(data)];
    memcpy(data_writable, data, sizeof(data));
    void *arr_begin = (void*)data_writable;
    uint32_t bits_offset_from_arrbegin = 26;
    uint32_t nbits_sym = 9;
    uint32_t ret = utils::get_data(arr_begin, bits_offset_from_arrbegin, nbits_sym);
    printf("Test get data baddr:0x%x, off:%d, nsym:%d, ret:%d\n", arr_begin, bits_offset_from_arrbegin, nbits_sym, ret);
    printf("Array dump:\n");
    for (int i = 0; i<sizeof(data); i++){
        utils::printbits(data_writable[i] & 0xff);
        printf(" ");
    }
    uint32_t num_to_write = 511;
    printf("\nNow, write some bits, num to write %d\n", num_to_write);
    utils::set_data(data_writable, bits_offset_from_arrbegin, num_to_write);
    printf("\n");
    printf("Array dump after write:\n");
    for (int i = 0; i<sizeof(data); i++){
        utils::printbits(data_writable[i] & 0xff);
        printf(" ");
    }
    printf("\n");
    printf("Read val after write ret:%d\n",
           utils::get_data(arr_begin, bits_offset_from_arrbegin, utils::nbits_forsymcombinationsnumber(num_to_write)));
    delete []data_writable;
}


int main(int argc, char **argv){


    //{
    //    test_some_utils();
    //}



        uint32_t n_possible_symbols_dst = 256;
        uint32_t n_possible_symbols_src = 511;
        uint32_t count_symbols_src = 32;
        printf("Test util count bytes, count_symbols_to_fit(n_possible_symbols_dst = %d, n_possible_symbols_src = %d, count_symbols_src = %d : %d\n",
                n_possible_symbols_dst,
                n_possible_symbols_src,
                count_symbols_src,
                utils::count_symbols_to_fit(n_possible_symbols_dst, n_possible_symbols_src, count_symbols_src));


    char namebuf[60];

    //test single

    Qr_frame_producer frame_producer;

    frame_producer.set_external_file_info("tv4.txt", "/repos/qr/", 444);

    int num = 0;
    for(int i=0; i<7*40; i++){
        snprintf(namebuf, sizeof(namebuf), "QRNE_%d_frame", num);
        frame_producer.produce_next_qr_image_to_file(namebuf);
        num++;
    }

    frame_producer.tell_no_more_generating_header();

    for(int i=0; i<35*20; i++){
        snprintf(namebuf, sizeof(namebuf), "QRNE_%d_frame", num);
        frame_producer.produce_next_qr_image_to_file(namebuf);
        num++;
    }

    exit(0);


    //test different file sizes
    for (int b = 1; b < 128; b++){
    Qr_frame_producer frame_producer;

    char fname[9];
    memset(fname, 0, sizeof(fname));
    snprintf(fname, sizeof(fname), "%d.txt", b);

    frame_producer.set_external_file_info(fname, "/repos/qr/qrenctestfiles/", 31);

    int num = 0;
    for(int i=0; i<7*20; i++){
        snprintf(namebuf, sizeof(namebuf), "QRNE_%d_frame_batch%d", num, b);
        frame_producer.produce_next_qr_image_to_file(namebuf);
        num++;
    }

    frame_producer.tell_no_more_generating_header();

    for(int i=0; i<511*1; i++){
        snprintf(namebuf, sizeof(namebuf), "QRNE_%d_frame_batch%d", num, b);
        frame_producer.produce_next_qr_image_to_file(namebuf);
        num++;
    }
    }


    return 1;
}
