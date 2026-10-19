CC      = gcc
CFLAGS  = -Wall -Wextra -std=c11 -g
SRC     = src/main.c src/allocator.c
OBJ     = $(SRC:.c=.o)
TARGET  = allocator

all: $(TARGET)

$(TARGET): $(OBJ)
	$(CC) $(CFLAGS) -o $@ $^

%.o: %.c src/allocator.h
	$(CC) $(CFLAGS) -c $< -o $@

test: $(TARGET)
	./$(TARGET) 1000 < tests/basic.txt

clean:
	rm -f $(OBJ) $(TARGET)
