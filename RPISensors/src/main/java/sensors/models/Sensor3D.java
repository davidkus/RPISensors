package sensors.models;

import dataTypes.Data3D;
import dataTypes.TimestampedData3D;
import sensors.Implementations.MPU9250.MPU9250RegisterOperations;

/**
 * RPITank - devices.sensors
 * Created by GJWood on 18/07/2016.
 */
public abstract class Sensor3D extends Sensor<TimestampedData3D,Data3D>
{	
	public Sensor3D(int sampleRate, int sampleSize, MPU9250RegisterOperations ro) {
		super(sampleRate, sampleSize, ro);
	}

	@Override
	public TimestampedData3D OffsetAndScale(TimestampedData3D value)
    {
    		TimestampedData3D oSVal = value.clone();
            oSVal.setX(value.getX()*valScaling.getX() -valBias.getX()); 
            oSVal.setY(value.getY()*valScaling.getY() -valBias.getY()); 
            oSVal.setZ(value.getZ()*valScaling.getZ() -valBias.getZ()); 
            return oSVal;
    }
}
