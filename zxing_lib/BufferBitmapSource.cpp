#include "BufferBitmapSource.h"
#include <iostream>
 
namespace qrviddec {
 
BufferBitmapSource::BufferBitmapSource(int inWidth, int inHeight, unsigned char * inBuffer) 
{
	width = inWidth; 
	height = inHeight; 
	buffer = inBuffer; 
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
 
unsigned char * BufferBitmapSource::getRow(int y, unsigned char * row)
{
	if (y < 0 || y >= height) 
	{
		fprintf(stderr, "ERROR, attempted to read row %d of a %d height image.\n", y, height); 
		return NULL; 
	}
	// WARNING: NO ERROR CHECKING! You will want to add some in your code. 
	if (row == NULL) row = new unsigned char[width]; 
	for (int x = 0; x < width; x ++)
	{
		row[x] = buffer[y*width+x]; 
	}
	return row; 
}
 
unsigned char * BufferBitmapSource::getMatrix()
{
	return buffer; 
}
 
}