package telemetry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;

public interface RemoteTelemetry extends Remote
{
	Instant getLastUpdateTime() throws RemoteException;
	double getVoltage() throws RemoteException;
	double getCurrent() throws RemoteException;
	double getPower() throws RemoteException;
	double getVelocity() throws RemoteException;
	double getDisplacement() throws RemoteException;
	double getValue(String name) throws RemoteException;
}