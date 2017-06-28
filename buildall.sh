export LD_LIBRARY_PATH=/repos/qr/encoder_x86:/repos/qr/encoder_x86/bin_fec_x64:$LD_LIBRARY_PATH
make -f Makefile_x86_64 all
make -f Makefile_arm all
make -f Makefile_armv7a all

mkdir androidarchs
mkdir androidarchs/x86_64
mkdir androidarchs/armeabi
mkdir androidarchs/armeabi-v7a
 
cp bin_fec_x64/*.so androidarchs/x86_64
cp bin_fec_arm/*.so androidarchs/armeabi
cp bin_fec_armv7a/*.so androidarchs/armeabi-v7a
