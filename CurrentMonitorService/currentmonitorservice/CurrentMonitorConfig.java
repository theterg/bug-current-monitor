package currentmonitorservice;

import java.io.Serializable;

public class CurrentMonitorConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	//The number of samples to average for normal operation
	final static int HISTORY_LEN = 20;
	//The number of samples to average while calculating the zero point
	final static int SAMPLES_TO_ZERO = 80;

	//The threshold
	public int thresh = Integer.MAX_VALUE;
	public int zeroPoint = Integer.MIN_VALUE;
	//How much headroom to consider "ON".  Roughly ~30mA per mult
	public int THRESHOLD_MULTIPLIER = 5;		//roughly 150mA
}
