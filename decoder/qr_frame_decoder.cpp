#include "qr_frame_decoder.h"
#include "hash-library/sha256.h"
#include <iostream>

#ifdef ANDROID
#include <android/log.h>
#endif

#include "globaldefs.h"

void* QR_frame_decoder::thrfunc(void* arg){
    AsyncInfo* as = (AsyncInfo*)arg;
    QR_frame_decoder* self = (QR_frame_decoder*)(as->chlistener);

    while (true) {
        bool running = true;

        pthread_mutex_lock(&(as->async_mutex_));
        running = as->async_thread_alive_;
        //pthread_mutex_unlock(&(as->async_mutex_));

        if(!running)
            break;

        //pthread_mutex_lock(&(as->async_mutex_));
        while (as->async_thread_waiting_) {
            printf("Async is going to wait...\n");
            pthread_cond_wait(&(as->async_condvar_), &(as->async_mutex_));
        }
        //pthread_mutex_unlock(&(as->async_mutex_));


        //do some action here, using the async info provided


        bool filecompleted = false;
        //pthread_mutex_lock(&(as->async_mutex_));
        filecompleted = self->is_all_file_processing_done_;
        pthread_mutex_unlock(&(as->async_mutex_));
        if(!filecompleted)
            as->current_decoder->execute_RS_async_action();



        pthread_mutex_lock(&(as->async_mutex_));
        as->async_thread_waiting_ = true;
        as->async_main_is_waiting_for_thread_to_complete_ = false;
        printf("Signaling main thread work is done, can stop to wait...\n");
        pthread_cond_broadcast(&(as->async_main_wait_));
        pthread_mutex_unlock(&(as->async_mutex_));

    }

};

QR_frame_decoder::QR_frame_decoder(){
    pthread_mutex_init(&(this->async_info_.async_mutex_), NULL);

    pthread_cond_init(&(this->async_info_.async_condvar_), NULL);
    pthread_cond_init(&(this->async_info_.async_main_wait_), NULL);

    pthread_mutex_lock(&(this->async_info_.async_mutex_));


    this->header_data_.resize(0);
    this->header_data_tmp_.resize(0);
    this->main_chunk_data_.resize(0);
    this->main_chunk_data_tmp_.resize(0);
    this->decoder_ = new RS_decoder(this);
    this->res_decoder_ = new RS_decoder(this);
    this->is_header_generating_ = true;

    this->qr_byte_length = 0;
    //
    this->header_decoder_ = new RS_decoder(this);

    this->last_analyzed_header_pos_ = 0;
    this->header_detection_done_ = false;
    this->decoder_res_bytes_len_ = 0;
    this->decoder_bytes_len_ = 0;
    this->first_proper_framenumber_into_res_decoder_ = 0;
    this->is_switched_to_residual_data_decoder_ = false;
    this->position_in_file_to_flush_ = 0;
    this->api_told_filepath_ = std::string("");
    this->file_info_.cache_suffix = std::string(".__tmpcache");
    this->is_all_file_processing_done_ = false;
    this->is_hash_of_flushed_file_correct_ = false;
    this->file_info_.fp = NULL;
    this->last_header_frame_number_processed_ = -1;
    this->last_frame_number_processed_ = -1;
    ////async part setup


    this->async_info_.async_thread_alive_ = true;
    this->async_info_.async_main_is_waiting_for_thread_to_complete_ = false;
    this->async_info_.async_thread_waiting_ = true;
    this->async_info_.chlistener = this;
    this->async_info_.internal_mem = NULL;
    this->async_info_.recreated_data = NULL;
    pthread_mutex_unlock(&(this->async_info_.async_mutex_));
    pthread_create(&(this->async_info_.async_thr_id_), NULL, QR_frame_decoder::thrfunc, (void*)(&(this->async_info_)));

}

