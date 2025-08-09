
/*
 * last modified: 2013.12.04
 */

#ifndef INTERNAL_H
#define INTERNAL_H

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>

#include <stdio.h>
#include <sys/stat.h>

#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

void*  i_malloc (size_t size);
void   i_free   (void *ptr);

/* ----------------------------------------------------------- */

FILE*  i_fopen  (const char *filename, const char *modes);
int    i_fclose (FILE *stream);
size_t i_fread  (void *ptr, size_t size, size_t n, FILE *stream);
size_t i_fwrite (const void *ptr, size_t size, size_t n, FILE *s);
int    i_fileno (FILE *stream);
int    i_fstat  (int fd, struct stat *buf);

#ifdef __cplusplus
}
#endif

#endif /* INTERNAL_H */
