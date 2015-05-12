package ssd;

import java.util.LinkedList;


public class Block {
	public static int size,lifetime;
	public int valid,invalid,age,id;
	public double clearTime=0;
	LinkedList<Integer> content=null;
	public Block(int id, int va, int inva) {
		valid=va;
		invalid=inva;
		this.id=id;
	}
	public void invalidate(int pageId) {
		valid--;
		invalid++;
	}
	public void erase(int wearing){
		invalid=0;
		age+=wearing;
	}
	public void write(int pageId) {
		valid++;
	}
	public LinkedList<Integer> getContent(){
		if(content==null){
			content=new LinkedList<>();
		}
		return content;
	}
}
