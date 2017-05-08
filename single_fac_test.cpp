

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
extern "C"{
#include "fec_include/fec.h"
}

struct etab {
  int symsize;
  int genpoly;
  int fcs;
  int prim;
  int nroots;
  int ntrials;
} Tab = {10, 0x409,   1,   1, 512, 10 };


etab Tab2 = {3, 0xb,     1,   1, 2, 10};



int go(struct etab *e);

double currmili(){

    struct timeval start;

    double mtime, seconds, useconds;
    gettimeofday(&start, NULL);
    seconds  = start.tv_sec;
    useconds = start.tv_usec;

    mtime = ((seconds) * 1000.0 + useconds/1000.0) + 0.5;
    return mtime;
}







int go2(){

    int N = (1<<Tab2.symsize) - 1;
    int K = 3;
    int nroots = N - K;

    int* mem = new int[N];
    int* erasureloc = new int[N];

    memset(mem, 0, N);
    memset(erasureloc, 0, N);

    mem[0] = 3;
    mem[1] = 2;
    mem[2] = 7;
    //mem[3] = 0;
    //mem[4] = 6;
    //mem[5] = 5;
    //mem[6] = 4;

    for (int i = 0; i<N; i++)
        printf("(mem[%d]=%d), ", i, mem[i]);
    printf("\n");

    void* rs = init_rs_int(Tab2.symsize,Tab2.genpoly,Tab2.fcs,Tab2.prim,nroots,0);
    encode_rs_int(rs, mem, &mem[K]);

    for (int i = 0; i<N; i++)
        printf("(mem[%d]=%d), ", i, mem[i]);
    printf("\n");

    free_rs_int(rs);

    //make erasures
    mem[0]=0;
    mem[1]=0;
    mem[2]=0;
    mem[5]=0;

    //make info in the location array
    erasureloc[0]=0;
    erasureloc[1]=1;
    erasureloc[2]=2;
    erasureloc[3]=5;

    printf("Corrupted content : \n");

    //reprint
    for (int i = 0; i<N; i++)
        printf("(mem[%d]=%d), ", i, mem[i]);
    printf("\n");

    //recreate RS engine
    rs = init_rs_int(Tab2.symsize,Tab2.genpoly,Tab2.fcs,Tab2.prim,nroots,0);
    int nr = decode_rs_int(rs,mem,erasureloc,4/* num of erasures*/);

    printf("Info from dec %d\n", nr);

    //reprint
    for (int i = 0; i<N; i++)
        printf("(mem[%d]=%d), ", i, mem[i]);
    printf("\n");

    free_rs_int(rs);

    delete []erasureloc;
    delete []mem;
}




int main(){
    go2();
    exit(0);
  int i;

  srandom(time(NULL));

    int nn,kk;

    nn = (1<<Tab.symsize) - 1;
    kk = nn - Tab.nroots;
    printf("Testing (%d,%d) code...\n",nn,kk);

    go(&Tab);

  exit(0);
}

int go(struct etab *e){
    int nn = (1<<e->symsize) - 1;
      int block[nn],tblock[nn];
      int derrlocs[nn];
      int i;
      int errors;
      int derrors,kk;
      int errval,errloc;
      int erasures;
      int decoder_errors = 0;
      void *rs;

  /* Compute code parameters */
  kk = nn - e->nroots;

  rs = init_rs_int(e->symsize,e->genpoly,e->fcs,e->prim,e->nroots,0);
  if(rs == NULL){
    printf("init_rs_int failed!\n");
    return -1;
  }
  /* Test up to the error correction capacity of the code */
  errors = e->nroots/2-1;

    /* Load block with random data and encode */
    for(i=0;i<kk;i++)
      block[i] = random() & (nn);
    block[0]=nn;
    block[1]=0;

    printf("Content of the original block:\n");
    for(int i=0;i<nn;i++)
        printf(" %d", block[i]);
    printf("\n");





    memset(derrlocs,0, sizeof(derrlocs));


    double s = currmili();
    encode_rs_int(rs,block,&block[kk]);

    printf("Encoding done, ms %f, Content of the block after encode:\n",currmili()-s);
    for(int i=0;i<nn;i++)
        printf(" %d", block[i]);
    printf("\n");

    //encoding done
    free_rs_int(rs);

    //now start the decoding part
    rs = init_rs_int(e->symsize,e->genpoly,e->fcs,e->prim,e->nroots,0);

    //memcpy(tblock,block,sizeof(block));



    erasures = 0;
    /*derrlocs[erasures++] = 3;
    derrlocs[erasures++] = 7;
    derrlocs[erasures++] = 11;
    derrlocs[erasures++] = 15;
    derrlocs[erasures++] = 19;
    block[3] = 0;
    block[7] = 0;
    block[11] = 0;
    block[15] = 0;
    block[19] = 0;
*/

    //block[3] = 0;
    //block[7] = 0;
    //block[11] = 0;
    //block[15] = 0;
    //block[19] = 0;

    for(int i=0; i<nn; i+=1){
        if(i>nn/4 && i<nn/2)
        block[i]=0;
    }
    printf("Content of the erasure-corrupted block:\n");
    for(int i=0;i<nn;i++)
        printf(" %d", block[i]);
    printf("\n");



    //memcpy(tblock,block,sizeof(block));
    //memset(derrlocs,0,sizeof(derrlocs));


    /* Make temp copy, seed with errors */
    //memcpy(tblock,block,sizeof(block));



    /* Decode the errored block */
    double start = currmili();
    derrors = decode_rs_int(rs,block,derrlocs,0);

    printf("Decoding done, ms: %f, Content of the block after decode:\n", currmili() - start);
    for(int i=0;i<nn;i++)
        printf(" %d", block[i]);
    printf("\n");

    printf("Claimed number of errors %d\n",derrors);



  free_rs_int(rs);
  return 0;
}
