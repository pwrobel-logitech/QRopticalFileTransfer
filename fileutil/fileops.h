
#define OS_LINUX


#include <stdint.h>
#include <vector>
#include <string>
#include <stdio.h>



#ifdef ANDROID
#include <android/log.h>
#endif

extern "C"{

#ifdef ANDROID
void android_log(android_LogPriority type, const char *fmt, ...);
#define LOGI(...) android_log(ANDROID_LOG_INFO, __VA_ARGS__)
#define LOGW(...) android_log(ANDROID_LOG_WARN, __VA_ARGS__)
#endif

uint32_t get_file_size(const char* filepath);
uint32_t get_file_size_fp(void* fp);

//to avoid having distinct large 0's block visible cleraly in the QR frames
void apply_pos_xor_to_arr(char* data, int datalen, unsigned frpos);

int read_file(const char* filepath, char* data_after_read, uint32_t offset, uint32_t size);

int read_file_fp(void* fp, char* data_after_read, uint32_t offset, uint32_t size);
int write_file_fp(void* fp, const char* data_to_write, uint32_t offset, uint32_t size);
int remove_file(const char* filenamefull);
void FileClose(void* fn);
void* FileOpenToRead(const char* fn);
void* FileOpenToWrite(const char* fn);

int FileRename(const char *oldname, const char *newname); // returns -1 for the error


#ifdef OS_WIN
uint32_t get_file_size(char* filepath){
    return 0;
};

int read_file(char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    return -1;
};
#endif

namespace utils{

   void Dosleep(int milis);
   double currmili();

   uint32_t nbits_forsymcombinationsnumber(uint32_t ncomb);


   //how many bytes (265 symbols possible - so dst)
   //needed to encode 32 symbols of 511 combinations (so, 9bits per symbol - no 512 - so one combination lost)
   //answer : 36
   //roughly, the ret*n_possible_symbols_dst=~n_possible_symbols_src*count_symbols_src is satisfied
   uint32_t count_symbols_to_fit(uint32_t n_possible_symbols_dst,
                                 uint32_t n_possible_symbols_src,
                                 uint32_t count_symbols_src);

   // up to 31 bits symbol length
   // 2 uints_32 in LE, we want to get the value created by s bits(nbits=9),
   // o - offset bits num = 26
   // arr_begin: | oooooooo oooooooo oooooooo ssssssoo | nnnnnsss nnnnnnnn nnnnnnnn nnnnnnnn |
   // arr_begin: | B0(char) B1(char) B2(char) B3(char) | B4(char) B5(char) B6(char) B7(char)
   // it creates 2 numbers : B3B2B1B0 and B7B6B5B4
   // to get the data of arbitrary bit length not starting at the byte boundary
   // (arr_begin starts at the boundary, though)
   uint32_t get_data(const void* arr_begin, uint32_t bits_offset_from_arrbegin, uint32_t nbits_sym);

   // used in conjunction with the get_data above
   void set_data(void* arr_begin, uint32_t bits_offset_from_arrbegin,
                 uint32_t value_to_set);


   void printbits(unsigned char v);

#ifndef WIN
   class ScopeLock {
   public:
       ScopeLock(pthread_mutex_t &m);
       ~ScopeLock();
   private :
       pthread_mutex_t* mp;
   };
#endif

}

}
