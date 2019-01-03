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
#define NUMBER_OF_CLIENTS 16
#define NUMBER_OF_CHATROOMS 16

pthread_mutex_t mutex_users = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mutex_chatrooms = PTHREAD_MUTEX_INITIALIZER;

//user ID is the user's place in the list
struct user_t {
    int socket;
    char name[64];
    //int chatrooms[64]; // lista pokoi w których znajduje
};

struct chatroom_t {
    int id;
    char name[64];
    int users[64];
    char messages[64][MESSAGE_LENGTH];
};

struct thread_data_t {
    int user_counter; //same as user ID
    int soc;
    char incoming_message[MESSAGE_LENGTH];
    int bytes_read;
};

struct user_t users[NUMBER_OF_CLIENTS];
struct chatroom_t chatrooms[NUMBER_OF_CHATROOMS];

char messages[64][MESSAGE_LENGTH];
char incoming_message[MESSAGE_LENGTH];

// zwraca ID chatroomu o podanej nazwie
int getChatroomID(char name[64]){
    int exists;
    for (int i=0; i<NUMBER_OF_CHATROOMS; i++){
        exists = i;
        for (int j=0; j<64; j++) {
            if (name[j] != chatrooms[i].name[j]) exists = -1;
        }
        if (exists > -1) return exists;
    }
    return exists;
}

int getFristFreeSlotInChatroom(int chatroom_id) { for (int i=0; i<64; i++) if (chatrooms[chatroom_id].users[i] == -1) return i; }

int findUserInChatroom(int user_id, int chatroom_id) {
    for (int i=0; i<64; i++) if (chatrooms[chatroom_id].users[i] == user_id) return i;
    else return 0;
}

// jeśli pokój nie istnieje, jego ID wynosi -1
int getFirstFreeChatroomID() {
    for (int i=0; i<NUMBER_OF_CHATROOMS; i++) {
        if (chatrooms[i].id == -1) return i;
    }
    return -1;
}

//sends the given string to everyone connected
void Broadcast_Message(char message[MESSAGE_LENGTH]) {
    for (int i=0; i<NUMBER_OF_CLIENTS; i++) {
        if (users[i].socket != 0) {
            //write(users[i].socket, message, MESSAGE_LENGTH);
            write(users[i].socket, users[0].name, 64);
        }
    }
}

void *Thread_Listening(void *t_data) {

    // odbierz wiadomość od tego użytkownika i przekaż ją wszystkim

    pthread_detach(pthread_self());
    struct thread_data_t *th_data = (struct thread_data_t*)t_data;
    // dostęp: (*th_data).pole

    int i, j;
    char c;
    char roomname[64];

    while(1) {

        memset((*th_data).incoming_message, 0, sizeof((*th_data).incoming_message));
        memset(roomname, 0, sizeof(roomname));

        //odczytanie wiadomości
        (*th_data).bytes_read = read((*th_data).soc, (*th_data).incoming_message, sizeof((*th_data).incoming_message));
        if ((*th_data).bytes_read > 0) {
            if ((*th_data).incoming_message[0] == '#') switch ((*th_data).incoming_message[1]) {
                
                case '0': // zmiana nazwy użytkownika
                    for (i=3; i<(*th_data).bytes_read; i++){
                        c = (*th_data).incoming_message[i];
                        if (c != '$') users[(*th_data).user_counter].name[i-3] = c;
                        else break; //TODO: nadaj listę użytkowników
                    }
                    break;

                case '1': // utworzenie chatroomu
                    for (i=3; i<(*th_data).bytes_read; i++) {
                        c = (*th_data).incoming_message[i];
                        if (c != '$') roomname[i] = c;
                        else break;
                    }
                    if (getChatroomID(roomname) == -1) {
                        //stwórz chatroom
                        i = getFirstFreeChatroomID();
                        chatrooms[i].id = i;
                        for (j=0; j<64; j++) chatrooms[i].name[j] = roomname[j];
                        chatrooms[i].users[0] = (*th_data).user_counter;
                    }
                    break;
                
                case '2': // dołączenie do chatroomu
                    for (i=3; i<(*th_data).bytes_read; i++) {
                        c = (*th_data).incoming_message[i];
                        if (c != '$') roomname[i] = c;
                        else break;
                    }
                    i = getChatroomID(roomname);
                    if (i > -1 && findUserInChatroom((*th_data).user_counter, i) == -1) { // pokój istnieje i nie ma w nim użytkownika
                        chatrooms[i].users[getFristFreeSlotInChatroom(i)] = (*th_data).user_counter;
                    }
                    break;

                case '3': // opuszczenie chatroomu
                    for (i=3; i<(*th_data).bytes_read; i++) {
                        c = (*th_data).incoming_message[i];
                        if (c != '$') roomname[i] = c;
                        else break;
                    }
                    i = getChatroomID(roomname);
                    j = findUserInChatroom((*th_data).user_counter, i);
                    if (i > -1 && j > -1) { // pokój istnieje i jest w nim użytkownik
                        chatrooms[i].users[j] = -1;
                    }
                    
                    break;

                case '4': // wysłanie wiadomości do chatroomu
                    break;
            }
            else Broadcast_Message((*th_data).incoming_message);
        }
        else {
            free(t_data);
            pthread_exit(NULL);
        }

    }

    //free(t_data);
    //pthread_exit(NULL);
}

void handleConnection(int connection_socket_descriptor, int user_counter) {
    printf("New user! Took slot: %d\n", user_counter + 1);
    users[user_counter].socket = connection_socket_descriptor;

    pthread_t thread1;

    struct thread_data_t *t_data1;
    t_data1 = malloc(sizeof(struct thread_data_t));

    t_data1->soc = connection_socket_descriptor;
    t_data1->user_counter = user_counter;

    // wątek będzie nasłuchiwał komunikatów od klienta
    if (pthread_create(&thread1, NULL, Thread_Listening, (void *)t_data1)) {
        printf("Błąd przy próbie utworzenia wątku\n");
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

    int i, user_counter = 0;

    //zerowanie struktur
    for (i=0; i<NUMBER_OF_CLIENTS; i++){
        users[i].socket = 0;
        memset(users[i].name, 0, sizeof(users[i].name));
    }
    for (i=0; i<NUMBER_OF_CHATROOMS; i++){
        chatrooms[i].id = -1;
        memset(chatrooms[i].name, 0, sizeof(chatrooms[i].name));
        memset(chatrooms[i].users, -1, sizeof(chatrooms[i].users));
        memset(chatrooms[i].messages, 0, sizeof(chatrooms[i].messages[0][0] * 64 * MESSAGE_LENGTH));
    }

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