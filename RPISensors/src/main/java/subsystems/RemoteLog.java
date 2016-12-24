package subsystems;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * RPISensors - subsystems
 * Created by MAWood on 24/12/2016.
 */
public interface RemoteLog extends Remote
{
    ArrayList<SystemLog.LogEntry> getEntries() throws RemoteException;
}
