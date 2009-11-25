import com.illposed.osc.*;
import java.util.*;

public class JRTuioClient implements OSCListener {
	
	private final int UNDEFINED = -1;
	
	private int port = 3333;
	private OSCPortIn oscPort;
	private Vector<TuioListener> listenerList;
	
	private Hashtable<Long,TuioObject> objectList;
	private Vector<Long> aliveObjectList;
	private Vector<Long> newObjectList;
	private Hashtable<Long,TuioCursor> cursorList;
	private Vector<Long> aliveCursorList;
	private Vector<Long> newCursorList;
	private Vector<TuioCursor> freeCursorList;
	
	private int maxFingerID = -1;
	private long currentFrame = 0;
	private long lastFrame = 0;
	private long startTime = 0;
	private long lastTime = 0;

	public JRTuioClient ( ) {
		this(3333);
	}
	
	public JRTuioClient ( int port ) {
		this.port = port;
		this.objectList = new Hashtable<Long,TuioObject>();
		this.aliveObjectList = new Vector<Long>();
		this.newObjectList = new Vector<Long>();
		this.cursorList = new Hashtable<Long,TuioCursor>();
		this.aliveCursorList = new Vector<Long>();
		this.newCursorList = new Vector<Long>();
		this.freeCursorList = new Vector<TuioCursor>();
		this.listenerList = new Vector<TuioListener>();
	}
	
	public void connect() {
		try {
			oscPort = new OSCPortIn(port);
			oscPort.addListener("/tuio/2Dobj",this);
			oscPort.addListener("/tuio/2Dcur",this);
			oscPort.startListening();
			startTime = System.currentTimeMillis();
		} catch (Exception e) {
			System.err.println("ERROR: JRTuioClient: failed to connect to port "+port);
		}
	}
	
	public void disconnect() {
		System.out.println("DEBUG: JRTuioClient: disconnect() begin");
		oscPort.stopListening();
		try { Thread.sleep(100); }
		catch (Exception e) {};
		oscPort.close();
		System.out.println("DEBUG: JRTuioClient: disconnect() end");
	}
	
	public void addTuioListener(TuioListener listener) {
		listenerList.addElement(listener);
	}
	
	public void removeTuioListener(TuioListener listener) {	
		listenerList.removeElement(listener);
	}

	public Vector<TuioObject> getTuioObjects() {
		return new Vector<TuioObject>(objectList.values());
	}
	
	public Vector<TuioCursor> getTuioCursors() {
		return new Vector<TuioCursor>(cursorList.values());
	}	

	public TuioObject getTuioObject(long s_id) {
		return (TuioObject)objectList.get(new Long(s_id));
	}
	
	public TuioCursor getTuioCursor(long s_id) {
		return (TuioCursor)cursorList.get(new Long(s_id));
	}	

