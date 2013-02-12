package currentmonitorservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class CurrentMonitorConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	//The number of samples to average for normal operation
	final static int HISTORY_LEN = 20;
	//The number of samples to average while calculating the zero point
	final static int SAMPLES_TO_ZERO = 80;

	//The threshold
	private int thresh = Integer.MAX_VALUE;
	private int zeroPoint = Integer.MIN_VALUE;
	//How much headroom to consider "ON".  Roughly ~30mA per mult
	private int thresholdMultiplier = 5;		//roughly 150mA
	
	public String toString(){
		return "[thresh]="+thresh+" [zeroPoint]="+zeroPoint+" [thresholdMultiplier]="+thresholdMultiplier;
	}
	
	public void setThresh(int threshold) {
		thresh = threshold;
		updated();
	}
	
	public int getThresh(){
		return thresh;
	}
	
	public void setZeroPoint(int zero_point) {
		zeroPoint = zero_point;
		updated();
	}
	
	public int getZeroPoint(){
		return zeroPoint;
	}
	
	public void setThresholdMultiplier(int threshold_multiplier){
		thresholdMultiplier = threshold_multiplier;
		updated();
	}
	
	public int getThresholdMultiplier(){
		return thresholdMultiplier;
	}
	
	private void updated(){
		save();
		//TODO - Something more elaborate?
	}
	
	public CurrentMonitorConfig() {
		load();
	}
	
	public boolean load() {
		File f = new File("CurrentMonitorSettings.ser");
		try {
			InputStream fs = new BufferedInputStream(new FileInputStream(f));
			ObjectInput in = new ObjectInputStream(fs);
			CurrentMonitorConfig loaded = (CurrentMonitorConfig)in.readObject();
			thresh = loaded.thresh;
			zeroPoint = loaded.zeroPoint;
			thresholdMultiplier = loaded.thresholdMultiplier;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean save() {
		File f = new File("CurrentMonitorSettings.ser");
		try {
			OutputStream fs = new BufferedOutputStream(new FileOutputStream(f));
			ObjectOutput out = new ObjectOutputStream(fs);
			out.writeObject(this);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
