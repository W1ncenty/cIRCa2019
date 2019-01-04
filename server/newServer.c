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
#define NUMBER_OF_MESSAGES 64

pthread_mutex_t mutex_users = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mutex_chatrooms = PTHREAD_MUTEX_INITIALIZER;

//user ID is the user's place in the list
struct user_t {
    int socket;
    char name[64];
    int chatrooms[NUMBER_OF_CHATROOMS]; // lista pokoi w których znajduje
};

struct chatroom_t {
    int id;
    char name[64];
    int users[64];
    char messages[NUMBER_OF_MESSAGES][MESSAGE_LENGTH];
};

struct thread_data_t {
    int socket;
    int user_counter; //same as user ID
    char incoming_message[1024];
    int bytes_read;
};

struct user_t users[NUMBER_OF_CLIENTS];
struct chatroom_t chatrooms[NUMBER_OF_CHATROOMS];

char incoming_message[MESSAGE_LENGTH];

void deleteUser(int user_id) {
    int i = user_id;
    users[i].socket = -1;
    memset(users[i].name, 0, sizeof(users[i].name));
    memset(users[i].chatrooms, -1, sizeof(users[i].chatrooms));
    for (i=0; i<NUMBER_OF_CHATROOMS; i++) for (int j=0; j<NUMBER_OF_CLIENTS; j++) if (chatrooms[i].users[j] == user_id) chatrooms[i].users[j] = -1;
}

void deleteChatroom(int chatroom_id){
    int i = chatroom_id;
    chatrooms[i].id = -1;
    memset(chatrooms[i].name, 0, sizeof(chatrooms[i].name));
    memset(chatrooms[i].users, -1, sizeof(chatrooms[i].users));
    memset(chatrooms[i].messages, 0, sizeof(chatrooms[i].messages[0][0] * 64 * MESSAGE_LENGTH));
    for (i=0; i<NUMBER_OF_CLIENTS; i++) for (int j=0; j<NUMBER_OF_CHATROOMS; j++) if (users[i].chatrooms[j] == chatroom_id) users[i].chatrooms[j] = -1;
}

int isChatroomEmpty(int chatroom_id) {
    for (int i=0; i<64; i++) if (chatrooms[chatroom_id].users[i] > -1) return 0;
    return 1;
}

// zwraca ID chatroomu o podanej nazwie
int getChatroomIDbyName(char name[64]){
    int exists;
    for (int i=0; i<NUMBER_OF_CHATROOMS; i++){
        exists = i;
        for (int j=0; j<64; j++) if (name[j] != chatrooms[i].name[j]) exists = -1;
        if (exists > -1) return exists;
    }
    return -1;
}

int getFristFreeSlotInChatroom(int chatroom_id) {
    for (int i=0; i<64; i++) if (chatrooms[chatroom_id].users[i] == -1) return i;
    return -1;
}

int getFirstFreeUserSlot() {
    for (int i=0; i<NUMBER_OF_CLIENTS; i++) if (users[i].socket == -1) return i;
    return -1;
}

int findUserInChatroom(int user_id, int chatroom_id) {
    for (int i=0; i<64; i++) if (chatrooms[chatroom_id].users[i] == user_id) return i;
    else return -1;
}

void joinChatroom(int user_id, int chatroom_id) {
    if (findUserInChatroom(user_id, chatroom_id) == -1) {
        if(getFristFreeSlotInChatroom(chatroom_id) != -1) {
            chatrooms[chatroom_id].users[getFristFreeSlotInChatroom(chatroom_id)] = user_id;
        }
    }
    int x = 1;
    for (int i=0; i<NUMBER_OF_CHATROOMS; i++) if (users[user_id].chatrooms[i] == chatroom_id) x = 0;
    if (x) {
        for (int i=0; i<NUMBER_OF_CHATROOMS; i++) {
            if (users[user_id].chatrooms[i] == -1) {
                users[user_id].chatrooms[i] = chatroom_id;
                break;
            }
        }
    }
}

// jeśli pokój nie istnieje, jego ID wynosi -1
int getFirstFreeChatroomID() {
    for (int i=0; i<NUMBER_OF_CHATROOMS; i++) {
        if (chatrooms[i].id == -1) return i;
    }
    return -1;
}

