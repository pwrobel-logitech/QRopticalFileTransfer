#include "qr_frame_decoder.h"
#include "hash-library/sha256.h"
#include <iostream>

QR_frame_decoder::QR_frame_decoder(){
    this->header_data_.resize(0);
    this->header_data_tmp_.resize(0);
    this->main_chunk_data_.resize(0);
    this->main_chunk_data_tmp_.resize(0);
    this->decoder_ = new RS_decoder();
    this->res_decoder_ = new RS_decoder();

    this->qr_byte_length = 0;
    //
    this->header_decoder_ = new RS_decoder();
    this->last_analyzed_header_pos_ = 0;
    this->header_detection_done_ = false;
    this->decoder_res_bytes_len_ = 0;
    this->decoder_bytes_len_ = 0;
    this->first_proper_framenumber_into_res_decoder_ = 0;
}

void QR_frame_decoder::reconfigure_qr_size(int qrlen){

    this->qr_byte_length = qrlen;

    int headerRSn = 7;
    int headerRSk = 3;

    this->header_decoder_->set_header_frame_generating(true);
    this->header_decoder_->set_nchannels_parallel(utils::count_symbols_to_fit(headerRSn, 256, qrlen-6)-1);
    this->header_decoder_->set_bytes_per_generated_frame(qrlen-6);
    this->header_decoder_->set_RS_nk(headerRSn, headerRSk);
    this->header_decoder_->set_chunk_listener(this);

}

QR_frame_decoder::~QR_frame_decoder(){
    if (this->decoder_ != NULL)
        delete this->decoder_;
    if (this->res_decoder_ != NULL)
        delete this->res_decoder_;
    if (this->header_decoder_ != NULL)
        delete this->header_decoder_;
}

