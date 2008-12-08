import java.util.*;

public class JRConnectionManager implements TuioListener {

	private JRTree jrTree;
	private Hashtable<Long,JRTuioObject> objectList = new Hashtable<Long,JRTuioObject>();
	private Hashtable<Long,TuioCursor> cursorList = new Hashtable<Long,TuioCursor>();
	
	public JRConnectionManager ( JRTree jrTree ) {
		this.jrTree = jrTree;
	}
	
	public void addTuioObject(TuioObject tobj) {
		int fiducialID = tobj.getFiducialID();
		long sessionID = tobj.getSessionID();
	
		// update object list
		objectList.put ( sessionID, new JRTuioObject(tobj) );
		
		System.out.println("add obj "+fiducialID+" ("+sessionID+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle());	
		// if the tree has no head, set head
		if ( jrTree.getHead() == null ) {
			try { jrTree.setHead( JRNode.getInstance ( fiducialID ) ); }
			catch (JRException e) { System.err.println(e.getMessage()); }
		}
		else {
			// recreate tree based on proximity rules
			System.out.println("recreate tree based on proximity rules");
		}
	}

	public void updateTuioObject(TuioObject tobj) {
		JRTuioObject demo = (JRTuioObject)objectList.get(tobj.getSessionID());
		demo.update(tobj);
		System.out.println("set obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle()+" "+tobj.getMotionSpeed()+" "+tobj.getRotationSpeed()+" "+tobj.getMotionAccel()+" "+tobj.getRotationAccel());
		// update the properties of the appropriate node in the tree (use mutators)
		// acquire reference to JRNode by either:
			// 1. traverse the tree looking for a session id (but traversal is cpu expensive)
			// 2. keep a reference to each node in this connection manager
	}
	
	public void removeTuioObject(TuioObject tobj) {
		objectList.remove(tobj.getSessionID());
		System.out.println("del obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+")");
		// if there is no corresponding node in the tree
			// throw an exception
		// else
			// recreate tree based on proximity rules
			System.out.println("recreate tree based on proximity rules");
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
