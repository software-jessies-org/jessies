package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import e.util.*;

public class VmConnector {
	
	/** The key to determine whether or not to launch the debuggee or attach to an existing VM. */
	public static final String CONNECTION_TYPE_KEY = "Connection type";
	
	/** Indicates the debugger should launch the debuggee. */
	public static final String CONNECTION_TYPE_LAUNCH = "Launch";
	
	/** Options to specify when the connection type is "Launch". */
	public static final String LAUNCH_CLASSPATH = "classpath";
	public static final String LAUNCH_MAIN_CLASS = "main_class";
	public static final String LAUNCH_OPTIONS = "options";
	
	/** Indicates the debugger should attach to an existing debuggee. */
	public static final String CONNECTION_TYPE_ATTACH = "Attach";
	
	/** Options to specify when the connection type is "Attach". */
	public static final String ATTACH_TRANSPORT = "main_class";
	public static final String ATTACH_ADDRESS = "options";
	
	/** Valid values for the Transport parameter. */
	public static final String ATTACH_TRANSPORT_SOCKET = "dt_socket";
	public static final String ATTACH_TRANSPORT_SHARED_MEMORY = "dt_shmem";
	public static final String ATTACH_TRANSPORT_PID = "pid";
	
	/**
	 * Map transport names to appropriate AttachingConnector names.
	 * See http://java.sun.com/j2se/1.5.0/docs/guide/jpda/conninv.html for details.
	 */
	private static final Map<String, String> ATTACHING_CONNECTOR_TYPES = new HashMap<String, String>();
	static {
		ATTACHING_CONNECTOR_TYPES.put(ATTACH_TRANSPORT_SOCKET, "com.sun.jdi.SocketAttach");
		ATTACHING_CONNECTOR_TYPES.put(ATTACH_TRANSPORT_SHARED_MEMORY, "com.sun.jdi.SharedMemoryAttach");
		ATTACHING_CONNECTOR_TYPES.put(ATTACH_TRANSPORT_PID, "sun.jvm.hotspot.jdi.SAPIDAttachingConnector");
	}
	
	/**
	 * Connects to a target for debugging, according to the parameters defined in params.
	 */
	public TargetVm connectToTarget(final Map<String, String> params) {
		Log.warn("Connecting to target...");
		try {
			String connectionType = params.get(CONNECTION_TYPE_KEY);
			if (connectionType.equals(CONNECTION_TYPE_LAUNCH)) {
				String classpath = params.get(LAUNCH_CLASSPATH);
				String mainClass = params.get(LAUNCH_MAIN_CLASS);
				String options = params.get(LAUNCH_OPTIONS);
				return launchVm(classpath, mainClass, options);
			} else if (connectionType.equals(CONNECTION_TYPE_ATTACH)) {
				String transport = params.get(ATTACH_TRANSPORT);
				String address = params.get(ATTACH_ADDRESS);
				return attachToVm(transport, address);
			}
		} catch (IOException ioex) {
			Log.warn("IOException while connecting.", ioex);
		} catch (IllegalConnectorArgumentsException caex) {
			Log.warn("Illegal connector arguments.", caex);
		} catch (VMStartException vmex) {
			Log.warn("VM didn't start.", vmex);
		}
		return null;
	}
	
	/**
	 * Launches a new VM and connects to it.
	 * @param classpath the debugged application's classpath
	 * @param mainClass the debugged application's main class
	 * @param options command line options for the debugged application. May be null.
	 */
	private TargetVm launchVm(String classpath, String mainClass, String options) throws IllegalConnectorArgumentsException, IOException, VMStartException {
		VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
		LaunchingConnector connector = vmManager.defaultConnector();
		Map<String, Connector.Argument> args = connector.defaultArguments();
		args.get("options").setValue("-classpath " + classpath);
		args.get("main").setValue(mainClass + (options == null ? "" : " " + options));
		for (String arg : args.keySet()) {
			Log.warn("Connect argument: " + args.get(arg));
		}
		VirtualMachine vm = connector.launch(args);
		Log.warn("Connected to " + vm);
		return new TargetVm(vm);
	}
	
	/**
	 * Connects to an already-running VM. Depending on the transport type used,
	 * the target VM may need to be invoked with the appropriate debugging options
	 * <code>-agentlib:jdwp=transport=xxx,server=y</code>. For attaching to a
	 * process, the target VM need not have been launched with any debug parameters.
	 * @param transport the transport type to use to communicate between debugger
	 * and debuggee. May be null, default is ATTACH_TRANSPORT_SOCKET.
	 * @param address the transport address at which the target VM is listening. For socket
	 * transport, "hostname:port", for shared memory transport the shared memory address,
	 * and for attaching to a process, the PID.
	 */
	private TargetVm attachToVm(String transport, String address) throws IllegalConnectorArgumentsException, IOException {
		if (transport == null) {
			transport = ATTACH_TRANSPORT_SOCKET;
		}
		AttachingConnector connector = getAttachingConnector(transport);
		Map<String, Connector.Argument> args = connector.defaultArguments();
		if (transport.equals(ATTACH_TRANSPORT_SOCKET)) {
			// match hostname:port, where hostname is optional.
			Pattern p = Pattern.compile("^(.*):(\\d+)$");
			Matcher m = p.matcher(address);
			if (m.matches()) {
				if (m.group(1).length() > 0) {
					args.get("hostname").setValue(m.group(1));
				}
				args.get("port").setValue(m.group(2));
			}
		} else if (transport.equals(ATTACH_TRANSPORT_PID)) {
			args.get("pid").setValue(address);
		} else if (transport.equals(ATTACH_TRANSPORT_SHARED_MEMORY)) {
			args.get("name").setValue(address);
		}
		for (String arg : args.keySet()) {
			Log.warn("Connect argument: " + args.get(arg));
		}
		VirtualMachine vm = connector.attach(args);
		Log.warn("Connected to " + vm);
		return new TargetVm(vm);
	}
	
	/**
	 * Returns an appropriate AttachingConnector for the given transport type.
	 * @throws IllegalArgumentException if the transport type was not recognized.
	 */
	private static AttachingConnector getAttachingConnector(String transport) {
		if (ATTACHING_CONNECTOR_TYPES.containsKey(transport) == false) {
			throw new IllegalArgumentException("Unknown transport type \"" + transport + "\".");
		}
		String connectorName = ATTACHING_CONNECTOR_TYPES.get(transport);
		VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
		List<AttachingConnector> connectors = vmManager.attachingConnectors();
		for (AttachingConnector connector : connectors) {
			if (connector.name().equals(connectorName)) {
				return connector;
			}
		}
		Log.warn("Couldn't find a connector for " + transport);
		return null;
	}
}