void QR_frame_decoder::reconfigure_qr_size(int qrlen){

    this->qr_byte_length = qrlen;

    int headerRSn = 7;
    int headerRSk = 3;

    this->header_decoder_->set_header_frame_generating(true);
    this->header_decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(headerRSn, 256, qrlen-6)-1);
    this->header_decoder_->set_bytes_per_generated_frame(qrlen-6);
    this->header_decoder_->set_RS_nk(headerRSn, headerRSk);
    //this->header_decoder_->set_chunk_listener(this);

}

immediate_status QR_frame_decoder::destroy_and_get_filetransfer_status(){
    immediate_status final_status = ERRONEUS; //fake
    pthread_mutex_lock(&(this->async_info_.async_mutex_));

    this->async_info_.async_thread_waiting_ = false;
    pthread_cond_broadcast(&(this->async_info_.async_condvar_));



    while (this->async_info_.async_main_is_waiting_for_thread_to_complete_){
        pthread_cond_wait(&(async_info_.async_main_wait_), &(async_info_.async_mutex_));
    }

    if(this->is_all_file_processing_done_ && this->is_hash_of_flushed_file_correct_){
        final_status = ALREADY_CORRECTLY_TRANSFERRED;
    }

    if(this->is_all_file_processing_done_ && !this->is_hash_of_flushed_file_correct_){
        final_status = ERRONEUS_HASH_WRONG;
    }

    if(!this->is_all_file_processing_done_){ //if processing failed, clean up temp file
        std::string fullpathname = this->file_info_.filepath + this->file_info_.filename + this->file_info_.cache_suffix;
        remove_file(fullpathname.c_str()); //remove suffixed file - it is wrong
    }

    this->async_info_.async_thread_alive_ = false;
    this->async_info_.async_thread_waiting_ = false;
    this->async_info_.async_main_is_waiting_for_thread_to_complete_ = false;
    this->is_all_file_processing_done_ = true;
    pthread_cond_broadcast(&(this->async_info_.async_main_wait_));
    pthread_cond_broadcast(&(this->async_info_.async_condvar_));

    //pthread_mutex_unlock(&(this->async_info_.async_mutex_));

    //pthread_mutex_lock(&(this->async_info_.async_mutex_));

    //wait for the async action to complete

    //pthread_mutex_unlock(&(this->async_info_.async_mutex_));


    //pthread_mutex_lock(&(this->async_info_.async_mutex_));
    if (this->decoder_ != NULL)
        delete this->decoder_;
    if (this->res_decoder_ != NULL)
        delete this->res_decoder_;
    if (this->header_decoder_ != NULL)
        delete this->header_decoder_;
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "DEL", "XXXX1");
#endif
    if(this->file_info_.fp){
        if(this->file_info_.fp != NULL){
            FileClose(this->file_info_.fp);
            this->file_info_.fp = NULL;
        }
    }



    ///async destruction part

    this->async_info_.async_thread_alive_ = false;
    this->async_info_.async_main_is_waiting_for_thread_to_complete_ = false;
    pthread_cond_broadcast(&(this->async_info_.async_main_wait_));
    pthread_cond_broadcast(&(this->async_info_.async_condvar_));

    pthread_mutex_unlock(&(this->async_info_.async_mutex_));
    pthread_join(this->async_info_.async_thr_id_, NULL);
    return final_status;
};

QR_frame_decoder::~QR_frame_decoder(){

}

