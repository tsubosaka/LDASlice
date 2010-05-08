package lda;

public class DocTable {
  int did;
  int index[];   
  int rindex[]; // rindex[ index[i] ] = i
  int count[];  // count[i] <= count[i + 1]
  public DocTable(int d , int T) {
    did   = d;
    index = new int[T];
    rindex = new int[T];
    count = new int[T];
    for(int i = 0 ; i < index.length ; ++i){
      index[i] = i;
      rindex[i] = i;
    }
  }
  
  void swap(int i , int j , int array[]){
    int tmp = array[i];
    array[i] = array[j];
    array[j] = tmp;
  }
  
  void incr(int w){
    int wi = index[w];
    count[wi]++;
    if(wi == index.length - 1 || count[wi] <= count[wi + 1]){
      return ;
    }
    for(int j = wi + 1 ; j < count.length ; ++j){
      if(count[j - 1] > count[j]){
        swap(j - 1 , j , count);
        swap(rindex[j - 1] , rindex[j] , index);
        swap(j - 1 , j , rindex);
      }else{
        return ;
      }
    }
  }
  
  void decr(int w){
    int wi = index[w];
    count[wi]--;
    if(wi == 0 || count[wi - 1] <= count[wi]){        
      return ;
    }
    
    for(int i = wi - 1 ; i >= 0 ; --i){
      if(count[i] > count[i + 1]){
        swap(i + 1 , i , count);
        swap(rindex[i + 1] , rindex[i] , index);
        swap(i + 1 , i , rindex);
      }else{
        return ;
      }
    }
  }
}
