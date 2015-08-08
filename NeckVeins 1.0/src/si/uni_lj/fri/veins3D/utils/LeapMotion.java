package si.uni_lj.fri.veins3D.utils;

import java.io.IOException;
import java.lang.Math;
import java.util.LinkedList;

import si.uni_lj.fri.veins3D.gui.settings.NeckVeinsSettings;

import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

public class LeapMotion {
	Controller leapController;
	
	float[] translations;
	double[] rotations; // pitch / roll / yaw
	
	private NeckVeinsSettings settings;
	
	private Vector startPosition=new Vector(0, 0, 0);
	private Vector rotationPosition=new Vector(0,0,0);
	
	private LimitedQueue<Vector> positionAverage = new LimitedQueue<Vector>(20);
	private LimitedQueue<Vector> angleAverage = new LimitedQueue<Vector>(20);
	
	private boolean openPalm=true;
	
	public LeapMotion(NeckVeinsSettings settings){
		this.settings = settings;
        leapController = new Controller();
        try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if(leapController.isConnected()) System.out.println("Leap Motion device connected");
        else System.err.println("Leap Motion was not found");
        
        translations=new float[3];
        rotations=new double[3];
	}
	
	private class LimitedQueue<E> extends LinkedList<E> {
        private int limit;
        public LimitedQueue(int limit) {
            this.limit = limit;
        }
 
        @Override
        public boolean add(E o) {
            super.add(o);
            while (size() > limit) { super.remove(); }
            return true;
        }
    }
	
	private Vector average(LimitedQueue<Vector> vectors)
    {
        float x=0f, y=0f, z=0f;
        for(Vector v:vectors){
            x=x+v.getX(); y=y+v.getY(); z=z+v.getZ();
        }
        return new Vector(x/vectors.size(), y/vectors.size(), z/vectors.size());
    }
	
    public void poll() {
        // Get the most recent frame and report some basic information
        Frame frame = leapController.frame();
        
        if (!frame.hands().isEmpty()) {
            // Get the first hand
            Hand hand = frame.hands().get(0);
            
            // Get the hand's normal vector and direction
            Vector normal = hand.palmNormal();
            Vector direction = hand.direction();

            Vector handRotation=new Vector((float)Math.toDegrees(direction.pitch()), (float)Math.toDegrees(normal.roll()), (float)Math.toDegrees(direction.yaw()));
            angleAverage.add(handRotation);
            Vector averageHandRotation=average(angleAverage);
            
            Vector handPostion=hand.palmPosition();
            positionAverage.add(handPostion);
            Vector averageHandPostion=average(positionAverage);
            
            //System.out.println("========\n Translation: "+averageHandPostion.toString()+" \n Rotation: "+averageHandRotation.toString()+"\n");
            //axis=(relativePosition.minus(startPosition).plus(averageHandPostion)).toFloatArray();
                   
            //If grab is detected
            if(openPalm && hand.grabStrength()>0.9f){
            	openPalm=false;
            	startPosition=handPostion;
            	rotationPosition=handRotation;
            	
            	//System.out.println("Grabbed at "+startPosition.toString());
            }
            // If plam is opened
            if(!openPalm && hand.grabStrength()<=0.9f){
            	openPalm=true; 
            	//System.out.println("Palm opend");
            }
            
            
            if(!openPalm){
            	translations=startPosition.minus(handPostion).toFloatArray();
      			Vector relativeRotation=rotationPosition.minus(handRotation);
            	
      			//System.out.println(relativeRotation);

      			rotations[0]=-relativeRotation.getX()/(double)settings.leapSensitivity;
            	rotations[1]=-relativeRotation.getY()/(double)settings.leapSensitivity;
				rotations[2]=-relativeRotation.getZ()/(double)settings.leapSensitivity;
            	
            	startPosition=handPostion;
            	rotationPosition=handRotation;
            }
            else{
            	translations[0]=0;
    			translations[1]=0;
				translations[2]=0;
            	
				rotations[0]=0;
				rotations[1]=0;
				rotations[2]=0;
            }
        }

    }
    public float[] getAxisTranslations(){
  	   	return translations;
    }
    public double[] getAxisRotations(){
    	return rotations;
    }
    public boolean isPalm(){
    	return openPalm;
    }
    public boolean isConnected(){
    	return leapController.isConnected();
    }

}