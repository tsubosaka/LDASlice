package lda;

import java.util.*;


public class LDANormal {
  int D; // number of document
  int K; // number of topic
  int W; // number of unique word
  int wordCount[][];
  int docCount[][];
  int topicCount[];
  // hyper parameter
  double alpha, beta;
  Token tokens[];
  double P[];
  // topic assignment
  int z[];
  Random rand;
  public LDANormal(int documentNum, int topicNum, int wordNum, List<Token> tlist,
      double alpha, double beta, int seed) {
    wordCount = new int[wordNum][topicNum];
    topicCount = new int[topicNum];
    docCount = new int[documentNum][topicNum];
    D = documentNum;
    K = topicNum;
    W = wordNum;
    tokens = tlist.toArray(new Token[0]);
    z = new int[tokens.length];
    this.alpha = alpha;
    this.beta = beta;
    P = new double[K];
    rand = new Random(seed);
    init();
  }

  private void init() {
    for (int i = 0; i < z.length; ++i) {
      Token t = tokens[i];
      int assign = rand.nextInt(K);
      wordCount[t.wordId][assign]++;
      docCount[t.docId][assign]++;
      topicCount[assign]++;
      z[i] = assign;
    }
  }
  
  private int selectNextTopic(Token t) {
    for (int k = 0; k < P.length; ++k) {
      P[k] = (wordCount[t.wordId][k] + beta) * (docCount[t.docId][k] + alpha)
          / (topicCount[k] + W * beta);
      if (k != 0) {
        P[k] += P[k - 1];
      }
    }
    double u = rand.nextDouble() * P[K - 1];
    for (int k = 0; k < P.length; ++k) {
      if (u < P[k]) {
        return k;
      }
    }
    return K - 1;
  }
  private void decr(Token t , int topic){
    wordCount[t.wordId][topic]--;
    docCount[t.docId][topic]--;
    topicCount[topic]--;    
  }

  private void incr(Token t , int topic){
    wordCount[t.wordId][topic]++;
    docCount[t.docId][topic]++;
    topicCount[topic]++;    
  }

  private void resample(int tokenId) {
    Token t = tokens[tokenId];
    int assign = z[tokenId];
    // remove from current topic
    decr(t , assign);
    assign = selectNextTopic(t);
    // assign new topic
    incr(t, assign);
    z[tokenId] = assign;
  }

//  private void resample(int tokenId) {
//    Token t = tokens[tokenId];
//    int assign = z[tokenId];
//    // remove from current topic
//    wordCount[t.wordId][assign]--;
//    docCount[t.docId][assign]--;
//    topicCount[assign]--;
//    assign = selectNextTopic(t);
//    // assign new topic
//    wordCount[t.wordId][assign]++;
//    docCount[t.docId][assign]++;
//    topicCount[assign]++;
//    z[tokenId] = assign;
//  }
  
  private void shuffle(int array[]){
    for(int i = 0 ; i < array.length ; ++i){
      int j = rand.nextInt(array.length - i) + i;
      int t = array[j];
      array[j] = array[i];
      array[i] = t;
    }
  }
  
  public void update(int iter){
    int order[] = new int[tokens.length];    
    for(int i = 0 ; i < order.length ; ++i){
      order[i] = i;
    }
    System.out.println("iter\tSampling speed(word/sec)\tPPL");
    for(int i = 0 ; i < iter ; ++i){
      shuffle(order);
      long tstart = System.currentTimeMillis();
      for (int tid : order) {
        resample(tid);
      }
      long tend = System.currentTimeMillis();
      long tpass = tend - tstart;
      double wps = order.length * 1.0 / tpass;
      if(i % 10 == 0){
        System.out.printf("%d\t%f\t%f\n" , i , wps * 1000 , perplexity());        
      }
    }
  }

  public double[][] getTheta() {
    double theta[][] = new double[D][K];
    for (int i = 0; i < D; ++i) {
      double sum = 0.0;
      for (int j = 0; j < K; ++j) {
        theta[i][j] = alpha + docCount[i][j];
        sum += theta[i][j];
      }
      // normalize
      double sinv = 1.0 / sum;
      for (int j = 0; j < K; ++j) {
        theta[i][j] *= sinv;
      }
    }
    return theta;
  }

  public double[][] getPhi() {
    double phi[][] = new double[K][W];
    for (int i = 0; i < K; ++i) {
      double sum = 0.0;
      for (int j = 0; j < W; ++j) {
        phi[i][j] = beta + wordCount[j][i];
        sum += phi[i][j];
      }
      // normalize
      double sinv = 1.0 / sum;
      for (int j = 0; j < W; ++j) {
        phi[i][j] *= sinv;
      }
    }
    return phi;
  }
  
  double perplexity(){
    double theta[][] = getTheta();
    double phi[][]   = getPhi();
    double l = 0.0;
    for(int i = 0 ; i < tokens.length; ++i){
      Token t = tokens[i];
      double s = 0.0;
      for(int k = 0 ; k < K ; ++k){
        s += theta[t.docId][k] * phi[k][t.wordId];
      }
      l += Math.log(s);
    }
    return Math.exp(- l / tokens.length);
  }
}