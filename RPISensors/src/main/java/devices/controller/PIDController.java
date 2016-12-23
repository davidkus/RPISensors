package devices.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * RPISensors - devices
 * Created by MAWood on 04/12/2016.
 */
public class PIDController extends Thread
{
    public enum OperatingMode
    {
        AUTOMATIC,
        MANUAL
    }

    private Instant lastTime;
    private double input, output, setpoint;
    private double ITerm, lastInput;
    private double kp, ki, kd;
    private double sampleRate;
    private double outMin, outMax;
    private OperatingMode mode;
    private List<PIDControlled> controlledOutputs;
    private PIDInputProvider inputProvider;
    private final boolean debug;

    public PIDController(double setPoint, double sampleRate, double kp, double ki, double kd, double outMin, double outMax, OperatingMode mode)
    {
        this(setPoint,sampleRate,kp,ki,kd,outMin,outMax,mode,false);
    }

    public PIDController(double setPoint, double sampleRate, double kp, double ki, double kd, double outMin, double outMax, OperatingMode mode, boolean debug)
    {
        controlledOutputs = new ArrayList<>();
        this.setpoint = setPoint;
        this.sampleRate = sampleRate;
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.outMin = outMin;
        this.outMax = outMax;
        this.mode = mode;
        this.input = 0;
        this.output = 0;
        this.debug = debug;
    }

    public void initialise()
    {
        lastInput = input;
        ITerm = output;
        if(ITerm> outMax) ITerm= outMax;
        else if(ITerm< outMin) ITerm= outMin;
        this.start();
    }

    @Override
    public void run()
    {
        lastTime = Instant.now();
        super.run();
        while(!Thread.interrupted())
        {
            //System.out.println("Thread running");
            if(ChronoUnit.MILLIS.between(lastTime, Instant.now()) > (1000*(1/sampleRate)))
            {
                compute();
            } else try
                {
                    Thread.sleep((long)Math.floor(100*(1/sampleRate)));
                } catch (InterruptedException ignored) {}
        }
    }

    void compute()
    {
        if(mode != OperatingMode.AUTOMATIC) return;
        if(inputProvider == null) return;
        if(controlledOutputs.isEmpty()) return;

        input = inputProvider.getInput();
        //System.out.println("Fetched input: " + input);

        Instant now = Instant.now();

   /*Compute all the working error variables*/
        double error = setpoint - input;
        ITerm += ki * error;
        if(ITerm> outMax) ITerm= outMax;
        else if(ITerm< outMin) ITerm= outMin;
        double dInput = error - lastInput;

   /*Compute PID output*/
        output = kp * error + ITerm - kd * dInput;
        //System.out.println("output calc: kp =" + kp + " error =" + error + " ITerm =" + ITerm + " kd =" + kd + " dInput =" + dInput);
        if(output > outMax) output = outMax;
        else if(output < outMin) output = outMin;

   /*Remember some variables for next time*/
        lastInput = input;
        lastTime = now;

        alertOutputs();
    }

    private void alertOutputs()
    {
        System.out.println(setpoint + "," + input + "," + output/2f);
        for(PIDControlled controlledOutput: controlledOutputs) controlledOutput.setOutput((float)this.output);
    }

    void setTunings(double Kp, double Ki, double Kd)
    {
        double sampleTime = 1/sampleRate;
        kp = Kp;
        ki = Ki * sampleTime;
        kd = Kd / sampleTime;
    }
    void setSampleRate(double newSampleRate)
    {
        if (newSampleRate <= 0) return;
        double ratio = sampleRate/newSampleRate;
        ki *= ratio;
        kd /= ratio;
        sampleRate = newSampleRate;
    }

    void setOutputLimits(double Min, double Max)
    {
        if(Min > Max) return;
        outMin = Min;
        outMax = Max;

        if(output > outMax) output = outMax;
        else if(output < outMin) output = outMin;

        if(ITerm> outMax) ITerm= outMax;
        else if(ITerm< outMin) ITerm= outMin;
    }

    public void setOperatingMode(OperatingMode mode)
    {
        if(mode == OperatingMode.AUTOMATIC && this.mode != OperatingMode.AUTOMATIC) initialise();
        if(mode == OperatingMode.MANUAL && this.mode != OperatingMode.MANUAL) Thread.interrupted();
        this.mode = mode;
    }

    public void addOutputListener(PIDControlled controlledOutput)
    {
        this.controlledOutputs.add(controlledOutput);
    }

    public void setInputProvider(PIDInputProvider inputProvider)
    {
        this.inputProvider = inputProvider;
    }

    public double getOutput()
    {
        return output;
    }

    public double getSetpoint()
    {
        return setpoint;
    }

    public void setSetpoint(double setpoint)
    {
        this.setpoint = setpoint;
    }
}