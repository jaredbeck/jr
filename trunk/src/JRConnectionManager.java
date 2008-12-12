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
		
		Then, create a list of objects which are input-satisfied, but
		output-un-satisfied.  Refer to these objects as "eager".
		
		An object is input-satisfied when it can produce an output.  In
		the case of a filter, which has one audio input and one audio
		output, input-satisfaction means a connected audio input. In the
		case of a controller, which has one control output, and no inputs,
		input-satisfaction is immediate.

		Next, connect eager objects to the nearest appropriate input.
		
		After connecting all eager objects, re-iterate.
		
		Iteration terminates when no objects are eager. This can result in
		objects with unconnected outputs.  This is not an error. 
		*/
		
		else {
			System.out.println("DEBUG: buildTree: Build non-trivial tree");
			jrTree.clear();
			AbstractSet<JRNode> nodes = getNodes();
			try { 
				
				/* The head of a non-trivial tree is a Mixer. This mixer
				is not represented by a tangible object on the tabletop. */
				JRMixer m = new JRFinalMixer();
				m.setX( 0.5F );
				m.setY( 0.5F );
				jrTree.setHead( m );
				nodes.add( m );
				
				// Begin algorithm
				boolean continueIteration = true;
				while ( continueIteration ) {
					System.out.println("DEBUG: buildTree: Itterate");

					/* Count input-satisfied nodes */
					AbstractSet inputSatisfiedNodes = getInputSatisfiedNodes( nodes );
					System.out.println("DEBUG: buildTree:  There are " + inputSatisfiedNodes.size() + " input-satisfied nodes");

					/* Actually, we are only concerned with nodes which are both
					input-satisfied and output-unsatisfied.  Let's call these
					"eager" nodes. */
					AbstractSet eagerNodes = getEagerNodes( nodes );
					System.out.println("DEBUG: buildTree:  There are " + eagerNodes.size() + " eager nodes");
					
					/* Iteration terminates when no objects are eager. */
					if ( eagerNodes.size() == 0 ) { continueIteration = false; }
					else {
					
						/* Sort eager nodes by distance to the nearest appropriate
						input on the table.  For example, if one controller is
						distance 50 from the nearest control input, and another
						controller is distance 40, we want to create the shorter
						edge first. */
						System.out.println("DEBUG: buildTree:  Distances of shortest valid edges:");
						AbstractMap<Float,JRNode[]> mapDist = new HashMap<Float,JRNode[]>();
						
						// for each eager node,
						Iterator eagerIterator = eagerNodes.iterator();
						while ( eagerIterator.hasNext() ) {
							JRNode en = (JRNode)eagerIterator.next();
							
							// which node (with appropriate input) is closest?
							JRNode closestNode = null;
							float smallestDistance = Float.POSITIVE_INFINITY;
							
							// for each node n in all nodes
							// (except the current eager node)
							Iterator allNodeIterator = nodes.iterator();
							while ( allNodeIterator.hasNext() ) {
								JRNode n = (JRNode)allNodeIterator.next();
								if ( n.getSessionID() != en.getSessionID() ) {
		
									// if it has an appropriate input to match the eager output
									if ( n.hasInputType( en.getOutputType() ) ) {
										float d = en.getDistance(n);
										if ( d < smallestDistance ) {
											smallestDistance = d;
											closestNode = n;
										}
									}
								}
							}
							// either there are no nodes with an appropriate input
							// or the closest node was found
							if ( closestNode != null ) {
								JRNode[] edge = new JRNode[2];
								edge[0] = en;
								edge[1] = closestNode;
								mapDist.put( smallestDistance, edge );
								System.out.println("DEBUG: buildTree:    " + en.getSessionID() + "->" + closestNode.getSessionID() + " == " + smallestDistance);
							}
						}
						
						// convert map to sorted list
						Set<Float> distSet = mapDist.keySet();
						Float[] sortedDistances = (Float[])distSet.toArray(new Float[0]);
						Arrays.sort(sortedDistances);
						
						/*System.out.print("DEBUG: buildTree:  " + sortedDistances.length + " sorted distances: ");
						for (int blah = 0; blah < sortedDistances.length; blah++) {
							System.out.print(sortedDistances[blah] + " ");
						}
						System.out.println();*/
						
						AbstractList<JRNode[]> sortedEdges = new ArrayList<JRNode[]>();
						for (int i = 0; i < sortedDistances.length; i++) {
							JRNode[] edge = mapDist.get( sortedDistances[i] );
							sortedEdges.add( edge );
						}
						
						/* Now, given the list of valid edges sorted by distance,
						we can apply that list to the collection of nodes.  For each
						edge, we call parent.addChild() */
						System.out.print("DEBUG: buildTree:  Valid edges (sorted by distance): ");
						Iterator edgeIterator = sortedEdges.iterator();
						while (edgeIterator.hasNext()) {
							JRNode[] edge = (JRNode[])edgeIterator.next();
							System.out.print(edge[0].getSessionID() + "->" + edge[1].getSessionID() + " ");
							JRNode child = edge[0];
							JRNode parent = edge[1];
							parent.addChild(child);
							}
						System.out.println();

					System.out.println("DEBUG: buildTree:  End working with eager nodes");

					} // end working with eager nodes
						
					System.out.println("DEBUG: buildTree: End of an iteration");

				} // end iteration

			} // end try block
			catch (JRException e) { 
				System.err.println( "ERROR: Tree construction failed: " + e.getMessage() );
				jrTree.clear();
			}
			
		} // end non-trivial tree construction
		
		System.out.println("DEBUG: buildTree: exit");
	}

	/* An "eager" node is input-satisfied, but output-unsatisfied. */
	private AbstractSet getEagerNodes( AbstractSet nodes ) {
		HashSet<JRNode> eagerNodes = new HashSet<JRNode>();
		Iterator i = nodes.iterator();
		while ( i.hasNext() ) {
			JRNode n = (JRNode)i.next();
			if ( n.isInputSatisfied() && ! n.isOutputSatisfied() ) { 
				eagerNodes.add( n ); 
			}
		}
		return eagerNodes;
	}
	
	// getInputSatisfiedNodes() is only used for debugging
	private AbstractSet getInputSatisfiedNodes( AbstractSet nodes ) {
		HashSet<JRNode> satisfiedNodes = new HashSet<JRNode>();
		Iterator i = nodes.iterator();
		while ( i.hasNext() ) {
			JRNode n = (JRNode)i.next();
			if ( n.isInputSatisfied() ) { satisfiedNodes.add( n ); }
		}
		return satisfiedNodes;
	}
	
	private AbstractSet<JRNode> getNodes() {
		HashSet<JRNode> nodes = new HashSet<JRNode>();
		Enumeration e = objectList.keys();
		while ( e.hasMoreElements() ) {
			JRTuioObject t = (JRTuioObject)objectList.get(e.nextElement());
			try { 
				JRNode n = JRNode.getInstance( t.getFiducialID() ); 
				n.setSessionID( t.getSessionID() );
				n.setX( t.getX() );
				n.setY( t.getY() );
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
