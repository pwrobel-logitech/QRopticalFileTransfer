#Make the custom toolchain first from the android NDK
#$NDK/build/tools/make_standalone_toolchain.py \
#    --arch arm --api 14 --install-dir /tmp/my-android-toolchain

current_dir = $(shell pwd)
BASE=/home/pwrobel/Android/Sdk/my_toolchain
STRIP=$(BASE)/bin/arm-linux-androideabi-strip
CFLAGS_ARM=-I. -I$(BASE)/include/c++/4.9.x/bits/ -Wall
CPP_ARM=$(BASE)/bin/arm-linux-androideabi-g++

x86_lib: open_rs_encoder.cpp qr_frame_producer.cpp
	g++ -I$(current_dir) -fPIC -O0 -g -shared open_rs_encoder.cpp qr_frame_producer.cpp -o libRSencoder.so
fec_x86_test: ka9q_fac_test.cpp single_fac_test.cpp
	g++ -g -O0 ka9q_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/fec_x86_test -L$(current_dir)/bin_fec_x64 -lfec
	g++ -g -O0 single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/test_single -L$(current_dir)/bin_fec_x64 -lfec
clean:
	rm bin_fec_x64/fec_x86_test bin_fec_x64/test_single bin_fec_arm/test_single bin_fec_arm/main_arm *.o *.so main bin_fec_arm/libRSencoder.so
fec_arm_test: single_fac_test.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -DANDROID -fPIE -pie single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_arm/test_single -L$(current_dir)/bin_fec_arm -lfec
main: main.cpp
	g++ -I$(current_dir) -g -O0 main.cpp -o main -L$(current_dir) -lRSencoder
arm_lib: open_rs_encoder.cpp qr_frame_producer.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -I$(current_dir) -fPIC -DANDROID -O2 -shared open_rs_encoder.cpp qr_frame_producer.cpp -o bin_fec_arm/libRSencoder.so
	$(STRIP) $(current_dir)/bin_fec_arm/libRSencoder.so
main_arm: main.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -DANDROID -fPIE -I$(current_dir) -g -O0 main.cpp -o bin_fec_arm/main_arm -L$(current_dir)/bin_fec_arm -lRSencoder
all: x86_lib fec_x86_test fec_arm_test main 



