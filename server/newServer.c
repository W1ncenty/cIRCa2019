#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <netdb.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#define SERVER_PORT 1234
#define QUEUE_SIZE 5

struct thread_data_t {
    int licznik;
    int soc;
    char r_msg[64];
    char w_msg[64];
};

pthread_mutex_t mutex_w = PTHREAD_MUTEX_INITIALIZER;;
pthread_mutex_t mutex_r = PTHREAD_MUTEX_INITIALIZER;;

int client_sockets[5];

void *ThreadBehavior_Read(void *t_data) {

    pthread_detach(pthread_self());
    struct thread_data_t *th_data = (struct thread_data_t*)t_data;
    //dostęp do pól struktury: (*th_data).pole

    // odbieranie wiadomości i wypisywanie ich na konsolę
    while(1){
        //pthread_mutex_lock(&mutex_r);
        //printf("blokada dla odczytu!\n");
        memset((*th_data).r_msg, 0, sizeof((*th_data).r_msg));
        read((*th_data).soc, (*th_data).r_msg, sizeof((*th_data).r_msg));
        for (int i=0; i<64; i++){
            printf("%c", (*th_data).r_msg[i]);
        }
        //pthread_mutex_unlock(&mutex_r);
        //printf("odblokada dla odczytu!\n");
    }

    free(t_data);
    pthread_exit(NULL);
}

void *ThreadBehavior_Write(void *t_data) {

    pthread_detach(pthread_self());

    struct thread_data_t *th_data = (struct thread_data_t*)t_data;
    //dostęp do pól struktury: (*th_data).pole

    // wysyłanie wiadomości do klienta
    while(1) {
        //pthread_mutex_lock(&mutex_w);
        //printf("blokada dla zapisu!\n");
        memset((*th_data).w_msg, 0, sizeof((*th_data).w_msg));
        fgets((*th_data).w_msg, sizeof((*th_data).w_msg), stdin);

        for (int i=0; i<5; i++) {
            if (client_sockets[i] != 0) write(client_sockets[i], (*th_data).w_msg, sizeof((*th_data).w_msg));
        }
        //bylo: write((*th_data).soc, (*th_data).w_msg, sizeof((*th_data).w_msg));

        //pthread_mutex_unlock(&mutex_w);
        //printf("odblokada dla zapisu!\n");
    }

    free(t_data);
    pthread_exit(NULL);
}

void handleConnection(int connection_socket_descriptor, int licznik) {

    printf("New user! Took slot: %d\n", licznik);
    client_sockets[licznik] = connection_socket_descriptor;

    pthread_t thread1;
    pthread_t thread2;

    //struct thread_data_t t_data;
    
    struct thread_data_t *t_data1;
    struct thread_data_t *t_data2; 

    t_data1 = malloc(sizeof(struct thread_data_t));
    t_data2 = malloc(sizeof(struct thread_data_t));
    
    memset(&t_data1->soc, 0, sizeof(t_data1->soc));
    t_data1->soc = connection_socket_descriptor;
    memset(t_data1->r_msg, 0, sizeof(t_data1->r_msg));
    memset(t_data1->w_msg, 0, sizeof(t_data1->w_msg));

    memset(&t_data2->soc, 0, sizeof(t_data2->soc));
    t_data2->soc = connection_socket_descriptor;
    memset(t_data2->r_msg, 0, sizeof(t_data2->r_msg));
    memset(t_data2->w_msg, 0, sizeof(t_data2->w_msg));

    // Wątek odczytujący wiadomości od klienta
    int create_result = pthread_create(&thread1, NULL, ThreadBehavior_Read, (void *)t_data1);
    if (create_result) {
        printf("Błąd przy próbie utworzenia wątku, kod błędu: %d\n", create_result);
        exit(1);
    };

    // Wątek wysyłający wiadomości do wszystkich klientów
    create_result = pthread_create(&thread2, NULL, ThreadBehavior_Write, (void *)t_data2);
    if (create_result) {
        printf("Błąd przy próbie utworzenia wątku, kod błędu: %d\n", create_result);
        exit(1);
    };
}

int main(int argc, char*argv[]) {

    int port_num;
    int server_socket_descriptor;
    int connection_socket_descriptor;
    char reuse_addr_val = 1;
    struct sockaddr_in server_address;

    // przypisanie numeru portu z argv[1] - pierwszy argument
    if (argc < 2) {
        printf("Podaj numer portu\n");
        exit(1);
    } else {
        sscanf (argv[1],"%d",&port_num);
    }

    printf("cIRCa2019\n");

    //initializacja gniazda serwera
    memset(&server_address, 0, sizeof(struct sockaddr));
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htonl(INADDR_ANY);
    //server_address.sin_port = htons(SERVER_PORT);
    server_address.sin_port = htons(port_num);

    server_socket_descriptor = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket_descriptor < 0) {
        fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda..\n", argv[0]);
        exit(1);
    }
    setsockopt(server_socket_descriptor, SOL_SOCKET, SO_REUSEADDR, (char*)&reuse_addr_val, sizeof(reuse_addr_val));

    if (bind(server_socket_descriptor, (struct sockaddr*)&server_address, sizeof(struct sockaddr)) < 0){
        fprintf(stderr, "%s: Błąd przy próbie dowiązania adresu IP i numeru portu do gniazda.\n", argv[0]);
        exit(1);
    }

    if (listen(server_socket_descriptor, QUEUE_SIZE) < 0){
        fprintf(stderr, "%s: Błąd przy próbie ustawienia wielkości kolejki.\n", argv[0]);
        exit(1);
    }

    int licznik = 0;
    for (int i=0; i<5; i++) client_sockets[i] = 0;

    while(1) {
        connection_socket_descriptor = accept(server_socket_descriptor, NULL, NULL);
        if (connection_socket_descriptor < 0) {
            fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda dla połączenia.\n", argv[0]);
            exit(1);
        }
        
        handleConnection(connection_socket_descriptor, licznik);
        licznik = (licznik + 1)%5;

    }

    close(server_socket_descriptor);
    printf("whoopsie");
    return(0);

}