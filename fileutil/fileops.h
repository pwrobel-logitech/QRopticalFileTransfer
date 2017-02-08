
#define OS_LINUX

#ifdef OS_LINUX
#include <stdint.h>
#include <vector>
#include <string>
#include <stdio.h>




uint32_t get_file_size(const char* filepath){
    uint32_t size = 0;
    FILE * fp;
    fp = fopen (filepath, "r");
    fseek(fp, 0L, SEEK_END);
    size = ftell(fp);
    fclose(fp);
    return size;
}

int read_file(const char* filepath, char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    FILE * fp = fopen (filepath, "r");
    if(fp == NULL)
        return -1;
    if (fseek(fp, offset, SEEK_SET) != 0)
        return -1;
    if (fread(data_after_read, 1, size, fp) != size)
        return -1;
    fclose(fp);
    return 1;
}


#endif
#ifdef OS_WIN
uint32_t get_file_size(char* filepath){
    return 0;
};

int read_file(char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    return -1;
};
#endif

namespace utils{

   inline uint32_t nbits_forsymcombinationsnumber(uint32_t ncomb){
       uint32_t mlt = 1;
       uint32_t bits = 0;
       while(mlt < ncomb){
           mlt = mlt << 1;//multiply by 2
           bits++;
       }
       return bits;
   }


   //how many bytes (265 symbols possible - so dst)
   //needed to encode 32 symbols of 511 combinations (so, 9bits per symbol - no 512 - so one combination lost)
   //answer : 36
   //roughly, the ret*n_possible_symbols_dst=~n_possible_symbols_src*count_symbols_src is satisfied
   uint32_t count_symbols_to_fit(uint32_t n_possible_symbols_dst,
                                 uint32_t n_possible_symbols_src,
                                 uint32_t count_symbols_src){
       uint32_t ret = 0;
       uint32_t mlt = 1;
       uint32_t bits_dst_sym = 0;
       while(mlt < n_possible_symbols_dst){
           mlt = mlt << 1;//multiply by 2
           bits_dst_sym++;
       }
       mlt = 1;
       uint32_t bits_src_sym = 0;
       while(mlt < n_possible_symbols_src){
           mlt = mlt << 1;//multiply by 2
           bits_src_sym++;
       }

       uint32_t total_src_bits = bits_src_sym * count_symbols_src;
       if(total_src_bits % bits_dst_sym != 0){
           ret = total_src_bits / bits_dst_sym + 1;//one sym more
       }else{
           ret = total_src_bits / bits_dst_sym;
       }
       return ret;
   };

   //to get the data of arbitrary bit length not starting at the byte boundary(arr_begin starts at the boundary, though)
   uint32_t get_data(const void* arr_begin, uint32_t bits_offset_from_arrbegin, uint32_t nbits_sym){
       uint32_t data = 0;
       uint32_t datachunk1 = *(((uint32_t*)arr_begin)+bits_offset_from_arrbegin/32);
       uint32_t datachunk2 = *(((uint32_t*)arr_begin)+bits_offset_from_arrbegin/32+1);
       return data;
   }



}
