package inertialNavigation;

import messages.Message;
import messages.Message.CommandType;
import messages.Message.ErrorMsgType;
import messages.Message.MessageType;
import messages.Message.ParameterType;
import sensors.interfaces.UpdateListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.EnumSet;

/**
 * NavigationDisplay - inertialNavigation
 * Created by MAWood on 25/11/2016.
 */
public class Client implements Runnable, UpdateListener
{
    private final InetAddress address;
    private final int port;
    private final String name;
    private final DatagramSocket socket;
    //private Message.NavRequestType reqType;
    private boolean dataReady;
    private EnumSet<ParameterType> params = EnumSet.noneOf(ParameterType.class);
    private boolean stopped;
    private long msgsSent;

    Client(InetAddress address, int port, String name, DatagramSocket socket,ParameterType ParamType)
    {
        this(address,port,name,socket);
        this.params.add(ParamType);
    }
    Client(InetAddress address, int port, String name, DatagramSocket socket)
    {
        this.address = address;
        this.port = port;
        this.name = name;
        this.socket = socket;
        this.dataReady = false;
        this.stopped = false;
        this.msgsSent = 0;
    }

    @Override
    public void run()
    {
        while(!Thread.interrupted() && !stopped)
        {
            if(dataReady) sendData();
        }
    }

    public boolean matches(Client newClient)
    {
    	return (this.address.toString().equals(newClient.address.toString()) && (this.port == newClient.getPort()));
    }
    
    private void sendData()
    {
        dataReady = false;
        for(ParameterType param :params)
        {
        	if(isSet(param))
        	{
        		//System.out.print(param.name()+" ");
        		Message msg = new Message();
        		msg.setMsgType(MessageType.STREAM_RESP);
        		buildParameterMsg(param,msg);
        		sendMsg(msg);
        	}
        }
    }

    public static void buildParameterMsg(ParameterType param ,Message msg )
    {
    	msg.setParameterType(param);
    	msg.setErrorMsgType(ErrorMsgType.SUCCESS);
    	msg.setCommandType(CommandType.EXECUTE);
    	switch (param)
        {
            case TAIT_BRYAN:
                msg.setNavAngles(Instruments.getAngles());
                break;
            case QUATERNION:
            	msg.setQuaternion(Instruments.getQuaternion());
            	break;
            case MAGNETOMETER:
            	msg.setNavAngles(Instruments.getMagnetometer());
            	break;
            case ACCELEROMETER:
                msg.setNavAngles(Instruments.getAccelerometer());
                break;
            case GYROSCOPE:
                msg.setNavAngles(Instruments.getGyroscope());
                break;
            default:
            	msg.setErrorMsgType(ErrorMsgType.UNSUPPORTED);
        }
    }
    public void sendMsg(Message msg)
    {
        try
        {
        	byte[] ba = msg.serializeMsg();
            socket.send(new DatagramPacket(ba, ba.length, this.getAddress(), this.getPort()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        msgsSent++;
    	if (msgsSent <= 3) System.out.println(msg.toString()); else	System.out.print(".");
    }

    // Getters & Setters
    InetAddress getAddress() {return address;}
    int getPort() {return port;}
    String getName() {return name;}   
    
    EnumSet<ParameterType> getParams(){return params;}    
    void addParam(ParameterType p){params.add(p);}
    void removeParam(ParameterType p){params.remove(p);}
    boolean isSet(ParameterType p){return params.contains(p);}
   
    public String toString()
    {
        return "Name:" + name + " Address: " + address.getHostAddress() + " Port: "+ port + " Params " + params.toString();
    }

    @Override
    public void dataUpdated() {dataReady = true;}

    public void stop() {this.stopped = true;}
}