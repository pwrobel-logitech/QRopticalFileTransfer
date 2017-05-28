#include "rs_decoder.h"
#include "fec.h"

#ifdef ANDROID
#include <android/log.h>
#endif

#include "globaldefs.h"

OpenRSEncodedFrame::OpenRSEncodedFrame(int RSn, int RSk) :
    EncodedFrame::EncodedFrame(RSn, RSk){}

OpenRSEncodedFrame::OpenRSEncodedFrame() :
    EncodedFrame::EncodedFrame(){}

void OpenRSEncodedFrame::set_frame_RSnk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
};

void OpenRSEncodedFrame::set_frame_capacity(uint16_t capacity){

};

bool OpenRSEncodedFrame::is_header_frame(){

};

uint32_t OpenRSEncodedFrame::get_frame_number(){
    return this->n_frame_;
};

void OpenRSEncodedFrame::set_frame_number(uint32_t frame_number){
    this->n_frame_ = frame_number;
};

void OpenRSEncodedFrame::set_max_frames(uint32_t max_frames){

};


void RS_decoder::set_chunk_listener(ChunkListener* l){
    this->chunk_listener_ = l;
};

void RS_decoder::set_configured(bool configured){
    this->configured_ = configured;
};

bool RS_decoder::get_configured(){
    return this->configured_;
};

RS_decoder::RS_decoder(){
    //this->bytes_currently_read_from_file_ = 0;
    this->internal_memory_ = NULL;
    this->n_dataframe_processed_ = 0;
    this->n_header_frame_processed_ = 0;
    //this->byte_of_file_currently_processed_to_frames_ = 0;
    this->file_data_.clear();
    this->RSfecDec = NULL;
    this->internal_RS_erasure_location_mem_ = NULL;
    this->internal_RS_successfull_indexes_per_chunk_ = NULL;
    this->old_chunk_number_ = 0;
    this->status_ = RS_decoder::STILL_OK;
    this->configured_ = false;
    this->fist_proper_framedata_number_for_this_decoder_ = 0;
    this->next_erasure_successful_num_position_ = 0;
    this->is_residual_ = false;
    this->processed_once_ = false;
}

RS_decoder::~RS_decoder(){
    if(this->RSfecDec)
        free_rs_int(this->RSfecDec);
    if(this->internal_RS_erasure_location_mem_ != NULL){
        delete []this->internal_RS_erasure_location_mem_;
        this->internal_RS_erasure_location_mem_ = NULL;
    }
    if(this->internal_RS_successfull_indexes_per_chunk_ != NULL){
        delete []this->internal_RS_successfull_indexes_per_chunk_;
        this->internal_RS_successfull_indexes_per_chunk_ = NULL;
    }
    if(this->internal_memory_ != NULL){
        delete []this->internal_memory_;
        this->internal_memory_ = NULL;
    }
}

void RS_decoder::set_header_frame_generating(bool isheader){
    this->is_header_frame_generating_ = isheader;
};

void RS_decoder::fist_proper_framedata_number_for_this_decoder(uint32_t first){
    this->fist_proper_framedata_number_for_this_decoder_ = first;
};

void RS_decoder::internal_getdata_from_internal_memory(){

    //for(int k = 0; k<this->RSn_*this->n_channels_; k++)
    //    printf("ind %d, val %d\n",k, this->internal_memory_[k]);
    //

    char* data = NULL;
    uint32_t length=0;
    double t = utils::currmili();
    uint32_t nerr = this->apply_RS_decode_to_internal_memory();
    printf("Decode time %f\n", utils::currmili()-t);

    //for(int k = 0; k<this->RSn_*this->n_channels_; k++)
    //    printf("indadec %d, val %d\n",k, this->internal_memory_[k]);
    //

    if(nerr>0)
        DLOG("Warning, nerr = %d\n", nerr);
    int internal_status = RS_decoder::STILL_OK;
    if (/*(nerr>(this->get_RSn()-this->get_RSk())/2) ||*/ (nerr==-1)) //-1 is enough = data recovery failed
        internal_status = RS_decoder::TOO_MUCH_ERRORS;

    this->recreate_original_arr(this->internal_memory_, &data, &length);
    int k;
    //printf("Trying to printf chunk: \n", data);
    //for(int q=0;q<length;q++)printf("%c",data[q]);
    //printf("\n");

    if (internal_status != RS_decoder::TOO_MUCH_ERRORS)
        if(this->chunk_listener_){
            int context;
            if(this->is_header_frame_generating_){
                context = 1;
            } else {
                context = 0;
            }
            this->chunk_listener_->notifyNewChunk(length, data, context);
        }else{
            DLOG("Warn: no chunk listener to pass the decoded data to..\n");
        }
    if(length > 0 && data != NULL)
        delete []data;
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "RSdec", "Cleaining1 internal mem RS%d", (int)this);
#endif
    memset(this->internal_memory_, 0, sizeof(uint32_t)*this->n_channels_*this->RSn_);
}

RS_decoder::detector_status RS_decoder::tell_no_more_qr(){
    this->internal_getdata_from_internal_memory();
    return status_;
};

