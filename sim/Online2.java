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

public class Online2 {
	//using CDF
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
	static int bufferSize=32;
	static PriorityQueue<Event> windowQ=new PriorityQueue<Event>();
	static HashMap<Integer, Block> blocks=new HashMap<>();
	static HashMap<Integer, Block> badBlocks=new HashMap<>();
	
	static void offlineSchedule(int nBlocks, int lifetime, int blockSize){ //input requests have increasing arrival time
		//init
		Online2.blockSize=blockSize;
		Online2.nBlocks=nBlocks;
		Online2.lifetime=lifetime;
		invalidStatus=new ArrayList<>(nBlocks*blockSize);
		for (int i = 0; i < nBlocks*blockSize; i++) invalidStatus.add(-0.1);
		blocks.clear();
		for (int i = 0; i < nBlocks; i++) blocks.put(i, new Block(i, 0, 0));
		badBlocks.clear();
		windowQ.clear();
		int lastReqIndex=-1, roundnum=0, limit=0;
		while(true){
			roundnum++;
			double cleanTime=start.get(lastReqIndex+1);
			int freeBlockSpace=0;
			PriorityQueue<Event> freeBlocks=new PriorityQueue<>();
			limit=blockSize+1;
			while(freeBlockSpace<300 && limit>1){
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
			}// until we get at least freeBlockSpace>=300
			if (limit==1) {
				System.out.println("limit = 1");
			}
			if(freeBlockSpace>0){
				System.out.println("free space "+freeBlockSpace+" limit "+limit);
				PriorityQueue<Event> freeBlocks2=new PriorityQueue<>();	freeBlocks2.addAll(freeBlocks);
				
				LinkedList<Integer> writingSchedule=new LinkedList<>();
				
				int schedule[]=new int[freeBlockSpace];
				for (int j = 0; j < schedule.length; j++) schedule[j]=-1;
				int method=1; //1- CDF, 2- buffer min max, 3- buffer sample 
				if(1==method){
					for(int j=0;j<freeBlockSpace;j++){
						lastReqIndex++;
						double length=end.get(lastReqIndex)-start.get(lastReqIndex);
						double base=1.5;
						double logBase=Math.log(base);
						double roundLen=Math.pow(base, Math.ceil(Math.log(length)/logBase));
						double endtime=start.get(lastReqIndex)+roundLen;
						//double endtime=end.get(lastReqIndex);
						int idealPos=(int)(CDF(cleanTime, endtime,freeBlockSpace)*(freeBlockSpace-1));
						int l=idealPos, r=idealPos;
						while(true){
							if(schedule[l]==-1){
								schedule[l]=lastReqIndex;
								break;
							}
							if(schedule[r]==-1){
								schedule[r]=lastReqIndex;
								break;
							}
							if(l>0)
								l--; 
							if(r<freeBlockSpace-1)
								r++;
						}
					}
				}
				if(2==method){
					LinkedList<Integer> buffer=new LinkedList<>();
					int l=0, r=freeBlockSpace-1, flag=0;
					for(int j=0;j<freeBlockSpace;j++){
						lastReqIndex++;
						//int idealPos=CDF(end.get(i),freeBlockSpace)*freeBlockSpace;
						buffer.add(lastReqIndex);
						while(buffer.size()==bufferSize || j==freeBlockSpace-1){
							if (buffer.size()==0) break;
							if(flag<1){
								schedule[l++]=poolMin(buffer);
								//flag++;
							}else{
								schedule[r--]=poolMax(buffer);
								flag=0;
							}
						}
					}
				}
				if(3==method){
					LinkedList<Integer> buffer=new LinkedList<>();
					for(int j=0;j<freeBlockSpace;j++){
						lastReqIndex++;
						buffer.add(lastReqIndex);
						while(buffer.size()==bufferSize || j==freeBlockSpace-1){
							
						}
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
	private static int poolMax(LinkedList<Integer> buffer) {
		double max=-1;
		Integer maxi=null;
		for(int i:buffer){
			if (max<end.get(i)){
				max=end.get(i);
				maxi=i;
			}
		}
		buffer.remove(maxi);
		return maxi.intValue();
	}

	private static double CDF(Double cleantime, Double time, int freeBlockSpace) {
		time=time-cleantime;
		double t=time-freeBlockSpace; //residual lifetime after full
		if(t<0)
			return 0.0;
		double cumulated=0;
		for(int i=0;i<freeBlockSpace;i++){
			//cumulated+=1/((double)freeBlockSpace)*paratoCDF(t+i); //pareto estimation
			cumulated+=1/((double)freeBlockSpace)*(1 - Math.pow(Math.E, -(t+i)/Sim.lifetime_interval)); //exponential estimation
		}
		return cumulated;
	}


	private static double paratoCDF(double x) {
		double xm=Sim.paretoC;
		if(x<=xm)
			return 0.0;
		return 1-(xm/x)*(xm/x);
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
			if (invalidStatus.get(i)<clearTime && invalidStatus.get(i)<start.get(u.peek().id)){
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