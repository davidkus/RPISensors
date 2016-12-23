package main;

import com.pi4j.io.gpio.*;
import devices.driveAssembly.DriveAssembly;
import devices.driveAssembly.EncoderFeedbackPIDControlledDriveAssembly;
import devices.driveAssembly.TankDriveAssembly;
import devices.encoder.Encoder;
import devices.motors.DCMotor;
import devices.motors.Motor;

import java.util.concurrent.TimeUnit;

/**
 * RPISensors - main
 * Created by MAWood on 23/12/2016.
 */
public class PIDTest
{
    private final Encoder leftEncoder;
    private final Encoder rightEncoder;

    private final EncoderFeedbackPIDControlledDriveAssembly da;

    private PIDTest() throws InterruptedException
    {
        leftEncoder = new Encoder(RaspiPin.GPIO_13,RaspiPin.GPIO_14,"LH",427.5f);
        rightEncoder = new Encoder(RaspiPin.GPIO_10,RaspiPin.GPIO_11,"RH",427.5f);

        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalOutput RA =
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Right motor A", PinState.LOW);
        final GpioPinDigitalOutput RB =
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Right motor B", PinState.LOW);
        final GpioPinDigitalOutput LA =
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Left motor A", PinState.LOW);
        final GpioPinDigitalOutput LB =
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Left motor B", PinState.LOW);
        Motor left = new DCMotor(LA,LB);
        Motor right = new DCMotor(RA,RB);

        System.out.println("Set Point,Input,Output/2");

        da = new EncoderFeedbackPIDControlledDriveAssembly(left,right, leftEncoder,rightEncoder);

        da.setSpeed(0.4f);
        TimeUnit.SECONDS.sleep(5);
        da.setSpeed(0f);
        TimeUnit.SECONDS.sleep(5);
        da.stop();

        da.shutdown();
    }

    public static void main(String[] args) throws InterruptedException
    {
        new PIDTest();
    }
}