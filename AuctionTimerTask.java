public class AuctionTimerTask {
	private String text;
	private long time;
	
    public AuctionTimerTask(String text, long time) {
        this.text = text;
        this.time = time;
    }

    public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public long getTime() {
		return this.time;
	}

	public void setTime(long time) {
		this.time = time;
	}
    
}
