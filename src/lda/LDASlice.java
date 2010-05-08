package lda;

import java.util.*;


public class LDASlice {
  int D; // number of document
  int K; // number of topic
  int W; // number of unique word
  int wordCount[][];
  int docCount[][];
  int topicCount[];
  DocTable ds[];
  // hyper parameter
  double alpha, beta;
  Token tokens[];
  double P[];
  // topic assignment
  int z[];
  Random rand;
  public LDASlice(int documentNum, int topicNum, int wordNum, List<Token> tlist,
      double alpha, double beta, int seed) {
    wordCount = new int[wordNum][topicNum];
    docCount = new int[documentNum][topicNum];
    topicCount = new int[topicNum];
    ds = new DocTable[documentNum];    
    for(int i = 0 ; i < ds.length ; ++i){
      ds[i] = new DocTable(i, topicNum);
    }
    D = documentNum;
    K = topicNum;
    W = wordNum;
    tokens = tlist.toArray(new Token[0]);
    z = new int[tokens.length];
    this.alpha = alpha;
    this.beta = beta;
    P = new double[K];
    agendaId = new int[K];
    agendaValue = new double[K];
    rand = new Random(seed);
    init();
  }

  private void init() {
    for (int i = 0; i < z.length; ++i) {
      Token t = tokens[i];
      int assign = rand.nextInt(K);
      incr(t, assign);     
      z[i] = assign;
    }
  }
  
  private int selectNextTopic(Token t) {    
    for (int k = 0; k < P.length; ++k) {
      P[k] = (docCount[t.docId][k] + alpha) * 
             (wordCount[t.wordId][k] + beta) / (topicCount[k] + W * beta);
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
  
  int[] agendaId;
  double[] agendaValue;
  private int selectNextTopicWithSlice(Token t , int current){    
    int index[] = ds[t.docId].rindex;
    int count[] = ds[t.docId].count;
    
    double u = rand.nextDouble() * (docCount[t.docId][current] + alpha);    
    // threshold
    int countThreshold = (int)(Math.ceil(Math.max(0,  u - alpha)));
    int agendaNum = 0;
    double sum = 0.0;
    for(int i = count.length - 1 ; i >= 0 ; --i){
      if(count[i] < countThreshold){
        break;
      }
      agendaId[agendaNum] = index[i];
      agendaValue[agendaNum] = (wordCount[t.wordId][index[i]] + beta) / (topicCount[index[i]] + W * beta);
      sum += agendaValue[agendaNum];
      ++agendaNum;
    }
    u = rand.nextDouble() * sum;
    for(int k = 0 ; k < agendaNum ; ++k){
      if(u < agendaValue[k]){
        return agendaId[k];
      }else{
        u -= agendaValue[k];
      }
    }
    return agendaId[agendaNum - 1];
  }

  private void decr(Token t , int topic){
    wordCount[t.wordId][topic]--;
    docCount[t.docId][topic]--;
    ds[t.docId].decr(topic);
    topicCount[topic]--;    
  }

  private void incr(Token t , int topic){
    wordCount[t.wordId][topic]++;
    docCount[t.docId][topic]++;
    ds[t.docId].incr(topic);
    topicCount[topic]++;    
  }

  private void resample(int tokenId , boolean useSlice) {
    Token t = tokens[tokenId];
    int assign = z[tokenId];
    // remove from current topic
    decr(t , assign);
    if(useSlice){
      assign = selectNextTopicWithSlice(t, assign);      
    }else{
      assign = selectNextTopic(t);
    }
    // assign new topic
    incr(t, assign);
    z[tokenId] = assign;
  }
  
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
    System.out.println("iter\tSampling speed(word/sec)\tPPL\tRejection Rate");
    for(int i = 0 ; i < iter ; ++i){
      shuffle(order);
      long tstart = System.currentTimeMillis();
      boolean useSlice = true; //i < 10 ? false : true;
      int rejectNum = 0;
      for (int tid : order) {
        int prev = z[tid];
        resample(tid , useSlice);
        if(z[tid] == prev){
          ++rejectNum;
        }
      }
      long tend = System.currentTimeMillis();
      long tpass = tend - tstart;
      double wps = order.length * 1.0 / tpass;
      double rrate = rejectNum * 1.0 / order.length;
      if(i % 10 == 0){
        System.out.printf("%d\t%f\t%f\t%f\n" , i , wps * 1000 , perplexity() , rrate);        
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