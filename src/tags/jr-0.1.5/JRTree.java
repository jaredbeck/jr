import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JRTree {
	
	private JRNode head;
	
	public JRTree ( ) {
	}
	
	public JRTree ( JRNode h ) throws JRInvalidNodeException {
		setHead( h );
	}
	
	public synchronized void clear ( ) {
		// is it enough to just set the head to null?
		this.head = null;
		// will other nodes be garbage collected?
	}
	
	public synchronized void dump ( ) {
		System.out.println( "Dumping tree with preorder traversal .." );
		this.dump( this.head, 0, "preorder" );
		System.out.println( "Dumping tree with postorder traversal .." );
		this.dump( this.head, 0, "postorder" );
	}
	
	public synchronized void dump ( JRNode n, int level, String order ) {
		// preorder output
		if ( order.equals("preorder") ) { 
			for ( int t = 0; t < level; t++ ) { System.out.print( "  " ); }
			System.out.println( n.toString() ); 
		}
		
		// recursion logic
		if ( n.getDegree() > 0 ) {
			Iterator i = n.getChildIterator();
			while ( i.hasNext() ) {
				dump( (JRNode)i.next(), level + 1, order );
			}
		}
		
		// postorder output
		if ( order.equals("postorder") ) { 
			for ( int l = 1; l < n.getLevel(); l++ ) { System.out.print( "  " ); }
			System.out.println( n.toString() ); 
		} 
	}
	
	public synchronized JRNode getHead ( ) {
		return this.head;
	}
	
	public synchronized JRNode getNode ( long sessionID ) {
		// if the tree is empty, return null
		if ( this.head == null ) { return null; }
		// else traverse the tree looking for the given sessionID
		return this.getNode( sessionID, this.head );
	}
	
	private synchronized JRNode getNode ( long sessionID, JRNode current ) {
	
		// TODO: I think this could be written better ...
		// it's kind of insane, but seems to work
		
		if ( current.getSessionID() == sessionID ) {
			return current; // base case
		}
		else {
			if ( current.getDegree() == 0 ) {
				return null; // base case
			}
			else {
				Iterator i = current.getChildIterator();
				while ( i.hasNext() ) {
					JRNode n = getNode( sessionID, (JRNode)i.next() ); // recurse
					if (n != null) {
						return n; // base case
					}
				}
			}
		}
		return null; // base case
	}
	
	public synchronized void setHead ( JRNode h ) throws JRInvalidNodeException {
		if ( h.getNumAudioOutputs() == 1) {
			this.head = h;
		}
		else {
			throw new JRInvalidNodeException( "Invalid head.  Head must have exactly one audio out." );
		}
	}

}