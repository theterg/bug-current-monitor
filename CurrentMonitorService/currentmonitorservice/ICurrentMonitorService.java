package currentmonitorservice;

public interface ICurrentMonitorService {
	public void calibrateZero();
	public void setThresh(int threshold);
	public int getThresh();
	public void setThresholdMultiplier(int threshold_multiplier);
	public int getThresholdMultiplier();
	public int getLastAvgReading();
	public int getLastReading();
}
