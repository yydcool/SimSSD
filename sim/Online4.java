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

public class Online4 {
	//max empty expectation for general page life distribution
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
		Online4.blockSize=blockSize;
		Online4.nBlocks=nBlocks;
		Online4.lifetime=lifetime;
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
			double max_mean=-1;
			frontier=-1;
			for(Integer b:blocks.keySet()){
				double mean=0;
				int freeCount=0;
				for (int j = b*blockSize; j < (b+1)*blockSize; j++){ 
					if (invalidTime.get(j)<cleanTime){
						mean+=90.5+cdf(6400-freeCount);
						freeCount++;
					}else{
						mean+=cdf_given(6400+cleanTime-validTime.get(j),cleanTime-validTime.get(j));
					}
				}
				if (max_mean<mean){
					max_mean=mean;
					frontier=b;
				}
			}// get by max empty expectation
			
			int freeBlockSpace=blockCountLessThan(cleanTime, frontier);
			System.out.println("free space "+freeBlockSpace+" maxmean "+max_mean+" cleantime "+cleanTime+" at round"+roundnum);
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
	

	private static double cdf_given(double x, double given) {
		return (cdf(x)-cdf(given))/(1-cdf(given));
	}

	private static double cdf(double x) {
		double ret=0;
		//paretoCdf(x);
		ret=CdfPattern6(x); // = uniformCdf(x);
		
		if (ret>1.0) return 1.0;
		if (ret<0.0) return 0.0;
		return ret;
	}

	private static double CdfPattern6(double x) {
		double p=Sim.lifetime_interval*2/(Sim.BlockCapacity*nBlocks*3);
		if(x<6400) return 1.0-p;
		if(x<12800) return 1.0-p/2;
		return 1.0;
	}
	
	private static double paretoCdf(double x) {
		return 1-(Sim.paretoC/x)*(Sim.paretoC/x);
	}
	
	private static double uniformCdf(double x) {
		return x/(2*Sim.lifetime_interval);
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
			if (invalidTime.get(i)<eraseTime) 
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
			if (invalidTime.get(i)<clearTime ){ //&& invalidStatus.get(i)<start.get(u.peek().id)  no need this since request store in buffer at first
				int id=arrivals.poll().id;
				invalidTime.set(i, end.get(id));
				if(arrivals.size()==0) break;
			}
		}
		if (arrivals.size()==0)
			return true;
		else return false;
	}

}
