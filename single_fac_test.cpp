

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


int main(){
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
