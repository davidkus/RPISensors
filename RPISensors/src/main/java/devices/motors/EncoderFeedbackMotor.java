package devices.motors;

import devices.controller.PIDController;

/**
 * RPISensors - devices.motors
 * Created by MAWood on 23/12/2016.
 */
public class EncoderFeedbackMotor implements Motor
{
    private final Encoder encoder;
    private final Motor motor;
    private final PIDController PID;

    private final double kp;
    private final double ki;
    private final double kd;

    private final float sampleRate;

    private final boolean debug;

    public EncoderFeedbackMotor(Encoder encoder, Motor motor, double kp, double ki, double kd, float sampleRate, boolean reversed)
    {
        this(encoder, motor, kp, ki, kd, sampleRate, reversed, false);
    }

    @SuppressWarnings("WeakerAccess")
    public EncoderFeedbackMotor(Encoder encoder, Motor motor, double kp, double ki, double kd, float sampleRate, boolean reversed, boolean debug)
    {
        this.encoder = encoder;
        this.motor = motor;

        this.kp = kp;
        this.ki = ki;
        this.kd = kd;

        this.sampleRate = sampleRate;

        this.debug = debug;

        PID = new PIDController(reversed,0,this.sampleRate,this.kp,this.ki,this.kd,-1,1, PIDController.OperatingMode.AUTOMATIC, debug);//, true);
        PID.setInputProvider(this.encoder);
        PID.addOutputListener(this.motor);

        PID.initialise();
    }

    @Override
    public void setOutput(float speed)
    {
        if(debug) System.out.println("new setpoint: " + speed);
        PID.setOperatingMode(PIDController.OperatingMode.AUTOMATIC);
        PID.setSetPoint(speed);
    }

    @Override
    public float getSpeed()
    {
        return (float)PID.getSetPoint();
    }

    @Override
    public void stop()
    {
        PID.setSetPoint(0);
        PID.setOperatingMode(PIDController.OperatingMode.MANUAL);
        motor.stop();
    }
}
