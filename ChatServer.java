import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ChatServer implements Runnable {  
   
   // Array of clients	
   private ChatServerThread clients[] = new ChatServerThread[50];
   private ServerSocket server = null;
   private Thread       thread = null;
   private int clientCount = 0;
   private static final long AUCTION_TIME = 60000; // Time limit for each auction in milliseconds

   private AuctionItem currentAuctionItem = new AuctionItem();
   private ArrayList<AuctionItem> auctionItemList = new ArrayList<AuctionItem>();
   private ArrayList<AuctionTimerTask> auctionTimerTask = new ArrayList<AuctionTimerTask>();
   private Timer timer;
   private int clientID;
   private int currentClient = 0; // keeps count of how many clients on auction

   public ChatServer(int port) {
	    try {
        System.out.println("Binding to port " + port + ", please wait  ...");
        server = new ServerSocket(port);
        System.out.println("Server started: " + server.getInetAddress());
        start();
        addTimerTasks();
        addItems();
        addToAuction(0);
      } catch(IOException ioe) {
        System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
      }
   }

   public void addTimerTasks() {
     auctionTimerTask.add(new AuctionTimerTask("45 seconds left!", 15000));
     auctionTimerTask.add(new AuctionTimerTask("30 seconds left!", 30000));
     auctionTimerTask.add(new AuctionTimerTask("20 seconds left!", 40000));
     auctionTimerTask.add(new AuctionTimerTask("10 seconds left!", 50000));
     auctionTimerTask.add(new AuctionTimerTask("5 seconds left!", 55000));
     auctionTimerTask.add(new AuctionTimerTask("4 seconds left!", 56000));
     auctionTimerTask.add(new AuctionTimerTask("3 seconds left!", 57000));
     auctionTimerTask.add(new AuctionTimerTask("2 seconds left!", 58000));
     auctionTimerTask.add(new AuctionTimerTask("1 seconds left!", 59000));
   }

   public void addItems() {
    //add  items on auction list
    auctionItemList.add(new AuctionItem("Iphone 7", 50));
    auctionItemList.add(new AuctionItem("Xbox One", 220));
   }
   public void addToAuction(int index) {
     currentAuctionItem = new AuctionItem(auctionItemList.get(index).getName(), auctionItemList.get(index).getPrice());
   }

   public synchronized void broadcast(int ID, String input) {   
     if (input.equals("exit")) {
        clients[findClient(ID)].send("Goodbye!");
        remove(ID);
     } else {
          int clientBid = Integer.decode(input); //convert user input to int

          //check if the bid is bigger than the item price
        	if (clientBid > currentAuctionItem.getPrice() && auctionItemList.size() > 0) {
              currentAuctionItem.setPrice(clientBid);
              
              for(int i = 0; i < clientCount; i++) {
                  clients[i].send("\nUPDATE! Auction item: " + currentAuctionItem.getName() + " | Highest bid: " + currentAuctionItem.getPrice()); //send current auction status to auctioneers
                  clients[findClient(ID)].send("\nYou have placed bid of " + currentAuctionItem.getPrice() + " for " + currentAuctionItem.getName()); //notify highest bidder
                  
                  for (int x = 0; x < clientCount; x++) {
                    if(clients[x].getID() != ID) {
                      System.out.println(ID + clientBid);
                    }
                    clientID = ID; //get id of highest bidder
                  }

                  currentClient = 1;
                  timer.cancel();
                  runAuction();//starts the timer again when new bid was placed
              } 
          } else {  //informs the client if bid is not valid
            clients[findClient(ID)].send("\nA bid higher than " + currentAuctionItem.getPrice() + " is required to make a bid on " + currentAuctionItem.getName());
          }
      }
      notifyAll();
    }
   

   private void addThread(Socket socket) {
	   if (clientCount < clients.length){
        System.out.println("Client accepted: " + socket);
        clients[clientCount] = new ChatServerThread(this, socket);
        try {
			      clients[clientCount].open();
            clients[clientCount].start();
            clientCount++;
            welcomeMessage();
        } catch(IOException ioe) {
			      System.out.println("Error opening thread: " + ioe);
		    }
      } else {
          System.out.println("Client refused: maximum " + clients.length + " reached.");
      }
   }

	public void welcomeMessage() { // displays message when a bidder joins
		if(clientCount > 1 && auctionItemList.size() >= 1) { // message displayed when there is enough bidder in the auction
      sendMessageToBidders(clientCount, "\nWELCOME! Current item is " +  currentAuctionItem.getName() + " at " + currentAuctionItem.getPrice());
			runAuction(); // start auction
		} else if(clientCount == 1 && auctionItemList.size() >= 1) { // wait for another client to connect to start the auction
      sendMessageToBidders(clientCount, "\nWELCOME! Waiting for 1 more bidder. Please wait...");
		}
	}

  private void runAuction() { // auction logic
		timer = new Timer("Countdown");
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				if(currentClient == 0) { // if no one placed a bid

            // place item at the end of arraylist
            auctionItemList.remove(0); 
            auctionItemList.add(new AuctionItem(currentAuctionItem.getName(), currentAuctionItem.getPrice()));
            addToAuction(0);

            if (auctionItemList.size() == 1) { // if only 1 item in the auction list
              sendMessageToBidders(clientCount, "\n_____________________________________________________________________________");
              sendMessageToBidders(clientCount, "\nLast item in auction is " +  currentAuctionItem.getName() + " with bid starting at " + currentAuctionItem.getPrice());
              runAuction();
            } else if(auctionItemList.size() > 1) { // if multiple items left in auction list
              sendMessageToBidders(clientCount, "\n_____________________________________________________________________________");
              sendMessageToBidders(clientCount, "\nNext item in auction is " +  currentAuctionItem.getName() + " with bid starting at " + currentAuctionItem.getPrice());
              runAuction();
            } 
				} else {  // if a bid is placed
            if(auctionItemList.size() > 1) { //broadcast winning bidder and run auction again with another item 
              Toolkit.getDefaultToolkit().beep();    
              sendMessageToBidders(clientCount, "\n_____________________________________________________________________________");
              sendMessageToBidders(clientCount, "\nLast item in auction is " +  currentAuctionItem.getName() + " with bid starting at " + currentAuctionItem.getPrice());
              
              auctionItemList.remove(0);
              addToAuction(0);
              
              sendMessageToBidders(clientCount, "\nLast item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
              runAuction();
            } else if(auctionItemList.size() == 1) { // if last item in auction list
              Toolkit.getDefaultToolkit().beep();            
              sendMessageToBidders(clientCount," \n"+  currentAuctionItem.getName() + "  was sold to " + clientID +" at the price: " + currentAuctionItem.getPrice());
              sendMessageToBidders(clientCount, "\n_____________________________________________________________________________");

              sendMessageToBidders(clientCount, "\n------Auction has now ended. Thank you for participating!------");
              auctionItemList.remove(0);
              timer.cancel();
            }

            currentClient = 0; //reset back to 0
				}
			}
		}, AUCTION_TIME);

    // add auctiontimerTasks
    for(AuctionTimerTask task: auctionTimerTask) {
        TimerTask timerTask = new TimerTask() {
          @Override
          public void run() {
            for (int i = 0; i < clientCount; i++) {
                clients[i].send(task.getText());
            }
            
            if(currentClient == 1) {
                Toolkit.getDefaultToolkit().beep();
            }
          };   
        };

        timer.schedule(timerTask, task.getTime());  
	  }

  }

  public static void main(String args[]) {
    ChatServer server = null;
    if (args.length != 1){
        System.out.println("Usage: java ChatServer port");
    } else {
        server = new ChatServer(Integer.parseInt(args[0]));
    }
  }

  public void sendMessageToBidders(int cCount, String message) {
      for(int i = 0; i < cCount; i++) {
        clients[i].send(message);
			}
  }

  public void start() {
		if (thread == null) {
		  thread = new Thread(this);
      thread.start();
    }
  }

   public void stop() {
	   thread = null;
   }

	private int findClient(int ID)
	{
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;
	}

   public void run() {
      while (thread != null) {
        try{
          System.out.println("Waiting for a client ...");
          addThread(server.accept());

          int pause = (int)(Math.random()*3000);
          Thread.sleep(pause);

        } catch(IOException ioe) {
          System.out.println("Server accept error: " + ioe);
          stop();
        } catch (InterruptedException e){
          System.out.println(e);
        }
      }
   }

  public synchronized void remove(int ID) {
	  int pos = findClient(ID);
    if (pos >= 0) {
		    ChatServerThread toTerminate = clients[pos];
        System.out.println("Removing client thread " + ID + " at " + pos);

        if (pos < clientCount - 1)
            for (int i = pos+1; i < clientCount; i++)
               clients[i-1] = clients[i];
         clientCount--;

        try{
          toTerminate.close();
        } catch(IOException ioe) {
			    System.out.println("Error closing thread: " + ioe);
		    }
        toTerminate = null;
        System.out.println("Client " + pos + " removed");
        notifyAll();
      }
   }
}