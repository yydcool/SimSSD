package sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import ssd.Block;
import util.Util;

public class Greedy {
	//minimize valid expectation for the sum of this and next erase
	static ArrayList<Double> start,end;
	static ArrayList<Integer> fileSize;
	static boolean[] type1;
	static void readDatapath(ArrayList<Double> s, ArrayList<Double> e, ArrayList<Integer> f){
		start=s;
		end=e;
		fileSize=f; //todo
	}
	 
	
	private static double max(ArrayList<Double> array, int a, int b) {
		double m=-1e99;
		for(int i=a;i<b;i++)
			m=m<array.get(i)?array.get(i):m;
		return m;
	}
	
	static int blockSize;
	static int lifetime;
	static int nBlocks;
	static int frontier=0;
	static int bufferSize=64;
	static PriorityQueue<Event> windowQ=new PriorityQueue<Event>();
	static HashMap<Integer, Block> blocks=new HashMap<>();
	static HashMap<Integer, Block> badBlocks=new HashMap<>();
	static ArrayList<Double> invalidTime=new ArrayList<>();
	static ArrayList<Double> validTime=new ArrayList<>();
	
	static void offlineSchedule(int nBlocks, int lifetime, int blockSize){ //input requests have increasing arrival time
		//init
		Greedy.blockSize=blockSize;
		Greedy.nBlocks=nBlocks;
		Greedy.lifetime=lifetime;
		invalidTime=new ArrayList<>(nBlocks*blockSize);
		for (int i = 0; i < nBlocks*blockSize; i++) invalidTime.add(-0.1);
		validTime=new ArrayList<>(nBlocks*blockSize);
		for (int i = 0; i < nBlocks*blockSize; i++) validTime.add(-0.1);
		blocks.clear();
		for (int i = 0; i < nBlocks; i++) blocks.put(i, new Block(i, 0, 0));
		badBlocks.clear();
		//windowQ.clear();
		int lastReqIndex=-1, roundnum=0;
		
		while(true){
			roundnum++;
			double cleanTime=start.get(lastReqIndex+1);
			double min_mean=blockSize+1;
			for(Integer b:blocks.keySet()){
				double f=blockSize-blockCountLessThan(cleanTime, b);

				if (min_mean>f || (min_mean==f && blocks.get(b).age<blocks.get(frontier).age)){
					min_mean=f;
					frontier=b;
				}
			}
			
			
			int freeBlockSpace=blockCountLessThan(cleanTime, frontier);
			System.out.println("free space "+freeBlockSpace+" maxmean "+min_mean+" cleantime "+cleanTime+" at round"+roundnum);
			for (int j = frontier*blockSize; j < (frontier+1)*blockSize; j++){
				if (invalidTime.get(j)<cleanTime){
					lastReqIndex++;
					invalidTime.set(j, end.get(lastReqIndex));
					validTime.set(j, start.get(lastReqIndex));
				}
			}//update page info
			
				
			blocks.get(frontier).age++;
			if(blocks.get(frontier).age==lifetime){
				blocks.remove(frontier);
			}
			
			if (blocks.size()==0 || freeBlockSpace==0){
				System.out.println("disk died at "+cleanTime+" write req "+lastReqIndex);
				break;
			}
		}
	}


	private static int blockCountLessThan(double eraseTime, int frontier) {
		int count=0;
		for (int i = frontier*blockSize; i < (frontier+1)*blockSize; i++) {
			if (invalidTime.get(i)<eraseTime) 
				count++;
		}
		return count;
	}
	


}
