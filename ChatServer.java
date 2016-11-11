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

    private static final long TIMER1 = 60000; //Times for end of the biding and sounds

   private AuctionItem currentAuctionItem = new AuctionItem();
   private ArrayList<AuctionItem> auctionItemList = new ArrayList<AuctionItem>();
   private ArrayList<AuctionTimerTask> auctionTimerTask = new ArrayList<AuctionTimerTask>();
   private Timer timer;
   private int clientID;
   private int INDEX = 0;
   private int current_client = 0; //very important int which determines if the bid was placed or not
   private static int itemsCount = 2; //keeping the track of the amount of items

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
     currentAuctionItem = new AuctionItem();
     currentAuctionItem.setName(auctionItemList.get(index).getName());
     currentAuctionItem.setPrice(auctionItemList.get(index).getPrice());
   }

   public synchronized void broadcast(int ID, String input) {
     int clientBid = Integer.decode(input); //convert user input to int
     
     if (input.equals(".bye")) {
        clients[findClient(ID)].send(".bye");
        remove(ID);
     } else {
         //check if the bid is bigger than the item price
        	if (clientBid > currentAuctionItem.getPrice() && clientBid > 0 && auctionItemList.size() > 0) {

              currentAuctionItem.setPrice(clientBid);
              for(int i=0;i<clientCount; i++) {
                clients[i].send("\nAuction item: " + currentAuctionItem.getName() + " | Highest bid: " + currentAuctionItem.getPrice().doubleValue()); //sending info to auctioneers whats the current highest bid
              clients[findClient(ID)].send("\nYou have placed bid of " + currentAuctionItem.getPrice().doubleValue() + " for " + currentAuctionItem.getName()); //sends info to the bidder that he got the highest bid
              
              for (int i = 0; i < clientCount; i++) {
                if(clients[i].getID() != ID){
                  System.out.println(ID + clientBid);
                }

                clientID = ID;// getting the id of the client who placed the bid
              }
              current_client = 1;
              timer.cancel();// reset the timer
              runTimer();//starts the timer again when new bid was placed
            }
            else if(auctionItemList.size() == 0)
            {
              clients[findClient(ID)].send("\nNo more items in the auction!");
            }
            else //informs the client that the bid is not valid
            {
              clients[findClient(ID)].send("\nYou've placed either the same or lower bid for the " +  currentAuctionItem.getName() + " which is currently at the price of: " + currentAuctionItem.getPrice() + " Euro");
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

	public void welcomeMessage() // displays message after user connects to the server
	{
		if(clientCount == 2 && auctionItemList.size() > 0) // message displayed when 2nd auctioneer join the server and items are currently on sale
		{
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Current item for sale is:  " +  currentAuctionItem.getName() + " at the price: " + currentAuctionItem.getPrice());
			}
			runTimer();//we start the auction
		}
		else if(clientCount >=3 && auctionItemList.size() > 0)// message displayed when another auctioneers connect to server
		{												// the auction doesn't restarts
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Current item for sale is:  " +  currentAuctionItem.getName() + " at the price: " + currentAuctionItem.getPrice());
			}
		}
		else if(clientCount == 1 && auctionItemList.size() > 0)// waiting for another client to connect to start the auction
		{
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Auction will start when we get another client! Please wait...");
			}
		}
		else if(clientCount >=1 && auctionItemList.size() < 1)// when client connects but there is no more items for the auction
		{
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nAuction is over now. Please come again sometime soon");
			}
		}
	}

  	private void runTimer() //timer function where the whole auction logic developed
	{
		timer = new Timer("Countdown");
		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				if(current_client == 0)// if this equals to 0 then nobody have placed the bid
				{
					auctionItemList.remove(INDEX); // item is removed from the list
					auctionItemList.add(new AuctionItem(currentAuctionItem.getName(), currentAuctionItem.getPrice())); //the dame item is placed at the end of the array list
					addToAuction(INDEX);//since we removed the 1st item now the 2nd item is the 1st item
					if (auctionItemList.size() == 1) // if there is only one item the auctioneers are notified that it is the last item
					{
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send( " \n----------------------------------------------------------------------");
							clients[i].send("\nLast item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
						}
						timer.cancel();
						runTimer();
					}
					else if(auctionItemList.size() == 0) // in case something goes wrong we don't want to start the auction again
					{
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send("\n Please come again :)");
						}
					}
					else //if item wasn't sold we display the name and the price of the next item
					{
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send( " \n----------------------------------------------------------------------");
							clients[i].send("\nNext item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
						}
						timer.cancel();// we cancel the timer and then start it again for 1 minute
						runTimer();
					}
				}
				else // if bid was placed
				{
					if(auctionItemList.size() == 2)//notifying auctioneers which item was sold, to who and for how much
					{
						Toolkit.getDefaultToolkit().beep();// 3rd beep noise notifying auctioneers that the
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send( " \n"+  currentAuctionItem.getName() + "  Was sold to " + clientID +" at the price:  " + currentAuctionItem.getPrice());
							clients[i].send( " \n----------------------------------------------------------------------");
						}
						auctionItemList.remove(INDEX);//remove the item from the list
						addToAuction(INDEX);//adding the last item to the list

						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send("\nLast item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice()); // notifying auctioneers that last item on sale in this auction
						}
						timer.cancel();//reset the timer again
						runTimer();
					}
					else if(auctionItemList.size() == INDEX)
					{
						Toolkit.getDefaultToolkit().beep();
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send( "\n"+  currentAuctionItem.getName() + "  Was sold to " + clientID +" at the price:  " + currentAuctionItem.getPrice());
							clients[i].send( " \n----------------------------------------------------------------------");
						}
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send("\n------We have reached to end of the auction! Thank you very much for your participation------"); //message at the end of the auction
						}
						auctionItemList.remove(INDEX);
						timer.cancel();// cancels the timer as the auction is over

					}
					else if(itemsCount == 0)// just for error handling. it shouldn't ever be displayed
					{
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send("\nAuction is over now :(");
						}
					}
					else//message send to auctioneers when item was sold
					{
						Toolkit.getDefaultToolkit().beep();
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send( " \n"+  currentAuctionItem.getName() + "  Was sold to " + clientID +" at the price:  " + currentAuctionItem.getPrice());
							clients[i].send( " \n----------------------------------------------------------------------");
						}
						auctionItemList.remove(INDEX);//remove the item from the list
						addToAuction(INDEX);//adding the last item to the list
						itemsCount--;//deducting the item count
						for (int i = 0; i < clientCount; i++)
						{
							clients[i].send("\nNext item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
						}
						timer.cancel();
						runTimer();
					}
					current_client = 0;//setting the int to 0 so if no bid was placed for the next item then the item can be resold
				}
			}
		}, TIMER1);

    for(AuctionTimerTask task: auctionTimerTask) {
        TimerTask timerTask = new TimerTask() {
          @Override
          public void run() {
            for (int i = 0; i < clientCount; i++) {
                clients[i].send(task.getText()); //reminding auctioneers how much left do they have to bid
            }
            
            if(current_client == 1) {
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

  public void start() {
		if (thread == null) {
		  thread = new Thread(this);
      thread.start();
    }
  }

   public void stop() {
	   thread = null;
   }

   private int findClient(int ID) {
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

        if (pos < clientCount-1)
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