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

#define SERVER_PORT_TCP 1239
#define SERVER_PORT_UDP 1240
#define SERVER_PORT_UDP_TO_PORT 1241
#define QUEUE_SIZE 5

#define bufor_size 500
#define max_clients 50

//struktura zawierająca dane, które zostaną przekazane do wątku
struct thread_data_t
{
    int desk_klient;
    struct sockaddr_in addr;
    int id;
    char nick[20];
    char buff[10000];
};
int i,j;
int podlaczone[max_clients];///-2 off -1 on 0-49 rozmawia z kims ///podlaczone[10]=1 użytkowniki o id 10 rozmawia z użytkownikiem o id 1
char do_wyslania[bufor_size];
struct sockaddr_in server_address;
pthread_t watki[max_clients]; //watki
struct thread_data_t t_data[max_clients];//dane z watków
int udpSocket;
int numberOfClients = 0;
void setCON(char *tab,int id)
{
    memset(tab,0,sizeof(tab));
    tab[0] = 4;
    tab[1] = 'C';
    tab[2] = 'O';
    tab[3] = 'N';
    strcat (tab, t_data[id].nick);
}
void setDIS(char *tab,int id)
{
    memset(tab,0,sizeof(tab));
    tab[0] = 4;
    tab[1] = 'D';
    tab[2] = 'I';
    tab[3] = 'S';
    strcat (tab, t_data[id].nick);
}
void setCAL(char *tab,int id)
{
    memset(tab,0,sizeof(tab));
    tab[0] = 4;
    tab[1] = 'C';
    tab[2] = 'A';
    tab[3] = 'L';
    strcat (tab, t_data[id].nick);
}
void setROZ(char *tab,int id)
{
    memset(tab,0,sizeof(tab));
    tab[0] = 4;
    tab[1] = 'R';
    tab[2] = 'O';
    tab[3] = 'Z';
    strcat (tab, t_data[id].nick);
}
void* ThreadBehavior(void *args)
{
    struct thread_data_t *data = args;
    int nr;
    int id = data->id; //id klienta tego watku
    int odczytane = 0;
    int w_buforze = 0;
    int whole = 0;

    char buf[bufor_size]; //bufor na surowe dane z socket
    char mbuf[bufor_size]; //bufor na dane polaczone
    char tmp[2];

    pthread_detach(pthread_self());
    printf("Dołączył użytkownik o id: %d\n", id);
    numberOfClients++;
    podlaczone[id]=-1;

    for(i=0; i < bufor_size; i++)
        mbuf[i]=0;

    while(1)
    {
        for(i=0; i<bufor_size; i++)
            buf[i]=0;
        odczytane = read(t_data[id].desk_klient,buf,bufor_size*sizeof(char));
        printf("Otrzymano wiadomosc: %s\n", buf);
        if(odczytane==0)
        {
            setDIS(mbuf,id);
            for(i=0; i<max_clients; i++)
            {
                if(podlaczone[i]>-2 && i!=id)
                {
                    write(t_data[i].desk_klient,mbuf,sizeof(mbuf));
                }
            }
            printf("Użytkownik %d wyszedł\n",id);
            numberOfClients--;
            podlaczone[id]=-2;
            shutdown(t_data[id].desk_klient, 2);
            return NULL;
        }

        for(i=0; i<odczytane; i++)
        {
            if(buf[i] == '\n')
                whole = 1;
        }

        j=0;
        for(i = w_buforze; i<w_buforze+odczytane; i++)
        {
            mbuf[i] = buf[j];
            j++;
        }

        w_buforze+=odczytane;

        if(whole == 1)
        {
            if(mbuf[0]==4)
            {
                if(mbuf[1]=='P'&&mbuf[2]=='O'&&mbuf[3]=='L')//polacz
                {
                    char tmpbuff[20];
                    memcpy(tmpbuff, mbuf+4, 20 * sizeof(char));
                    for(int i=0; i<max_clients; i++)
                        {
                            if(strcmp(t_data[i].nick, tmpbuff) == 0)
                            {
                                nr=i;
                                break;
                            }
                        }
                    //nr = atoi(memcpy(tmp, mbuf+4, 2 * sizeof(char)));
                    if(podlaczone[nr]==-1)  //nie rozmawia to połącz
                    {
                        podlaczone[nr]=id;
                        podlaczone[id]=nr;
                        setCAL(mbuf,id);
                        write(t_data[nr].desk_klient,mbuf,sizeof(mbuf));

                    }
                }
                else if(mbuf[1]=='R'&&mbuf[2]=='O'&&mbuf[3]=='Z')//rozlacz
                {
                    nr =  atoi(memcpy(tmp, mbuf+4, 2 * sizeof(char)));
                    podlaczone[nr]=-1;
                    podlaczone[id]=-1;
                    setROZ(mbuf,id);
                    write(t_data[nr].desk_klient,mbuf,sizeof(mbuf));
                }
                else if(mbuf[1]=='N'&&mbuf[2]=='C'&&mbuf[3]=='K') {
                        memcpy(t_data[id].nick, mbuf+4, 20 * sizeof(char));
                    for(i=0; i<max_clients; i++)
                        {
                            if(podlaczone[i]>-2 && i!=id)
                            {
                                setCON(mbuf,id);
                                write(t_data[i].desk_klient,mbuf,sizeof(mbuf));
                                setCON(mbuf,i);
                                write(t_data[id].desk_klient,mbuf,sizeof(mbuf));
                            }
                        }
                }
            }
            else
            {
                if(podlaczone[id]>-1)
                    write(t_data[podlaczone[id]].desk_klient,mbuf,w_buforze*sizeof(char));
            }

            whole = 0;
            for(i=0; i < bufor_size; i++)
                mbuf[i]=0;
            w_buforze = 0;
        }
    }
    pthread_exit(NULL);
}