immediate_status QR_frame_decoder::tell_no_more_qr(){
    immediate_status stat = RECOGNIZED;
    if(this->is_header_generating_){
        if (this->header_decoder_){
            this->header_decoder_->tell_no_more_qr();
        }
    }else{
        if (this->decoder_){
            this->decoder_->tell_no_more_qr();
        }
        if (this->res_decoder_){
            this->res_decoder_->tell_no_more_qr();
        }
    }
    return stat;
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
    this->decoder_->set_chunk_listener(this);
    this->decoder_->set_configured(true);
    this->decoder_->fist_proper_framedata_number_for_this_decoder(0);

    this->res_decoder_->set_header_frame_generating(false);
    int nchpr = utils::count_symbols_to_fit(this->RSn_rem_, 256, qrlen-4)-1;
    this->res_decoder_->set_nchannels_parallel(nchpr);
    this->res_decoder_->set_bytes_per_generated_frame(qrlen-4);
    this->res_decoder_->set_RS_nk(this->RSn_rem_, this->RSk_rem_);
    this->res_decoder_->set_chunk_listener(this);
    this->res_decoder_->set_configured(true);

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

        //hash small hash - not the filename content and not the hashes itself
        SHA256 sha256stream;
        sha256stream.add(start, 6); //hash
        sha256stream.add(start + 22, 23);
        std::string h_small = sha256stream.getHash();

        std::string hs_low  = std::string(h_small.c_str(), 8);
        uint32_t hs_wlow = (uint32_t)strtol(hs_low.c_str(), NULL, 16);
        std::string hs_high = std::string(h_small.c_str() + 8, 8);
        uint32_t hs_whigh = (uint32_t)strtol(hs_high.c_str(), NULL, 16);
        uint32_t writen_hs_wlow  = *((uint32_t*)(start + 6));
        uint32_t writen_hs_whigh = *((uint32_t*)(start + 10));
        if ((hs_wlow != writen_hs_wlow) || (hs_whigh != writen_hs_whigh)) {
            continue; // if first hash over header does not match, ingnore this batch and go further
        }

        // now we trust all the header fields, if the hashes match - file name lenth included

        //hash big - file name text as well
        SHA256 sha256streamB;
        sha256streamB.add(start, 6); //hash
        sha256streamB.add(start + 22, 23);
        sha256streamB.add(start + 45, fname_length);
        std::string h_big = sha256streamB.getHash();

        std::string hb_low  = std::string(h_big.c_str(), 8);
        uint32_t hb_wlow = (uint32_t)strtol(hb_low.c_str(), NULL, 16);
        uint32_t written_hb_wlow = *((uint32_t*)(start + 6 + 8));
        std::string hb_high = std::string(h_big.c_str() + 8, 8);
        uint32_t hb_whigh = (uint32_t)strtol(hb_high.c_str(), NULL, 16);
        uint32_t written_hb_whigh = *((uint32_t*)(start + 10 + 8));
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
    char* generated_data;
    immediate_status ret_status =
            generate_data_from_qr_greyscalebuffer(&generated_datalength, &generated_data,
                                                  grayscale_qr_data,
                                                  image_width, image_height);
    ////////////////////////// process generated_data = extract frame number
    uint32_t nfr = *((uint32_t*)generated_data);

    if((nfr != 0xffffffff) && (!this->header_detection_done_))
        return ERR_DATAFRAME_TOO_EARLY;

    if((nfr != 0xffffffff) && (this->header_detection_done_)){
        this->is_header_generating_ = false; //switch to data detection
    }

    if(nfr==0xffffffff && (this->header_decoder_->get_configured()==false)){
        this->is_header_generating_ = true;
        DCHECK(generated_datalength > 8);
        this->reconfigure_qr_size(generated_datalength);
        this->header_decoder_->set_configured(true);
    }

    uint32_t nhfr = *((uint16_t*)(generated_data+4));

    if(this->is_header_generating_){
        printf("qqrh %d, ",nhfr);
        for(int k = 0; k<generated_datalength;k++)printf("0x%02hhx ", (generated_data)[k]);
        printf("\n");
    }else{
        printf("qqr %d, ",nfr);
        for(int k = 0; k<generated_datalength;k++)printf("0x%02hhx ", (generated_data)[k]);
        printf("\n");
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
        fr->framedata_.resize(generated_datalength);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(this->header_decoder_->get_RSn(), this->header_decoder_->get_RSk());

        printf("header frame set %d\n",nfr);
        printf("FR send : \n");
        for(int q=0;q<fr->framedata_.size();q++){
            printf("%d ", fr->framedata_[q]);
        }
        RS_decoder::detector_status dec_status = this->header_decoder_->send_next_frame(fr);
        //if (dec_status == Decoder::TOO_MUCH_ERRORS)
        //    ret_status = ERRONEUS;
        this->analyze_header();
    }else{

        Decoder* final_decoder;
        int chosenRSn, chosenRSk;

        if(nfr < first_proper_framenumber_into_res_decoder_){
            final_decoder = this->decoder_;
        }else{
            this->decoder_->tell_no_more_qr();
            final_decoder = this->res_decoder_;
        }
        chosenRSn = final_decoder->get_RSn();
        chosenRSk = final_decoder->get_RSk();

        fr->set_frame_number(nfr);
        fr->framedata_.resize(generated_datalength);
        memcpy(&(fr->framedata_[0]), generated_data, generated_datalength);
        fr->set_frame_RSnk(chosenRSn, chosenRSk);

        printf("frame set %d\n",nfr);
        printf("FR send : \n");
        for(int q=0;q<fr->framedata_.size();q++){
            printf("%d ", fr->framedata_[q]);
        }
        RS_decoder::detector_status dec_status = final_decoder->send_next_frame(fr);
        if (dec_status == Decoder::TOO_MUCH_ERRORS)
            ret_status = ERRONEUS;
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
    }

    return 0;
}

void QR_frame_decoder::print_current_header(){
    printf("Curr header tmp, size %d : \n", this->header_data_tmp_.size());
    for(int i=0; i<this->header_data_tmp_.size(); i++)
        printf("%c", this->header_data_tmp_[i]);
};

void QR_frame_decoder::print_current_maindata(){
    printf("Curr maindata tmp , size %d : \n", this->main_chunk_data_tmp_.size());
    for(int i=0; i<this->main_chunk_data_tmp_.size(); i++)
        printf("%c", this->main_chunk_data_tmp_[i]);
};
