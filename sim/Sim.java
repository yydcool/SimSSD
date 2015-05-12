package sim;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.TreeMap;

import util.Util;
import util.Log;
import util.Zipf;

public class Sim {
	private static final boolean doTest = true;
	static int nBlocks=100;
	static int BlockCapacity=64;
	static int BlockLifetime=1000; //lifetime is the num of times that a block can be erase
	//static int utilization=2400;
	public static double time;
	static PriorityQueue<Event> eventQueue;
	static int accessPattern =7; //0-exponential, 1-pareto, 2-constant+exp, 3-uniform, 4-exp500*4+exp8000, 5-pareto2, 6-0+6400+12800 
	static int requestId=0;
	//static int ssdGcPolicy = 1; //0-random, 1-greedy, 2-Dchoice, 3-greedySwap
	//static Zipf zipf;
	static int fileSizeMean=1+0;
	public static void init(){
		
		
	}
	
	public static void main(String[] args){
		init();
		Log.init();
		time=0;
		
		ArrayList<Double> startTime=new ArrayList<Double>();
		ArrayList<Double> endTime=new ArrayList<Double>();
		ArrayList<Integer> fileSize=new ArrayList<>();
		double totFsz=0;
		while (true){
			time=time+arrivalInterval();
			//System.out.print("time is "+time);
			if (time>6400000)
				break;	
			requestId++;
			double depatureTime=time+lifteInterval();
			double length=depatureTime-time;
			if(0<=length && length<1e99){
				int fsz=fileSizeDist();
				totFsz+=fsz;
				for (int i = 0; i < fsz; i++) {
					Log.logRequests(time+"\t"+depatureTime+"\n");
					startTime.add(time);
					endTime.add(depatureTime);
				}
				
				//fileSize.add();
			};
		}
		System.out.println("arrival rate is "+totFsz/time);
		Log.closeAll();
		if (doTest) {
			Greedy.readDatapath(startTime, endTime, fileSize);
			Greedy.offlineSchedule(nBlocks, BlockLifetime, BlockCapacity);
			System.out.println("test done");
		}
		
	}
	public static double arrival_interval=1*fileSizeMean;
	public static double lifetime_interval=5000;
	public static double paretoC=lifetime_interval/2; //xm
	public static double arrivalInterval() {
		return Util.randExp(arrival_interval);
	}
	public static int fileSizeDist(){
		return (int)(1+Util.rand(fileSizeMean*2-1));
	}
	public static double lifteInterval() {
		double t=0;
		if (accessPattern==0) {
				t=Util.randExp(lifetime_interval);
			return t;
		}
		if (accessPattern==1) {
			return Util.randPareto(2, paretoC);
		}
		if (accessPattern==2) {
			if (requestId<1200) 	
				return 1e10;
			else
				return Util.randExp(800);
		}
		if (accessPattern==3) {
			return Util.randUni(0.0, 2*lifetime_interval);
		}
		if (accessPattern==4) {
			if( Util.randUni(0, 1)<0.8 )
				return Util.randExp(lifetime_interval/4);
			else {
				return Util.randExp(lifetime_interval*4);
			}
		};
		if (accessPattern==5) {
			double paretoC=lifetime_interval*2/3;
			return Util.randPareto(3, paretoC);
		};
		if (accessPattern==6) {
			double p=lifetime_interval*2/(BlockCapacity*nBlocks*3);
			if (Util.randUni(0.0, 1.0)>p)
				return 0;
			else{
				if (Util.randUni(0.0, 1.0)>0.5) return 6400.0;
				else return 12800.0;
			}
		};
		if (accessPattern==7) {
			double p=lifetime_interval/(6500.0);
			if (Util.randUni(0.0, 1.0)>p)
				return 0;
			else{
				return 6500.0;
			}
		};
		return 0;
		
	}
}
