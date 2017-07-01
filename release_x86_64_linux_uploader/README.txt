	Camera Uploader version 0.0.1 beta

To upload the file/files as a stream of QR frames (picked up by the Android
application 'Optical File Transfer' with the camera) just do the following 
from the bash terminal :

	./uplcam file.pdf

That will use the default setting. If you want to upload with different speed/option,
you can do

	./uplcam -q 858 -s 20 -e 60 -t 8 file.pdf

That will use 585-byte QR frames, 20FPS(frames per second), maximum allowed
error of 60% and startup sequence of 8seconds

Any of these flags can be skipped - default value will then be used instead. 
The default values can be visible by doing the 
	
	./uplcam --help
	

	
DEPENDENCIES :

This program requires OpenGL installed + SDL2.0 and SDL2.0_ttf libs as well. For the
OpenGL, install the graphics driver in the system (likely you have it already
present), or 

	sudo apt-get install libgl1-mesa-dev
	
For the SDL2.0 part : 

	sudo apt-get install libsdl2-dev libsdl2-ttf-dev
	

	
ADDITIONAL NOTES :

Multiple files/wildcard is allowed. Like :

	./uplcam file1.pdf file2.pdf

If the transmission fails, try sending the frames slower/increase error tolerance
with the '-e' flag



LICENCES : 

External components are used. For the NotoMono-Regular.ttf font file from Google, 
see the LICENSE_OFL.txt to look for its licence.

For command-line argument parsing, the TCLAP library is used, it is under MIT 
licence, for the text of the MIT licence see the https://en.wikipedia.org/wiki/MIT_License

For the RS manipulations, the KA9Q library is used. See the https://github.com/Opendigitalradio/ka9q-fec.git
It is licenced under the LGPL licence - see the attached LGPL_licence.txt
To comply with this licence, the KA9Q library is compiled from that repository
as a shared library named libfec.so - you can compile that code yourself with 
the gcc 5.4.0 20160609 version (configuration below) and replace the libfec.so and use with this program.


For the QR encoding, the https://github.com/fukuchi/libqrencode library is used. It is being
wrapped as the libqrencoder_wrapper.so
The libqrencode is under the LGPL licence, so to comply with it, there is attached code for
that wrapper in the 'lgpl_libqrencode' dir - you can compile libqrencoder_wrapper.so yourself, if 
you wish to upgrade the libqrencode version.






Built with the system compiler configured as :
Using built-in specs.
COLLECT_GCC=gcc
COLLECT_LTO_WRAPPER=/usr/lib/gcc/x86_64-linux-gnu/5/lto-wrapper
Target: x86_64-linux-gnu
Configured with: ../src/configure -v --with-pkgversion='Ubuntu 5.4.0-6ubuntu1~16.04.4' --with-bugurl=file:///usr/share/doc/gcc-5/README.Bugs --enable-languages=c,ada,c++,java,go,d,fortran,objc,obj-c++ --prefix=/usr --program-suffix=-5 --enable-shared --enable-linker-build-id --libexecdir=/usr/lib --without-included-gettext --enable-threads=posix --libdir=/usr/lib --enable-nls --with-sysroot=/ --enable-clocale=gnu --enable-libstdcxx-debug --enable-libstdcxx-time=yes --with-default-libstdcxx-abi=new --enable-gnu-unique-object --disable-vtable-verify --enable-libmpx --enable-plugin --with-system-zlib --disable-browser-plugin --enable-java-awt=gtk --enable-gtk-cairo --with-java-home=/usr/lib/jvm/java-1.5.0-gcj-5-amd64/jre --enable-java-home --with-jvm-root-dir=/usr/lib/jvm/java-1.5.0-gcj-5-amd64 --with-jvm-jar-dir=/usr/lib/jvm-exports/java-1.5.0-gcj-5-amd64 --with-arch-directory=amd64 --with-ecj-jar=/usr/share/java/eclipse-ecj.jar --enable-objc-gc --enable-multiarch --disable-werror --with-arch-32=i686 --with-abi=m64 --with-multilib-list=m32,m64,mx32 --enable-multilib --with-tune=generic --enable-checking=release --build=x86_64-linux-gnu --host=x86_64-linux-gnu --target=x86_64-linux-gnu
