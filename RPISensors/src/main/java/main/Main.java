package main;

import hardwareAbstractionLayer.Wiring;
import mapping.MappingSubsystem;
import devices.driveAssembly.DriveAssemblySubSystem;
import inertialNavigation.InstrumentsSubSystem;
import subsystems.*;
import subsystems.SubSystem.SubSystemType;
import telemetry.TelemetrySubSystem;
import logging.SystemLog;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;

import hardwareAbstractionLayer.NanoClock;


public class Main extends Thread implements RemoteMain
{
	private static Main main;
	private final HashMap<SubSystemType, SubSystem> subSystems;
	private final NanoClock clock;
	private final Registry reg;
	private boolean running;

	/**
	 * Main						-	Constructor
	 * @throws RemoteException	-	exception during RMI operation
	 */
	private Main(boolean noHW) throws RemoteException
    {
		main = this;
		clock = new NanoClock();
		reg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Starting SubSystem manager");
        reg.rebind("Main", UnicastRemoteObject.exportObject(this,0));
        Wiring.setI2Cdevices(!noHW);
        Wiring.initialialseI2CBus1();
        Wiring.logGpioPinAllocation();
		subSystems = new HashMap<>();
		prepareSubSystems();
		SystemLog.log(this.getClass(),SystemLog.LogLevel.USER_INFORMATION, "System started");
        running = true;
		this.start();
		System.out.println("System ready");
	}

	/**
	 * main				        -	Entry point for entire program
	 * @param args		        -	command line arguments
	 * @throws RemoteException	-	exception during RMI operation
	 */
	public static void main(String[] args) throws RemoteException
	{
		boolean noHW = false;
		try {
			//noinspection ConstantConditions
			System.setProperty("java.rmi.server.hostname", getLocalAddress().getHostAddress());
		}catch (NullPointerException e)
		{
			System.out.println("Failed to get local address");
			System.exit(-1);
		}
		for (String s: args)
		{
			if (0 == s.compareToIgnoreCase("noHW"))noHW = true;
		}

		new Main(noHW);
	}

	public NanoClock getClock(){return clock;}

	public static Main getMain() {return main;}

    private void prepareSubSystems()
    {
		SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Preparing subSystems");
        subSystems.put(SubSystemType.DRIVE_ASSEMBLY, new DriveAssemblySubSystem());
        subSystems.put(SubSystemType.INSTRUMENTS, new InstrumentsSubSystem());
        subSystems.put(SubSystemType.TELEMETRY, new TelemetrySubSystem());
		subSystems.put(SubSystemType.MAPPING, new MappingSubsystem());
        //subSystems.put(SubSystemType.TESTING, new TestINA219SubSystem());
    }

	/**
	 * start	                -	starts a set of requested subystems
	 * @param systems           -   set of subsystems
	 * @throws RemoteException	-	exception during RMI operation
	 */
    @Override
	public void start(EnumSet<SubSystemType> systems) throws RemoteException
	{

        for(SubSystemType systemType:systems)
		{
                SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Starting " + systemType.name());
                if(subSystems.get(systemType).getSubSysState() != SubSystemState.RUNNING)
                {
                    subSystems.get(systemType).startup();
                    SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Started " + systemType.name());
                } else SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, systemType.name() + " already running");

		}
	}

	/**
	 * stop	                    -	stops a set of requested subystems
	 * @param systems           -   set of subsystems
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
	public void shutdown(EnumSet<SubSystemType> systems) throws RemoteException
	{
        for(SubSystemType systemType:systems)
        {
            SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Stopping " + systemType.name());
            if(subSystems.get(systemType).getSubSysState() != SubSystemState.IDLE)
            {
                subSystems.get(systemType).shutdown();
                SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, "Stopped " + systemType.name());
            } else SystemLog.log(this.getClass(),SystemLog.LogLevel.TRACE_MAJOR_STATES, systemType.name() + " not running");
        }
	}

	/**
	 * restart                  -   restarts specified subsystems
	 * @param systems           -   set of subsystems
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
	public void restart(EnumSet<SubSystemType> systems) throws RemoteException
	{
		shutdown(systems);
		start(systems);
	}

	/**
	 * shutdownAll              -   shutdown all subsystems
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
	public void shutdownAll() throws RemoteException
	{
		for(SubSystem system:subSystems.values()) system.shutdown();
		Wiring.closeI2CBus1();
	}

	/**
	 * getSubSystems	        -	returns which subsystems are available to main
	 * @return                  -
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
	public EnumSet<SubSystemType> getSubSystems() throws RemoteException
	{
		return EnumSet.copyOf(subSystems.keySet());
	}

	/**
	 * getSubSystemState	-	returns the state of the specified subsystem
	 * @param systemType	-	a subsystem
	 * @return	state of the specified subsystem
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
    public SubSystemState getSubSystemState(SubSystemType systemType) throws RemoteException
    {
        return subSystems.get(systemType).getSubSysState();
    }

	/**
	 * exit
	 * @throws RemoteException	-	exception during RMI operation
	 */
	@Override
	public void exit() throws RemoteException
	{
        System.out.println("exiting");
        this.interrupt();
        running = false;
	}

	/**
	 * InetAddress
	 * @return		-	The network address of the Raspberry Pi
	 */
	private static InetAddress getLocalAddress()
	{
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			// go through all network interfaces
			while(interfaces.hasMoreElements())
			{
				Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
				// go through all associated addresses
				while(addresses.hasMoreElements())
				{
					InetAddress address = addresses.nextElement();
					// if the address is not the loopback address and it is IPV4, return it
					if(address instanceof Inet4Address && !address.isLoopbackAddress()) return address;
				}
			}
		} catch (SocketException ignored) {}
		return null;
	}

	@Override
	public void run()
	{
		while(!Thread.interrupted() && running)
		{
            try
			{
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {}
		}
		try
		{
			shutdownAll();
			reg.unbind("Main");
            System.out.println("System shutdown gracefully");
            System.exit(0);
		} catch (RemoteException | NotBoundException e)
		{
			e.printStackTrace();
            System.out.println("System shutdown disgracefully");
            System.exit(-1);
		}
	}
}
