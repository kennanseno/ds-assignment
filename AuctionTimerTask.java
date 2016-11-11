public class AuctionTimerTask {
	private String text;
	private int time;
	
    public AuctionTimerTask(String text, int time) {
        this.text = text;
        this.time = time;
    }

    public String text() {
		return this.text;
	}

	public void setName(String text) {
		this.text = text;
	}

	public int getPrice() {
		return this.time;
	}

	public void setPrice(int time) {
		this.time = time;
	}
    
}
