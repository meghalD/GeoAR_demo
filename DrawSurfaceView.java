package com.meghal.yeppar.mygeoar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.net.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

import static com.meghal.yeppar.mygeoar.MainActivity.lat;
import static com.meghal.yeppar.mygeoar.MainActivity.lng;
import static com.meghal.yeppar.mygeoar.MainActivity.props;

public class DrawSurfaceView extends View implements View.OnTouchListener {
	Point me = new Point(lat, lng, "Me");
	Paint mPaint = new Paint();
	private double OFFSET = 0d;
	private double screenWidth, screenHeight = 0d;
	private Bitmap[] mSpots, mBlips;
	private Bitmap mRadar;
	public float xval,yval;
	double lat2,lng2;
	Rect rectangle;
	List<Rect> LRect=new ArrayList<>();
	Context context;
	public DrawSurfaceView(Context c, Paint paint) {
		super(c);
	}

	public void Refresh()
	{

		me = new Point(lat, lng, "Me");
		mPaint.setColor(Color.GREEN);
		mPaint.setTextSize(50);
		mPaint.setStrokeWidth(DpiUtils.getPxFromDpi(getContext(), 2));
		mPaint.setAntiAlias(true);

		mRadar = BitmapFactory.decodeResource(context.getResources(), R.drawable.radar);

		mSpots = new Bitmap[props.size()];
		for (int i = 0; i < mSpots.length; i++)
			mSpots[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.dot);

		mBlips = new Bitmap[props.size()];
		for (int i = 0; i < mBlips.length; i++)
			mBlips[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.blip);
		//Log.e("mSpots"," "+mSpots.length);
		//Log.e("mBlips"," "+mBlips.length);
		invalidate();
		postInvalidate();
	}
	public DrawSurfaceView(Context context, AttributeSet set) {

		super(context, set);
		this.context=context;

		mPaint.setColor(Color.GREEN);
		mPaint.setTextSize(50);
		mPaint.setStrokeWidth(DpiUtils.getPxFromDpi(getContext(), 2));
		mPaint.setAntiAlias(true);
		
		mRadar = BitmapFactory.decodeResource(context.getResources(), R.drawable.radar);
		
		mSpots = new Bitmap[props.size()];
		for (int i = 0; i < mSpots.length; i++) 
			mSpots[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.dot);

		mBlips = new Bitmap[props.size()];
		for (int i = 0; i < mBlips.length; i++)
			mBlips[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.blip);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		//Log.e("onSizeChanged", "in here w=" + w + " h=" + h);
		screenWidth = (double) w;
		screenHeight = (double) h;
	}

