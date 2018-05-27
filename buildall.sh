export LD_LIBRARY_PATH=/repos/qr/encoder_x86:/repos/qr/encoder_x86/bin_fec_x64:$LD_LIBRARY_PATH
make -f Makefile_x86_64 all
make -f Makefile_x86_64_release all
make -f Makefile_arm all
make -f Makefile_armv7a all
make -f Makefile_arm64 all

mkdir androidarchs
mkdir androidarchs/x86_64
mkdir androidarchs/armeabi
mkdir androidarchs/armeabi-v7a
mkdir androidarchs/arm64-v8a
 
cp bin_fec_x64/*.so androidarchs/x86_64
cp bin_fec_arm/*.so androidarchs/armeabi
cp bin_fec_armv7a/*.so androidarchs/armeabi-v7a
cp bin_fec_arm64/*.so androidarchs/arm64-v8a


mkdir release_x86_64_linux_uploader/lgpl_libqrencode_wrapper
cp libqrencoder_wrapper/libqrencoder_wrapper.cpp libqrencoder_wrapper/libqrencoder_wrapper.h libqrencoder_wrapper/Makefile  release_x86_64_linux_uploader/lgpl_libqrencode_wrapper
cp -R release_x86_64_linux_uploader linux_OpticalFileUploader
tar -czf linux_OpticalFileUploader.tar.gz linux_OpticalFileUploader

mkdir win32encoder_libs/external_lgpl_rebuild
cp libqrencoder_wrapper/libqrencoder_wrapper.cpp win32encoder_libs/external_lgpl_rebuild
cp libqrencoder_wrapper/libqrencoder_wrapper.h win32encoder_libs/external_lgpl_rebuild
cp public_encoder_api.h win32encoder_libs