void* ThreadBehaviorUDP(void *args) {
    struct sockaddr_in address;
    int size;
    char buff[10000];
    pthread_detach(pthread_self());
    int serv_addr_size = sizeof(address);
    while(1) {
       if (numberOfClients > 1) {
            size = recvfrom(udpSocket, buff, 10000, 0, (struct sockaddr*)&address, &serv_addr_size);
            for(i=0; i<max_clients - 2; i++) {
                if(t_data[i].addr.sin_addr.s_addr == address.sin_addr.s_addr && size > 0) {
                    if(podlaczone[i]>=0) {
                        address.sin_family = AF_INET;
                        address.sin_addr.s_addr = t_data[podlaczone[i]].addr.sin_addr.s_addr;
                        address.sin_port = htons(SERVER_PORT_UDP_TO_PORT);
                        size = sendto(udpSocket, buff, 10000, 0, (struct sockaddr*)&address, sizeof(struct sockaddr));
                    }
                }
            }
       }
    }
}
void handleConnection(int connection_socket_descriptor, struct sockaddr_in addr)
{
    int create_result = 0;
    int id;

    for(i=0; i<max_clients; i++)
    {
        if(podlaczone[i]==-2)
        {
            id=i;
            break;
        }
    }
    podlaczone[id]=-1;
    t_data[id].desk_klient = connection_socket_descriptor;
    t_data[id].id = id;
    t_data[id].addr = addr;
    t_data[id].buff[0] = '\n';
    create_result = pthread_create(&watki[id], NULL, ThreadBehavior, &t_data[id]);
    if (create_result)
    {
        printf("Błąd przy próbie utworzenia wątku, kod błędu: %d\n", create_result);
        exit(-1);
    }
}

int main(int argc, char* argv[])
{
    for(i=0; i<max_clients; i++)
        podlaczone[i]=-2;

    int server_socket_descriptor;
    int connection_socket_descriptor;
    int bind_result;
    int listen_result;
    char reuse_addr_val = 1;
    struct sockaddr_in server_address;

    //inicjalizacja gniazda serwera

    memset(&server_address, 0, sizeof(struct sockaddr));
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htonl(INADDR_ANY);
    server_address.sin_port = htons(SERVER_PORT_TCP);

    server_socket_descriptor = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket_descriptor < 0)
    {
        fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda..\n", argv[0]);
        exit(1);
    }
    setsockopt(server_socket_descriptor, SOL_SOCKET, SO_REUSEADDR, (char*)&reuse_addr_val, sizeof(reuse_addr_val));

    bind_result = bind(server_socket_descriptor, (struct sockaddr*)&server_address, sizeof(struct sockaddr));
    if (bind_result < 0)
    {
        fprintf(stderr, "%s: Błąd przy próbie dowiązania adresu IP i numeru portu do gniazda.\n", argv[0]);
        exit(1);
    }

    listen_result = listen(server_socket_descriptor, QUEUE_SIZE);
    if (listen_result < 0)
    {
        fprintf(stderr, "%s: Błąd przy próbie ustawienia wielkości kolejki.\n", argv[0]);
        exit(1);
    }
    int bindResult;
    memset(&server_address, 0, sizeof(struct sockaddr));
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htonl(INADDR_ANY);
    server_address.sin_port = htons(SERVER_PORT_UDP);

    udpSocket = socket(AF_INET, SOCK_DGRAM, 0);
     if (udpSocket < 0)
    {
        printf("UDP Błąd przy próbie utworzenia gniazda..\n");
        exit(1);
    }
    bindResult = bind(udpSocket, (struct sockaddr*)&server_address, sizeof(struct sockaddr));

    int create_result = pthread_create(&watki[max_clients - 1], NULL, ThreadBehaviorUDP, &t_data[max_clients - 2]);

    if (create_result)
    {
        printf("Błąd przy próbie utworzenia wątku, kod błędu: %d\n", create_result);
        exit(-1);
    }
    while(1)
    {
        struct sockaddr_in addr;
        int address_len = sizeof(struct sockaddr);
        connection_socket_descriptor = accept(server_socket_descriptor, (struct sockaddr*)&addr, &address_len);
        if (connection_socket_descriptor < 0)
        {
            fprintf(stderr, "%s: Błąd przy próbie utworzenia gniazda dla połączenia.\n", argv[0]);
            exit(1);
        }

        handleConnection(connection_socket_descriptor, addr);
    }

    close(server_socket_descriptor);
    return(0);
}
