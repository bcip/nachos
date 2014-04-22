#include "syscall.h"

int main(){
	int i = 0;
	int status = 0;
	int ret = 0;
	char* params[] = {"a", "b"};
	for(; i < 100; i++){
		join(exec("child.coff", 2, params), &status);
		printf("%d: %d %d\n",(i+1), ret, status);
	}
	halt();
}
