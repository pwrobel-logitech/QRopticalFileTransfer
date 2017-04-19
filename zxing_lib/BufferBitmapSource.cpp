#include "BufferBitmapSource.h"
#include <iostream>
 
namespace qrviddec {
 
BufferBitmapSource::BufferBitmapSource(int inWidth, int inHeight, unsigned char* v)
    : LuminanceSource(inWidth, inHeight)
{
	width = inWidth; 
	height = inHeight; 
    buffer = v;
}

 
BufferBitmapSource::~BufferBitmapSource()
{
}
 
int BufferBitmapSource::getWidth() const
{
	return width; 
}
 
int BufferBitmapSource::getHeight() const
{
	return height; 
}
 
zxing::ArrayRef<char> BufferBitmapSource::getRow(int y, zxing::ArrayRef<char> row) const
{
	if (y < 0 || y >= height) 
	{
		fprintf(stderr, "ERROR, attempted to read row %d of a %d height image.\n", y, height); 
		return NULL; 
	}
	// WARNING: NO ERROR CHECKING! You will want to add some in your code. 
    ArrayRef<char> arrout(width);
	for (int x = 0; x < width; x ++)
	{
        arrout[x] = buffer[y*width+x];
	}
    const ArrayRef<char> resrow = arrout;
    return resrow;
}
 
zxing::ArrayRef<char> BufferBitmapSource::getMatrix() const
{
   ArrayRef<char> arrout(width*height);
   for(int q = 0; q < width * height; q++)
       arrout[q] = buffer[q];
   const ArrayRef<char> res = arrout;
   return res;
}
 
}
