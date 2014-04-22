#include "syscall.h"

int main(){
	int i = 0;
	int status = 0;
	int ret = 0;
	char* params[] = {"a", "b"};
	int eret = 0;
	for(; i < 100; i++){
		eret = exec("child.coff", 2, params);
		ret = join(eret, &status);
		printf("%d: exec: %d, join: %d\n",(i+1), eret, ret);
	}
	halt();
}
