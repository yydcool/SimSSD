package util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
//Based on code by Hyunsik Choi
//http://diveintodata.org/2009/09/zipf-distribution-generator-in-java/
public class Zipf {
     private Random rnd = Util.rand;
     private int size;
     private double skew;
     private double bottom = 0;
     static int batchLen=300000;
     double t[]=new double[batchLen];
     int value[]=new int[batchLen];
     Map <Double, Integer> index = new HashMap<Double, Integer>();
     int point=-1;

     public Zipf(int size, double skew) {
             this.size = size;
             this.skew = skew;

             for (int i = 1; i <= size; i++) {
                     this.bottom += (1 / Math.pow(i, this.skew));
                 }
    }


    // the next() method returns an rank id. The frequency of returned rank ids
    // are follows Zipf distribution.
    public int nextInt() {
    	 if (point==-1) {
    		 index.clear();
    		 for (int i = 0; i < batchLen; i++) {
				t[i]=rnd.nextDouble();
				index.put(t[i], i);
    		 }
    		 Arrays.sort(t);
    		 int head=0;
    		 double cdf=0;
    		 for (int i = 1; i <= size; i++) {
				cdf+=getProbability(i);
				while(head<batchLen && cdf>t[head]){
					value[index.get(t[head])]=i;
					head++;
				}
    		 }
    		 point=0;
    		 index.clear();
    		 //System.out.println("batch gen");
			return nextInt();
		}else{
			int ret = value[point];
			point++;
			if (point==batchLen) {
				point=-1;
			}
			//System.out.println(ret);
			return ret-1;
		}
    	 /*
             int rank;
             double friquency = 0;
             double dice;

             rank = rnd.nextInt(size);
             friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
             dice = rnd.nextDouble();
             int i=0;
             while (!(dice < friquency)) {
            	 	i++;
                     rank = rnd.nextInt(size);
                     friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
                     dice = rnd.nextDouble();
             }
             System.out.println(i);
             return rank;
             */
     }

     // This method returns a probability that the given rank occurs.
     public double getProbability(int rank) {
             return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
     }
}