immediate_status QR_frame_decoder::tell_no_more_qr(){
    bool switched_to_residual_data_decoder;
    pthread_mutex_lock(&(this->async_info_.async_mutex_));
    switched_to_residual_data_decoder = this->is_switched_to_residual_data_decoder_;
    pthread_mutex_unlock(&(this->async_info_.async_mutex_));

    if (this->qr_byte_length <= 0)
        return NOT_INITIALIZED;
    immediate_status stat = RECOGNIZED;
    if(this->is_header_generating_){
        if (this->header_decoder_){
            this->header_decoder_->tell_no_more_qr();
        }
    }else{
        if (this->decoder_){
            if(!switched_to_residual_data_decoder)
                this->decoder_->tell_no_more_qr();
        }
        if (this->res_decoder_){
            if(switched_to_residual_data_decoder)
                this->res_decoder_->tell_no_more_qr();
        }
    }
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if (this->is_all_file_processing_done_ && (!this->is_hash_of_flushed_file_correct_))
        stat = ERRONEUS_HASH_WRONG;
    if (this->is_all_file_processing_done_ && (this->is_hash_of_flushed_file_correct_))
        stat = ALREADY_CORRECTLY_TRANSFERRED;
    return stat;
};

void QR_frame_decoder::tell_file_generation_path(const char* filepath){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    this->api_told_filepath_ = std::string(filepath);
    if(this->api_told_filepath_.c_str()[this->api_told_filepath_.length()-1] != '/')
      this->api_told_filepath_ += "/";
};

int QR_frame_decoder::get_total_frames_of_data_that_will_be_produced(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if(this->is_header_generating_)
        return -1; //we do not know yet

    if(this->decoder_bytes_len_ <= 0)
        return -1;
    int nchunk_main = this->file_info_.filelength / this->decoder_bytes_len_;
    return nchunk_main*this->RSn_ + this->RSn_rem_;
};

int QR_frame_decoder::get_last_number_of_frame_detected(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    return this->last_frame_number_processed_;
};

int QR_frame_decoder::get_last_number_of_header_frame_detected(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    return this->last_header_frame_number_processed_;
};

void QR_frame_decoder::setup_detector_after_header_recognized(){
    //printf("QQQFL : %d\n", this->file_info_.filelength);
    //exit(0);

    uint32_t qrlen = this->qr_byte_length;
    this->RSn_ = this->file_info_.RSn;
    this->RSk_ = this->file_info_.RSk;
    this->RSn_rem_ = this->file_info_.RSn_residual;
    this->RSk_rem_ = this->file_info_.RSk_residual;
    this->decoder_->set_header_frame_generating(false);
    int nchp = utils::count_symbols_to_fit(this->RSn_, 256, qrlen-4)-1;
    this->decoder_->set_nchannels_parallel(nchp);
    this->decoder_->set_bytes_per_generated_frame(qrlen-4);
    this->decoder_->set_RS_nk(this->RSn_, this->RSk_);
    //this->decoder_->set_chunk_listener(this);
    this->decoder_->set_configured(true);
    this->decoder_->fist_proper_framedata_number_for_this_decoder(0);

    this->res_decoder_->set_header_frame_generating(false);
    int nchpr = utils::count_symbols_to_fit(this->RSn_rem_, 256, qrlen-4)-1;
    this->res_decoder_->set_nchannels_parallel(nchpr);
    this->res_decoder_->set_bytes_per_generated_frame(qrlen-4);
    this->res_decoder_->set_RS_nk(this->RSn_rem_, this->RSk_rem_);
    //this->res_decoder_->set_chunk_listener(this);
    this->res_decoder_->set_configured(true);
    //this->res_decoder_->is_residual_ = true;

    uint32_t chun_len = this->RSk_ * nchp * utils::nbits_forsymcombinationsnumber(this->RSn_) / 8;
    uint32_t chun_len_res = this->RSk_rem_ * nchpr * utils::nbits_forsymcombinationsnumber(this->RSn_rem_) / 8;

    this->decoder_bytes_len_ = chun_len;
    this->decoder_res_bytes_len_ = chun_len_res;

    int nchunk_main = this->file_info_.filelength / chun_len;
    //offset on the remainder decoder
    this->first_proper_framenumber_into_res_decoder_ = nchunk_main*this->RSn_;
    this->res_decoder_->fist_proper_framedata_number_for_this_decoder(nchunk_main*this->RSn_);
};


