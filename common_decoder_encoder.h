#include "stdint.h"
#include <string>
#include <vector>

struct FileInfo{
    std::string filename;
    std::string filepath;
    uint32_t filelength;
    int RSn;//for main part of the file
    int RSk;
    int RSn_residual; //for the residual part of the file
    int RSk_residual;
    std::vector<char> hash;
    //internal, system dependent file descriptor - on linux that's valid pointer for FILE struct
    void* fp;
};

