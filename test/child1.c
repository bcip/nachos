#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv){
	printf("child 1 starts running\n");
	int  i = 0, j = 0;
	int looptimes = 4;
	printf("child 1 gets params: ");
	for(; j < argc; j++)
		printf("%s ", argv[j]);
	for(; i < looptimes; i++)
		printf("\nchild 1 in loop %d", i);
	printf("\nchild 1 ends running\n");
	return 0;
}
