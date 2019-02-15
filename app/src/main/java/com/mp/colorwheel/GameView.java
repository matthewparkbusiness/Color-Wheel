package com.mp.colorwheel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;


public class GameView extends SurfaceView implements Runnable {

    static int width, height;
    static int freq = 0;
    static int mode = 1;
    static int wheelRadius = 100;
    static int blockRadius = 5;

    static boolean reverseTypesPossible = false;

    private SurfaceHolder holder;

    private boolean threadAlreadyStarted;
    private Thread thread;

    private Paint red;
    private Paint blue;
    private Paint green;
    private Paint purple;
    private Paint black;
    private Paint white;
    private Paint title;
    private Paint whiteUncentered;

    private int score = 0;
    private int time = 20000;
    private int totalDots = 0;
    private int gameMode = 0;
    private int menuAngle;

    private final RectF oval = new RectF();

    private float startAngle = 0;
    private float diff = 0;
    private float angleFound = 0;

    private ArrayList<Block> blocks = new ArrayList<Block>();
    private Path myPath = new Path();

    private long counter = 0;
    private long lastTime = 0;


    /**
     * Initializing paints, start game thread after holder callback
     * @param c
     */
    public GameView(Context c){
        super(c);
        red = new Paint();
        red.setTextSize(30);
        red.setTextAlign(Paint.Align.CENTER);
        red.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        red.setColor(Color.RED);

        blue = new Paint();
        blue.setColor(Color.BLUE);

        green = new Paint();
        green.setColor(Color.GREEN);

        purple = new Paint();
        purple.setColor(Color.MAGENTA);

        black = new Paint();
        black.setColor(Color.BLACK);

        white=  new Paint();
        white.setColor(Color.WHITE);
        white.setTextSize(30);
        white.setTextAlign(Paint.Align.CENTER);
        white.setTypeface(Typeface.create("Arial", Typeface.BOLD));

        title=  new Paint();
        title.setColor(Color.WHITE);
        title.setTextSize(80);
        title.setTextAlign(Paint.Align.CENTER);
        title.setTypeface(Typeface.create("Arial", Typeface.BOLD));

        whiteUncentered=  new Paint();
        whiteUncentered.setColor(Color.WHITE);
        whiteUncentered.setTextSize(30);
        whiteUncentered.setTypeface(Typeface.create("Arial", Typeface.BOLD));


        thread = new Thread(this);

        holder = this.getHolder();
        holder.addCallback(

                new SurfaceHolder.Callback(){

                    public void surfaceChanged(SurfaceHolder arg0,
                                               int arg1, int arg2, int arg3) {
                    }

                    public void surfaceCreated(SurfaceHolder arg0) {
                        if(!threadAlreadyStarted){
                            Display display = MainActivity.context.getWindowManager().getDefaultDisplay();
                            int width = display.getWidth();
                            if(width < wheelRadius*4){
                                new AlertDialog.Builder(MainActivity.context)
                                        .setTitle("Resolution")
                                        .setMessage("Your screen resolution is a little low.\nDo you still want to play?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                thread.start();
                                                threadAlreadyStarted = true;
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }
                            else {
                                thread.start();
                                threadAlreadyStarted = true;
                            }
                        }
                    }

                    public void surfaceDestroyed(SurfaceHolder arg0) {}

                });
    }


    /*
     * Runnable game loop: game thread starts here
     */
    public void run(){


        Canvas c = holder.lockCanvas();
        // Get width and height for graphics initialization
        width = c.getWidth();
        height = c.getHeight();
        holder.unlockCanvasAndPost(c);


        long sleepTime = 10;
        while(true){

            c = holder.lockCanvas();

            c.drawColor(Color.BLACK);

            // Render game display
            draw(c);

            // Post canvas
            holder.unlockCanvasAndPost(c);

            try{
                Thread.currentThread().sleep(sleepTime);
            }
            catch(InterruptedException ie){ }

            if(MainActivity.starting) counter ++;
        }

    }


    @Override
    public void draw(Canvas c){
        super.draw(c);
        // Game Mode Indices : 0 - main menu, 1 - mid-game, 2 - win, 3 - lose

        if(gameMode == 0){
            // Display game menu if gameMode is 0

            // Play button
            c.drawText("Play", width/2, height/2 + 80, title);

            // Settings button - adjust difficulty here
            c.drawText("Settings", width/2, height/2 + 200, title);

            // Draw wheel
            drawArc(c, width / 2, height / 2 - wheelRadius - 40, startAngle + menuAngle, 1200, red);
            drawArc(c, width / 2, height / 2 - wheelRadius - 40, startAngle + 120 + menuAngle, 120, blue);
            drawArc(c, width/2, height/2 - wheelRadius - 40, startAngle + 240 + menuAngle, 120, green);
            c.drawCircle(width / 2, height / 2 - wheelRadius - 40, wheelRadius - 10, black);

            // Access shared preferences for highest score
            c.drawText("" + MainActivity.highestScore, width / 2, height / 2 - wheelRadius - 10, title);
            c.drawText("Highest Score", width / 2, height/2 - wheelRadius*2 - 90, white);

            menuAngle = menuAngle + 1;
            if(menuAngle >=360){
                menuAngle = 0;
            }
        }
        else if(gameMode == 1){

            // Start timer
            if(MainActivity.starting) time = time - 10;

            if(time <= 0){
                gameMode = 2;

                int percent = (int)(((double)score/totalDots)*100);
                if(percent > MainActivity.highestScore){
                    SharedPreferences.Editor editor = MainActivity.sharedPref.edit();
                    editor.putInt(MainActivity.context.getString(R.string.save_data28), percent);
                    editor.commit();

                    MainActivity.highestScore = percent;
                }

                lastTime = System.currentTimeMillis();
            }
            else {
                /**
                 * Game logic: blocks converge to center, if blocks reach color wheel at the
                 * appropriate spot, player gets point, limited time, certain number of blocks
                 * speed adjusted with difficulty
                 *
                 */
                int i;
                for (i = blocks.size() - 1; i >= 0; i--) {
                    Block each = blocks.get(i);
                    blocks.get(i).moveAndDraw(c);
                    if (blocks.get(i).checkExceeds()) {
                        angleFound = blocks.get(i).findAngle();

                        if ((angleFound >= startAngle && angleFound < startAngle + 120)
                                || (angleFound + 360 >= startAngle && angleFound + 360 < startAngle + 120)
                                || (angleFound - 360 >= startAngle && angleFound - 360 < startAngle + 120)) {
                            if (each.reverseType == 0) {
                                if(each.type == 0) score++;
                            }
                            else {
                                if(each.type != 0) score ++;
                            }
                        } else if ((angleFound >= startAngle + 120 && angleFound < startAngle + 240)
                                || (angleFound + 360 >= startAngle + 120 && angleFound + 360 < startAngle + 240)
                                || (angleFound - 360 >= startAngle + 120 && angleFound - 360 < startAngle + 240)) {

                            if (each.reverseType == 0) {
                                if(each.type == 2) score++;
                            }
                            else {
                                if(each.type != 2) score ++;
                            }

                        } else {
                            if (each.reverseType == 0) {
                                if(each.type == 1) score++;
                            }
                            else {
                                if(each.type != 1) score ++;
                            }
                        }
                        // counter of total dots
                        totalDots++;
                        // remove if hit
                        blocks.remove(i);
                    }
                }
                // Render wheel on screen
                drawWheel(c);

                c.drawCircle(width / 2, height / 2, blockRadius, black);

                c.drawText("" + score, width / 2, height / 2, white);

                if(MainActivity.starting) {
                    c.drawRect(width - 45, height - 45, width - 30, height - 10, white);
                    c.drawRect(width - 25, height - 45, width - 10, height - 10, white);
                }
                else{
                    c.drawLine(width - 45, height - 45, width - 10, height - 25, white);
                    c.drawLine(width - 45, height - 5, width - 10, height - 25, white);
                    c.drawLine(width - 45, height - 45, width - 45, height - 5, white);

                }
                c.drawText(time/1000 + ":" + (time%1000), 10, height - 10, whiteUncentered);
                if (counter % freq == 0) {
                    blocks.add(new Block());
                    counter = 0;
                }

            }


        }
        else if(gameMode == 2){
            c.drawText("Congrats! You got...", width/2, height/2, white);
            c.drawText((int)(((double)score/totalDots)*100) + "%.", width/2, height/2 + 80, title);
            if(System.currentTimeMillis() - lastTime > 3000){
                gameMode = 0;
            }
        }
        else if(gameMode == 3){
            // Draw Game Modes

            c.drawText("Game Modes", width/2, height/2 - 240, title);
            if(mode == 1) c.drawText("VERY EASY", width/2, height/2 - 150, red);
            else c.drawText("> VERY EASY", width/2, height/2 - 150, white);

            if(mode == 2)c.drawText("MEDIUM", width/2, height/2 - 60, red);
            else c.drawText("> MEDIUM", width/2, height/2 - 60, white);

            if(mode == 3)c.drawText("HARD", width/2, height/2 + 30, red);
            else c.drawText("> HARD", width/2, height/2 + 30, white);

            if(mode == 4)c.drawText("VERY HARD", width/2, height/2 + 120, red);
            else c.drawText("> VERY HARD", width/2, height/2 + 120, white);

            c.drawText("< Back", width/2, height/2 + 220, white);
        }
    }


    public void drawArc(Canvas c, int x, int y, float startAngle, int sweepAngle, Paint p){
        oval.set(x - wheelRadius, y - wheelRadius, x + wheelRadius, y + wheelRadius);
        c.drawArc(oval, startAngle, -sweepAngle, true, p);

    }


    /**
     * Draws wheel at appropriate location and orientation
     * @param c
     */
    public void drawWheel(Canvas c){

        drawArc(c, width/2, height/2, startAngle, 1200, red);
        drawArc(c, width/2, height/2, startAngle + 120, 120, blue);
        drawArc(c, width/2, height/2, startAngle + 240, 120, green);

        c.drawCircle(width / 2, height / 2, wheelRadius - 20, black);


    }


    /**
     * Responds to on touch events
     * @param e
     * @return true if continue to next OnTouchListener
     *         false if otherwise
     */
    public boolean onTouchEvent(MotionEvent e){

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:


                if(gameMode == 0) {
                    if(y >= height/2 && y <= height/2 + 80) {
                        gameMode = 1;
                        blocks.clear();
                        startAngle = 0;
                        score = 0;
                        totalDots = 0;
                        time = 20000;
                    }
                    if(y >= height/2 + 120 && y <= height/2 + 200){
                        gameMode = 3;

                    }
                }
                else if(gameMode == 1){
                    if(x > width - 50 && y > height- 50 && gameMode == 1){
                        MainActivity.starting = !MainActivity.starting;
                    }
                    else{
                        diff = (float) Math.toDegrees(angleBetweenTwoPointsWithFixedPoint(x, y, 0, 0, width / 2, height / 2)) - startAngle;
                    }
                }
                else if(gameMode == 3){
                    //150
                    if(y <= height/2 - 140 && y >= height/2 - 180){
                        mode = 1;
                    }
                    //60
                    else if(y <= height/2 - 50 && y >= height/2 - 90){
                        mode = 2;
                    }
                    //30
                    else if(y <= height/2  + 40 && y >= height/2){
                        mode = 3;
                    }
                    //120
                    else if(y <= height/2  + 130 && y >= height/2 + 90){
                        mode = 4;
                    }
                    //220
                    else if(y <= height/2  + 230 && y >= height/2 + 190){
                        gameMode = 0;
                        SharedPreferences.Editor editor = MainActivity.sharedPref.edit();
                        editor.putInt(MainActivity.context.getString(R.string.mode), mode);
                        editor.commit();
                        initMode(mode);
                    }
                }

            case MotionEvent.ACTION_MOVE:

                if(gameMode == 1){
                    if(MainActivity.starting)
                        startAngle = (float) (Math.toDegrees(angleBetweenTwoPointsWithFixedPoint(x, y, 0, 0, width / 2, height / 2)) - diff)%360;

                }

        }


        return true;
    }

