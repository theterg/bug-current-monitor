bug-current-monitor
===================

A buglabs OSGI project: uses a current sensor to detect when an electrical device is turned on

Connect a current sensor to ADC0 where the DC voltage is proportional to current.  Connect a button between GPIO0 and GND.  

Enable debug logging in felix and deploy this bundle.  With no load connected, or the load turned off, press the button.  The zero point will be averaged and a threshold automatically calculated.  Then switch on the load.  Within a second or two, the service will report the device has been switched on. 
