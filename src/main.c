#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "allocator.h"

int main(int argc, char *argv[]) {
    if (argc != 2 || atoi(argv[1]) <= 0) {
        fprintf(stderr, "Usage: %s <memory_size>\n", argv[0]);
        return 1;
    }
    mem_init(atoi(argv[1]));
    char cmd[16];
    printf("allocator> ");
    while (scanf("%15s", cmd) == 1) {
        if (strcmp(cmd, "X") == 0) break;
        else if (strcmp(cmd, "STAT") == 0) mem_report();
        else if (strcmp(cmd, "C") == 0) mem_compact();
        /* TODO: RQ <name> <size> <F|B|W>, RL <name> */
        printf("allocator> ");
    }
    return 0;
}