    public static double angleBetweenTwoPointsWithFixedPoint(double point1X, double point1Y,
                                                             double point2X, double point2Y,
                                                             double fixedX, double fixedY) {

        double angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
        double angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);

        return angle1 - angle2;
    }


    public static void initMode(int mode){
        if(mode == 1){
            freq = 150;
            reverseTypesPossible = false;
        }
        else if(mode == 2){
            freq = 80;
            reverseTypesPossible = false;
        }
        else if(mode == 3){
            freq = 80;
            reverseTypesPossible = true;
        }
        else if(mode == 4){
            freq = 30;
            reverseTypesPossible = true;
        }
    }


    /**
     * Contains coordinates of each block converging to center, color, and reverse type.
     */
    public class Block{

        int type = 0;
        double x;
        double y;

        Paint p;
        int reverseType;

        public Block(){

            if(reverseTypesPossible) reverseType  = (int)(Math.random()*2);
            else reverseType = 0;

            type = (int) (Math.random()*3);
            if(type ==0) p= red;
            else if(type == 1) p = green;
            else p = blue;

            int pos = (int) (Math.random()*4);
            if(pos == 0) {
                x = -blockRadius;
                y = (int)(Math.random()*height);
            }
            else if(pos == 1){
                x = width + blockRadius;
                y = (int)(Math.random()*height);
            }
            else if(pos == 2){
                x = (int)(Math.random()*width);
                y = -blockRadius*2;
            }
            else if(pos == 3){
                x = (int)(Math.random()*width);
                y = height + blockRadius;
            }



        }

        public void moveAndDraw(Canvas c){

            if(MainActivity.starting) {
                x = x + (width / 2 - x) / 60;
                y = y + (height / 2 - y) / 60;
            }
            if(reverseType == 0) c.drawCircle((float)x, (float)y, blockRadius, p);
            else {
                c.drawRect((float) x - blockRadius, (float) y - blockRadius,
                        (float) x + blockRadius, (float) y + blockRadius, p);
                c.drawLine((float) x - blockRadius*4, (float) y,
                        (float) x + blockRadius*4, (float) y, p);
                c.drawLine((float) x,(float) y - blockRadius*4,(float)x,
                        (float) y + blockRadius*4, p);
            }
        }


        public boolean checkExceeds(){
            if(x < width/2 - wheelRadius - 10 || y < height/2 - wheelRadius - 10 ||
                    x > width/2 + wheelRadius + 10 || y > height/2 + wheelRadius + 10){
                return false;
            }

            if((x- width/2)*(x-width/2) + (y - height/2)*(y-height/2) <= wheelRadius*wheelRadius){
                return true;
            }
            return false;
        }

        public float findAngle(){
            return (float) Math.toDegrees(angleBetweenTwoPointsWithFixedPoint(x,y, 0,0, width / 2, height / 2));

        }


    }
}