	public void acceptMessage(Date date, OSCMessage message) {
	
		Object[] args = message.getArguments();
		String command = (String)args[0];
		String address = message.getAddress();

		
		if (address.equals("/tuio/2Dobj")) {

			if (command.equals("set")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
				long s_id  = ((Integer)args[1]).longValue();
				int f_id  = ((Integer)args[2]).intValue();
				float xpos = ((Float)args[3]).floatValue();
				float ypos = ((Float)args[4]).floatValue();
				float angle = ((Float)args[5]).floatValue();
				float xspeed = ((Float)args[6]).floatValue();
				float yspeed = ((Float)args[7]).floatValue();
				float rspeed = ((Float)args[8]).floatValue();
				float maccel = ((Float)args[9]).floatValue();
				float raccel = ((Float)args[10]).floatValue();
				
				if (objectList.get(s_id) == null) {
				
					TuioObject addObject = new TuioObject(s_id,f_id,xpos,ypos,angle);
					objectList.put(s_id,addObject);
					
					for (int i=0;i<listenerList.size();i++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(i);
						if (listener!=null) listener.addTuioObject(addObject);
					}
				} else {
				
					TuioObject updateObject = (TuioObject)objectList.get(s_id);
					if ((updateObject.xpos!=xpos) || (updateObject.ypos!=ypos) || (updateObject.angle!=angle) || (updateObject.x_speed!=xspeed) || (updateObject.y_speed!=yspeed) || (updateObject.rotation_speed!=rspeed) || (updateObject.motion_accel!=maccel) || (updateObject.rotation_accel!=raccel)) {
						updateObject.update(xpos,ypos,angle,xspeed,yspeed,rspeed,maccel,raccel);
						for (int i=0;i<listenerList.size();i++) {
							TuioListener listener = (TuioListener)listenerList.elementAt(i);
							if (listener!=null) listener.updateTuioObject(updateObject);
						}
					}
				
				}
	
				//System.out.println("set obj " +s_id+" "+f_id+" "+xpos+" "+ypos+" "+angle+" "+xspeed+" "+yspeed+" "+rspeed+" "+maccel+" "+raccel);
				
			} else if (command.equals("alive")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
	
				for (int i=1;i<args.length;i++) {
					// get the message content
					long s_id = ((Integer)args[i]).longValue();
					newObjectList.addElement(s_id);
					// reduce the object list to the lost objects
					if (aliveObjectList.contains(s_id))
						 aliveObjectList.removeElement(s_id);
				}
				
				// remove the remaining objects
				for (int i=0;i<aliveObjectList.size();i++) {
					TuioObject removeObject = (TuioObject)objectList.remove(aliveObjectList.elementAt(i));
					if (removeObject==null) continue;
					removeObject.remove();
					//System.out.println("remove "+id);
					for (int j=0;j<listenerList.size();j++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(j);
						if (listener!=null) listener.removeTuioObject(removeObject);
					}
				}
				
				Vector<Long> buffer = aliveObjectList;
				aliveObjectList = newObjectList;
				
				// recycling of the vector
				newObjectList = buffer;
				newObjectList.clear();
					
			} else if (command.equals("fseq")) {
				if (currentFrame>=0) lastFrame = currentFrame;
				currentFrame = ((Integer)args[1]).intValue();
				
				if ((currentFrame>=lastFrame) || (currentFrame<0)) {
					
					long currentTime = lastTime;
					if (currentFrame>lastFrame) {
						currentTime = System.currentTimeMillis()-startTime;
						lastTime = currentTime;
					}

					Enumeration<TuioObject> refreshList = objectList.elements();					
					while(refreshList.hasMoreElements()) {
						TuioObject refreshObject = refreshList.nextElement();
						if (refreshObject.getUpdateTime()==UNDEFINED) refreshObject.setUpdateTime(currentTime);
					}
					
					for (int i=0;i<listenerList.size();i++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(i);
						if (listener!=null) listener.refresh(currentTime);
					}
				}
			}

		} else if (address.equals("/tuio/2Dcur")) {

			if (command.equals("set")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;

				long s_id  = ((Integer)args[1]).longValue();
				float xpos = ((Float)args[2]).floatValue();
				float ypos = ((Float)args[3]).floatValue();
				float xspeed = ((Float)args[4]).floatValue();
				float yspeed = ((Float)args[5]).floatValue();
				float maccel = ((Float)args[6]).floatValue();
				
				if (cursorList.get(s_id) == null) {
				
					int f_id = cursorList.size();
					if (cursorList.size()<=maxFingerID) {
						TuioCursor closestCursor = freeCursorList.firstElement();
						Enumeration<TuioCursor> testList = freeCursorList.elements();
						while (testList.hasMoreElements()) {
							TuioCursor testCursor = testList.nextElement();
							if (testCursor.getDistance(xpos,ypos)<closestCursor.getDistance(xpos,ypos)) closestCursor = testCursor;
						}
						f_id = closestCursor.getFingerID();
						freeCursorList.removeElement(closestCursor);
					} else maxFingerID = f_id;		
					
					TuioCursor addCursor = new TuioCursor(s_id,f_id,xpos,ypos);
					cursorList.put(s_id,addCursor);

					for (int i=0;i<listenerList.size();i++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(i);
						if (listener!=null) listener.addTuioCursor(addCursor);
					}
				} else {
				
					TuioCursor updateCursor = (TuioCursor)cursorList.get(s_id);
					if ((updateCursor.xpos!=xpos) || (updateCursor.ypos!=ypos) || (updateCursor.x_speed!=xspeed) || (updateCursor.y_speed!=yspeed) || (updateCursor.motion_accel!=maccel)) {

						updateCursor.update(xpos,ypos,xspeed,yspeed,maccel);
						for (int i=0;i<listenerList.size();i++) {
							TuioListener listener = (TuioListener)listenerList.elementAt(i);
							if (listener!=null) listener.updateTuioCursor(updateCursor);
						}
					}
				}
				
				//System.out.println("set cur " + s_id+" "+xpos+" "+ypos+" "+xspeed+" "+yspeed+" "+maccel);
				
			} else if (command.equals("alive")) {
				if ((currentFrame<lastFrame) && (currentFrame>0)) return;
	
				for (int i=1;i<args.length;i++) {
					// get the message content
					long s_id = ((Integer)args[i]).longValue();
					newCursorList.addElement(s_id);
					// reduce the cursor list to the lost cursors
					if (aliveCursorList.contains(s_id)) 
						aliveCursorList.removeElement(s_id);
				}
				
				// remove the remaining cursors
				for (int i=0;i<aliveCursorList.size();i++) {
					TuioCursor removeCursor = (TuioCursor)cursorList.remove(aliveCursorList.elementAt(i));
					if (removeCursor==null) continue;
					removeCursor.remove();
					
					if (removeCursor.finger_id==maxFingerID) {
						maxFingerID = -1;
						if (cursorList.size()>0) {
							Enumeration<TuioCursor> clist = cursorList.elements();
							while (clist.hasMoreElements()) {
								int f_id = clist.nextElement().finger_id;
								if (f_id>maxFingerID) maxFingerID=f_id;
							}
							
							Enumeration<TuioCursor> flist = freeCursorList.elements();
							while (flist.hasMoreElements()) {
								int c_id = flist.nextElement().getFingerID();
								if (c_id>=maxFingerID) freeCursorList.removeElement(c_id);
							}
						} 
					} else if (removeCursor.finger_id<maxFingerID) freeCursorList.addElement(removeCursor);
					
					//System.out.println("remove "+id);
					for (int j=0;j<listenerList.size();j++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(j);
						if (listener!=null) listener.removeTuioCursor(removeCursor);
					}
				}
				
				Vector<Long> buffer = aliveCursorList;
				aliveCursorList = newCursorList;
				
				// recycling of the vector
				newCursorList = buffer;
				newCursorList.clear();
			} else if (command.equals("fseq")) {
				if (currentFrame>=0) lastFrame = currentFrame;
				currentFrame = ((Integer)args[1]).intValue();
				
				if ((currentFrame>=lastFrame) || (currentFrame<0)) {
					long currentTime = lastTime;
					if (currentFrame>lastFrame) {
						currentTime = System.currentTimeMillis()-startTime;
						lastTime = currentTime;
					}

					Enumeration<TuioCursor> refreshList = cursorList.elements();					
					while(refreshList.hasMoreElements()) {
						TuioCursor refreshCursor = refreshList.nextElement();
						if (refreshCursor.getUpdateTime()==UNDEFINED) refreshCursor.setUpdateTime(currentTime);
					}
					
					for (int i=0;i<listenerList.size();i++) {
						TuioListener listener = (TuioListener)listenerList.elementAt(i);
						if (listener!=null) listener.refresh(currentTime);
					}
				}
			} 

		}
	}

}