//medatada DD pattern
// MMMMTThhh..hhHHH..HH|NNKKnnkkQQQQQLLhhhh..hhcc..ccc
// MM - 0xBAADA551 - magic byte seq
// 2byte length total, 1 byte length of hash, XB hash metadata, XB hash metadata + variable length content |
// 4B (N,K), 4B (n,k), 5Bfilelength(Q), 2B length fname, 1B hash length, XB file name, XB file hash content

int QR_frame_decoder::analyze_header(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "NATIVE", "XX0 start analyze header, TEST1S %d", strtol("0x90695ffc", NULL, 16));
#endif
    if(this->header_detection_done_) //detection already succeeded earlier
        return 1;
    int status = 0;
    if(this->header_data_tmp_.size() - this->last_analyzed_header_pos_ < 32)
        return 0;
    int spos = this->last_analyzed_header_pos_;
    int pos = spos;
    while (pos < this->header_data_tmp_.size() - 32){
        char* start = &this->header_data_tmp_[pos];
        uint32_t pos_start = pos;
        uint32_t potential_magic = *((uint32_t*)start);
            if(potential_magic != 0xBAADA551){
                pos++;
                continue;
            }
        //we have magic header start - check further
        //end

        pos += 4; //skip over magic bytes
        uint16_t totalL = *((uint16_t*) (start + pos-pos_start));
        pos += 2; //skip over total length
        char* header_hash = start + pos;
        pos += 8; //skip over the header hash content
        char* header_hash_varL = start + pos;
        pos += 8; //skip over the header hash content with variable length
        uint16_t N = *((uint16_t*)(start+pos-pos_start+0));
        uint16_t K = *((uint16_t*)(start+pos-pos_start+2));
        uint16_t n = *((uint16_t*)(start+pos-pos_start+4));
        uint16_t k = *((uint16_t*)(start+pos-pos_start+6));
        pos += 8; //skip over nk fields
        uint32_t flength = *((uint32_t*)(pos-pos_start+start));
        pos += 5; //skip over the file length
        uint16_t fname_length = *((uint16_t*)(start+pos-pos_start));
        pos += 2; //skip the file name length field
        uint16_t fcontent_hash = *((uint16_t*)(start+pos-pos_start));
        pos += 8; //skip file content hash
        /// now, before proceeding with reading file name, we first check correctness of the
        /// first header hash

#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO,
        //                    "NATIVE", "XX2 badass detected, N%d, K%d, tl%d, fl%d, fnl%d",
        //                    N, K, totalL, flength, fname_length);
#endif

        //hash small hash - not the filename content and not the hashes itself
        SHA256 sha256stream;
        sha256stream.add(start, 6); //hash
        sha256stream.add(start + 22, 23);
        std::string h_small = sha256stream.getHash();

        std::string hs_low  = std::string(h_small.c_str(), 8);
        uint32_t hs_wlow = (uint32_t)strtoul(hs_low.c_str(), NULL, 16);
        std::string hs_high = std::string(h_small.c_str() + 8, 8);
        uint32_t hs_whigh = (uint32_t)strtoul(hs_high.c_str(), NULL, 16);
        uint32_t writen_hs_wlow  = *((uint32_t*)(start + 6));
        uint32_t writen_hs_whigh = *((uint32_t*)(start + 10));
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO,
        //                    "NATIVE", "XX2a hl%d hh%d, whl%d whh%d, c1 %d, c2 %d",
        //                    hs_wlow, hs_whigh, writen_hs_wlow, writen_hs_whigh, (hs_wlow == writen_hs_wlow), (hs_whigh == writen_hs_whigh));
#endif
        if ((hs_wlow != writen_hs_wlow) || (hs_whigh != writen_hs_whigh)) {
            continue; // if first hash over header does not match, ingnore this batch and go further
        }

#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "NATIVE", "XX3 got small hash", 1);
#endif

        // now we trust all the header fields, if the hashes match - file name lenth included

        //hash big - file name text as well
        SHA256 sha256streamB;
        sha256streamB.add(start, 6); //hash
        sha256streamB.add(start + 22, 23);
        sha256streamB.add(start + 45, fname_length);
        std::string h_big = sha256streamB.getHash();

        std::string hb_low  = std::string(h_big.c_str(), 8);
        uint32_t hb_wlow = (uint32_t)strtoul(hb_low.c_str(), NULL, 16);
        uint32_t written_hb_wlow = *((uint32_t*)(start + 6 + 8));
        std::string hb_high = std::string(h_big.c_str() + 8, 8);
        uint32_t hb_whigh = (uint32_t)strtoul(hb_high.c_str(), NULL, 16);
        uint32_t written_hb_whigh = *((uint32_t*)(start + 10 + 8));

#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO,
        //                    "NATIVE", "XX3a hl%d hh%d, whl%d whh%d",
        //                    hb_wlow, hb_whigh, written_hb_wlow, written_hb_whigh);
#endif

        if ((hb_wlow != written_hb_wlow) || (hb_whigh != written_hb_whigh)) {  //big hash mismatch
            continue;
        }
        //now, here we are sure(hash collision unlikely) based on all the hashes, that we
        //have the correct metadata guessed, we can save it and not bother to analyze it further

        this->file_info_.RSn = N;
        this->file_info_.RSk = K;
        this->file_info_.RSn_residual = n;
        this->file_info_.RSk_residual = k;
        this->file_info_.filelength = flength;
        this->file_info_.filename = std::string(start + 45, fname_length);
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "NATIVE", "QQ got header correctly %s", this->file_info_.filename.c_str());
#endif
        this->file_info_.filepath = this->api_told_filepath_;
        if(this->file_info_.fp != NULL){
            FileClose(this->file_info_.fp);
            this->file_info_.fp = NULL;
        }
        std::string fullpathname = this->file_info_.filepath + this->file_info_.filename + this->file_info_.cache_suffix;
        this->file_info_.fp = FileOpenToWrite(fullpathname.c_str());
        this->file_info_.hash.resize(8);
        for(int q = 0; q < 8; q++)
            this->file_info_.hash[q] = ((char*)(start + 37))[q];

        this->header_detection_done_ = true;
        this->setup_detector_after_header_recognized();
        return 1;
    }
    if (pos - this->last_analyzed_header_pos_ > 1024)
        this->last_analyzed_header_pos_ = pos;
    return status;
}

