package ssd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import sim.Sim;
import util.Util;
public class SSD {
	int nBlocks;
	int BlockSize;
	int BlockLifetime;
	public int availableCapacity;
	Block frontier;
	ArrayList<Block> blocks; //available blocks
	ArrayList<Block> badBlocks; //unavailable blocks
	Hashtable<Integer, Block> mapping=new Hashtable<Integer, Block>();
	public int isWearing=0;
	public int gcAlgorithm=0;
	int Dchoice=2;
	public SSD(int nb,int bc,int bl, int gca){
		gcAlgorithm=gca;
		nBlocks=nb;
		BlockSize=bc;
		BlockLifetime=bl;
		availableCapacity=nBlocks*BlockSize;
		Block.size=BlockSize;
		Block.lifetime=BlockLifetime;
		blocks=new ArrayList<Block>();
		for (int i = 0; i < nBlocks; i++) {
			blocks.add(new Block(i,0,0));
		}
		frontier=blocks.get(0);
		badBlocks=new ArrayList<>();
	}
	public void debug() {
		int sum=0;
		for(Block b:blocks)
			sum+=b.valid;
		for(Block b:badBlocks)
			sum+=b.valid;
		System.out.println(" all valids "+sum);
	}
	public boolean invalidate(int pageId) {
		Block block=mapping.get(pageId);
		if (block==null){
			return false;
		}
		block.invalidate(pageId);
		mapping.remove(pageId);	
		if (block.age==BlockLifetime) {
			availableCapacity--;
		}
		return true;
	}
	
	public boolean write(int pageId){
		while (frontier.invalid+frontier.valid==BlockSize){
			frontier=gc();
			if (frontier==null) {
				return false;
			}
		}
		frontier.write(pageId);
		mapping.put(pageId, frontier);
		return true;
	}
	
	private void write(int page, Block b) {
		b.write(page);
		mapping.put(page, b);
	}

	private Block gc() {
		if(gcAlgorithm==0)
			return gcRandom();
		if(gcAlgorithm==1)
			return gcGreedy();
		if(gcAlgorithm==2)
			return gcDchoice();
		if(gcAlgorithm==3)
			return gcGreedySwap();
		return null;
	}

	private Block gcGreedySwap() {
		ArrayList<Block> candidates=greedyCandidates();
		if (candidates.size()==0) return null;
		Block greedy;
		greedy=candidates.get(Util.rand(candidates.size())); //random choose one
		if(greedy.valid+greedy.invalid<BlockSize) return greedy; //at the begining, no need to erase
		if (greedy.age==BlockLifetime-1) { //if greedy page is dieing, old
			Block block=null;
			int wear_min=BlockLifetime+1;
			for (Block b : blocks) {
				if (b.age<wear_min) {
					wear_min=b.age;
					block=b;
				}
			}
			assert block!=null;
			if(block==null || block.valid+block.invalid<BlockSize) return block;
			if (block.age>=BlockLifetime-2 ||block==greedy)	{//an old block too?
				erase(greedy);
				return greedy; 
			}
			HashSet<Integer> pages=reverseMapping(block);
			HashSet<Integer> greedyPages=reverseMapping(greedy);
			for (Integer page : pages){
				invalidate(page);
			}
			for (Integer page : greedyPages){
				invalidate(page);
			}
			erase(block);
			erase(greedy);
			for (Integer page : pages){
				write(page, greedy);
			}
			for (Integer page : greedyPages){
				if (greedy.valid<BlockSize)
					write(page, greedy);
				else 
					write(page, block);
			}
			if (greedy.valid<BlockSize)
				return greedy;
			else
				return block;
		}else{
			erase(greedy);
			return greedy;
		}
			
		/*
		Block greedy=gcGreedy();
		if (greedy==null) {
			return null;
		}
		if (greedy.age==BlockLifetime) { //if greedy page is dieing, old
			Block block=null;
			int wear_min=BlockLifetime+1;
			if (blocks.isEmpty()) return greedy;
			for (Block b : blocks) {
				if (b.age<wear_min) {
					wear_min=b.age;
					block=b;
				}
			}
			if (block==greedy) {
				
			}
			if (block.age>=BlockLifetime-2)
				return greedy;
			HashSet<Integer> pages=reverseMapping(block);
			HashSet<Integer> greedyPages=reverseMapping(greedy);
			
			for (Integer page : pages) { //move page in block to greedy
				invalidate(page);
				write(page,greedy);
			}
			
			boolean erased=false;
			for (Integer page : greedyPages) {
				if (greedy.valid<=BlockSize) break;
				if (erased==false) {
					erased=true;
					block.erase(isWearing);
				}
				invalidate(page);
				greedy.invalid--;
				write(page,block);
			}
			
			return block;//return greedy to call gc again
		}else
			return greedy;
		*/
	}

	

