package util;

import java.util.Random;

public class Util {
	static long seed=1;
	static Random rand=new Random(seed);
	static Random randexp=new Random(seed+1);
	static Random randpareto=new Random(seed+1);
	public static int rand(int N){
		return rand.nextInt(N); // [0, N-1]
	}
	
	public static double randExp(double mu){
		return -mu*Math.log(randexp.nextDouble());
	}
	
	public static double randPareto(double alpha, double c) { //mean is (alpha*c)/(alpha-1);

        return c*Math.pow(randpareto.nextDouble(),-1.0/alpha);

   }

	public static double randUni(double a, double b) {
		return a+rand.nextDouble()*(b-a);
	}
}