immediate_status QR_frame_decoder::send_next_grayscale_qr_frame(const char *grayscale_qr_data,
                                                                int image_width, int image_height){
    int generated_datalength;
    char* generated_data = NULL;

    immediate_status ret_status =
            generate_data_from_qr_greyscalebuffer(&generated_datalength, &generated_data,
                                                  grayscale_qr_data,
                                                  image_width, image_height);

    uint32_t nfr, nhfr;
    {utils::ScopeLock l(this->async_info_.async_mutex_);

    if(generated_data == NULL)
        return NOT_RECOGNIZED;

    if(ret_status == NOT_RECOGNIZED)
        return NOT_RECOGNIZED;
    ////////////////////////// process generated_data = extract frame number
    nfr = *((uint32_t*)generated_data);
    nhfr = *((uint16_t*)(generated_data+4));

    if(nfr == 0xffffffff)
        this->last_header_frame_number_processed_ = nhfr;
    else
        this->last_frame_number_processed_ = nfr;

    if((nfr != 0xffffffff) && (!this->header_detection_done_))
        return ERR_DATAFRAME_TOO_EARLY;

    if((nfr == 0xffffffff) && (this->header_detection_done_)){
        return HEADER_ALREADY_DETECTED;
    }

    if((nfr != 0xffffffff) && (this->header_detection_done_)){
        this->is_header_generating_ = false; //switch to data detection
    }

    if(nfr==0xffffffff && (this->header_decoder_->get_configured()==false)){
        this->is_header_generating_ = true;
        DCHECK(generated_datalength > 8);
        this->reconfigure_qr_size(generated_datalength);
        this->header_decoder_->set_configured(true);
    }

    if (generated_datalength != this->qr_byte_length)
        return LENGTH_QR_CHANGED;

    if(this->is_header_generating_){
        //printf("qqrh %d, ",nhfr);
        //for(int k = 0; k<generated_datalength;k++)printf("0x%02hhx ", (generated_data)[k]);
        //printf("\n");
    }else{
        //printf("qqr %d, ",nfr);
        //for(int k = 0; k<generated_datalength;k++)printf("0x%02hhx ", (generated_data)[k]);
        //printf("\n");
#ifdef ANDROID
        //LOGI("qqr %d, ",nfr);
        //for(int k = 0; k<generated_datalength;k++)LOGI("0x%02hhx ", (generated_data)[k]);
        //LOGI("\n");
#endif
    }
    }
    /*
    if(nfr < this->decoder_->get_nframe()){ //impossible - we got less than previously received
        ret_status = ERRONEUS;
        return ret_status;
    }

    if(nfr > this->decoder_->get_nframe() + this->RSn_){//we missed too many frames - recovery will be impossible for sure
        ret_status = ERRONEUS;
        return ret_status;
    }
    */
    //int ipos = nfr % this->RSn_;


    EncodedFrame* fr = new OpenRSEncodedFrame();

    if(this->is_header_generating_){
        fr->set_frame_number(nhfr);
        fr->framedata_.resize(generated_datalength+end_corruption_overhead);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(this->header_decoder_->get_RSn(), this->header_decoder_->get_RSk());

        printf("header frame set %d\n",nfr);
        printf("FR send : \n");
        //for(int q=0;q<fr->framedata_.size()-end_corruption_overhead;q++){
        //    printf("%d ", fr->framedata_[q]);
        //}
        RS_decoder::detector_status dec_status = this->header_decoder_->send_next_frame(fr);
        //if (dec_status == Decoder::TOO_MUCH_ERRORS)
        //    ret_status = ERRONEUS;
        //pthread_mutex_lock(&this->async_info_.async_mutex_);
        this->analyze_header();
        //pthread_mutex_unlock(&this->async_info_.async_mutex_);
    }else{

        Decoder* final_decoder;
        int chosenRSn, chosenRSk;

        if(nfr < first_proper_framenumber_into_res_decoder_){
            final_decoder = this->decoder_;
            printf("ABCQ1 : decoder set - main\n");
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "ABCQ1", "decoder set - main");
#endif
        }else{
            bool switched_to_residual_data_decoder;
            pthread_mutex_lock(&(this->async_info_.async_mutex_));
            switched_to_residual_data_decoder = this->is_switched_to_residual_data_decoder_;
            pthread_mutex_unlock(&(this->async_info_.async_mutex_));
            if(!switched_to_residual_data_decoder){
                if(this->file_info_.filelength >= this->decoder_bytes_len_)
                    this->decoder_->tell_no_more_qr();
                pthread_mutex_lock(&(this->async_info_.async_mutex_));
                this->is_switched_to_residual_data_decoder_ = true;
                pthread_mutex_unlock(&(this->async_info_.async_mutex_));
            }
            final_decoder = this->res_decoder_;
            printf("ABCQ1 : decoder set - residual\n");
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "ABCQ1", "decoder set - residual");
#endif
        }
        pthread_mutex_lock(&(this->async_info_.async_mutex_));
        chosenRSn = final_decoder->get_RSn();
        chosenRSk = final_decoder->get_RSk();

        fr->set_frame_number(nfr);
        fr->framedata_.resize(generated_datalength+end_corruption_overhead);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(chosenRSn, chosenRSk);

        printf("frame set %d\n",nfr);
        printf("FR send : \n");
        for(int q=0;q<fr->framedata_.size()-end_corruption_overhead;q++){
            printf("%d ", fr->framedata_[q]);
        }
        pthread_mutex_unlock(&(this->async_info_.async_mutex_));
        RS_decoder::detector_status dec_status = final_decoder->send_next_frame(fr);
        delete fr;
        pthread_mutex_lock(&(this->async_info_.async_mutex_));
        if (dec_status == Decoder::TOO_MUCH_ERRORS)
            ret_status = ERRONEUS;
        if (ret_status != ERRONEUS){
            if (this->is_all_file_processing_done_ && (!this->is_hash_of_flushed_file_correct_))
                ret_status = ERRONEUS_HASH_WRONG;
            if (this->is_all_file_processing_done_ && (this->is_hash_of_flushed_file_correct_))
                ret_status = ALREADY_CORRECTLY_TRANSFERRED;
        }
        pthread_mutex_unlock(&(this->async_info_.async_mutex_));
    }
    delete []generated_data;
    return ret_status;
}

