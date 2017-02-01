x86_lib: open_rs_encoder.cpp
	g++ -fPIC -O0 -g -shared open_rs_encoder.cpp -o libRSencoder.so
all: x86_lib
clean:
	rm *.o *.so
