#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "allocator.h"

static Block *head = NULL;
static int total_mem = 0;

static Block *new_block(int start, int size, const char *name, Block *next) {
    Block *b = calloc(1, sizeof(Block));
    if (!b) { perror("calloc"); exit(1); }
    b->start = start;
    b->size = size;
    if (name) snprintf(b->name, NAME_LEN, "%s", name);
    b->next = next;
    return b;
}

void mem_init(int total) {
    mem_destroy();
    total_mem = total;
    head = new_block(0, total, "", NULL);
}

void mem_destroy(void) {
    while (head) {
        Block *n = head->next;
        free(head);
        head = n;
    }
    total_mem = 0;
}

Block *mem_head(void) { return head; }
int mem_total(void) { return total_mem; }

int mem_request(const char *name, int size, Strategy s) {
    if (!name || !name[0] || strlen(name) >= NAME_LEN || size <= 0)
        return MEM_BAD_ARG;

    for (Block *b = head; b; b = b->next)
        if (b->name[0] && strcmp(b->name, name) == 0)
            return MEM_DUPLICATE;

    /* стратегийн дагуу hole сонгох */
    Block *pick = NULL;
    for (Block *b = head; b; b = b->next) {
        if (b->name[0] || b->size < size) continue;
        if (s == FIRST_FIT) { pick = b; break; }
        if (!pick ||
            (s == BEST_FIT  && b->size < pick->size) ||
            (s == WORST_FIT && b->size > pick->size))
            pick = b;
    }
    if (!pick) return MEM_NO_HOLE;

    /* hole-г хуваах: үлдсэн хэсэг нь шинэ hole болно */
    if (pick->size > size) {
        pick->next = new_block(pick->start + size, pick->size - size, "", pick->next);
        pick->size = size;
    }
    snprintf(pick->name, NAME_LEN, "%s", name);
    return MEM_OK;
}

int mem_release(const char *name) {
    Block *target = NULL;
    for (Block *b = head; b; b = b->next)
        if (b->name[0] && strcmp(b->name, name) == 0) { target = b; break; }
    if (!target) return -1;

    target->name[0] = '\0';

    /* зэргэлдээ hole-уудыг нэгтгэх */
    for (Block *b = head; b && b->next; ) {
        if (!b->name[0] && !b->next->name[0]) {
            Block *n = b->next;
            b->size += n->size;
            b->next = n->next;
            free(n);
        } else {
            b = b->next;
        }
    }
    return 0;
}

void mem_compact(void) {
    Block *procs = NULL, *tail = NULL, *b = head, *n;
    int pos = 0;

    while (b) {
        n = b->next;
        if (b->name[0]) {                 /* процессыг эхэнд нь шахах */
            b->start = pos;
            pos += b->size;
            b->next = NULL;
            if (tail) tail->next = b; else procs = b;
            tail = b;
        } else {
            free(b);                      /* hole-уудыг устгана */
        }
        b = n;
    }
    if (pos < total_mem) {                /* төгсгөлд нэг том hole */
        Block *h = new_block(pos, total_mem - pos, "", NULL);
        if (tail) tail->next = h; else procs = h;
    }
    head = procs;
}

int mem_free_total(void) {
    int sum = 0;
    for (Block *b = head; b; b = b->next)
        if (!b->name[0]) sum += b->size;
    return sum;
}

int mem_largest_hole(void) {
    int max = 0;
    for (Block *b = head; b; b = b->next)
        if (!b->name[0] && b->size > max) max = b->size;
    return max;
}

/* external fragmentation = 1 - (хамгийн том hole / нийт чөлөөт) */
double mem_fragmentation(void) {
    int f = mem_free_total();
    if (f == 0) return 0.0;
    return (1.0 - (double)mem_largest_hole() / f) * 100.0;
}

void mem_report(void) {
    for (Block *b = head; b; b = b->next)
        printf("Addresses [%d:%d] %s %s\n", b->start, b->start + b->size - 1,
               b->name[0] ? "Process" : "Unused", b->name);
    printf("Free: %d, Largest hole: %d, External fragmentation: %.1f%%\n",
           mem_free_total(), mem_largest_hole(), mem_fragmentation());
}