void sendToChatroom(int chatroom_id, char message[MESSAGE_LENGTH], int msg_length){
    
    // check if there is space for a message
    int i, j, spot = -1;
    for (i=0; i<NUMBER_OF_MESSAGES; i++) {
        if (chatrooms[chatroom_id].messages[i][0] == 0) { spot = i; break; }
    }

    if (spot > -1) {
        for (i=0; i<msg_length; i++) chatrooms[chatroom_id].messages[spot][i] = message[i];
    } // if there's no space for a new message, forget the oldest (message[0]) and push back the others. Then post new message as message[64]
    
    else {
        for (i=0; i < NUMBER_OF_MESSAGES - 1; i++)
            for (j=0; j<MESSAGE_LENGTH; j++)
                chatrooms[chatroom_id].messages[i][j] = chatrooms[chatroom_id].messages[i+1][j];
        memset(chatrooms[chatroom_id].messages[NUMBER_OF_MESSAGES-1], 0, sizeof(chatrooms[chatroom_id].messages[NUMBER_OF_MESSAGES-1]));
        for (j=0; j<msg_length; j++) chatrooms[chatroom_id].messages[NUMBER_OF_MESSAGES-1][j] = message[j];
    }
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

    int i, j, k, l;
    char c;
    char helpful_string[MESSAGE_LENGTH];

    while(1) {

        memset((*th_data).incoming_message, 0, sizeof((*th_data).incoming_message));

        //odczytanie wiadomości
        (*th_data).bytes_read = read((*th_data).socket, (*th_data).incoming_message, sizeof((*th_data).incoming_message));
        
        if ((*th_data).bytes_read > 0) {
            
            if ((*th_data).incoming_message[0] == '#') {
                
                memset(helpful_string, 0, sizeof(helpful_string));
                switch ((*th_data).incoming_message[1]) { // TODO: zapisuj u użytkownika w których jest chatroomach // TODO: odpowiedzi do klienta
                                                          // TODO: #4 musi sprawdzać czy użytkownik jest w pokoju
                
                    case '0': // zmiana nazwy użytkownika
                        for (i=3; i<(*th_data).bytes_read; i++){
                            c = (*th_data).incoming_message[i];
                            if (c != '$') users[(*th_data).user_counter].name[i-3] = c;
                            else break;
                        }
                        break;

                    case '1': // utworzenie chatroomu
                        for (i=3; i<(*th_data).bytes_read; i++) {
                            c = (*th_data).incoming_message[i];
                            if (c != '$') helpful_string[i-3] = c;
                            else break;
                        }
                        if (getChatroomIDbyName(helpful_string) == -1) {
                            //stwórz chatroom
                            i = getFirstFreeChatroomID();
                            chatrooms[i].id = i;
                            for (j=0; j<64; j++) chatrooms[i].name[j] = helpful_string[j];

                            // zapisz użytkownika w chatroomie
                            joinChatroom((*th_data).user_counter, i);
                        }
                        break;
                    
                    case '2': // dołączenie do chatroomu
                        for (i=3; i<(*th_data).bytes_read; i++) {
                            c = (*th_data).incoming_message[i];
                            if (c != '$') helpful_string[i-3] = c;
                            else break;
                        }
                        i = getChatroomIDbyName(helpful_string);
                        if (i > -1 && findUserInChatroom((*th_data).user_counter, i) == -1) { // pokój istnieje i nie ma w nim użytkownika
                            joinChatroom((*th_data).user_counter, i);
                        }
                        break;

                    case '3': // opuszczenie chatroomu
                        for (i=3; i<(*th_data).bytes_read; i++) {
                            c = (*th_data).incoming_message[i];
                            if (c != '$') helpful_string[i-3] = c;
                            else break;
                        }
                        i = getChatroomIDbyName(helpful_string); // id chatroomu, z którego wychodzi użytkownik
                        j = findUserInChatroom((*th_data).user_counter, i); // slot zajmowany obecnie przez użytkownika w chatroomie
                        if (i > -1 && j > -1) chatrooms[i].users[j] = -1;

                        //wyczysc w tablicy uzytkownika informacje o chatroomie
                        for (k=0; k<NUMBER_OF_CHATROOMS; k++)
                            if (users[(*th_data).user_counter].chatrooms[k] == i) users[(*th_data).user_counter].chatrooms[k] = -1;
                        
                        if (isChatroomEmpty(i)) deleteChatroom(i);
                        break;

                    case '4': // wysłanie wiadomości do chatroomu
                        for (i=3; i<(*th_data).bytes_read; i++) {
                            c = (*th_data).incoming_message[i];
                            if (c != '%') helpful_string[i-3] = c;
                            else break;
                        }

                        j = i + 1; // od tego znaku zaczyna się wiadomość
                        i = getChatroomIDbyName(helpful_string); // id chatroomu
                        
                        // jeśli chatroom nie istnieje
                        if (i == -1) break;
                        
                        // jeśli użytkownika nie ma w pokoju
                        if (findUserInChatroom((*th_data).user_counter, i) == -1) break;

                        // wczytaj wiadomość do tablicy helpful_string
                        memset(helpful_string, 0, sizeof(helpful_string));
                        for (k = 0; k<MESSAGE_LENGTH; k++) {
                            c = (*th_data).incoming_message[j + k];
                            if (c != '$') helpful_string[k] = (*th_data).incoming_message[j + k];
                            else break;
                        }
                        sendToChatroom(i, helpful_string, k+1);
                        printf("%c", helpful_string[0]);
                        printf("Wysłano wiadomość o długości %d do chatroomu %d\n", k, i);
                        break;

                    case 'x':
                        printf("Users:\n");
                        for (i=0; i<getFirstFreeUserSlot(); i++) {
                            for (j=0; j<64; j++) printf("%c", users[i].name[j]);
                            printf("\n");
                        }
                        printf("Chatrooms:\n");
                        for (i=0; i<getFirstFreeChatroomID(); i++) {
                            for (j=0; j<64; j++) printf("%c", chatrooms[i].name[j]);
                            printf("\n");
                        }
                        printf("Messages:\n");
                        for (i=0; i<5; i++) {
                            for (j=0; j<MESSAGE_LENGTH/2; j++) printf("%c", chatrooms[0].messages[i][j]);
                            printf("\n");
                        }
                        break;
                }
            }
            // jeśli wiadomość nie jest jednym z komunikatów (nie zaczyna się od #)
            else Broadcast_Message((*th_data).incoming_message);
        }
        else {
            // jeśli użytkownik rozłączył się - wyczyść jego dane i zakończ wątek
            deleteUser((*th_data).user_counter);
            free(t_data);
            pthread_exit(NULL);
        }
    }
}

