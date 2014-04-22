#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv){
	printf("child 2 starts running\n");
	int  i = 0, j = 0;
	int looptimes = 10;
	printf("child 2 gets params: ");
	for(; j < argc; j++)
		printf("%s ", argv[j]);
	for(; i < looptimes; i++)
		printf("\nchild 2 in loop %d", i);
	printf("\nchild 2 ends running\n");
	return 0;
}
