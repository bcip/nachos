#include "syscall.h"

int main(){
    exec("bigarray.coff", 0, 0);
	halt();
}
