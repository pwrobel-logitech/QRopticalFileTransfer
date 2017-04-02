#include "common_decoder_encoder.h"
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include "open_rs_encoder.h"


class Qr_frame_producer : public FileDataProvider
{
public:
    Qr_frame_producer();
    ~Qr_frame_producer();
    //-1 = error, 0 - ok, 1 - finished
    // image template - "dump_%d" = will generate dump_1, dump_2, dump_3.. files
    int produce_next_qr_image_to_file(const char* imagetemplate);
    int produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width);

    //static int needMetaData(FileChunk*);
    //static int needData(FileChunk*);
    static char* file;

    std::vector<char> metadata_; //metadata for the file is held here

    int set_external_file_info(const char* filename, const char* filepath, int suggested_qr_payload_length);

private:
    int getFileData(FileChunk* chunk);
    void setup_metadata_encoder();
    void setup_encoder();
    ///we will pass it for the encoder as the data feeding callback

    std::string filename_;
    Encoder* encoder_;

    // metadata encoder on its own is a decoder with a fixed RS()
    Encoder* metadata_encoder_;//this stores filename, length, hash, main RS(n,k) and remainded RS(n,k) -
                               //for the shorter file end
            


    // describes whether we are generating the header frame now.
    bool is_header_frame_generating_;


    //default qr data length size
    uint16_t total_chars_per_QR_;

    FileInfo file_info_;

    void produce_metadata();

    //in bytes
    int estimate_capacity(int N, int K, int charperQR);
};






