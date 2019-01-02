#include <stdio.h>

void f(int a, int b){
    a = a*a;
    b = b*b;
    printf("f: %d, %d\n", a, b);
}

void g(int *a, int *b){
    *a = *a * *a;
    *b = *b * *b;
    printf("g: %d, %d\n", *a, *b);
}

int main(int argc, char*argv[]) {

    printf("hello there\n");

    int x = 2;
    int z = 3;

    f(x, z);
    printf("program after f: %d, %d\n", x, z);
    g(&x, &z);
    printf("program after g: %d, %d\n", x, z);

}