package jobs;

public class ReserveJob extends Job {
	private static final String RESERVE = "reserve";
	
	@Override
	public String action() {
		return RESERVE;
	}
}