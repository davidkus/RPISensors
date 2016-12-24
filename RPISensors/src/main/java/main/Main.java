package main;

import subsystems.DriveAssemblySubSystem;
import subsystems.InstrumentsSubSystem;
import subsystems.SubSystem;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.EnumSet;
import java.util.HashMap;

public class Main implements RemoteMain
{
	private final HashMap<SubSystemType, SubSystem> subSystems;

	public Main(String hostname) throws RemoteException
    {
        System.setProperty("java.rmi.server.hostname", hostname) ;
        Registry reg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

        reg.rebind("Main", UnicastRemoteObject.exportObject(this,0));

		subSystems = new HashMap<>();
		prepareSubSystems();
	}

    private void prepareSubSystems()
    {
        subSystems.put(SubSystemType.DRIVE_ASSEMBLY, new DriveAssemblySubSystem());
        subSystems.put(SubSystemType.INSTRUMENTS, new InstrumentsSubSystem());
    }

	@Override
	public void start(EnumSet<SubSystemType> systems) throws RemoteException
	{
        for(SubSystemType systemType:systems) subSystems.get(systemType).startup();
	}

	@Override
	public void shutdown(EnumSet<SubSystemType> systems) throws RemoteException
	{
		for(SubSystemType systemType:systems) subSystems.get(systemType).shutdown();
	}

	@Override
	public void restart(EnumSet<SubSystemType> systems) throws RemoteException
	{
		shutdown(systems);
		start(systems);
	}

	@Override
	public void shutdownAll() throws RemoteException
	{
		for(SubSystem system:subSystems.values()) system.shutdown();
	}

	@Override
	public void exit() throws RemoteException
	{
	    shutdownAll();
	    System.exit(0);
	}

	public static void main(String[] args) throws RemoteException
    {
        if(args.length < 1)
        {
            System.out.println("No hostname specified");
            return;
        }
		new Main(args[0]);
	}
}
