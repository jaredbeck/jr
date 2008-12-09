import java.util.*;

public class JRConnectionManager implements TuioListener {

	private JRTree jrTree;
	private Hashtable<Long,JRTuioObject> objectList = new Hashtable<Long,JRTuioObject>();
	private Hashtable<Long,TuioCursor> cursorList = new Hashtable<Long,TuioCursor>();
	
	public JRConnectionManager ( JRTree jrTree ) {
		this.jrTree = jrTree;
	}
	
	public void buildTree ( ) {
		// How many TUIO objects are on the table?
		int numTuioObjects = objectList.size();
		
		// For 0 or 1 TUIO objects, tree construction is trivial
		if ( numTuioObjects == 0 ) { jrTree.clear(); }
		else if ( numTuioObjects == 1) { 
			JRTuioObject tobj = (JRTuioObject)objectList.values().toArray()[0];
			int fiducialID = tobj.getFiducialID();
			try { jrTree.setHead( JRNode.getInstance ( fiducialID ) ); }
			catch (JRException e) { System.err.println(e.getMessage()); }
		}
	
		/* For more than one TUIO objects, tree construction follows a
		fairly simple algorithm based on object proximity. 
		
		First, the tree head is set to a new JRMixer()
		
		Then, while any objects remain input-satisfied, but
		output-un-satisfied, connect those outputs to the nearest
		appropriate inputs.  
		
		An objeect is input-satisfied when it can produce an output.  In
		the case of a filter, which has one audio input and one audio
		output, input-satisfaction means a connected audio input.

		In the case of a controller, which has one control output, and no
		inputs, input-satisfaction is immediate.

		A generator has one audio output and N control outputs.

		Thus, the first itteration will connect each controller's control
		output to the nearest control input.  The first itteration will
		also connect each generator's audio output to the nearest audio
		input.
		
		Itteration terminates when no objects are input-satisfied. This
		can result in objects with unconnected outputs.  This is not an
		error. 
		*/
		
		else {
			System.out.println("DEBUG: buildTree: Build non-trivial tree");
			jrTree.clear();
			AbstractSet nodes = getNodes();
			try { 
				jrTree.setHead( new JRMixer() );
				boolean continueItteration = true;
				while ( continueItteration ) {
					System.out.println("DEBUG: buildTree: Itterate");
					AbstractSet inputSatisfiedNodes = getInputSatisfiedNodes( nodes );
					System.out.println("DEBUG: buildTree:  There are " + inputSatisfiedNodes.size() + " input-satisfied nodes");
					/* Actually, you should only be concerned with nodes which
					are input-satisfied and output-unsatisfied.  Let's call
					these "eager" nodes. */
					
					/* Sort eager nodes by distance to the nearest appropriate
					input on the table.  For example, if one controller is
					distance 50 from the nearest control input, and another
					controller is distance 40, we want to create the shorter
					edge first. */
					throw new JRException( "Planned premature termination" );
				}
			}
			catch (JRException e) { 
				System.err.println( "ERROR: Tree construction failed: " + e.getMessage() );
				jrTree.clear();
			}
		}
	}
	
	private AbstractSet getInputSatisfiedNodes( AbstractSet nodes ) {
		HashSet<JRNode> satisfiedNodes = new HashSet<JRNode>();
		Iterator i = nodes.iterator();
		while ( i.hasNext() ) {
			JRNode n = (JRNode)i.next();
			if ( n.isInputSatisfied() ) { satisfiedNodes.add( n ); }
		}
		return satisfiedNodes;
	}
	
	private AbstractSet getNodes() {
		HashSet<JRNode> nodes = new HashSet<JRNode>();
		Enumeration e = objectList.keys();
		while ( e.hasMoreElements() ) {
			JRTuioObject t = (JRTuioObject)objectList.get(e.nextElement());
			try { 
				JRNode n = JRNode.getInstance( t.getFiducialID() ); 
				n.setSessionID( t.getSessionID() );
				nodes.add( n );
			}
			catch (JRException ex) { System.err.println("Error: " + ex.getMessage()); }
		}
		return nodes;
	}
	
	public void addTuioObject(TuioObject tobj) {
		int fiducialID = tobj.getFiducialID();
		long sessionID = tobj.getSessionID();
		System.out.println("add obj "+fiducialID+" ("+sessionID+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle());	
		objectList.put ( sessionID, new JRTuioObject(tobj) );
		buildTree();
	}

	public void updateTuioObject(TuioObject tobj) {
		JRTuioObject demo = (JRTuioObject)objectList.get(tobj.getSessionID());
		demo.update(tobj);
		//System.out.println("set obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+") "+tobj.getX()+" "+tobj.getY()+" "+tobj.getAngle()+" "+tobj.getMotionSpeed()+" "+tobj.getRotationSpeed()+" "+tobj.getMotionAccel()+" "+tobj.getRotationAccel());
		// update the properties of the appropriate node in the tree (use mutators)
		// acquire reference to JRNode by either:
			// 1. traverse the tree looking for a session id 
				// but traversal is cpu expensive
			// 2. keep a reference to each node in this connection manager
				// this raises the reference count to two, so it
				// could possibly break garbage collection if I'm not careful
	}
	
	public void removeTuioObject(TuioObject tobj) {
		objectList.remove(tobj.getSessionID());
		System.out.println("del obj "+tobj.getFiducialID()+" ("+tobj.getSessionID()+")");
		// if there is no corresponding node in the tree
			// throw an exception
		// else
			// recreate tree based on proximity rules
			buildTree();
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