	private void erase(Block b) {
		b.erase(isWearing);
		if (b.age>=BlockLifetime){
			badBlocks.add(b);
			blocks.remove(b);
		}
	}

	private HashSet<Integer> reverseMapping(Block block) {
		HashSet<Integer> s=new HashSet<>();
		Set<Integer> pages=mapping.keySet();
		for(Integer p:pages){
			if (mapping.get(p)==block) {
				s.add(p);
			}
		}
		return s;
	}

	public Block gcRandom() {	// random GC
		Block block = null;
		int i,avaliable=0;
		for (Block b:blocks){
			if(b.valid<BlockSize){
				avaliable++;
				break;
			}
		}
		if (avaliable==0) {
			return null;
		}
		//System.out.println("bad "+badBlocks.size());
		do {
			i=Util.rand(blocks.size());
			block=blocks.get(i);
			if(block.valid>=BlockSize) //skip full blocks
				continue;
			if(block.valid+block.invalid==BlockSize){
				if (block.age>=BlockLifetime){
					continue;
				}else {
					if(block.valid+block.invalid==BlockSize) erase(block);
					//System.out.println("erase called "+block.id);
				}
			}
			break;
		} while (true);
		
		return block;
	}
	private ArrayList<Block> greedyCandidates() {
		ArrayList<Block> candidates=new ArrayList<>();
		int v=BlockSize;
		for (Block b : blocks) {
			if (b.valid<v) {
				v=b.valid;
			}
		}
		if (v==BlockSize)
			return candidates;
		
		for (Block b : blocks) {
			if(b.valid==v){
				candidates.add(b);
			}
		}
		return candidates;
	}
	public Block gcGreedy() {		// greedy GC
		
		ArrayList<Block> candidates=greedyCandidates();
		if (candidates.size()==0) return null;
		Block block;
		block=candidates.get(Util.rand(candidates.size())); //random choose one
	
		if(block.valid+block.invalid==BlockSize){
			if (block.age>=BlockLifetime){
				System.out.println("err2");
			}else {
				if(block.valid+block.invalid==BlockSize) erase(block);
				//System.out.println("erase called "+block.id);
			}
		}else
			System.out.println("gc a block does not need erase");	
		return block;
	}

	public Block gcDchoice() {	// D-choice GC
		Block block;
		int i=0,v=BlockSize;
		int avaliable=0;
		for (Block b:blocks){
			if(b.valid<BlockSize){
				avaliable++;
				break;
			}
		}
		if (avaliable==0) {
			return null;
		}
		do {
			for (int j = 0; j < Dchoice; j++) {
				int ii=Util.rand(blocks.size());
				Block b=blocks.get(ii);
				if (v>b.valid) {
					i=ii;
					v=b.valid;
				}
			}
			block=blocks.get(i);
			
			if(block.valid>=BlockSize) //skip full blocks
				continue;
			if(block.valid+block.invalid==BlockSize){
				if (block.age>=BlockLifetime){
					continue;
				}else {
					if(block.valid+block.invalid==BlockSize) erase(block);
					//System.out.println("erase called "+block.id);
				}
			}
			break;
		} while (true);
		
		return block;
	}
	public void showHistogram() {
		for (int i = 0; i <= BlockSize; i++) {
			int t=0;
			for(Block b:blocks){
				if (b.valid==i) {
					t++;
				}
			}
			System.out.print(t+" ");
		}
	}

	
	
}
