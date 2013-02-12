bug-current-monitor
===================

A buglabs OSGI project: uses a current sensor to detect when an electrical device is turned on

Connect a current sensor to ADC0 where the DC voltage is proportional to current.  Connect a button between GPIO0 and GND.  

Enable debug logging in felix and deploy this bundle.  With no load connected, or the load turned off, press the button.  The zero point will be averaged and a threshold automatically calculated.  Then switch on the load.  Within a second or two, the service will report the device has been switched on. 

### Produces

ICurrentMonitorNotification

Any bundles that register this service will be subscribed to updates of the CurrentMonitor

*	public void thresholdCrossed(boolean turningOn);
	*	This is called when the average reading crosses the threshold, both UP and DOWN.  turningOn is true when the current reading is ABOVE the threshold.

### Consumes

ICurrentMonitorService

Bundles can track this service to modify the configuration of the current monitor:

*	public void calibrateZero();
	*	Will immediately recalculate an optimal threshold.  Takes a few seconds.  This assumes that NO LOAD is present.
*	public void setThresh(int threshold);
	*	Manually set a new threshold to `threshold`.  The threshold is a raw ADC value.
*	public int getThresh();
	*	Get the current threshold, as a raw integer ADC value.
*	public void setThresholdMultiplier(int threshold\_multiplier);
	*	For the calibrateZero() function - the ThresholdMultiplier increases the headroom between ON and OFF states.  This value depends on the GAIN of the current sensor.  At maximum gain of the SparkFun ACS712 Low Current Sensor Breakout, each threshold\_multiplier is approximately ~30mA of headroom above 0.  Default is 5.
*	public int getThresholdMultiplier();
	*	Get the current ThresholdMultiplier that will be used on the next calibrateZero();
*	public int getLastAvgReading();
	*	Get the last ADC measurement after a 20 step running average is applied.
*	public int getLastReading();
	*	Get the last raw ADC measurement.
