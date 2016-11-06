package sensors.Implementations.MPU9250;

import java.io.IOException;
import java.util.Arrays;

import dataTypes.DataFloat3D;
import dataTypes.TimestampedDataFloat3D;
import sensors.models.Sensor3D;

public class MPU9250Gyroscope extends Sensor3D {
	private GyrScale gyroScale; 
	public MPU9250Gyroscope(int sampleRate, int sampleSize, MPU9250RegisterOperations ro) 
	{
		super(sampleRate, sampleSize, ro);
		gyroScale = GyrScale.GFS_2000DPS;
		this.setValScaling( new DataFloat3D(	(float)gyroScale.getRes(),
										(float)gyroScale.getRes(),
										(float)gyroScale.getRes()));
	}

	@Override
	public void updateData() throws IOException {
        short registers[];
        //roMPU.readByteRegister(Registers.GYRO_XOUT_H, 6);  // Read again to trigger
        registers = ro.read16BitRegisters(Registers.GYRO_XOUT_H,3);
        this.addValue(OffsetAndScale(new TimestampedDataFloat3D(registers[0],registers[1],registers[2])));
	}
	

	@Override
	public void calibrate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selfTest() {
		// TODO Auto-generated method stub

	}
    public void setGyroBiases(short[] gyroBiasAvg)
    {
    	System.out.println("setGyroBiases");
        short gyrosensitivity = 131;     // = 131 LSB/degrees/sec
        short[] gyroBiasAvgLSB = new short[] {0,0,0};
        
        // Construct the gyro biases for push to the hardware gyro bias registers, which are reset to zero upon device startup
        // Divide by 4 to get 32.9 LSB per deg/s to conform to expected bias input format
        // Biases are additive, so change sign on calculated average gyro biases
        
        gyroBiasAvgLSB[0] = (short)(-gyroBiasAvg[0]/4);
        gyroBiasAvgLSB[1] = (short)(-gyroBiasAvg[1]/4);
        gyroBiasAvgLSB[2] = (short)(-gyroBiasAvg[2]/4);
        System.out.print("gyroBiasAvgLSB: "+Arrays.toString(gyroBiasAvgLSB));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n",gyroBiasAvgLSB[0],gyroBiasAvgLSB[1],gyroBiasAvgLSB[2]);
    	
        // Push gyro biases to hardware registers
    	ro.write16bitRegister(Registers.XG_OFFSET_H,gyroBiasAvgLSB[0]);
    	ro.write16bitRegister(Registers.YG_OFFSET_H,gyroBiasAvgLSB[1]);
    	ro.write16bitRegister(Registers.ZG_OFFSET_H,gyroBiasAvgLSB[2]);
         
        // set super class NineDOF variables
        this.setValBias(new DataFloat3D(	(float) gyroBiasAvg[0]/(float) gyrosensitivity,
        							(float) gyroBiasAvg[1]/(float) gyrosensitivity,
        							(float) gyroBiasAvg[2]/(float) gyrosensitivity));
        //System.out.println("gyrBias (float): "+Arrays.toString(gyrBias));
    	System.out.println("End setGyroBiases");
    }
}