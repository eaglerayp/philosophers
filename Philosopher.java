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
	  private String address ="localhost";
	  private static int MESSAGE_LENGTH=100;
	  private static final byte REQUEST_CODE = 'R';
	  private static final byte GRANT_CODE = 'G';
	  private static final byte [] REQUEST_MESSAGE = { REQUEST_CODE };
	  private static final byte [] GRANT_MESSAGE = { GRANT_CODE };
	  private boolean [] waitingForfork;
	  private boolean [] hasfork;
	  private boolean [] neighborHasRequestedfork;
	  private int meanthink=0;
	  private int meaneat=0;
	  private int Name;
	  private State state;
	  ArrayList<Integer> idlist;
	  ArrayList<Integer> portlist;
	  
	  private Socket listensock;
	  HashMap<Integer,Socket> socketlist = new HashMap<Integer,Socket>();
	  ArrayList<Socket> socketlistenlist = new ArrayList<Socket>();
	  public enum State{
	        HUNGRY,
	        EATING,
	        THINKING;
	  }
	  
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
	                .println(String.format("Philosopher %s is thinking", Name));
	        System.out.flush();
	        Thread.sleep(getPoissonRandom(meanthink));
	        hungry=true;
	        System.out
            .println(String.format("Philosopher %s is hungry", Name));
	  }
	  
	  private void eat() throws InterruptedException {
		  for (int port:portlist){
			  sendToNeighbor(port,REQUEST_MESSAGE);
			  System.out.println("sendtoport"+port);
		  }
	        System.out
	                .println(String.format("Philosopher %s is eating", Name));
	        System.out.flush();
	        Thread.sleep(getPoissonRandom(meaneat));
	        hungry=false;
	        think();
	  }
	  
	    /** Send a message to a neighbor
	     *
	     *  @param side the side of send to
	     *  @param message the message to send
	     *  @exception IOException if an exception was thrown during communication
	     */
	    private void sendToNeighbor(int port, byte [] message)
	    {
	        try
	        {
	        	socketlist.get(port).getOutputStream().write(message);
	        }
	        catch(IOException e)
	        {
	            System.err.println("Error sending " + (char) message[0] + " to " +
	                               port + " " + e);
	            System.exit(1);
	        }


	    }


	  public Philosopher (int id,ArrayList<Integer> idlist1,final int selfport,ArrayList<Integer> portlist1,int mean_think,int mean_eat) throws IOException, InterruptedException  
	  {  
		state = State.THINKING;
		Name=id;
		meanthink=mean_think;
		meaneat=mean_eat;
        /*waitingForfork=new boolean [portlist1.size()];
        neighborHasRequestedfork=new boolean [portlist1.size()];
        hasfork=new boolean [portlist1.size()];*/
        idlist = new ArrayList <Integer>(idlist1);
        portlist = new ArrayList <Integer>(portlist1);
        
        new Thread(this) {
            public void run()
            {
                listenForConnectionRequest(selfport);
            }
        }.start();

       /* synchronized(this){  wait all thread  listen  and then connect
        	wait();
	        for(int port:portlist1){
				Socket sock= new Socket();
				try {
					sock.connect(new InetSocketAddress(address,port));
					socketlist.put(port,sock);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
	        }
        }*/
		Thread thread = new Thread(this);
	    thread.start(); 
	  }  
	  
      private void listenForConnectionRequest(int port)
      {
    	    ServerSocket serverSocket = null;
	        try{
	            serverSocket = new ServerSocket(port);
	        }
	        catch(IOException e){
	        	System.err.println("THREAD"+Thread.currentThread().getId());
	            System.err.println("Error creating server socket on name:" + 
	            		Name +"port"+port+ " " + e);
	            System.exit(1);
	        }
	        while (true){  //listen
	             Socket connectionSocket = null;
	            try{
	                connectionSocket = serverSocket.accept();
	                final Socket listen=connectionSocket;
			        new Thread() {
			            public void run()
			            {
			            	listenForMessages(listen);
			            }
			        }.start();
	            }
	            catch(IOException e){
	                System.err.println("Error accepting connection on  thread:" + 
	                		Thread.currentThread().getId() + " " + e);
	                System.exit(1);
	            }
	            //System.out.println("Name"+Name);
	            //System.out.println("Listenport"+serverSocket.getLocalPort());
	            //System.out.println("comingsockIP"+connectionSocket.getRemoteSocketAddress());
	            //System.out.println("comingsockPORT"+connectionSocket.getPort());
	            synchronized(this){
	                socketlistenlist.add(connectionSocket); 
	                notifyAll();
	            }            

	        }  
      }
      /** Method executed by a thread that listens for incoming messages
      *
      *  @param side the side on which to listen - one of LEFT, RIGHT
      */
     private void listenForMessages(Socket listensock)
     {
         while(true)
         {
             byte [] messageReceived = receiveMessage(listensock);
             String file_string = "";

             for(int i = 0; i < messageReceived.length; i++)
             {
                 file_string += (char)messageReceived[i];
             }
             System.out.println(file_string);
             /*switch(messageReceived[0])
             {
                 case REQUEST_CODE:

                     if (hasfork[side])
                         neighborHasRequestedfork[side] = true;
                     else if (side == RIGHT && waitingForfork[side])
                         neighborHasRequestedfork[side] = true;
                     else
                         sendToNeighbor(side, GRANT_MESSAGE);
                     break;

                 case GRANT_CODE:

                     synchronized(this)
                     {
                         waitingForfork[side] = false;
                         hasfork[side] = true;
                         notifyAll();
                     }
                     break;
             }*/
         }
     }
      /** Receive a message from a neighbor.  This method blocks until a
       *  message is received
       *
       *  @param side the side to receive from
       *  @return the message received from this neighbor
       */
      private byte [] receiveMessage(Socket listensock)
      {
          // Cannot start listening until we have a connection to neighbor
          while(socketlist == null)
          {
              synchronized(this)
              {
                  try
                  {
                      wait();
                  }
                  catch(InterruptedException e)
                  { }
              }
          }

          byte [] buffer = new byte[MESSAGE_LENGTH];
          try
          {
        	  listensock.getInputStream().read(buffer);
          }
          catch(IOException e)
          {
              System.err.println("Error reading message from thread:" +
            		  Name + " " + e);
              System.exit(1);
          }
          return buffer;
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
			//System.out.println(port[i]);
		}
		  
		
		
	    List<Philosopher> Philosophers = new ArrayList<Philosopher>();  
	      
	    System.out.println("This is currently running on the main thread, " +  
	        "the id is: " + Thread.currentThread().getId());  
	  
	    Date start = new Date();  
	  
	    // start 6 philosopher   (portlist send every connection port) 
	    ArrayList<Integer> idlist = new ArrayList<Integer>();
	    ArrayList<Integer> portlist = new ArrayList<Integer>();
	    for (int i=1; i<=6; i++)  
	    {  
	    	for (int j=0;j<index;j++){
	    		if(philosopher_relation[j][0]==i){
	    			idlist.add(philosopher_relation[j][1]);
	    			portlist.add(port[philosopher_relation[j][1]-1]);
	    		}
	    		else if(philosopher_relation[j][1]==i){
	    			idlist.add(philosopher_relation[j][0]);
	    			portlist.add(port[philosopher_relation[j][0]-1]);
	    		}
	    	}
	    	Philosophers.add(new Philosopher(i,idlist,port[i-1],portlist,mean_think,mean_eat));   
	    	portlist.clear();
	    	idlist.clear();
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

	    for(int port:portlist){
	        //System.out.println(port);
			Socket sock= new Socket();
			try {
				sock.connect(new InetSocketAddress(address,port));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			socketlist.put(port,sock);
			//System.out.println("connect");
		}	


	    try   
	    {  
	      // this will pause this spawned thread for 5 seconds  
	      //  (5000 is the number of milliseconds to pause)  
	      // Also, the Thread.sleep() method throws an InterruptedException  
	      //  so we must "handle" this possible exception, that's why I've  
	      //  wrapped the sleep() method with a try/catch block  
		  /*for(final Socket listen:socketlistenlist){
		    	System.out.println("Name"+Name);
		        new Thread() {
		            public void run()
		            {
		            	listenForMessages(listen);
		            }
		        }.start();
		  }*/
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
