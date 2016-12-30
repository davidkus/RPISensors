package sensors.Implementations.MPU9250;

import java.io.IOException;
import java.util.Arrays;

import dataTypes.Data3f;
import dataTypes.TimestampedData3f;
import logging.SystemLog;
import sensors.models.Sensor3D;
import subsystems.SubSystem;

/**
 * MPU 9250 Accelerometer sensor
 * Created by G.J.Wood on 1/11/2016
 * Based on MPU9250_MS5637_t3 Basic Example Code by: Kris Winer date: April 1, 2014
 * https://github.com/kriswiner/MPU-9250/blob/master/MPU9250_MS5637_AHRS_t3.ino
 * 
 * This class handles the operation of the Accelerometer sensor and is a subclass of Sensor3D, it provides those methods
 * which are hardware specific to the MPU-9250 such as calibration configuring, self test and update
 * This class is independent of the bus implementation, register addressing etc as this is handled by RegisterOperations
 *  
 * Hardware registers controlled by this class
 * 0x0D 13 SELF_TEST_X_ACCEL 	- Accelerometer X axis self test byte
 * 0x0E 14 SELF_TEST_Y_ACCEL 	- Accelerometer Y axis self test byte
 * 0x0F 15 SELF_TEST_Z_ACCEL 	- Accelerometer Z axis self test byte
 * 0x77 119 XA_OFFSET			- Accelerometer X axis bias offset (15 bits big endian [14:7], [6:0] bit0 is Thermometer Compensation)
 * 0x7A 122 YA_OFFSET			- Accelerometer Y axis bias offset (15 bits big endian [14:7], [6:0] bit0 is Thermometer Compensation)
 * 0x7B 123 XA_OFFSET			- Accelerometer Z axis bias offset (15 bits big endian [14:7], [6:0] bit0 is Thermometer Compensation)
 * 0x1C 28 ACCEL_CONFIG			- Accelerometer configuration byte
 * 0x1D 29 ACCEL_CONFIG 2		- Accelerometer configuration byte 2
 * 0x3B 59 ACCEL_XOUT			- Accelerometer X axis reading (16 bits big endian)
 * 0x3D 61 ACCEL_YOUT			- Accelerometer Y axis reading (16 bits big endian)
 * 0x3F 63 ACCEL_ZOUT			- Accelerometer Z axis reading (16 bits big endian)
**/
public class MPU9250Accelerometer extends Sensor3D  {
    protected MPU9250RegisterOperations ro;
    protected MPU9250 parent;
	private AccScale accelScale ;
	private final short accelSensitivity = 16384;  // = 16384 LSB/g
	//private A_DLPF aDLFP;

	MPU9250Accelerometer(int sampleSize, MPU9250RegisterOperations ro, MPU9250 parent)
	{
		super(sampleSize);
		accelScale = AccScale.AFS_4G;
		this.setDeviceScaling(new Data3f(	(float)accelScale.getRes(),
											(float)accelScale.getRes(),
											(float)accelScale.getRes()));
		this.ro = ro;
		this.parent = parent;
	}

	public void printState()
	{
		super.printState();
		System.out.println("accelScale: "+accelScale.toString()+ " minMax: "+accelScale.minMax+" Res: "+accelScale.getRes());
		System.out.println("accelSensitivity: "+accelSensitivity);
	}
	
	/**
	 * Prints the contents of registers used by this class 
	 */
	@Override
	public void printRegisters()
	{
	   	ro.printByteRegister(MPU9250Registers.ACCEL_CONFIG);
	   	ro.printByteRegister(MPU9250Registers.ACCEL_CONFIG2);
	   	ro.printByteRegister(MPU9250Registers.LP_ACCEL_ODR);
	   	ro.printByteRegister(MPU9250Registers.SELF_TEST_X_ACCEL);
	   	ro.printByteRegister(MPU9250Registers.SELF_TEST_Y_ACCEL);
	   	ro.printByteRegister(MPU9250Registers.SELF_TEST_Z_ACCEL);
	   	ro.printShort(MPU9250Registers.XA_OFFSET_H);
	   	ro.printShort(MPU9250Registers.YA_OFFSET_H);
	   	ro.printShort(MPU9250Registers.ZA_OFFSET_H);
	   	ro.printShort(MPU9250Registers.ACCEL_XOUT_H);
	   	ro.printShort(MPU9250Registers.ACCEL_YOUT_H);
	   	ro.printShort(MPU9250Registers.ACCEL_ZOUT_H);
	}

	public	AccScale getAccScale(){ return accelScale;}

	@Override
	public void updateData()
	{
         short registers[];
        //ro.readByteRegister(Registers.ACCEL_XOUT_H, 6);  // Read again to trigger
        registers = ro.readShorts(MPU9250Registers.ACCEL_XOUT_H,3);
        this.addValue(scale(new TimestampedData3f(registers[0],registers[1],registers[2])));
	}

