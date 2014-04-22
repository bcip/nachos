#include "syscall.h"

int main(){
   int a = exec("bigarray.coff", 0, 0);
	 printf("bigarray: %d\n", a);
	halt();
}
