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

public class Online3 {
	//greedy + buffer
	static ArrayList<Double> start,end;
	static ArrayList<Integer> fileSize;
	static boolean[] type1;
	static void readDatapath(ArrayList<Double> s, ArrayList<Double> e, ArrayList<Integer> f){
		start=s;
		end=e;
		fileSize=f; //todo
	}
	static ArrayList<Double> invalidStatus=new ArrayList<>(); 
	
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
	
	static void offlineSchedule(int nBlocks, int lifetime, int blockSize){ //input requests have increasing arrival time
		//init
		Online3.blockSize=blockSize;
		Online3.nBlocks=nBlocks;
		Online3.lifetime=lifetime;
		invalidStatus=new ArrayList<>(nBlocks*blockSize);
		for (int i = 0; i < nBlocks*blockSize; i++) invalidStatus.add(-0.1);
		blocks.clear();
		for (int i = 0; i < nBlocks; i++) blocks.put(i, new Block(i, 0, 0));
		badBlocks.clear();
		windowQ.clear();
		int lastReqIndex=-1, roundnum=0, limit=0;
		LinkedList<Integer> buffer=new LinkedList<>();
		while(true){
			roundnum++;
			double cleanTime=start.get(lastReqIndex+1);
			int freeBlockSpace=0;
			PriorityQueue<Event> freeBlocks=new PriorityQueue<>();
			limit=blockSize+1;
			while(freeBlockSpace<1 && limit>1){ //this is greedy
				limit--;
				freeBlockSpace=0;
				freeBlocks.clear();
				for(Integer b:blocks.keySet()){
					int count=blockCountLessThan(cleanTime, b);
					if(count>=limit){
						freeBlockSpace+=count;
						freeBlocks.add(new Event(blocks.get(b).age, 0, b));
					}
				}
			}// get by greedy
			if (limit==1) {
				System.out.println("limit = 1");
			}
			if(freeBlockSpace>0){
				Event selectedB=freeBlocks.poll();
				freeBlocks.clear();
				freeBlocks.add(selectedB);
				freeBlockSpace=blockCountLessThan(cleanTime, selectedB.id);
				
				System.out.println("free space "+freeBlockSpace+" limit "+limit);
				PriorityQueue<Event> freeBlocks2=new PriorityQueue<>();	freeBlocks2.addAll(freeBlocks);
				
				LinkedList<Integer> writingSchedule=new LinkedList<>();
				
				int schedule[]=new int[freeBlockSpace];
				for (int j = 0; j < schedule.length; j++) schedule[j]=-1;
				
					
					int l=0, r=freeBlockSpace-1, flag=0;
					while(l<freeBlockSpace){
						lastReqIndex++;
						//int idealPos=CDF(end.get(i),freeBlockSpace)*freeBlockSpace;
						buffer.add(lastReqIndex);
						while(buffer.size()==bufferSize){
							if (buffer.size()==0) break;
							schedule[l++]=poolMin(buffer);
						}
					}
				
				
				for(int j=0;j<freeBlockSpace;j++)
					writingSchedule.add(schedule[j]);
				
				int b=freeBlocks.poll().id;
				int bb=blockCountLessThan(cleanTime, b); //the space left in b
				while(writingSchedule.size()>0){
					int req=writingSchedule.pollFirst();
					if(bb==0){
						b=freeBlocks.poll().id;
						bb=blockCountLessThan(cleanTime, b);
					}
					bb--;
					boolean succ=updateInvalidStatus(b, req, cleanTime);
					if (!succ) {
						System.out.println("error1");
					}
				}
				
				
				
				while (freeBlocks2.size()>0) {
					Event e=freeBlocks2.poll();
					blocks.get(e.id).age++;
					if(blocks.get(e.id).age==lifetime){
						blocks.remove(e.id);
					}
				}
			}
			if (blocks.size()==0 || freeBlockSpace==0){
				System.out.println("disk died at "+cleanTime+" write req "+lastReqIndex);
				break;
			}
		}
	}
	

	private static int poolMin(LinkedList<Integer> buffer) {
		double min=1e99;
		Integer mini=null;
		for(int i:buffer){
			if (min>end.get(i)){
				min=end.get(i);
				mini=i;
			}
		}
		buffer.remove(mini);
		return mini.intValue();
	}
	


	private static boolean updateInvalidStatus(int b, int i,
			double cleanTime) {
		LinkedList<Event> u=new LinkedList<>();
		u.add(new Event(end.get(i), 0, i));
		return updateInvalidStatus(b,u,cleanTime);
	}


	private static int blockCountLessThan(double eraseTime, int frontier) {
		int count=0;
		for (int i = frontier*blockSize; i < (frontier+1)*blockSize; i++) {
			if (invalidStatus.get(i)<eraseTime) 
				count++;
		}
		return count;
	}
	
	private static boolean updateInvalidStatus(int frontier, LinkedList<Event> u, double clearTime) { // only use e.id in U
		if(u.size()==0) return true;
		PriorityQueue<Event> arrivals=new PriorityQueue<>();
		for (Event e:u)
			arrivals.add(new Event(start.get(e.id), 0, e.id));
		//u=(LinkedList<Event>) u.clone();
		for (int i = frontier*blockSize; i < (frontier+1)*blockSize; i++) {
			if (invalidStatus.get(i)<clearTime ){ //&& invalidStatus.get(i)<start.get(u.peek().id)  no need this since request store in buffer at first
				int id=arrivals.poll().id;
				invalidStatus.set(i, end.get(id));
				if(arrivals.size()==0) break;
			}
		}
		if (arrivals.size()==0)
			return true;
		else return false;
	}

}