current_dir = $(shell pwd)

x86_lib: open_rs_encoder.cpp
	g++ -fPIC -O0 -g -shared open_rs_encoder.cpp -o libRSencoder.so
fec_x86_test: ka9q_fac_test.cpp
	g++ -g -O0 ka9q_fac_test.cpp -Wl,-rpath=\$$ORIGIN -o bin_fec_x64/fec_x86_test -L$(current_dir)/bin_fec_x64 -lfec
clean:
	rm bin_fec_x64/fec_x86_test *.o *.so 
all: x86_lib fec_x86_test

