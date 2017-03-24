
#include "stdint.h"
#ifdef ANDROID
#include <vector>
#include <string>
#include <memory.h>
#else
#include <vector>
#include <string>
#endif

#include "fileutil/fileops.h"

extern "C"{
#include "fec_include/fec.h"
}

#define DEBUG

#ifdef DEBUG
#define DCHECK(a) if(!(a))printf("Condition "#a" failed in line %d, file %s !\n", __LINE__, __FILE__);
#endif

#ifdef DEBUG
#define DLOG printf
#else
#define DLOG (void)0
#endif

struct FileChunk;

//interface to provide the raw file fragments for the encoder
class FileDataProvider{
public:
    // provide readonly data for the encoder
    // returned value indicates status
    // chunk has requested size and length - fill its data - space is already allocated.
    virtual int getFileData(FileChunk* chunk) = 0;
};


class EncodedFrame{

public:
    EncodedFrame(int RSn, int RSk){
        RSn_ = RSn;
        RSk_ = RSk;
    };

    EncodedFrame(){
    };

    //Must be at least 4 bytes, to accomodate the frame number in the header of each frame
    //perform actual frame vector resize - allocation
    virtual void set_frame_capacity(uint16_t capacity) = 0;

    virtual bool is_header_frame() = 0;
    virtual uint32_t get_frame_number() = 0;
    //setting -1 implies that this is a header frame
    virtual void set_frame_number(uint32_t frame_number) = 0;
    virtual void set_max_frames(uint32_t max_frames) = 0;
    virtual void set_frame_RSnk(uint16_t n, uint16_t k) = 0;




protected:

    /*
      This tells the maximum numbers of the frames needed to encode data
    */
    uint32_t max_frames_;
    /*
      This indicates the frame number.
      Frame with the n_frame=0xFFFFFFFF corresponds to the header,
      which is used to synchronize the broadcast,
      tell the filename, file length, (n,k) values over the frames
      and hash value over the true file data.
    */
    uint32_t n_frame_;

    //when n_frame=-1, we got the header info frame and this 2-byte number counts the header frames
    //starts from 0
    uint8_t n_header_frame_;

    //RS (n,k) info of the frames
    uint16_t RSn_;
    uint16_t RSk_;

    /*
      Raw framedata of the frame, including all the metadata - that contains the info
      whether this is metadata frame or the payload frame
      Must be at least 4 bytes long - first 4 bytes will always compose the frame number
    */

    /*
      For the header frame the layout of the data is
      start (little-endian, hex):  0xFFFFFFFFNNNNDDDD...
      four of 0xFF - stands as n_frame=last(-1), 0xNNNN - uint16 of n_header_frame
      DDD... - data in the header frame
    */

    /*
      For the data frame, layout of the data is:
      start (little-endian, hex):  0xNNNNNNNNDDDDD
      N's - uint32!=0xFFFFFFFF  - frame number
      DD..  - data. It's the file data stored in RS code - with some redundancy
    */
public:
    std::vector<uint8_t> framedata_;
};

//file is split into the chunk of files, each one starts in an offset
struct FileChunk{
    char* chunkdata;
    uint32_t chunk_length;
    //offset into the file of this file chunk
    uint32_t chunk_fileoffset;
    short reason; // 0 - file, 1 - metadata, 2 - trailing chunk
};

class Encoder{

public:

    const uint32_t header_length = 512; // length of the header payload. Both the decoder and the encoder
    //know that constant. Payload starts at qr frame begin after the 0xffffffff - after sufficiently
    //qr frames with the payload has been emitted, the proces starts over continuously to give detector
    //more chance to pick it up


    enum generated_frame_status{
       Frame_OK_header = 0, //ok - produced next header frame
       Frame_OK_data, //ok, produced next frame data
       Frame_need_next_filechunk, //frame not produced - need to get more file data
       Frame_EOF,  //requested frame not generated, because whole file has been processed
       Frame_error //hit some generic error - frame not produced correctly
    };



    //set the filename
    virtual void set_filename(const char* filename) = 0;

    virtual void set_filelength(uint32_t file_length) = 0;

    virtual void set_datafeed_provider(FileDataProvider* provider) = 0;

    virtual void set_hashlength(uint16_t hash_length) = 0;

    virtual void set_nbytes_data_per_generated_frame(uint16_t nbytes) = 0;

    //set the Reed-Solomon code (n,k) of the each byte in the consecutive frames
    //n>k, and n-k will directly determine the number of overhead data frames
    virtual void set_RS_nk(uint16_t n, uint16_t k) = 0;

    //array produced must be of length hash_length
    virtual uint8_t* compute_hash() = 0;

    /*
     this returns the next encoded frame.
     Encoder allocates that object on the heap, and responsibility to release it
     is on whoever calls that function.
     Depending on the returned status, the decision for requesting the next
     frame is being made
    */
    virtual generated_frame_status produce_next_encoded_frame(EncodedFrame* frame) = 0;


    //this sets number of parralel channels processed by encoder -
    //number of data symbols per frame
    virtual void set_nchannels_parallel(uint32_t nch) = 0;

    //set whether we are encoding header or the actual data
    virtual void set_is_header_frame_generating(bool header) = 0;

protected:


    // data provider
    // returned 0 - file chunk delivered properly
    // something else than 0 - there were some error in delivering the requested file chunk
    FileDataProvider* filedata_provider_;

    //filename to encode - null terminated array
    std::string filename_;
    //raw binary file data in terms of array of file chunks
    std::vector<FileChunk*> file_data_;

    //encoder has requested so far this much bytes from the file
    uint32_t bytes_currently_read_from_file_;

    //encoder knows in advance how long the file is going to be
    uint32_t total_file_length_;

    //This pair encode the redundancy of the QR frames.
    //It is the classical Reed-Solomon code of the (n,k)
    //Typical code to correct up to 2 bits of errors would stand as (255,251)
    //It would correspond that each bit of the series of 255 frames encodes only
    //the 251 bits of real data - 4 frames would constitue the overhead.
    uint16_t RSn_;
    uint16_t RSk_;

    //file hash length - in bytes
    uint16_t hash_length_;

    //max number of bytes for the data payload of the each encoded frame produced
    uint16_t bytes_per_generated_frame_;

    // byte offset within the file, that we currently processed to frames. When this reach
    // the bytes_currently_read_from_file_ - it's time to ask for new data and update the
    // bytes_currently_read_from_file_ accordingly
    uint32_t byte_of_file_currently_processed_to_frames_;

};
