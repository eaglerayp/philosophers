import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.*;

public class Philosopher implements Runnable {
	  public boolean running = false;  
	  volatile boolean hungry=false;  
	  private static int getPoissonRandom(double mean) { // for model time period with poisson distribution 
		    Random r = new Random();
		    double L = Math.exp(-mean);
		    int k = 0;
		    double p = 1.0;
		    do {
		        p = p * r.nextDouble();
		        k++;
		    } while (p > L);
		    return k - 1;
	  }
	  
	  private void think() throws InterruptedException {
	        System.out
	                .println(String.format("Philosopher %s is thinking", Thread.currentThread().getId()));
	        System.out.flush();
	        Thread.sleep(getPoissonRandom(5000));
	        hungry=true;
	        System.out
            .println(String.format("Philosopher %s is hungry", Thread.currentThread().getId()));
	  }
	  private void eat() throws InterruptedException {
	        System.out
	                .println(String.format("Philosopher %s is eating", Thread.currentThread().getId()));
	        System.out.flush();
	        Thread.sleep(getPoissonRandom(7000));
	  }
	  
	  
	  public Philosopher (int selfport,ArrayList<Integer> portlist,int mean_think,int mean_eat)  
	  {  
	    Thread thread = new Thread(this);  
	    thread.start();  
	  }  
	    
	  public static void main (String[] args) throws InterruptedException, IOException  
	  {  
		//process input
		int mean_think=0;
		int mean_eat=0;
		int num_philosophers=0;
		int MAX_forks =100;
		int philosopher_relation[][]= new int [MAX_forks][2];
		int index=0;
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("DP_config.txt"));
			
			String text = in.readLine();
			mean_think=Integer.parseInt(text.split(" ")[2]);
			text = in.readLine();
			mean_eat=Integer.parseInt(text.split(" ")[2]);
			text = in.readLine();
			num_philosophers=Integer.parseInt(text.split(" ")[2]);
			
			
			text = in.readLine();
			for(String pair:text.split("\\(")){
				if(pair.split(",").length>1&&index<MAX_forks&&!pair.split(",")[0].equals("i")){
					philosopher_relation[index][0]=Integer.parseInt(pair.split(",")[0]);
					philosopher_relation[index][1]=Integer.parseInt(pair.split(",")[1].split("\\)")[0]);
					index++;
				}
			}
			in.close();
			  
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end input
		
		//for socket > thread port
		int port[]=new int [num_philosophers];
		for(int i=0;i<num_philosophers;i++){
			Random ran = new Random();
			port[i]=ran.nextInt(16384)+49152;
		}

		  
		
		
	    List<Philosopher> Philosophers = new ArrayList<Philosopher>();  
	      
	    System.out.println("This is currently running on the main thread, " +  
	        "the id is: " + Thread.currentThread().getId());  
	  
	    Date start = new Date();  
	  
	    // start 6 philosopher   (portlist send every connection port)  
	    ArrayList<Integer> portlist = new ArrayList<Integer>();
	    for (int i=1; i<=6; i++)  
	    {  
	    	for (int j=0;j<index;j++){
	    		if(philosopher_relation[j][0]==i){
	    			portlist.add(port[philosopher_relation[j][1]]);
	    		}
	    		else if(philosopher_relation[j][1]==i){
	    			portlist.add(port[philosopher_relation[j][0]]);
	    		}
	    	}
	    	System.out.println("USER:"+i);
	    	for(int a:portlist){
	    		System.out.println(a);
	    	}
	    	Philosophers.add(new Philosopher(port[i-1],portlist,mean_think,mean_eat));   
	    	portlist.clear();
	    }  
	      
	    // We must force the main thread to wait for all the workers  
	    //  to finish their work before we check to see how long it  
	    //  took to complete  
	    for (Philosopher philosopher : Philosophers)  
	    {  
	      while (philosopher.running)  
	      {  
	        Thread.sleep(100);  
	      }  
	    }  
	      
	    Date end = new Date();  
	      
	    long difference = end.getTime() - start.getTime();  
	      
	    System.out.println ("This whole process took: " + difference/1000 + " seconds.");  
	  }  
	    
	  @Override  
	  public void run()   
	  {  
	    this.running = true;  
	    System.out.println("This is currently running on a separate thread, " +  
	        "the id is: " + Thread.currentThread().getId());  
	      
	    try   
	    {  
	      // this will pause this spawned thread for 5 seconds  
	      //  (5000 is the number of milliseconds to pause)  
	      // Also, the Thread.sleep() method throws an InterruptedException  
	      //  so we must "handle" this possible exception, that's why I've  
	      //  wrapped the sleep() method with a try/catch block  
	      think();  
	      while(this.hungry){
	    	  eat();
	      }
	    }   
	    catch (InterruptedException e)   
	    {  
	      // As user Bernd points out in the comments section below, you should  
	      //  never swallow an InterruptedException.  
	      Thread.currentThread().interrupt();  
	    }  
	    this.running = false;  
	  }    
}
