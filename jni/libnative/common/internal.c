
/*
 * last modified: 2013.12.03
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <ctype.h>

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "common.h"

// return allocated address aligned to a 4-byte boundary
void* i_malloc (size_t size)
{
	void *ptr = malloc(size);
	if (!IS_ALIGNED(ptr, 4))
		E("ptr not aligned");

	return ptr;
}

void i_free (void *ptr)
{
	return free(ptr);
}

/* ----------------------------------------------------------- */

FILE* i_fopen (const char *filename, const char *modes)
{
	return fopen(filename, modes);
}

int i_fclose (FILE *stream)
{
	return fclose(stream);
}

size_t i_fread (void *ptr, size_t size, size_t n, FILE *stream)
{
	return fread(ptr, size, n, stream);
}

size_t i_fwrite (const void *ptr, size_t size, size_t n, FILE *s)
{
	return fwrite(ptr, size, n, s);
}

int i_fileno (FILE *stream)
{
	return fileno(stream);
}

int i_fstat (int fd, struct stat *buf)
{
	return fstat(fd, buf);
}
