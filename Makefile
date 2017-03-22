#Make the custom toolchain first from the android NDK
#$NDK/build/tools/make_standalone_toolchain.py \
#    --arch arm --api 14 --install-dir /tmp/my-android-toolchain

current_dir = $(shell pwd)
BASE=/home/pwrobel/Android/Sdk/my_toolchain
STRIP=$(BASE)/bin/arm-linux-androideabi-strip
CFLAGS_ARM=-I. -I$(BASE)/include/c++/4.9.x/bits/ -Wall
CPP_ARM=$(BASE)/bin/arm-linux-androideabi-g++
STRIP_ARM=$(BASE)/bin/arm-linux-androideabi-strip
LIBQRENCODER_LIBFOLDERNAME=libqrencode
LIBZXING=../zxing-cpp/core/src

x86_lib: open_rs_encoder.cpp qr_frame_producer.cpp fileutil/fileops.cpp libqrencode_wrapper
	g++ -I$(current_dir) -I$(current_dir)/libqrencoder_wrapper -fPIC -O0 -g -shared open_rs_encoder.cpp qr_frame_producer.cpp $(current_dir)/fileutil/fileops.cpp -o libRSencoder.so -L$(current_dir)/bin_fec_x64 -lfec -lqrencoder_wrapper
fec_x86_test: ka9q_fac_test.cpp single_fac_test.cpp fileutil/fileops.cpp
	g++ -g -O0 ka9q_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/fec_x86_test -L$(current_dir)/bin_fec_x64 -lfec
	g++ -g -O0 single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/test_single -L$(current_dir)/bin_fec_x64 -lfec
clean:
	rm bin_fec_x64/fec_x86_test bin_fec_x64/test_single bin_fec_arm/test_single bin_fec_arm/main_arm bin_fec_arm/libqrencoder* *.o *.so main bin_fec_arm/libRSencoder.so bin_fec_arm/libRSdecoder.so bin_fec_x64/libqrencoder_wrapper.so bin_fec_x64/libqrencoder_test bin_fec_x64/decoder_test bin_fec_x64/libRSdecoder.so
fec_arm_test: single_fac_test.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -DANDROID -pie single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_arm/test_single -L$(current_dir)/bin_fec_arm -lfec
main: main.cpp
	g++ -I$(current_dir) -g -O0 main.cpp -o main -L$(current_dir) -lRSencoder -L$(current_dir)/bin_fec_x64 -lfec -lqrencoder_wrapper
arm_lib: open_rs_encoder.cpp qr_frame_producer.cpp fileutil/fileops.cpp libqrencode_wrapper_arm
	$(CPP_ARM) $(CFLAGS_ARM) -I$(current_dir) -I$(current_dir)/libqrencoder_wrapper -fPIC -DANDROID -O2 -shared open_rs_encoder.cpp qr_frame_producer.cpp fileutil/fileops.cpp -o bin_fec_arm/libRSencoder.so -L$(current_dir)/bin_fec_arm -lfec -lqrencoder_wrapper
	$(STRIP) $(current_dir)/bin_fec_arm/libRSencoder.so
main_arm: main.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -DANDROID -pie -I$(current_dir) -O2 main.cpp -o bin_fec_arm/main_arm -Wl,-rpath=\$$ORIGIN -L$(current_dir)/bin_fec_arm -lRSencoder
	
libqrencode_wrapper:
	g++ -g -O0 -DSTATIC_IN_RELEASE=static -DMAJOR_VERSION=1 -DMINOR_VERSION=1 -DVERSION=\"1\" -DMICRO_VERSION=1 -I$(current_dir) -I$(current_dir)/.. -I$(current_dir)/../$(LIBQRENCODER_LIBFOLDERNAME) -I$(current_dir)/libqrencoder_wrapper libqrencoder_wrapper/libqrencoder_wrapper.cpp $(current_dir)/bin_jpeg/arch_x86_64/libjpeg.a -I$(current_dir)/$(LIBZXING) -I$(current_dir)/zxing_lib $(current_dir)/zxing_lib/BufferBitmapSource.cpp $(current_dir)/zxing_lib/arch_x86_64/libzxing.a $(current_dir)/../$(LIBQRENCODER_LIBFOLDERNAME)/*.c -fPIC -shared -o $(current_dir)/bin_fec_x64/libqrencoder_wrapper.so  
	g++ -g -O0 -I$(current_dir) -I$(current_dir)/libqrencoder_wrapper libqrencoder_wrapper/libqrencoder_wrapper_test.cpp -fPIC -pie -o $(current_dir)/bin_fec_x64/libqrencoder_test -L$(current_dir)/bin_fec_x64 -lqrencoder_wrapper
