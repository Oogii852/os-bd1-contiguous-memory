#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "allocator.h"

static int parse_size(const char *s, long *out) {
    char *end;
    errno = 0;
    long v = strtol(s, &end, 10);
    if (errno || *end != '\0' || v <= 0 || v > 1000000000L) return -1;
    *out = v;
    return 0;
}

int main(int argc, char *argv[]) {
    long total;
    if (argc != 2 || parse_size(argv[1], &total) != 0) {
        fprintf(stderr, "Usage: %s <memory_size>   (эерэг бүхэл тоо)\n", argv[0]);
        return 1;
    }
    mem_init((int)total);

    char cmd[16], name[64], strat[16], sz[32];
    printf("allocator> ");
    while (scanf("%15s", cmd) == 1) {
        if (strcmp(cmd, "X") == 0) break;
        else if (strcmp(cmd, "STAT") == 0) mem_report();
        else if (strcmp(cmd, "C") == 0) mem_compact();
        else if (strcmp(cmd, "RQ") == 0) {
            long size;
            if (scanf("%63s %31s %15s", name, sz, strat) != 3) {
                fprintf(stderr, "Usage: RQ <name> <size> <F|B|W>\n");
            } else if (parse_size(sz, &size) != 0) {
                fprintf(stderr, "Алдаа: хэмжээ эерэг бүхэл тоо байх ёстой\n");
            } else if (strlen(strat) != 1 || !strchr("FBW", strat[0])) {
                fprintf(stderr, "Алдаа: стратеги нь F, B эсвэл W байна\n");
            } else {
                Strategy s = strat[0] == 'F' ? FIRST_FIT :
                             strat[0] == 'B' ? BEST_FIT : WORST_FIT;
                switch (mem_request(name, (int)size, s)) {
                    case MEM_OK:        break;
                    case MEM_NO_HOLE:   fprintf(stderr, "Алдаа: %s-д хангалттай том hole алга\n", name); break;
                    case MEM_DUPLICATE: fprintf(stderr, "Алдаа: %s нэртэй процесс байна\n", name); break;
                    default:            fprintf(stderr, "Алдаа: буруу нэр эсвэл хэмжээ (нэр <= 15 тэмдэгт)\n");
                }
            }
        } else if (strcmp(cmd, "RL") == 0) {
            if (scanf("%63s", name) != 1)
                fprintf(stderr, "Usage: RL <name>\n");
            else if (mem_release(name) != 0)
                fprintf(stderr, "Алдаа: %s процесс олдсонгүй\n", name);
        } else {
            fprintf(stderr, "Үл мэдэгдэх команд: %s\n", cmd);
        }
        printf("allocator> ");
    }
    mem_destroy();
    return 0;
}
