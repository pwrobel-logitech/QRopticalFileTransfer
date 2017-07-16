#ifdef WIN
#include <windows.h>
#endif

#include "fileops.h"


#include <sys/time.h>

#include <iostream>
#include <stdlib.h>
#include <stdint.h>


#include <stdio.h>
#include <stdarg.h>
#include <time.h>



#ifdef ANDROID
static const char LOG_TAG[] = "Qjni";

void android_log(android_LogPriority type, const char *fmt, ...)
{
    static char buf[1024*8];
    static char *bp = buf;

    va_list vl;
    va_start(vl, fmt);
    int available = sizeof(buf) - (bp - buf);
    int nout = vsnprintf(bp, available, fmt, vl);
    if (nout >= available) {
        __android_log_write(type, LOG_TAG, buf);
        __android_log_write(ANDROID_LOG_WARN, LOG_TAG, "previous log line has been truncated!");
        bp = buf;
    } else {
        char *lastCR = strrchr(bp, '\n');
        bp += nout;
        if (lastCR) {
            *lastCR = '\0';
            __android_log_write(type, LOG_TAG, buf);

            char *rest = lastCR+1;
            int len = bp - rest; // strlen(rest)
            memmove(buf, rest, len+1); // no strcpy (may overlap)
            bp = buf + len;
        }
    }

    va_end(vl);
}
#endif //ANDROID

//pseudorandom generator part = http://www.electro-tech-online.com/threads/ultra-fast-pseudorandom-number-generator-for-8-bit.124249/

void apply_pos_xor_to_arr(char* data, int datalen, unsigned frpos){
    unsigned x = 0;
    unsigned a = frpos;
    unsigned b = frpos-2;
    unsigned c = frpos-3;
    a ^= 0xcafebabe;
    b ^= 0xbada5511;
    c ^= 66;
    x++;
    a = (a^c^x);
    b = (b+a);
    c = (c+(b>>1)^a);
    for (int i = 0; i < datalen; i++){
        unsigned random = 0;
        x++;               //x is incremented every round and is not affected by any other variable
        a = (a^c^x);       //note the mix of addition and XOR
        b = (b+a);         //And the use of very few instructions
        c = (c+(b>>1)^a);  //the right shift is to ensure that high-order bits from b can affect
        random = c;          //low order bits of other variables
        data[i] = data[i] ^ (random & 0xff);
    }
};


uint32_t get_file_size(const char* filepath){
    uint32_t size = 0;
    FILE * fp;
    fp = fopen (filepath, "rb");
    fseek(fp, 0L, SEEK_END);
    size = ftell(fp);
    fclose(fp);
    return size;
}

uint32_t get_file_size_fp(void* fp){
    uint32_t size = 0;
    fseek((FILE*)fp, 0L, SEEK_END);
    size = ftell((FILE*)fp);
    return size;
}

// system-dependent file manipulation functions
void* FileOpenToRead(const char* fn){
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "FUTIL", "fopen r %s", fn);
#endif
    return (void*)fopen(fn, "rb");
}

void* FileOpenToWrite(const char* fn){
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "FUTIL", "fopen w %s", fn);
#endif
    return (void*)fopen(fn, "wb");
}

void FileClose(void* fn){
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "FUTIL", "fclose %d", (int)fn);
#endif
    fclose((FILE*)fn);
}

int FileRename(const char *oldname, const char *newname){
   return rename(oldname, newname);
};

int read_file(const char* filepath, char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    FILE * fp = fopen (filepath, "rb");
    if(fp == NULL)
        return -1;
    if (fseek(fp, offset, SEEK_SET) != 0)
        return -1;
    if (fread(data_after_read, 1, size, fp) != size)
        return -1;
    fclose(fp);
    return 1;
}

int remove_file(const char* filenamefull){
    return remove(filenamefull);
};

//reads already opened file
int read_file_fp(void* fp, char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    if(fp == NULL)
        return -1;
    if (fseek((FILE*)fp, offset, SEEK_SET) != 0)
        return -1;
    if (fread(data_after_read, 1, size, (FILE*)fp) != size)
        return -1;
    return 1;
}

//writes to already opened file
int write_file_fp(void* fp, const char* data_to_write, uint32_t offset, uint32_t size){
    if(fp == NULL)
        return -1;
    if (fseek((FILE*)fp, offset, SEEK_SET) != 0)
        return -1;
    if (fwrite (data_to_write, size, 1, (FILE*)fp) != size)
        return -1;
    return 1;
}


