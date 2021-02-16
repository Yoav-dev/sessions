package jobs;

public class CheckPlacesJob extends Job {
	private static final String CHECK_PLACE = "checkPlace";
	
	@Override
	public String action() {
		return CHECK_PLACE;
	}
}