int QR_frame_decoder::notifyNewChunk(int chunklength, const char* chunkdata, int context){
    DCHECK(chunklength>=0);
    if(chunklength==0)
        DLOG("Warn, chunklength is zero\n");
    if(context == 1){
        for(int i = 0; i<chunklength; i++){
            this->header_data_tmp_.push_back(chunkdata[i]);
        }
        printf("New header temp chunkdata currently saved is %d \n", this->header_data_tmp_.size());
    }else if(context == 0){
        for(int i = 0; i<chunklength; i++){
            this->main_chunk_data_tmp_.push_back(chunkdata[i]);
        }
        printf("New temp chunkdata currently saved is %d \n", this->main_chunk_data_tmp_.size());
        uint32_t datalen = chunklength;
        if (this->async_info_.is_switched_to_residual_data_decoder_){
            datalen = this->file_info_.filelength % this->decoder_bytes_len_;
            if (datalen > chunklength)
                datalen = chunklength;
        }
        if(this->position_in_file_to_flush_ + datalen <= this->file_info_.filelength)
            this->flush_data_to_file(chunkdata, datalen);
        this->main_chunk_data_tmp_.resize(0); // eradicate, after flushing to file
#ifdef ANDROID
        __android_log_print(ANDROID_LOG_INFO, "NATIVE", "XX4 got new data chunk chl %d, contx %d, datalen %d",
                            chunklength, context, datalen);
#endif
    }

    return 0;
}

