package util;

import java.io.FileWriter;
import java.io.IOException;



public class Log {
	static FileWriter fLifetime=null;
	static FileWriter fRequests=null;
	public static String lifetime="lifetime";
	public static String requests="requests";
	public static void init() {
		try {
			fLifetime = new FileWriter("../"+lifetime+".txt");
			fRequests = new FileWriter("../"+requests+".txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static public void logLifetime(String s){
		try {
			fLifetime.write(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static public void logRequests(String s){
		try {
			fRequests.write(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void closeAll() {
		try {
			fLifetime.close();
			fRequests.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
