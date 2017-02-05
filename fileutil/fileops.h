
#define OS_LINUX

#ifdef OS_LINUX
#include "stdint.h"
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

int read_file(char* data_after_read, uint32_t offset, uint32_t size){ //-1 error
    return -1;
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


