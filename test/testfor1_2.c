#include "syscall.h"
#include "stdio.h"

int main() {
    test();
    return 0;
}

int test() {
    int fd1, fd2, fd3;
    char buffer[20], temp;
    fd1 = open("test");
    fd2 = open("test");
    fd3 = open("test");
    read(fd1, &temp, 1);
    if (temp != 'f') printf("Error6!\n");
    read(fd1, &temp, 1);
    if (temp != 'o') printf("Error7!\n");
    read(fd2, &temp, 1);
    if (temp != 'f') printf("Error8!\n");
    close(fd1);
    read(fd3, buffer, 6);
    buffer[6] = 0;
    printf("%s\n", buffer); // must be "foobar", otherwise Error!\n
    if (close(10) != -1) printf("Error9!\n");
    if (close(0) < 0) printf("Error10!\n");
    if (unlink("test_") != -1) printf("Error11!\n");
    if (unlink("test") < 0) printf("Error12!\n");
    if (open("test") != -1) printf("Error13!\n");
    close(fd2);
    close(fd3);
    printf("Right!");
    return 0;
}