void handleConnection(int connection_socket_descriptor) {
    
    // znajdź wolne miejsce dla użytkownika. Jeśli nie istnieje, odrzuć połączenie
    int user_counter = getFirstFreeUserSlot();
    if (user_counter != -1) {
        
        users[user_counter].socket = connection_socket_descriptor;
        printf("New user! Took slot: %d\n", user_counter + 1);

        pthread_t thread1;

        struct thread_data_t *t_data1;
        t_data1 = malloc(sizeof(struct thread_data_t));

        t_data1->socket = connection_socket_descriptor;
        t_data1->user_counter = user_counter;

        // wątek będzie nasłuchiwał komunikatów od klienta
        if (pthread_create(&thread1, NULL, Thread_Listening, (void *)t_data1)) {
            printf("Błąd przy próbie utworzenia wątku\n");
            exit(1);
        };

    } else close(connection_socket_descriptor);

}

int main(int argc, char*argv[]) {

    int port_num;
    int server_socket_descriptor;
    int connection_socket_descriptor;
    char reuse_addr_val = 1;
    struct sockaddr_in server_address;
    
    int i, msg_num = 0;

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

    //zerowanie struktur
    for (i=0; i<NUMBER_OF_CLIENTS; i++) deleteUser(i);
    for (i=0; i<NUMBER_OF_CHATROOMS; i++) deleteChatroom(i);

    while(1) {
        connection_socket_descriptor = accept(server_socket_descriptor, NULL, NULL);
        if (connection_socket_descriptor < 0) {
            fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda dla połączenia.\n", argv[0]);
            exit(1);
        }
        
        handleConnection(connection_socket_descriptor);

    }

    close(server_socket_descriptor);
    return(0);

}