	@Override
	protected void onDraw(Canvas canvas) {


		canvas.drawBitmap(mRadar, 0, 0, mPaint);

		int radarCentreX = 0;
		int radarCentreY = 0;
		try {
			radarCentreX = mRadar.getWidth() / 2;
			radarCentreY = mRadar.getHeight() / 2;
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		LRect=new ArrayList<>();
		for (int i = 0; i < mBlips.length; i++) {
			Bitmap blip = mBlips[i];
			Bitmap spot = mSpots[i];
			Point u = null;
			try {
				u = props.get(i);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			}

			//Log.e("mBlips"," "+i);
			double dist = distInMetres(me, u);
			double ydist = dist+10;//12Apr
			if (blip == null || spot == null)
				continue;
			
			if(dist > 70)
				dist = 70; //we have set points very far away for demonstration
			
			double angle = bearing(me.latitude, me.longitude, u.latitude, u.longitude) - OFFSET;
			double xPos, yPos;
			
			if(angle < 0)
				angle = (angle+360)%360;
			
			xPos = Math.sin(Math.toRadians(angle)) * dist;
			yPos = Math.sqrt(Math.pow(dist, 2) - Math.pow(xPos, 2));

			if (angle > 90 && angle < 270)
				yPos *= -1;
			
			double posInPx = angle * (screenWidth / 90d);
			
			int blipCentreX = blip.getWidth() / 2;
			int blipCentreY = blip.getHeight() / 2;
			
			xPos = xPos - blipCentreX;
			yPos = yPos + blipCentreY;
			canvas.drawBitmap(blip, (radarCentreX + (int) xPos), (radarCentreY - (int) yPos), mPaint); //radar blip
			
			//reuse xPos
			int spotCentreX = spot.getWidth() / 2;
			int spotCentreY = spot.getHeight() / 2;
			xPos = posInPx - spotCentreX;
			
			if (angle <= 45) 
				u.x = (float) ((screenWidth / 2) + xPos);
			
			else if (angle >= 315) 
				u.x = (float) ((screenWidth / 2) - ((screenWidth*4) - xPos));
			
			else
				u.x = (float) (float)(screenWidth*9); //somewhere off the screen
			
			u.y = (float)screenHeight/2 + spotCentreY;
			//canvas.drawBitmap(spot, u.x, u.y, mPaint); //camera spot
			//canvas.drawText(u.description, u.x, u.y, mPaint); //text
			drawMultiline(canvas, u.description + "\n(" + String.format("%.3f", ydist / 1000) + "KM)", u.x, u.y, mPaint); //12Apr
			xval = u.x;
			yval = u.y;
		}
	}

	//12Apr
	public void drawMultiline(Canvas canvas, String str, float x, float y, Paint paint)
	{
		for (String line: str.split("\n"))
		{
			canvas.drawText(line, x, y, paint);
			y += -paint.ascent() + paint.descent();
			rectangle = new Rect((int)x, (int)y, 300, 300);
			canvas.drawRect(x, y, 300, 300, paint);
			canvas.drawRect(rectangle,paint);
			LRect.add(rectangle);
		}
	}



	public void setOffset(float offset) {
		this.OFFSET = offset;
	}

	public void setMyLocation(double latitude, double longitude) {
		me.latitude = latitude;
		me.longitude = longitude;
	}

	protected double distInMetres(Point me, Point u) {

		double lat1 = me.latitude;
		double lng1 = me.longitude;

		lat2 = u.latitude;
		lng2 = u.longitude;



		double earthRadius = 6371;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double sindLat = Math.sin(dLat / 2);
		double sindLng = Math.sin(dLng / 2);
		double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;
		return dist * 1000;
	}

	protected static double bearing(double lat1, double lon1, double lat2, double lon2) {
		double longDiff = Math.toRadians(lon2 - lon1);
		double la1 = Math.toRadians(lat1);
		double la2 = Math.toRadians(lat2);
		double y = Math.sin(longDiff) * Math.cos(la2);
		double x = Math.cos(la1) * Math.sin(la2) - Math.sin(la1) * Math.cos(la2) * Math.cos(longDiff);

		double result = Math.toDegrees(Math.atan2(y, x));
		return (result+360.0d)%360.0d;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.e("meghalsss", "onTouchEvent: drawable touched ");
		int x = (int)event.getX();
		int y = (int)event.getY();
		//x=6055;
		//y=784;
		switch(event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				/*Log.e("value of x","x ="+x+"xval = "+xval);
				Log.e("value of y","y ="+y+"yval = "+yval);*/
				for(Rect r : LRect){
					Log.e("value of x","x ="+x+"xval = "+r.centerX());
					Log.e("value of y","y ="+y+"yval = "+r.centerY());
					int xval=r.centerX();
					int yval=r.centerY();
				//Check if the x and y position of the touch is inside the bitmap
				if( x > xval  && x < xval + 300  )
				{
					Log.e("TOUCHED", "Touched val  X: " + x + " Y: " + y);
					//Bitmap touched
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://maps.google.com/maps?&daddr="+lat2+","+lng2));
					context.startActivity(intent);
				}
				}
				return true;
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		/*event.getX();
		if (drawable.getBounds().contains(xval+50,yval+20)  &&
				event.getAction()==MotionEvent.ACTION_DOWN)
		{
			Log.e(TAG, "onTouchEvent: drawable touched ");
			return true;
		}*/
		return  false;

	}
}
