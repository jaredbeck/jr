import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JRTuioObject extends TuioObject {

	private Shape square;

	public JRTuioObject ( TuioObject tobj ) {
		super(tobj);
/*		int size = TuioDemo.object_size;
		square = new Rectangle2D.Float(-size/2,-size/2,size,size);
		
		AffineTransform transform = new AffineTransform();
		transform.translate(xpos,ypos);
		transform.rotate(angle,xpos,ypos);
		square = transform.createTransformedShape(square);*/
	}
	
	public void paint(Graphics2D g) {
	
/*		float Xpos = xpos*TuioDemo.width;
		float Ypos = ypos*TuioDemo.height;
		float scale = TuioDemo.height/(float)TuioDemo.table_size;

		AffineTransform trans = new AffineTransform();
		trans.translate(-xpos,-ypos);
		trans.translate(Xpos,Ypos);
		trans.scale(scale,scale);
		Shape s = trans.createTransformedShape(square);
	
		g.setPaint(Color.black);
		g.fill(s);
		g.setPaint(Color.white);
		g.drawString(fiducial_id+"",Xpos-10,Ypos);*/
	}

	public void update(TuioObject tobj) {
		
		/* float dx = tobj.getX() - xpos;
		float dy = tobj.getY() - ypos;
		float da = tobj.getAngle() - angle;

		if ((dx!=0) || (dy!=0)) {
			AffineTransform trans = AffineTransform.getTranslateInstance(dx,dy);
			square = trans.createTransformedShape(square);
		}
		
		if (da!=0)  {
			AffineTransform trans = AffineTransform.getRotateInstance(da,tobj.getX(),tobj.getY());
			square = trans.createTransformedShape(square);
		} */

		super.update(tobj);
	}

}
