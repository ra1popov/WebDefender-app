
#ifndef MINIZ_H
#define MINIZ_H

#ifdef __cplusplus
extern "C" {
#endif

typedef unsigned int mz_uint;
void *mz_zip_extract_archive_file_to_heap(const char *pZip_filename, const char *pArchive_name,
											size_t *pSize, mz_uint zip_flags);
void mz_free(void *p);

#ifdef __cplusplus
}
#endif

#endif /* MINIZ_H */