	@Override
	public void configure() throws IOException, InterruptedException
	{
		SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"acc.configure");
        // Set accelerometer full-scale range configuration
		byte c;
        c = ro.readByte(MPU9250Registers.ACCEL_CONFIG); // get current ACCEL_CONFIG register value
        c = (byte)(c & ~AccSelfTest.bitmask); // Clear self-test bits [7:5] ####
        c = (byte)(c & ~AccScale.bitMask);  // Clear AFS bit 3 and bits 2:0
        c = (byte)(c | accelScale.bits ); // Set full scale range for the accelerometer #### does not require shifting!!!!
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG, c); // Write new ACCEL_CONFIG register value

        // Set accelerometer sample rate configuration
        // It is possible to get a 4 kHz sample rate from the accelerometer by choosing 1 for
        // accel_fchoice_b bit [3]; in this case the bandwidth is 1.13 kHz
        
        c = ro.readByte(MPU9250Registers.ACCEL_CONFIG2); // get current ACCEL_CONFIG2 register value
        c = (byte)(c & ~A_DLPF.bitMask); // Clear accel_fchoice_b (bit 3) and A_DLPFG (bits [2:0]) ### this should be bits 3:2 & 1:0 but all bottom 4 bits are cleared!!!
        c = (byte)(c | A_DLPF.F1BW0044_3.bits);  // Set accelerometer rate to 1 kHz and bandwidth to 44.8 Hz  
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG2, c); // Write new ACCEL_CONFIG2 register value

        if (debugLevel() >=3) printState();
        SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"End acc.configure");
	}
	
	@Override
	public void selfTest() throws InterruptedException 
	{
		SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"acc.selfTest");        
		byte FS = 0; 
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG, (byte)(	AccSelfTest.NONE.bits |	// no self test
        														AccScale.AFS_2G.bits));	// Set full scale range for the accelerometer to 2 g 
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG2, (byte)(	A_DLPF.F1BW0099_2.bits ));	// Set accelerometer rate to 1 kHz and bandwidth to 99 Hz
        final int TEST_LENGTH = 200;

        int[] aSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        short[] registers; 
        for(int s=0; s<TEST_LENGTH; s++)
        {
            registers = ro.readShorts(MPU9250Registers.ACCEL_XOUT_H,3);
            aSum[0] += registers[0];
            aSum[1] += registers[1];
            aSum[2] += registers[2];
            if (debugLevel() >=8)
            {
            	System.out.format("ST acc value added [%d, %d, %d] [0x%X, 0x%X, 0x%X]%n",
        			registers[0],registers[1],registers[2],registers[0],registers[1],registers[2]);
            }
        }
        short[] aAvg = new short[] {0,0,0};
        for(int i = 0; i<3; i++)
        {
            aAvg[i] = (short) ((short)((aSum[i]/TEST_LENGTH) & (short)0xFFFF)); //average and mask off top bits
        }

        if (debugLevel() >=5) System.out.print("aAvg average: "+Arrays.toString(aAvg));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X]%n", aAvg[0], aAvg[1], aAvg[2]);
        
        // Configure the accelerometer for self-test
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG, (byte)(	AccSelfTest.XYZ.bits |	// Enable self test all axes
        														AccScale.AFS_2G.bits)); // Set accelerometer range to +/- 2 g
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG2, (byte)(	A_DLPF.F1BW0099_2.bits ));	// Set accelerometer rate to 1 kHz and bandwidth to 99 Hz
        Thread.sleep(25); // Delay a while to let the device stabilise
        //outputConfigRegisters();
        int[] aSelfTestSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        
        // get average self-test values of accelerometer
        for(int s=0; s<TEST_LENGTH; s++) 
        {
            registers = ro.readShorts(MPU9250Registers.ACCEL_XOUT_H,3);
            aSelfTestSum[0] += registers[0];
            aSelfTestSum[1] += registers[1];
            aSelfTestSum[2] += registers[2];
        }
        
        short[] aSTAvg = new short[] {0,0,0};

        for(int i = 0; i<3; i++)
        {
            aSTAvg[i] = (short) ((short)(aSelfTestSum[i]/TEST_LENGTH) & (short)0xFFFF); //average and mask off top bits
        }
        if (debugLevel() >=5)  System.out.print("aSTAvg average: "+Arrays.toString(aSTAvg));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X]%n", aSTAvg[0], aSTAvg[1], aSTAvg[2]);


        // Calculate Accelerometer accuracy
        short[] selfTestAccel = new short[3]; //Longer than byte to allow for removal of sign bit as this is unsigned
        selfTestAccel[0] = (short)((short)ro.readByte(MPU9250Registers.SELF_TEST_X_ACCEL) & 0xFF);
        selfTestAccel[1] = (short)((short)ro.readByte(MPU9250Registers.SELF_TEST_Y_ACCEL) & 0xFF);
        selfTestAccel[2] = (short)((short)ro.readByte(MPU9250Registers.SELF_TEST_Z_ACCEL) & 0xFF);
        if (debugLevel() >=5) System.out.print("Self test Accel bytes: "+Arrays.toString(selfTestAccel));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X]%n", selfTestAccel[0], selfTestAccel[1], selfTestAccel[2]);
        
        float[] factoryTrimAccel = new float[3];
        factoryTrimAccel[0] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[0] - 1f);
        factoryTrimAccel[1] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[1] - 1f);
        factoryTrimAccel[2] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[2] - 1f);
        if (debugLevel() >=5) System.out.println("factoryTrimAcc (float): "+Arrays.toString(factoryTrimAccel)); 

        float[] AccuracyAccel = new float[3];
        AccuracyAccel[0] = 100f*(((float)(aSTAvg[0] - aAvg[0]))/factoryTrimAccel[0]-1f);
        AccuracyAccel[1] = 100f*(((float)(aSTAvg[1] - aAvg[1]))/factoryTrimAccel[1]-1f);
        AccuracyAccel[2] = 100f*(((float)(aSTAvg[2] - aAvg[2]))/factoryTrimAccel[2]-1f);
        
        if (debugLevel() >=2)
        {
        	System.out.println("Accelerometer accuracy:(% away from factory values)");
        	System.out.println("x: " + AccuracyAccel[0] + "%");
        	System.out.println("y: " + AccuracyAccel[1] + "%");
        	System.out.println("z: " + AccuracyAccel[2] + "%");
        }
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG, (byte)(	AccSelfTest.NONE.bits |	// no self test
																AccScale.AFS_2G.bits));	// Set scale range for the accelerometer to 2 g 
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG2, (byte)(	A_DLPF.F1BW0099_2.bits ));	// Set accelerometer rate to 1 kHz and bandwidth to 99 Hz
        Thread.sleep(25); // Delay a while to let the device stabilise
        if (debugLevel() >=3) printState();
        SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"End acc.selfTest");
	}
	
	@Override
	public void calibrate() throws InterruptedException
	{
		// part of accelgyrocalMPU9250 in Kris Winer code - this code is only the Accelerometer elements
		SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"accel.calibrate");
		if (debugLevel() >=5) System.out.println("Scaling: "+getDeviceScaling().toString());
		if (debugLevel() >=5)  System.out.println("Bias: "+getDeviceBias().toString());

    	// Assumes we are in calibration bits via setCalibrationMode9250();

        // Configure MPU6050 accelerometer for bias calculation
        ro.writeByte(MPU9250Registers.ACCEL_CONFIG,(byte) AccScale.AFS_16G.bits); 		// Set accelerometer full-scale to 16 g, maximum sensitivity
        short[] readings = parent.operateFIFO(FIFO_Mode.ACC,40); //get a set of readings via the FIFO (MCU9250 function)
        int readingCount = readings.length;
        if (debugLevel() >=5) System.out.println("Readings length: " + readingCount);

        int sampleCount =  readingCount / 3; // 6 bytes per sample 3 x 16 bit values
        int[] accelBiasSum = new int[]{0,0,0}; //32 bit to allow for accumulation without overflow
        for(int s = 0; s < sampleCount; s++) //#KW L962
        {
            accelBiasSum[0] += readings[s];		//#KW L972
            accelBiasSum[1] += readings[s+1]; 	// Sum individual signed 16-bit biases to get accumulated signed 32-bit biases
            accelBiasSum[2] += readings[s+2];
        }
        if (debugLevel() >=5)  System.out.print("Accel Bias sum: "+Arrays.toString(accelBiasSum));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X]%n",accelBiasSum[0],accelBiasSum[1],accelBiasSum[2]);
        
        //calculate averages
        short[] accelBiasAvg = new short[]{0,0,0}; //16 bit average
        accelBiasAvg[0] = (short)((accelBiasSum[0] / sampleCount) & 0xffff); // #KW L980
        accelBiasAvg[1] = (short)((accelBiasSum[1] / sampleCount) & 0xffff); // Normalise sums to get average count biases
        accelBiasAvg[2] = (short)((accelBiasSum[2] / sampleCount) & 0xffff); 
        
        if (accelBiasAvg[2] > 0) accelBiasAvg[2] -= accelSensitivity; // #KW 987 Remove gravity from the z-axis accelerometer bias calculation
        else accelBiasAvg[2] += accelSensitivity;
        
        if (debugLevel() >=5)
        {
        	System.out.print("Accel sample count: " + sampleCount);
        	System.out.print("Accel bias average: " + Arrays.toString(accelBiasAvg));
        	System.out.format(" [0x%X, 0x%X, 0x%X]%n",accelBiasAvg[0],accelBiasAvg[1],accelBiasAvg[2]);
        }
    	
        //setHardwareBiases(accelBiasAvg); //doesn't work
        // set super class NineDOF variables
        this.setDeviceBias(new Data3f( 	(float)accelBiasAvg[0]/2.0f/(float)accelSensitivity,
        								(float)accelBiasAvg[1]/2.0f/(float)accelSensitivity,
        								(float)accelBiasAvg[2]/2.0f/(float)accelSensitivity));
							
        if (debugLevel() >=3) printState();
        SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERFACE_METHODS,"End accel.calibrate");
	}
    public void setHardwareBiases(short[] biasAvg)
    {
        // Construct the accelerometer biases for push to the hardware accelerometer bias registers. These registers contain
        // factory trim values which must be added to the calculated accelerometer biases; on boot up these registers will hold
        // non-zero values. In addition, bit 0 of the lower byte must be preserved since it is used for temperature
        // compensation calculations. Accelerometer bias registers expect bias input as 2048 LSB per g, so that
        // the accelerometer biases calculated above must be divided by 8.
        // XA_OFFSET is a 15 bit quantity with bits 14:7 in the high byte and 6:0 in the low byte with temperature compensation in bit0
        // so having got it in a 16 bit short, and having preserved the bottom bit, the number must be shifted right by 1 or divide by 2
        // to give the correct value for calculations. After calculations it must be shifted left by 1 or multiplied by 2 to get
        // the bytes correct, then the preserved bit0 can be put back before the bytes are written to registers
    	SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERNAL_METHODS,"setAccelerometerBiases");

        
        if(biasAvg[2] > 0) {biasAvg[2] -= this.accelSensitivity;}  // Remove gravity from the z-axis accelerometer bias calculation
        else {biasAvg[2] += this.accelSensitivity;}
    	System.out.format("z adjusted for gravity %d 0x%X%n",biasAvg[2],biasAvg[2]);
       
        short[] accelBiasReg = ro.readShorts( MPU9250Registers.XA_OFFSET_H, 3);
        if (debugLevel() >=5) System.out.print("accelBiasReg with temp compensation bit: "+Arrays.toString(accelBiasReg));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X] %n",accelBiasReg[0],accelBiasReg[1],accelBiasReg[2]);

        short mask = 0x0001; // Define mask for temperature compensation bit 0 of lower byte of accelerometer bias registers
        byte[] mask_bit = new byte[]{0, 0, 0}; // Define array to hold mask bit for each accelerometer bias axis

        for(int s = 0; s < 3; s++) {
            if((accelBiasReg[s] & mask)==1) mask_bit[s] = 0x01; // If temperature compensation bit is set, record that fact in mask_bit
            //divide accelBiasReg by 2 to remove the bottom bit and preserve any sign (java has no unsigned 16 bit numbers)
            accelBiasReg[s] /=2;
        }
        if (debugLevel() >=5)
        {
        	System.out.println("Temperature bits"+Arrays.toString(mask_bit));
        	System.out.print("accelBiasReg without temp compensation bit: "+Arrays.toString(accelBiasReg));
        	System.out.format(" [0x%X, 0x%X, 0x%X] %n",accelBiasReg[0],accelBiasReg[1],accelBiasReg[2]);
        }
        
        // Construct total accelerometer bias, including calculated average accelerometer bias from above
        for (int i = 0; i<3; i++)
        {
        	//Subtract calculated averaged accelerometer bias scaled to 2048 LSB/g (16 g full scale)
        	//multiply by two to leave the bottom bit clear and but all the bits in the correct bytes
        	//Add back the temperature compensation bit
        	accelBiasReg[i] = (short)((accelBiasReg[i] - biasAvg[i]/8)*2+mask_bit[0]);
        }
        if (debugLevel() >=5) System.out.print("(accelBiasReg - biasAvg/8)*2 + TCbit (16bit): "+Arrays.toString(accelBiasReg));
        if (debugLevel() >=5) System.out.format(" [0x%X, 0x%X, 0x%X] %n",accelBiasReg[0],accelBiasReg[1],accelBiasReg[2]);
    	
        // Push accelerometer biases to hardware registers  	
        ro.writeShort(MPU9250Registers.XA_OFFSET_H, accelBiasReg[0]);
        ro.writeShort(MPU9250Registers.YA_OFFSET_H, accelBiasReg[1]);
        ro.writeShort(MPU9250Registers.ZA_OFFSET_H, accelBiasReg[2]);
        
        SystemLog.log(SubSystem.SubSystemType.INSTRUMENTS,SystemLog.LogLevel.TRACE_INTERNAL_METHODS,"End setAccelerometerBiases");
    }
}