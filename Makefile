current_dir = $(shell pwd)

CFLAGS_ARM=--sysroot=/home/pwrobel/Android/Sdk/ndk-bundle/platforms/android-14/arch-arm/ -I/home/pwrobel/Android/Sdk/ndk-bundle/platforms/android-14/arch-arm/usr/include -I/home/pwrobel/Android/Sdk/ndk-bundle/sources/android/support/include/ -O2 -I. -Wall
CPP_ARM=/home/pwrobel/Android/Sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-g++

x86_lib: open_rs_encoder.cpp qr_frame_producer.cpp
	g++ -I$(current_dir) -fPIC -O0 -g -shared open_rs_encoder.cpp qr_frame_producer.cpp -o libRSencoder.so
fec_x86_test: ka9q_fac_test.cpp single_fac_test.cpp
	g++ -g -O0 ka9q_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/fec_x86_test -L$(current_dir)/bin_fec_x64 -lfec
	g++ -g -O0 single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/test_single -L$(current_dir)/bin_fec_x64 -lfec
clean:
	rm bin_fec_x64/fec_x86_test bin_fec_x64/test_single bin_fec_arm/test_single *.o *.so main
fec_arm_test: single_fac_test.cpp
	$(CPP_ARM) $(CFLAGS_ARM) -fPIE -pie single_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_arm/test_single -L$(current_dir)/bin_fec_arm -lfec
main: main.cpp
	g++ -I$(current_dir) -g -O0 main.cpp -o main -L$(current_dir) -lRSencoder
all: x86_lib fec_x86_test fec_arm_test main



