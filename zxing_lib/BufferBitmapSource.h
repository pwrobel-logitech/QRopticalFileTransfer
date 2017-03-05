#include <zxing/LuminanceSource.h>
#include <stdio.h>
#include <stdlib.h>
using namespace zxing; 
namespace qrviddec {
 
class BufferBitmapSource : public LuminanceSource {
private:
  int width, height; 
  unsigned char * buffer; 
 
public:
  BufferBitmapSource(int inWidth, int inHeight, unsigned char * inBuffer); 
  ~BufferBitmapSource(); 
 
  int getWidth() const; 
  int getHeight() const; 
  unsigned char* getRow(int y, unsigned char* row); 
  unsigned char* getMatrix(); 
}; 
}