void QR_frame_decoder::flush_data_to_file(const char *data, uint32_t datalen){
#ifdef ANDROID
        //__android_log_print(ANDROID_LOG_INFO, "NATIVE", "QQ0 flushing datalen %d to file c1 %c c2 %c", datalen,
        //                    data[0], data[1]);
        //if (datalen == 173){
        //   LOGI("QQ1b dat173 :");
        //   for (int i = 0; i<datalen; i++){
        //       LOGI("0x%02hhx ", data[i]);
        //   }
        //   LOGI("\n");
        //}
#endif
    int stat = 0;
    if(this->file_info_.fp)
        stat = write_file_fp(this->file_info_.fp, data, this->position_in_file_to_flush_, datalen);
    this->position_in_file_to_flush_ += datalen;
    if(this->async_info_.is_switched_to_residual_data_decoder_){ //last remaining part of file was saved, check hash
        if(this->file_info_.fp != NULL){
            FileClose(this->file_info_.fp);
            this->file_info_.fp = NULL;
        }

        std::string fullpathname = this->file_info_.filepath + this->file_info_.filename + this->file_info_.cache_suffix;
        this->file_info_.fp = FileOpenToRead(fullpathname.c_str());
        bool is_hash_ok = this->is_file_hash_correct();
        if(this->file_info_.fp != NULL){
            FileClose(this->file_info_.fp);
            this->file_info_.fp = NULL;
        }
        if(!is_hash_ok){
            remove_file(fullpathname.c_str()); //remove suffixed file - it is wrong
        }
        else { //move suffixed temp file to final file name
            std::string finalname = this->file_info_.filepath + this->file_info_.filename;
            int status = FileRename(fullpathname.c_str(), finalname.c_str());
            if(status == -1){
                DLOG("Failed to finally rename file!\n");
            }
        }
        this->file_info_.fp = NULL;
        this->is_all_file_processing_done_ = true;
        this->is_hash_of_flushed_file_correct_ = is_hash_ok;
    }
}

