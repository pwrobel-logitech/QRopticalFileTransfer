#include <cstdint>
#include <vector>

class EncodedFrame{

public:
    EncodedFrame();

    //Must be at least 4 bytes, to accomodate the frame number in the header of each frame
    virtual void set_frame_capacity(uint16_t capacity) = 0;

    virtual bool is_header_frame() = 0;
    virtual uint32_t get_frame_number() = 0;
    //setting 0 implies that this is a header frame
    virtual void set_frame_number(uint32_t frame_number) = 0;
    virtual void set_max_frames(uint32_t max_frames) = 0;



private:

    /*
      This tells the maximum numbers of the frames needed to encode data
    */
    uint32_t max_frames;
    /*
      This indicates the frame number.
      Frame with the n_frame=0xFFFFFFFF corresponds to the header,
      which is used to synchronize the broadcast,
      tell the filename, file length, (n,k) values over the frames
      and hash value over the true file data.
    */
    uint32_t n_frame;

    //when n_frame=-1, we got the header info frame and this 2-byte number counts the header frames
    //starts from 0
    uint16_t n_header_frame;

    //RS (n,k) info of the frames
    uint16_t RSn;
    uint16_t RSk;

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

    */
    std::vector<uint8_t> framedata;
};

class Encoder{

public:
    Encoder();

    //set the filename
    virtual void set_filename(char* filename) = 0;

    virtual void set_filedata(uint8_t* data_to_copy, uint size) = 0;

    virtual void set_hashlength(uint16_t hash_length) = 0;


    //set the Reed-Solomon code (n,k) of the each byte in the consecutive frames
    //n>k, and n-k will directly determine the number of overhead data frames
    virtual void set_RS_nk(uint16_t n, uint16_t k) = 0;

    //array produced must be of length hash_length
    virtual uint8_t* compute_hash() = 0;

    /*
     this returns the next encoded frame.
     Encoder allocates that object on the heap, and responsibility to release it
     is on whoever calls that function.
    */
    virtual void produce_next_encoded_frame(EncodedFrame* frame) = 0;

private:


    //filename to encode
    char* filename;
    //raw binary file data
    std::vector<uint8_t> original_file_data;

    //This pair encode the redundancy of the QR frames.
    //It is the classical Reed-Solomon code of the (n,k)
    //Typical code to correct up to 2 bits of errors would stand as (255,251)
    //It would correspond that each bit of the series of 255 frames encodes only
    //the 251 bits of real data - 4 frames would constitue the overhead.
    uint8_t RSn;
    uint8_t RSk;

    //file hash length - in bytes
    uint16_t hash_length;

};
