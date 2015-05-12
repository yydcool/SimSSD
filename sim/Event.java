package sim;

public class Event implements Comparable<Event>{
	double time=0;
	int type=0; //0 - request starts, 1 - request ends
	int id;
	public Event(double t, int ty, int pid){
		this.time=t;
		type=ty;
		id=pid;
	}
	
	public int compareTo(Event o) {
		if (time<o.time) {
			return -1;
		}
		if (time>o.time) {
			return 1;
		}
		return 0;
	}
	
	public String toString() {
		return ""+time;
	}
	
}
