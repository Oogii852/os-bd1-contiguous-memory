#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "allocator.h"

static Block *head = NULL;
static int total_mem = 0;

void mem_init(int total) {
    total_mem = total;
    head = calloc(1, sizeof(Block));
    head->start = 0;
    head->size = total;
}

Block *mem_head(void) { return head; }

/* TODO: mem_request (first/best/worst fit), mem_release (hole merge),
         mem_compact, mem_report */
int mem_request(const char *name, int size, Strategy s) { (void)name; (void)size; (void)s; return -1; }
int mem_release(const char *name) { (void)name; return -1; }
void mem_compact(void) {}
void mem_report(void) {
    for (Block *b = head; b; b = b->next)
        printf("Addresses [%d:%d] %s\n", b->start, b->start + b->size - 1,
               b->name[0] ? b->name : "Unused");
}