RS_decoder::detector_status RS_decoder::send_next_frame(EncodedFrame* frame){
    /////////////////// first action to recover previous chunk from the internal memory
    int ipos = (frame->get_frame_number() - this->fist_proper_framedata_number_for_this_decoder_) % this->RSn_;

    DCHECK(ipos >= 0);

    uint32_t curr_chunk = (frame->get_frame_number() - this->fist_proper_framedata_number_for_this_decoder_) / this->RSn_;

    DCHECK(curr_chunk >= 0);

#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "QRdec", "ipos %d, chunk %d, oldchn %d", ipos, curr_chunk, this->old_chunk_number_);
#endif

    if (curr_chunk > this->old_chunk_number_){ // time to decode the internal_memory_ + pack bits back to the original array
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "NATIVE", "XX5 time to prepare for new chunk");
#endif
        this->internal_getdata_from_internal_memory();
        //done encoding - reset erasure positions
        this->next_erasure_successful_num_position_ = 0;
        memset(this->internal_RS_successfull_indexes_per_chunk_, -1, sizeof(int) * this->RSn_);
        memset(this->internal_RS_erasure_location_mem_, 0, sizeof(int) * this->RSn_);
    }

    //save to the index array for calculation of the erasure position
    if(!this->is_successfull_pos_for_erasure_position_present(ipos)){
        this->internal_RS_successfull_indexes_per_chunk_[this->next_erasure_successful_num_position_] = ipos;
        this->next_erasure_successful_num_position_++;
    }
    DCHECK(this->next_erasure_successful_num_position_ <= this->RSn_);
    //

    /////////////////////////// action for the new frame that was actally send
    int nbits = utils::nbits_forsymcombinationsnumber(this->RSn_);

    int32_t numsym = this->n_channels_;

    //DCHECK(numsym==this->n_channels_);

    int offset = 4;
    if(this->is_header_frame_generating_){
        offset = 6;
    }

#ifdef ANDROID
      //LOGI("ABCQ2 : ipos %d : ", ipos);
#endif
    apply_pos_xor_to_arr((char*)(&(frame->framedata_[offset])), this->bytes_per_generated_frame_);
    for (uint32_t j = 0; j < numsym; j++){ //iterate over symbols within a frame
        uint32_t val = utils::get_data(&(frame->framedata_[offset]), j*nbits, nbits);
        if(val>this->RSn_)
            DLOG("ERROR decoder - value bigger than allowed symbol value !!!!\n");
        DCHECK(ipos+j*this->RSn_<this->RSn_*this->n_channels_);
        this->internal_memory_[ipos+j*this->RSn_] = val;
        //printf("QQQx indw %d, valw %d\n", ipos+j*this->RSn_, val);
#ifdef ANDROID
        //LOGI("(j%d)%d ", j, val);
#endif
    }

#ifdef ANDROID
      //LOGI("\n");
#endif


    this->old_chunk_number_ = curr_chunk;
    return this->status_;
};

void RS_decoder::set_nchannels_parallel(uint32_t nch){
    this->n_channels_ = nch;
};

void RS_decoder::set_bytes_per_generated_frame(uint32_t nb){
    this->bytes_per_generated_frame_ = nb;
}

Decoder::detector_status RS_decoder::get_detector_status(){

};

void RS_decoder::set_RS_nk(uint16_t n, uint16_t k){
    this->RSn_ = n;
    this->RSk_ = k;
    this->status_ = RS_decoder::STILL_OK;
    if(this->internal_memory_ != NULL){
        delete this->internal_memory_;
        this->internal_memory_ = NULL;
    }
    this->internal_memory_ = new uint32_t[n*this->n_channels_];
    memset(this->internal_memory_, 0, n*this->n_channels_*sizeof(uint32_t));

    int i = 0;
    while ((1<<RS_decoder::RSfecCodeConsts[i].symsize) != n+1)
        i++;
    this->RSfecCodeConsts_index_ = i;
    this->RSfecDec = init_rs_int(
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].symsize,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].genpoly,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].fcs,
            RS_decoder::RSfecCodeConsts[this->RSfecCodeConsts_index_].prim,
            n - k,
            0);
    if(this->internal_RS_erasure_location_mem_ != NULL){
        delete []this->internal_RS_erasure_location_mem_;
        this->internal_RS_erasure_location_mem_ = NULL;
    }
    this->internal_RS_erasure_location_mem_ = new int[this->RSn_];
    memset(this->internal_RS_erasure_location_mem_, 0, sizeof(uint32_t) * n);

    if(this->internal_RS_successfull_indexes_per_chunk_ != NULL){
        delete []this->internal_RS_successfull_indexes_per_chunk_;
        this->internal_RS_successfull_indexes_per_chunk_ = NULL;
    }
    this->internal_RS_successfull_indexes_per_chunk_ = new int[this->RSn_];
    memset(this->internal_RS_successfull_indexes_per_chunk_, -1, sizeof(uint32_t) * n);
    this->next_erasure_successful_num_position_ = 0;
};


