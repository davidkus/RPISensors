package sensors.models;

import java.io.IOException;

import dataTypes.CircularArrayRing;

/**
 * RPISensor - devices.sensors
 * Created by GJWood on 18/07/2016.
 */
public abstract class Sensor <T,S>
{
    protected final CircularArrayRing<T> vals;
    protected S deviceBias; 	//Hardware bias data calculated in calibration
    protected S deviceScaling;	//Hardware scale, depends on the scale set up when configuring the device
    protected int sampleRate;
    protected int sampleSize;
    private int debugLevel;

    /**
     * Sensor		- Constructor
     * @param sampleRate	- The sample rate per second
     * @param sampleSize	- The number of samples that can be held before overwriting
     */
    public Sensor(int sampleRate, int sampleSize)
    {
        vals = new CircularArrayRing<T>(sampleSize);
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.debugLevel=0;
    }

    // Methods implemented here, shouldn't need overriding
    public T getLatestValue(){return vals.get(0);}
    public T getValue(int i){return vals.get(i);}
    public int getReadingCount(){return vals.size();}
    public void setDeviceBias(S deviceBias){this.deviceBias = deviceBias;}
    public S getDeviceBias(){ return deviceBias;}
    public void setDeviceScaling(S deviceScaling){this.deviceScaling = deviceScaling;}
    public S getDeviceScaling(){return deviceScaling;}
    public void addValue(T value){vals.add(value);}
    public int debugLevel(){return debugLevel;}
    public void setDebugLevel(int l){debugLevel=l;}
    
    // Methods that may need extending by sub classes
    public void printState()
    {
    	System.out.println("Vals: "+ vals.size());
    	System.out.println("deviceBias: "+ deviceBias.toString());
    	System.out.println("deviceScaling: "+ deviceBias.toString());
    	System.out.print("sampleRate: "+ sampleRate);
    	System.out.print(" sampleSize: "+ sampleSize);
    	System.out.println(" debugLevel: "+ debugLevel);  	  	
    }
    
    // Methods must be implemented but which can't be done here because the types are not known
    public abstract T getAvgValue();
    public abstract T scale(T value);
    public abstract void updateData() throws IOException;

    // Optional Methods
    public void calibrate() throws IOException, InterruptedException{/*if required implement and override in subclass*/}
    public void configure() throws IOException, InterruptedException{/*if required implement and override in subclass*/}
    public void selfTest() throws IOException, InterruptedException{/*if required implement and override in subclass*/}
    public void printRegisters(){/*if required implement and override in subclass*/} 
}