import java.io.File;

import java.util.*;

import lda.*;

class PComp implements Comparable<PComp> {
  int id;
  double prob;

  @Override
  public int compareTo(PComp o) {
    return Double.compare(prob, o.prob);
  }
}

public class NIPS {
  public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(new File("data/docword.nips.txt"));
    int D = sc.nextInt();
    int W = sc.nextInt();
    int N = sc.nextInt();
    List<Token> tlist = new ArrayList<Token>();
    for (int i = 0; i < N; ++i) {
      int did = sc.nextInt() - 1;
      int wid = sc.nextInt() - 1;
      int count = sc.nextInt();
      for (int c = 0; c < count; ++c) {
        tlist.add(new Token(did, wid));
      }
    }
    
    String words[] = new String[W];
    sc = new Scanner(new File("data/vocab.nips.txt"));
    for (int i = 0; i < W; ++i) {
      words[i] = sc.nextLine();
    }
    int K = 100;
//    LDANormal lda = new LDANormal(D, K, W, tlist, .1 , .1 ,777);
    LDASlice lda = new LDASlice(D, K, W, tlist, .1 , .1 ,777);
    lda.update(1000);
  }
}