#ifdef OS_WIN
uint32_t get_file_size(char* filepath){
    return 0;
};

int read_file(char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    return -1;
};
#endif


namespace utils{

    void Dosleep(int milis){
        timespec t;
        t.tv_sec =  milis / 1000;
        t.tv_nsec = (milis % 1000) * 1000000;
#ifndef WIN
        nanosleep(&t, NULL);
#else
        Sleep(milis);
#endif

    }

    double currmili(){
        struct timeval start;
        double mtime, seconds, useconds;
        gettimeofday(&start, NULL);
        seconds  = start.tv_sec;
        useconds = start.tv_usec;
        mtime = ((seconds) * 1000.0 + useconds/1000.0) + 0.5;
        return mtime;
    }

   uint32_t nbits_forsymcombinationsnumber(uint32_t ncomb){
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

   // up to 31 bits symbol length
   // 2 uints_32 in LE, we want to get the value created by s bits(nbits=9),
   // o - offset bits num = 26
   // arr_begin: | oooooooo oooooooo oooooooo ssssssoo | nnnnnsss nnnnnnnn nnnnnnnn nnnnnnnn |
   // arr_begin: | B0(char) B1(char) B2(char) B3(char) | B4(char) B5(char) B6(char) B7(char)
   // it creates 2 numbers : B3B2B1B0 and B7B6B5B4
   // to get the data of arbitrary bit length not starting at the byte boundary
   // (arr_begin starts at the boundary, though)
   uint32_t get_data(const void* arr_begin, uint32_t bits_offset_from_arrbegin, uint32_t nbits_sym){
       uint32_t data = 0;
       uint32_t datachunk1 = *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32);
       uint32_t och1 = bits_offset_from_arrbegin % 32;//offset in chunk1
       if(och1 == 0){
            data = datachunk1 & ((1 << nbits_sym) - 1); //take low n bits
            return data;
       }

       if(och1 + nbits_sym <= 32){ //no need to read data chunk2
            data = (datachunk1>>(och1))
                    & ((1<<nbits_sym) - 1);
            return data;
       }else{
            uint32_t datachunk2 = *(((uint32_t*)arr_begin)+bits_offset_from_arrbegin / 32 + 1);
            uint32_t sym_in_chunk2 = och1 + nbits_sym - 32;
            return (((datachunk2) & ((1 << sym_in_chunk2) - 1)) << (nbits_sym - sym_in_chunk2)) +
                    ((datachunk1 >> (och1))
                     & ((1 << (nbits_sym - sym_in_chunk2)) - 1));
       }
       return data;
   }

   // used in conjunction with the get_data above
   void set_data(void* arr_begin, uint32_t bits_offset_from_arrbegin,
                 uint32_t value_to_set){

        uint32_t och1 = bits_offset_from_arrbegin % 32;//offset in chunk1
        if(och1 == 0){
            *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32) = value_to_set;
            return;
        }

        uint32_t bits = nbits_forsymcombinationsnumber(value_to_set + 1); //if val=2^n it should return n+1 bits
        uint32_t chunk1 = *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32);
        if(bits+och1 <=32){//no need to write to second chunk
            uint32_t v = (value_to_set << och1);
            uint32_t mask = (((1 << bits) - 1) << och1);
            *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32) = (chunk1 & (~mask)) | v;
        }else{
            uint32_t chunk2 = *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32+1);
            uint32_t sym_in_chunk2 = och1 + bits - 32;
            uint32_t v1 = (value_to_set << och1);
            uint32_t mask1 = (((1 << (bits - sym_in_chunk2)) - 1) << och1);
            *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32) = (chunk1 & (~mask1)) | v1;

            uint32_t mask2 = ((1 << (sym_in_chunk2)) - 1);
            uint32_t v2 = (value_to_set >> (bits - sym_in_chunk2));
            *(((uint32_t*)arr_begin) + bits_offset_from_arrbegin / 32+1) = (chunk2 & (~mask2)) | v2;
        }
   }


   void printbits(unsigned char v) {
     int i; // for C89 compatability
     for(i = 7; i >= 0; i--) putchar('0' + ((v >> i) & 1));
   }

#ifndef WIN
   ScopeLock::ScopeLock(pthread_mutex_t &m){
       mp = &m;
       pthread_mutex_lock(mp);
   };
   ScopeLock::~ScopeLock(){
       pthread_mutex_unlock(mp);
   };
#endif
}

