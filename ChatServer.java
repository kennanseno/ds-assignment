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
	private static final long TIMER2 = 30000;
	private static final long TIMER3 = 45000;
	private static final long TIMER4 = 45200;
	private static final long TIMER5 = 59600;
	private static final long TIMER6 = 59800;

   private AuctionItem currentAuctionItem = new AuctionItem();
   private ArrayList<AuctionItem> auctionItemList = new ArrayList<AuctionItem>();
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
        addItems();
        addToAuction(0);
      } catch(IOException ioe) {
        System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
      }
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

   public synchronized void broadcast(int ID, String input) {
     int clientbid = Integer.decode(input); //we are getting user input and converting into int
     
     if (input.equals(".bye")) {
        clients[findClient(ID)].send(".bye");
        remove(ID);
     } else {
         //checking if the bid is bigger than the item price
        	if (clientbid > currentAuctionItem.getPrice() && clientbid > 0 && clientbid < 1000000000 && itemsCount > 0) {
              currentAuctionItem.setPrice(clientbid);
              for(int i=0;i<clientCount; i++)
              {
                clients[i].send("\nHighest bid for:  " + currentAuctionItem.getName() + " is: " + currentAuctionItem.getPrice()); //sending info to auctioneers whats the current highest bid
              }
              clients[findClient(ID)].send("\nYou have placed a new highest bid of: " + currentAuctionItem.getPrice() + " for: " + currentAuctionItem.getName()); //sends info to the bidder that he got the highest bid
              for (int i = 0; i < clientCount; i++)
              {
                if(clients[i].getID() != ID)
                  System.out.println(ID + clientbid);
                clientID = ID;// getting the id of the client who placed the bid
              }
              current_client = 1;
              timer.cancel();// reset the timer
              runTimer();//starts the timer again when new bid was placed
            }
            else if(itemsCount == 0)
            {
              clients[findClient(ID)].send("\nNo more bids allowed");
            }
            else //informs the client that the bid is not valid
            {
              clients[findClient(ID)].send("\nYou've placed either the same or lower bid for the " +  currentAuctionItem.getName() + " which is currently at the price of: " + currentAuctionItem.getPrice() + " Euro");
            }
     }
     notifyAll();
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

   private void addThread(Socket socket) {
	   if (clientCount < clients.length){
        System.out.println("Client accepted: " + socket);
        clients[clientCount] = new ChatServerThread(this, socket);
        try {
			      clients[clientCount].open();
            clients[clientCount].start();
            clientCount++;
            welcome();
        } catch(IOException ioe) {
			      System.out.println("Error opening thread: " + ioe);
		    }
      } else {
          System.out.println("Client refused: maximum " + clients.length + " reached.");
      }
   }

	public void welcome() // displays message after user connects to the server
	{
		if(clientCount == 2 && itemsCount > 0) // message displayed when 2nd auctioneer join the server and items are currently on sale
		{
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Current item for sale is:  " +  currentAuctionItem.getName() + " at the price: " + currentAuctionItem.getPrice());
			}
			runTimer();//we start the auction
		}
		else if(clientCount >=3 && itemsCount > 0)// message displayed when another auctioneers connect to server
		{												// the auction doesn't restarts
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Current item for sale is:  " +  currentAuctionItem.getName() + " at the price: " + currentAuctionItem.getPrice());
			}
		}
		else if(clientCount == 1 && itemsCount > 0)// waiting for another client to connect to start the auction
		{
			for(int i=0;i<clientCount; i++)
			{
				clients[i].send("\nWelcome to the auction. Auction will start when we get another client! Please wait...");
			}
		}
		else if(clientCount >=1 && itemsCount < 1)// when client connects but there is no more items for the auction
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
					if (itemsCount == 1) // if there is only one item the auctioneers are notified that it is the last item
					{
						for (int INDEX = 0; INDEX < clientCount; INDEX++)
						{
							clients[INDEX].send( " \n----------------------------------------------------------------------");
							clients[INDEX].send("\nLast item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
						}
						timer.cancel();
						runTimer();
					}
					else if(itemsCount == 0) // in case something goes wrong we don't want to start the auction again
					{
						for (int INDEX = 0; INDEX < clientCount; INDEX++)
						{
							clients[INDEX].send("\n Please come again :)");
						}
					}
					else //if item wasn't sold we display the name and the price of the next item
					{
						for (int INDEX = 0; INDEX < clientCount; INDEX++)
						{
							clients[INDEX].send( " \n----------------------------------------------------------------------");
							clients[INDEX].send("\nNext item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice());
						}
						timer.cancel();// we cancel the timer and then start it again for 1 minute
						runTimer();
					}
				}
				else // if bid was placed
				{
					if(itemsCount == 2)//notifying auctioneers which item was sold, to who and for how much
					{
						Toolkit.getDefaultToolkit().beep();// 3rd beep noise notifying auctioneers that the
						for (int INDEX = 0; INDEX < clientCount; INDEX++)
						{
							clients[INDEX].send( " \n"+  currentAuctionItem.getName() + "  Was sold to " + clientID +" at the price:  " + currentAuctionItem.getPrice());
							clients[INDEX].send( " \n----------------------------------------------------------------------");
						}
						auctionItemList.remove(INDEX);//remove the item from the list
						addToAuction(INDEX);//adding the last item to the list
						itemsCount--;//deducting the item count
						for (int INDEX = 0; INDEX < clientCount; INDEX++)
						{
							clients[INDEX].send("\nLast item for sale is:  " +  currentAuctionItem.getName() + "  at the price:  " + currentAuctionItem.getPrice()); // notifying auctioneers that last item on sale in this auction
						}
						timer.cancel();//reset the timer again
						runTimer();
					}
					else if(itemsCount == INDEX)//
					{
						itemsCount --;//deducting the item count
						Toolkit.getDefaultToolkit().beep();
						for (int i = 0; i < clientCount; i++)
						{
							clients[INDEX].send( "\n"+  currentAuctionItem.getName() + "  Was sold to " + clientID +" at the price:  " + currentAuctionItem.getPrice());
							clients[INDEX].send( " \n----------------------------------------------------------------------");
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

		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				for (int i = 0; i < clientCount; i++)
				{
					clients[i].send("\n30 seconds left for this auction!!!"); //reminding auctioneers how much left do they have to bid
				}
				if(current_client == 1) // beeps only when someone placed bid for the item
				{
					Toolkit.getDefaultToolkit().beep();
				}
			}
		}, TIMER2);

		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				for (int i = 0; i < clientCount; i++)
				{
					clients[i].send("\n15 seconds left for this auction!!!"); //reminding auctioneers how much left do they have to bid
				}
				if(current_client == 1) // beeps only when someone placed bid for the item
				{
					Toolkit.getDefaultToolkit().beep();
				}
			}
		}, TIMER3);
		timer.schedule(new TimerTask() { //beeping at certain times to get the auction feel

			@Override
			public void run()
			{
				if(current_client == 1)
				{
					Toolkit.getDefaultToolkit().beep();
				}
			}
		}, TIMER4);

		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				if(current_client == 1)
				{
					Toolkit.getDefaultToolkit().beep();
				}
			}
		}, TIMER5);

		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{	if(current_client ==1)
			{
				Toolkit.getDefaultToolkit().beep();
			}
			}
		}, TIMER6);
	}

   public static void main(String args[]) {
	    ChatServer server = null;
      if (args.length != 1){
         System.out.println("Usage: java ChatServer port");
      } else {
         server = new ChatServer(Integer.parseInt(args[0]));
      }
   }
}