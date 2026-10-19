#ifndef ALLOCATOR_H
#define ALLOCATOR_H

#define NAME_LEN 16

typedef enum { FIRST_FIT, BEST_FIT, WORST_FIT } Strategy;

typedef struct Block {
    int start;              /* эхлэх хаяг */
    int size;               /* хэмжээ (byte) */
    char name[NAME_LEN];    /* процессын нэр, хоосон бол "" (hole) */
    struct Block *next;
} Block;

/* mem_request-ийн буцаах код */
#define MEM_OK         0
#define MEM_NO_HOLE   -1    /* хангалттай том hole алга */
#define MEM_BAD_ARG   -2    /* буруу нэр эсвэл хэмжээ */
#define MEM_DUPLICATE -3    /* ийм нэртэй процесс аль хэдийн байна */

void  mem_init(int total);
void  mem_destroy(void);
int   mem_request(const char *name, int size, Strategy s);
int   mem_release(const char *name);   /* 0 = OK, -1 = олдсонгүй */
void  mem_compact(void);
void  mem_report(void);
Block *mem_head(void);                 /* UI-д зориулав */
int   mem_total(void);
int   mem_free_total(void);            /* нийт чөлөөт санах ой */
int   mem_largest_hole(void);
double mem_fragmentation(void);        /* external fragmentation, % */

#endif
