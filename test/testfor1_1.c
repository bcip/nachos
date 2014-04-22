#include "syscall.h"
#include "stdio.h"

int main() {
    test();
    return 0;
}

int test() {
    int fd;
    char temp;
    if (open("test") != -1) printf("Error1!\n");
    if ((fd = creat("test")) < 0) printf("Error2!\n");
    if (fd < 0) printf("Error3!\n");
    if (read(fd, &temp, 1) != 0) printf("Error4!\n");
    write(fd, "foobar", 6);
    if (close(fd) < 0) printf("Error5!\n");
    return 0;
}
