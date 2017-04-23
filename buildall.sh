export LD_LIBRARY_PATH=/repos/qr/encoder_x86:/repos/qr/encoder_x86/bin_fec_x64:$LD_LIBRARY_PATH
make -f Makefile_x86_64 all
make -f Makefile_arm all
make -f Makefile_armv7a all
