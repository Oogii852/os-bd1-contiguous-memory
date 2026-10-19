#ifndef ALLOCATOR_H
#define ALLOCATOR_H

typedef enum { FIRST_FIT, BEST_FIT, WORST_FIT } Strategy;

typedef struct Block {
    int start;            /* ehleh hayag */
    int size;             /* hemjee (byte) */
    char name[16];        /* process ner, hooson bol "" (hole) */
    struct Block *next;
} Block;

void  mem_init(int total);
int   mem_request(const char *name, int size, Strategy s); /* 0 = OK, -1 = algaa */
int   mem_release(const char *name);
void  mem_compact(void);
void  mem_report(void);
Block *mem_head(void);   /* UI-d zoriulav */

#endif
