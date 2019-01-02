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

//#define SERVER_PORT 1234
#define QUEUE_SIZE 5
#define MESSAGE_LENGTH 256
#define NUMBER_OF_CLIENTS 5

struct user_t {
    int id;
    char name[64];
    int chatrooms[64]; // lista pokoi w których znajduje
};

struct chatroom_t {
    int id;
    char name[64];
    int users[64];
    char messages[64][512];
};

struct thread_data_t {
    int licznik;
    int soc;
    char incoming_message[MESSAGE_LENGTH];
};

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

int client_sockets[NUMBER_OF_CLIENTS];
char messages[64][MESSAGE_LENGTH];
char incoming_message[MESSAGE_LENGTH];

//sends the given string to everyone connected
void Broadcast_Message(char message[MESSAGE_LENGTH]) {
    for (int i=0; i<NUMBER_OF_CLIENTS; i++) {
        if (client_sockets[i] != 0) {
            write(client_sockets[i], message, MESSAGE_LENGTH);
        }
    }
}

void *ThreadBehavior_ReadAndPass(void *t_data) {

    // odbierz wiadomość od tego użytkownika i przekaż ją wszystkim

    pthread_detach(pthread_self());
    struct thread_data_t *th_data = (struct thread_data_t*)t_data;
    // dostęp: (*th_data).pole

    while(1) {
        memset((*th_data).incoming_message, 0, sizeof((*th_data).incoming_message));
        
        //odczytanie wiadomości
        read((*th_data).soc, (*th_data).incoming_message, sizeof((*th_data).incoming_message));
        Broadcast_Message((*th_data).incoming_message);
    }

    free(t_data);
    pthread_exit(NULL);
}

void handleConnection(int connection_socket_descriptor, int user_counter) {
    printf("New user! Took slot: %d\n", user_counter + 1);
    client_sockets[user_counter] = connection_socket_descriptor;

    pthread_t thread1;

    struct thread_data_t *t_data1;
    t_data1 = malloc(sizeof(struct thread_data_t));

    t_data1->soc = connection_socket_descriptor;
    t_data1->licznik = user_counter;

    int create_result = pthread_create(&thread1, NULL, ThreadBehavior_ReadAndPass, (void *)t_data1);
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
    
    int msg_num = 0;

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

    int user_counter = 0;
    memset(client_sockets, 0, sizeof(client_sockets));

    while(1) {
        connection_socket_descriptor = accept(server_socket_descriptor, NULL, NULL);
        if (connection_socket_descriptor < 0) {
            fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda dla połączenia.\n", argv[0]);
            exit(1);
        }
        
        handleConnection(connection_socket_descriptor, user_counter);
        user_counter = (user_counter + 1)%NUMBER_OF_CLIENTS;

    }

    close(server_socket_descriptor);
    return(0);

}