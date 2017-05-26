#include "stdint.h"
#include <string>
#include <vector>

const int fixed_metadata_arr_size = 32*4096;
const int fixed_filehash_buff_size = 32*1024;
char fixed_filehash_buff[fixed_filehash_buff_size];

const int fixed_N_metadata = 7;
const int fixed_K_metadata = 3;

struct FileInfo{
    std::string filename;//can contain some relative dir to the base path. On the decoder, this is the pure name
    std::string filename_without_any_path; //just pure filename
    std::string filepath; //base dir of search. For decoder, this is the dump path of the files
    uint32_t filelength;
    int RSn;//for main part of the file
    int RSk;
    int RSn_residual; //for the residual part of the file
    int RSk_residual;
    std::vector<char> hash;
    //internal, system dependent file descriptor - on linux that's valid pointer for FILE struct
    void* fp;
};

