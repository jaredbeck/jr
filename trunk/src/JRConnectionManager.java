import java.util.*;

public class JRConnectionManager implements TuioListener {

	private JRTree jrTree;
	private Hashtable<Long,JRTuioObject> objectList = new Hashtable<Long,JRTuioObject>();
	private Hashtable<Long,TuioCursor> cursorList = new Hashtable<Long,TuioCursor>();
	
	public JRConnectionManager ( JRTree jrTree ) {
		this.jrTree = jrTree;
	}
	
	public void addTuioObject(TuioObject tobj) {
		objectList.put ( tobj.getSessionID(), new JRTuioObject(tobj) );
		System.out.println("add obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle());	
	}

	public void updateTuioObject(TuioObject tobj) {
		JRTuioObject demo = (JRTuioObject)objectList.get(tobj.getSessionID());
		demo.update(tobj);
		System.out.println("set obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle()+" "+tobj.getMotionSpeed()+" "+tobj.getRotationSpeed()+" "+tobj.getMotionAccel()+" "+tobj.getRotationAccel()); 	
	}
	
	public void removeTuioObject(TuioObject tobj) {
		objectList.remove(tobj.getSessionID());
		System.out.println("del obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+")");	
	}

	public void addTuioCursor(TuioCursor tcur) {
	
		if (!cursorList.containsKey(tcur.getSessionID())) {
			cursorList.put(tcur.getSessionID(), tcur);
		}
		
		//System.out.println("add cur "+tcur.getFingerID()+" ("+tcur.getSessionID()+") "+tcur.getX()+" "+tcur.getY());	
	}

	public void updateTuioCursor(TuioCursor tcur) {
		//System.out.println("set cur "+tcur.getFingerID()+" ("+tcur.getSessionID()+") "+tcur.getX()+" "+tcur.getY()+" "+tcur.getMotionSpeed()+" "+tcur.getMotionAccel()); 
	}
	
	public void removeTuioCursor(TuioCursor tcur) {
		cursorList.remove(tcur.getSessionID());	
		//System.out.println("del cur "+tcur.getFingerID()+" ("+tcur.getSessionID()+")"); 
	}

	public void refresh(long timestamp) {
		//System.out.println("refresh()");
	}
		
}
