int main(int argc, char **argv)
{
    char *name = "return.coff";
	char *args[1];
	args[0] = "return";
	int value[10];
	int t[20];
	int i = 0;
	for(i = 0; i < 20; i++)
	    t[i] = 0;
	for(i = 0; i < 10; i++)
	{
	    value[i] = exec(name, 1, args);
		printf("value %d = %d\n", i, value[i]);
		if(t[value[i]] = 0)
		    t[value[i]] = 1;
	    else
		    printf("Something same!\n");
	}
	return 0;
}