bool RS_decoder::recreate_original_arr(/*internal_memory*/uint32_t *symbols_arr,
                                       char **data_produced, uint32_t* length_produced){
    *length_produced = this->RSk_ * this->n_channels_ * utils::nbits_forsymcombinationsnumber(this->RSn_) / 8;
    *data_produced = new char[*length_produced + end_corruption_overhead];
    if(*data_produced == NULL)
        return false;
    memset(*data_produced, 0, *length_produced + end_corruption_overhead);
#ifdef ANDROID
    LOGI("DATAA recr_original_arr : n_channels %d, RSn_ %d, RSk_ %d : ", this->n_channels_, this->RSn_, this->RSk_);
#endif
    int nbits = utils::nbits_forsymcombinationsnumber(this->RSn_);
    for (uint32_t j = 0; j<this->n_channels_; j++)
        for (uint32_t i = 0; i<this->RSk_; i++){
            //int nbb = (*length_produced) * sizeof(char) * 8 - (j * this->RSk_+ i)*nbits;
            utils::set_data((void*)*data_produced,
                            (j * this->RSk_+ i)*nbits,
                            symbols_arr[i+j*this->RSn_]);


#ifdef ANDROID
        if(this->is_residual_ && !processed_once_)LOGI("(i%d,j%d)%d ", i, j, symbols_arr[i+j*this->RSn_]);
#endif
        }

        /*
        if(this->RSk_ > 3){ //print internal mem
            printf("VVV1 dec internalmem : \n");
            for(int q = 0; q<this->n_channels_*this->RSn_;q++){
                printf("VVV2 (%d) %d \n", q, internal_memory_[q]);
            }
            printf("\n");
        }*/

#ifdef ANDROID
        LOGI("\n");
        LOGI("DATAA1 : ");
        for(int q = 0; q < *length_produced; q++){
            LOGI("%d ", (*data_produced)[q]);
        }
        LOGI("\n");
        processed_once_ = true;
#endif
    return true;
}

uint32_t RS_decoder::apply_RS_decode_to_internal_memory(){
    //calculate erasure positions from the frame indexes array - as required by the FEC. See the (7,3) test code
    int last_compared_index_in_succ_arr = 0;
    int curr_index_in_erasure_arr = 0;
    for (int q = 0; q < this->RSn_; q++){
        if(q != this->internal_RS_successfull_indexes_per_chunk_[last_compared_index_in_succ_arr]){
            this->internal_RS_erasure_location_mem_[curr_index_in_erasure_arr++] = q;
        }else{
            last_compared_index_in_succ_arr++;
            if(q == this->RSn_)
                break;
        }
    }
    int nerasures = this->RSn_ - this->next_erasure_successful_num_position_;
    //
#ifdef ANDROID
        /*__android_log_print(ANDROID_LOG_INFO, "rsdec", "neras%d, tab : %d %d %d %d %d %d %d ", nerasures,
                            internal_RS_erasure_location_mem_[0],
                            internal_RS_erasure_location_mem_[1],
                            internal_RS_erasure_location_mem_[2],
                            internal_RS_erasure_location_mem_[3],
                            internal_RS_erasure_location_mem_[4],
                            internal_RS_erasure_location_mem_[5],
                            internal_RS_erasure_location_mem_[6]);*/
#endif
    if (nerasures > this->RSn_ - this->RSk_) //do not try to decode, when it is known it advance that it will fail
        return -1;
    //
    uint32_t nerr = 0;
    printf("\n");
    for (uint32_t j = 0; j < this->n_channels_; j++){
        uint32_t e = decode_rs_int(this->RSfecDec, j*this->RSn_ + (int*)this->internal_memory_,
                             this->internal_RS_erasure_location_mem_, nerasures);
        printf("%d ",e);
        if (e > nerr)
            nerr = e;
    }
    printf("\n");
    return nerr;
};

bool RS_decoder::is_successfull_pos_for_erasure_position_present(int ipos_for_query){
    for(int i = this->next_erasure_successful_num_position_ - 1; i>=0; i--)
        if(this->internal_RS_successfull_indexes_per_chunk_[i] == ipos_for_query)
            return true;
        return false;
}

RS_decoder::codeconst RS_decoder::RSfecCodeConsts[] = {
 {2, 0x7,     1,   1, 1, 10 },
 {3, 0xb,     1,   1, 2, 10 },
 {4, 0x13,    1,   1, 4, 10 },
 {5, 0x25,    1,   1, 6, 10 },
 {6, 0x43,    1,   1, 8, 10 },
 {7, 0x89,    1,   1, 10, 10 },
 {8, 0x11d,   1,   1, 32, 10 },
 {9, 0x211,   1,   1, 32, 10 },
 {10,0x409,   1,   1, 32, 10 },
 {11,0x805,   1,   1, 32, 10 },
 {12,0x1053,  1,   1, 32, 5 },
 {13,0x201b,  1,   1, 32, 2 },
 {14,0x4443,  1,   1, 32, 1 },
 {15,0x8003,  1,   1, 32, 1 },
 {16,0x1100b, 1,   1, 32, 1 },
};
