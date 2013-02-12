package currentmonitorexample;

import java.util.Map;

import org.osgi.service.log.LogService;

import com.buglabs.application.ServiceTrackerHelper.ManagedRunnable;

import currentmonitorservice.ICurrentMonitorNotification;
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
public class CurrentMonitorExampleApplication implements ManagedRunnable, ICurrentMonitorNotification {
	LogService ls;

	@Override
	public void run(Map<Object, Object> services) {			
		ls = (LogService) services.get(LogService.class.getName());
		ilog("Start");
	}

	@Override
	public void shutdown() {
		ilog("Stop");
	}

	@Override
	public void thresholdCrossed(boolean turningOn) {
		if (turningOn) {
			ilog("Holy Cow, the thing just turned on!  Let me do something about that");
		} else {
			ilog("Bummer, the thing just turned off...");
		}
	}
	
	//Wrappers for the log service (to standardize logged messages)
	void ilog(String message){  ls.log(LogService.LOG_INFO, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void dlog(String message){  ls.log(LogService.LOG_DEBUG, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void elog(String message){  ls.log(LogService.LOG_ERROR, "["+this.getClass().getPackage().getName()+"] "+message);	}
	void wlog(String message){  ls.log(LogService.LOG_WARNING, "["+this.getClass().getPackage().getName()+"] "+message);	}
}