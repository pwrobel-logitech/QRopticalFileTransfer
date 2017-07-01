current_dir = $(shell pwd)
LIBQRENCODER_LIBFOLDERNAME=libqrencode
CFLAGS=-O2
CROSS=
all: libqrencoder_wrapper.cpp
	 $(CROSS)g++ $(CFLAGS) -DSTATIC_IN_RELEASE=static -DMAJOR_VERSION=1 -DMINOR_VERSION=1 -DVERSION=\"1\" -DMICRO_VERSION=1 -I$(current_dir)/$(LIBQRENCODER_LIBFOLDERNAME) libqrencoder_wrapper.cpp $(current_dir)/$(LIBQRENCODER_LIBFOLDERNAME)/*.c -fPIC -shared -o $(current_dir)/libqrencoder_wrapper.so  