bool QR_frame_decoder::is_file_hash_correct(){
    uint32_t hash_chunk_size = fixed_filehash_buff_size;
    if (this->file_info_.fp == NULL)
        return false;
    int chunk_num = this->file_info_.filelength / hash_chunk_size;
    int bytes_remain = this->file_info_.filelength % hash_chunk_size;
    SHA256 sha256stream;
    uint32_t lastpos = 0;
    for(int i = 0; i < chunk_num; i++){
        if(read_file_fp(this->file_info_.fp, fixed_filehash_buff,
                        i*hash_chunk_size, hash_chunk_size) == -1){
                            DLOG("ERR, failed to read file !\n");
                            return false;
                        }
        sha256stream.add(fixed_filehash_buff, hash_chunk_size); //hash
        lastpos += hash_chunk_size;
    }
    if (bytes_remain > 0){
        if(read_file_fp(this->file_info_.fp, fixed_filehash_buff,
                        lastpos, bytes_remain) == -1){
                            DLOG("ERR, failed to read file !\n");
                            return false;
                        }
        sha256stream.add(fixed_filehash_buff, bytes_remain); //hash
    }
    std::string h_small = sha256stream.getHash();



    std::string hs_low  = std::string(h_small.c_str(), 8);
    uint32_t hs_wlow = (uint32_t)strtoul(hs_low.c_str(), NULL, 16);
    uint32_t hs_wlow_from_header =*((uint32_t*)(&this->file_info_.hash[0]));
    std::string hs_high = std::string(h_small.c_str() + 8, 8);
    uint32_t hs_whigh = (uint32_t)strtoul(hs_high.c_str(), NULL, 16);
    uint32_t hs_whigh_from_header = *((uint32_t*)(&this->file_info_.hash[4]));
    return ((hs_wlow == hs_wlow_from_header) && (hs_whigh == hs_whigh_from_header));
};

void QR_frame_decoder::print_current_header(){
    //printf("Curr header tmp, size %d : \n", this->header_data_tmp_.size());
    //for(int i=0; i<this->header_data_tmp_.size(); i++)
    //   printf("%c", this->header_data_tmp_[i]);
};

void QR_frame_decoder::print_current_maindata(){
    //printf("Curr maindata tmp , size %d : \n", this->main_chunk_data_tmp_.size());
    //for(int i=0; i<this->main_chunk_data_tmp_.size(); i++)
    //    printf("%c", this->main_chunk_data_tmp_[i]);
};

//API functions

int QR_frame_decoder::get_main_RSN(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if(this->header_detection_done_){
        return this->file_info_.RSn;
    }else{
        return -1;
    }
};

int QR_frame_decoder::get_main_RSK(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if(this->header_detection_done_){
        return this->file_info_.RSk;
    }else{
        return -1;
    }
};

int QR_frame_decoder::get_residual_RSN(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if(this->header_detection_done_){
        return this->file_info_.RSn_residual;
    }else{
        return -1;
    }
};

int QR_frame_decoder::get_residual_RSK(){
    utils::ScopeLock l(this->async_info_.async_mutex_);
    if(this->header_detection_done_){
        return this->file_info_.RSk_residual;
    }else{
        return -1;
    }
};