libqrencode_wrapper_arm:
	$(CPP_ARM) $(CFLAGS_ARM)  -O2 -fPIC -DSTATIC_IN_RELEASE=static -DMAJOR_VERSION=1 -DMINOR_VERSION=1 -DVERSION=\"1\" -DMICRO_VERSION=1 -I$(current_dir) -I$(current_dir)/.. -I$(current_dir)/../$(LIBQRENCODER_LIBFOLDERNAME) -I$(current_dir)/libqrencoder_wrapper libqrencoder_wrapper/libqrencoder_wrapper.cpp $(current_dir)/bin_jpeg/arch_arm/libjpeg.a -I$(current_dir)/zxing_lib/arch_arm $(current_dir)/zxing_lib/arch_arm/iconv.c -I$(current_dir)/$(LIBZXING) -I$(current_dir)/zxing_lib $(current_dir)/zxing_lib/BufferBitmapSource.cpp -fpermissive $(current_dir)/zxing_lib/arch_arm/libzxing.a $(current_dir)/../$(LIBQRENCODER_LIBFOLDERNAME)/*.c -fPIC -shared -o $(current_dir)/bin_fec_arm/libqrencoder_wrapper.so 
	$(STRIP_ARM) $(current_dir)/bin_fec_arm/libqrencoder_wrapper.so
	$(CPP_ARM) $(CFLAGS_ARM)  -O2 -I$(current_dir) -I$(current_dir)/libqrencoder_wrapper libqrencoder_wrapper/libqrencoder_wrapper_test.cpp -pie -o $(current_dir)/bin_fec_arm/libqrencoder_test -L$(current_dir)/bin_fec_arm -lqrencoder_wrapper

arm_decoder_lib: libqrencode_wrapper decoder/qr_frame_decoder.cpp decoder/rs_decoder.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -O2 -I$(current_dir) -I$(current_dir)/fec_include -I$(current_dir)/decoder decoder/qr_frame_decoder.cpp $(current_dir)/decoder/rs_decoder.cpp $(current_dir)/fileutil/fileops.cpp -shared -fPIC -o $(current_dir)/bin_fec_arm/libRSdecoder.so -L$(current_dir)/bin_fec_arm -lfec -lqrencoder_wrapper
	$(STRIP) $(current_dir)/bin_fec_arm/libRSdecoder.so
arm_decoder_test: arm_decoder_lib decoder/decoder_test.cpp 
	$(CPP_ARM) $(CFLAGS_ARM) -O2 -I$(current_dir) -I$(current_dir)/decoder decoder/decoder_test.cpp -pie -o $(current_dir)/bin_fec_arm/decoder_test -L$(current_dir)/bin_fec_arm -lRSdecoder -lqrencoder_wrapper
	
x86_decoder_lib: libqrencode_wrapper decoder/qr_frame_decoder.cpp decoder/rs_decoder.cpp
	g++ -g -O0 -I$(current_dir) -I$(current_dir)/fec_include -I$(current_dir)/decoder decoder/qr_frame_decoder.cpp $(current_dir)/decoder/rs_decoder.cpp $(current_dir)/fileutil/fileops.cpp -shared -fPIC -o $(current_dir)/bin_fec_x64/libRSdecoder.so -L$(current_dir)/bin_fec_x64 -lfec -lqrencoder_wrapper
x86_decoder_test: x86_decoder_lib decoder/decoder_test.cpp 
	g++ -g -O0 -I$(current_dir) -I$(current_dir)/decoder decoder/decoder_test.cpp -fPIC -o $(current_dir)/bin_fec_x64/decoder_test -L$(current_dir)/bin_fec_x64 -lRSdecoder -lqrencoder_wrapper
all: x86_lib fec_x86_test fec_arm_test main arm_lib main_arm libqrencode_wrapper_arm x86_decoder_test arm_decoder_test



