import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import sensors.*;

public class RoboSpyEngine {
	
	private boolean initialized = false;
	private boolean deployed;
	private RegulatedMotor m1, m2, md;
	private SimpleTouch t1, t4;
	private EV3TouchSensor ts1, ts4;
	private int speed;
	private SensorModes IRsensor;
	private Thread IRthread;
	private Sender sender;
	
	public RoboSpyEngine() {
		
		deployed = true;	// TODO: identify deploy status
		speed = 500;

		// sensors definition
		ts1 = new EV3TouchSensor(LocalEV3.get().getPort("S1"));
	    t1 = new SimpleTouch(ts1);
	    ts4 = new EV3TouchSensor(LocalEV3.get().getPort("S4"));
	    t4 = new SimpleTouch(ts4);
	    
	    IRsensor = new EV3IRSensor(LocalEV3.get().getPort("S2"));
	    
	    // motors definition
		m1 = new EV3LargeRegulatedMotor(MotorPort.A);
		m2 = new EV3LargeRegulatedMotor(MotorPort.D);
		md = new EV3MediumRegulatedMotor(MotorPort.B);
		m1.setSpeed(10);
		m2.setSpeed(10);
		md.setSpeed(500);		
		
	}
	
	public void setSender(Sender s) {
		sender = s;
	}
	
	public void dispatchMsg(String msg) {
		switch (msg) {

		case "stop": 		stopAllMotors(); break;
		
		case "deploy":		deploy(true); break;
		case "undeploy":	deploy(false); break;

		case "go_forw":		move("forw"); break;
		case "go_back":		move("back"); break;
		case "go_left":		move("left"); break;
		case "go_right":	move("right"); break;
	
		case "cam_up":		moveCamera("up"); break;
		case "cam_down":	moveCamera("down"); break;
		case "cam_left":	moveCamera("left");	break;
		case "cam_right":	moveCamera("right"); break;
		
		case "speed_1":		setSpeed(1); break;
		case "speed_2":		setSpeed(2); break;
		case "speed_3":		setSpeed(3); break;
		case "speed_4":		setSpeed(4); break;
		case "speed_5":		setSpeed(5); break;
		
		default: break;
		}
	}
	
	private void stopAllMotors() {
		m1.stop(true);
		m2.stop(true);
		md.flt();
	}
	
	public void radarStart() {
	      IRthread = new Thread("New Thread") {
	            public void run(){
	      	      SampleProvider distance= IRsensor.getMode("Distance");
	    	      float[] sample = new float[distance.sampleSize()];
	            	  
	            	while(true) { 
	            		distance.fetchSample(sample, 0);
	            		sender.sendClient("#d#" + sample[0]);
	            		Delay.msDelay(300);
	            	}	            	
	            }
	      };
	      IRthread.start();
	}
	
	public void radarStop() {
		IRthread.interrupt();
	}
	
	private void setSpeed(int newSpeed) {
		switch (newSpeed) {
			case 1: speed = 50; break;
			case 2: speed = 200; break;
			case 3: speed = 300; break;
			case 4: speed = 450; break;
			case 5: speed = 600; break;
			default: speed = 300;
		}
		if(!deployed) return;
		m1.setSpeed(speed);
		m2.setSpeed(speed);
	}
	
	private void move(String direction) {

		if(deployed) return; // not move while deployed 
		
		if(direction.equals("left") || direction.equals("right")) {
			m1.setSpeed(speed);
			m2.setSpeed(speed);
			if(direction.equals("left")) {
				m1.backward();
				m2.forward();
			}else {
				m1.forward();
				m2.backward();
			}
		}else {
			m1.setSpeed(speed);
			m2.setSpeed(speed);
			if(direction.equals("forw")) {
				m1.forward();
				m2.forward();
			}else {
				m1.backward();
				m2.backward();
			}
		}
	}

	private void moveCamera(String direction) {
		
		// TODO: improve left-right movement compensation
		
		if(!deployed) return; // move camera only if deployed
		
		if(direction.equals("left") || direction.equals("right")) {
			m1.setSpeed(25);
			m2.setSpeed(10);

			if(direction.equals("left")) {
				m2.backward();
				m1.backward();	// compensation
			}else {
				m2.forward();
				m1.forward();	// compensation
			}
		}else {
			m1.setSpeed(30);

			if(direction.equals("up")) {
				m1.forward();
			}else {
				m1.backward();
			}
		}
		
	}
	
	private void deploy(boolean state) {
		
		if(state) {
			if(initialized && deployed) return;
			
			md.forward();
			while (!t4.isPressed()) { Delay.msDelay(50); };
			md.flt();
			deployed = true;
		}else {
			if(initialized && !deployed) return;
			md.backward();
			while (!t1.isPressed()) { Delay.msDelay(50); };
			md.flt();
			deployed = false;
		}
		
		initialized = true;
	}
}
