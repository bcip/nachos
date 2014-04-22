#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main() {
	int i = 2;
	char a = 'a';
	printf("test create\n");
	int num = 10;
	for (i = 2; i < num; i++) {
		printf("%c:%d ", a, creat(&a));
		a = a + 1;
	}
	a = 'a';
	printf("\n%c:%d", a, creat(&a));

	printf("test open\n");
	printf("open %s:%d ", "a", open("a"));
	printf("open %s:%d ", "ab", open("ab"));

	printf("test write\n");
	printf("write to a:%d\n", write(2, "xieyh", 4));

	printf("test read\n");
	char buffer[20];
	printf("read a 10:%d\n", read(10, buffer, 2));
	printf("read text:%s\n", buffer);

	printf("read a 2:%d\n", read(10, buffer, 5));
	printf("read text:%s\n", buffer);
	printf("close a 10: %d\n", close(10));
	printf("read a 10: %d\n", read(10, buffer, 1));

	printf("unlink a: %d\n", unlink("a"));
	printf("close a 2: %d\n", close(2));
	printf("open a 10: %d\n", open("a"));

	printf("test exec\n");
	char* para1[] = {"p11", "p12"};
	int* status;
	int ret  = join(exec("child1.coff", 2, para1), status);
	printf("child 1 exec by join with return value %d and status %d\n", ret, *status); 
	for(i = 0; i < 5; i++){
		printf("parent in loop %d\n", i);
	}
	char* para2[] = {"p21", "p22", "p23"};
	ret  = join(exec("child2.coff", 3, para2), status);
	printf("child 2 exec by join with return value %d and status %d\n", ret, *status);
	for(i = 5; i < 8; i++){
		printf("parent in loop %d\n", i);
	}
	halt();
	return 0;
}
