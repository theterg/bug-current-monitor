package currentmonitorservice;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.buglabs.bug.module.vonhippel.pub.IVonHippelModuleControl;
import org.osgi.service.log.LogService;

import com.buglabs.application.ServiceTrackerHelper.ManagedRunnable;
/**
 * This class represents the running application when all service dependencies are fulfilled.
 * 
 * The run() method will be called with a map containing all the services specified in ServiceTrackerHelper.openServiceTracker().
 * The application will run in a separate thread than the caller of start() in the Activator.  See 
 * ManagedInlineRunnable for a thread-less application.
 * 
 * By default, the application will only be started when all service dependencies are fulfilled.  For 
 * finer grained service binding logic, see ServiceTrackerHelper.openServiceTracker(BundleContext context, String[] services, Filter filter, ServiceTrackerCustomizer customizer)
 */
public class CurrentMonitorServiceApplication implements ManagedRunnable, Runnable {
	LogService ls;
	IVonHippelModuleControl vh;
	Thread myThread;
	CurrentMonitorConfig cfg;
	
	boolean running = true;
	boolean buttonActive = false;
	int idx = 0;
	
	LinkedList<Integer> history = new LinkedList<Integer>();
	public int lastReading;
	public int avgReading;
	public int minReading;
	public int maxReading;

	@Override
	//this is executed when the service is started
	//we are still in the OSGI context, need to fork into our own thread.
	public void run(Map<Object, Object> services) {			
		vh = (IVonHippelModuleControl) services.get(IVonHippelModuleControl.class.getName());
		ls = (LogService) services.get(LogService.class.getName());
		ilog("Start");
		cfg = new CurrentMonitorConfig();
		ilog("Using configuration values: "+cfg);
		myThread = new Thread(this, this.getClass().getPackage().getName());
		myThread.start();
		//Execute the run() method below in a new thread.
	}
	
	private void setup() throws IOException {
		running = true;
		//Pre-configure ADC so we dont' need to do this every read
		vh.writeADC(vh.VH_ADC_W1_CH0 | vh.VH_ADC_W1_EN |0x8800);
		//Configure GPIO0 as an input with a pullup
		vh.setGPIO(0);
		vh.makeGPIOIn(0);
		//Turn off all LEDs
		vh.clearIOX(0);
		vh.clearIOX(1);
		vh.clearIOX(2);
		vh.clearIOX(3);
		//Check if a button is connected
		buttonActive = (vh.getStatus() & (1 << 8)) == 0;
	}

	@Override
	public void shutdown() {
		running = false;
		myThread.interrupt();
		//After signaling thread should close, wait for it!
		try {
			myThread.join();
		} catch (InterruptedException e) {
			wlog("Interrupt: Unable to wait for thread to close");
		}
		//make sure LEDs are off.
		try {
			vh.clearIOX(0);
			vh.clearIOX(1);
			vh.clearIOX(2);
			vh.clearIOX(3);
		} catch (IOException e) {}
		ilog("Stopped");
	}
	
	private void updateAverage(int reading){
		history.add(reading);
		if (history.size() > cfg.HISTORY_LEN) {
			history.poll();
		}
		long sum = 0;
		minReading = Integer.MAX_VALUE;
		maxReading = Integer.MIN_VALUE;
		for (Integer num: history){
			sum += num;
			if (num < minReading)
				minReading = num;
			if (num > maxReading)
				maxReading = num;
		}
		avgReading = (int)(sum/(long)history.size());
	}
	
	private void zero(){
		int[] samples = new int[cfg.SAMPLES_TO_ZERO];
		int reading;
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int avg = 0;
		long sum = 0;
		boolean state = true;
		
		ilog("Calibrating zero point threshold");
		try {
			vh.setIOX(3);
			Thread.sleep(500);
			for (int i=0;i<cfg.SAMPLES_TO_ZERO;i++){
				reading = readADC();
				sum += reading;
				if (reading > max)
					max = reading;
				if (reading < min)
					min = reading;
				if (state)
					vh.clearIOX(3);
				else
					vh.setIOX(3);
				state = !state;
			}
			vh.clearIOX(3);
			avg = (int)(sum/(long)cfg.SAMPLES_TO_ZERO);
		} catch (IOException e) {
			elog("IOException during zero, cancelling");
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			elog("Interrupted during zero, cancelling");
			return;
		}
		cfg.setZeroPoint(avg);
		cfg.setThresh(avg + (max-min)*cfg.getThresholdMultiplier());
		ilog("Results of zero: avg: "+avg+" P-P: "+(max-min)+" proposed thresh: "+cfg.getThresh());
	}
	
	//Read from the ADC.  This assumes ADC was already enabled and CH0 selected
	//IE, setup() must have been run first!
	private int readADC() throws IOException, InterruptedException{
		int reading;
		reading = vh.readADC();
		Thread.sleep(200);
		return ((reading - 0x800000) >> 6);
	}

	@Override
	public void run() {
		int idx = 0;
		boolean state = true;
		boolean on = false;
		
		try { setup(); } catch (IOException e) {
			elog("Unable to configure, quitting");
			e.printStackTrace();
			return;
		}
		
		while(running){
			try {
				lastReading = readADC();	//readADC takes 200ms to complete
				updateAverage(lastReading);
				//Check if button is connected and pressed.
				if (buttonActive && ((vh.getStatus() & (1 << 8)) != 0)) {
					//Calibrate a new zero point
					zero();
				}
				if (!on && (avgReading > cfg.getThresh())){
					ilog("DEVICE TURNED ON");
					on = true;
				} 
				if (on && (avgReading < cfg.getThresh())) {
					ilog("DEVICE TURNED OFF");
					on = false;
				}
				//Preform this every second.
				if (idx++ % 5 == 0){
					dlog("Reading: "+lastReading+" Avg: "+avgReading);
					if (state)
						vh.setIOX(2);
					else
						vh.clearIOX(2);
					state = !state;
				}
			} catch (IOException e) {
				wlog("IOException, continuing");
				e.printStackTrace();
			} catch (InterruptedException e){
				running = false;
				dlog("Thread quitting");
			}
		}
	}
	
	//Wrappers for the log service (to standardize logged messages)
	void ilog(String message){  ls.log(LogService.LOG_INFO, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void dlog(String message){  ls.log(LogService.LOG_DEBUG, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void elog(String message){  ls.log(LogService.LOG_ERROR, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void wlog(String message){  ls.log(LogService.LOG_WARNING, "["+this.getClass().getPackage().getName()+"] "+message);	}
}