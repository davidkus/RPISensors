package devices.sensors;

import devices.dataTypes.TimestampedData1D;
import devices.dataTypes.TimestampedData3D;
import devices.sensors.interfaces.Accelerometer;
import devices.sensors.interfaces.Gyroscope;
import devices.sensors.interfaces.Magnetometer;
import devices.sensors.interfaces.Thermometer;

public abstract class NineDOF extends SensorPackage implements Accelerometer, Gyroscope, Magnetometer, Thermometer
{
	protected Sensor<TimestampedData3D> mag;
	protected Sensor<TimestampedData3D> accel;
	protected Sensor<TimestampedData3D> gyro;
	protected Sensor<TimestampedData1D> therm;

	protected NineDOF(int sampleRate, int sampleSize) {
		super(sampleRate);
		// TODO Auto-generated constructor stub
	}

}
