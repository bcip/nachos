#include "syscall.h"

int main(){
	int a[10000];
	int i = 0;
	for(; i < 1000; i++){
		a[i] = i;
	}
	return 0;
}
