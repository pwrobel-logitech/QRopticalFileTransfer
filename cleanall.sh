make -f Makefile_x86_64 clean
make -f Makefile_x86_64_release clean
make -f Makefile_arm clean
make -f Makefile_armv7a clean
make -f Makefile_arm64 clean


rm androidarchs/x86_64/*
rm androidarchs/armeabi/*
rm androidarchs/armeabi-v7a/*
rm androidarchs/arm64-v8a/*
rm -rf linux_OpticalFileUploader
rm -rf linux_OpticalFileUploader.tar.gz