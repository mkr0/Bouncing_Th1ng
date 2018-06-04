package com.example.krebs.bouncing_th1ng;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.graphics.Bitmap.createScaledBitmap;

public class BouncingGame extends Activity {

    // gameView will be the view of the game
    // It will also hold the logic of the game
    // and respond to screen touches as well
    BouncingView bouncingView;
    long starttime = 0;
    int playtime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        starttime = System.currentTimeMillis();

        // Initialize gameView and set it as the view
        bouncingView = new BouncingView(this);
        setContentView(bouncingView);

    }



    // Here is our implementation of GameView
    // It is an inner class.
    // Note how the final closing curly brace }
    // is inside SimpleGameEngine

    // Notice we implement runnable so we have
    // A thread and can override the run method.
    class BouncingView extends SurfaceView implements Runnable {

        Bitmap background;
        Bitmap paddlepic;

        // This is our thread
        Thread gameThread = null;

        // This is new. We need a SurfaceHolder
        // When we use Paint and Canvas in a thread
        // We will see it in action in the draw method soon.
        SurfaceHolder ourHolder;

        // A boolean which we will set and unset
        // when the game is running- or not.
        volatile boolean playing;

        // Game is paused at the start
        boolean paused = true;

        // A Canvas and a Paint object
        Canvas canvas;
        Paint paint;


        // This variable tracks the game frame rate
        long fps;

        // This is used to help calculate the fps
        private long timeThisFrame;

        // The size of the screen in pixels
        int screenX;
        int screenY;

        // The player's paddle
        Paddle paddle;
        // A ball
        Ball ball;
        // Up to 200 obstacles
        Obstacle[] obstacles = new Obstacle[200];
        int numObstacles = 0;

        // For sound FX
        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;

        // The score
        int score = 0;

        // Lives
        int lives = 3;

        // When the we initialize (call new()) on gameView
        // This special constructor method runs
        public BouncingView(Context context) {
            // The next line of code asks the
            // SurfaceView class to set up our object.
            // How kind.
            super(context);

            // Bitmap initialisieren
            background = BitmapFactory.decodeResource(getResources(), R.drawable.road);
            paddlepic = BitmapFactory.decodeResource(getResources(), R.drawable.wood);


            // Initialize ourHolder and paint objects
            ourHolder = getHolder();
            paint = new Paint();

            // Get a Display object to access screen details
            Display display = getWindowManager().getDefaultDisplay();
            // Load the resolution into a Point object
            Point size = new Point();
            display.getSize(size);

            screenX = size.x;
            screenY = size.y;

            // Create a Paddle
            paddle = new Paddle(screenX, screenY);
            // Create a ball
            ball = new Ball(screenX, screenY);


            // Load the sounds

            // This SoundPool is deprecated but don't worry
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);

            try{
                // Create objects of the 2 required classes
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                // Load our fx in memory ready for use
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);

            }catch(IOException e){
                // Print an error message to the console
                Log.e("error", "failed to load sound files");
            }

            createObstaclesAndRestart();
        }

        public void createObstaclesAndRestart(){

            // Put the ball back to the start
            ball.reset(screenX, screenY);

            int brickWidth = screenX / 8;
            int brickHeight = screenY / 10;

            // Build a wall of bricks
            numObstacles = 0;

            for(int column = 0; column < 8; column ++ ){
                for(int row = 0; row < 3; row ++ ){
                    obstacles[numObstacles] = new Obstacle(row, column, brickWidth, brickHeight);

                    if(Math.random()*100 <50)
                        obstacles[numObstacles].setInvisible();
                    numObstacles ++;
                }
            }
            // Reset scores and lives
            score = 0;
            lives = 3;
        }//createObstaclesAndRestart


        @Override
        public void run() {
            while (playing) {

                // Capture the current time in milliseconds in startFrameTime
                long startFrameTime = System.currentTimeMillis();

                // Update the frame
                // Update the frame
                if(!paused){
                    update();
                }

                // Draw the frame
                draw();

                // Calculate the fps this frame
                // We can then use the result to
                // time animations and more.
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }

            }

        }

        // Everything that needs to be updated goes in here
        // Movement, collision detection etc.
        public void update() {

            // Move the paddle if required
            paddle.update(fps);

            // Check for ball colliding with a brick
            for(int i = 0; i < numObstacles; i++){

                if (obstacles[i].getVisibility()){
                    obstacles[i].update(fps);

                    if(RectF.intersects(obstacles[i].getRect(),ball.getRect())) {
                        obstacles[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }// for

            // Check for ball colliding with paddle
            if(RectF.intersects(paddle.getRect(),ball.getRect())) {
                //wenn sich paddle nicht bewegt
                if (paddle.getpaddleMoving()==0){
                    ball.setRandomXVelocity();
                }
                //wenn sich paddle nach links bewegt
                if (paddle.getpaddleMoving()==1){
                    if(ball.xVelocity>0){
                        ball.reverseXVelocity();
                    }
                }
                //wenn sich paddle nach rechts bewegt
                if (paddle.getpaddleMoving()==2){
                    if(ball.xVelocity<0){
                        ball.reverseXVelocity();
                    }
                }


                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }// Collission Ball and Paddle

            // Bounce the ball back when it hits the bottom of screen
            // And deduct a life
            if(ball.getRect().bottom > screenY){
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 2);

                // Lose a life
                lives --;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                if(lives == 0){
                    paused = true;
                    //createObstaclesAndRestart();
                    startActivity(new Intent(BouncingGame.this, GameOverActivity.class));


                }

            }// Bounce ball from bottom

            // Bounce the ball back when it hits the top of screen
            if(ball.getRect().top < 0){
                ball.reverseYVelocity();
                ball.clearObstacleY(12);
                soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }// Bounce from Top

            // If the ball hits left wall bounce
            if(ball.getRect().left < 0){
                ball.reverseXVelocity();
                ball.clearObstacleX(2);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // If the ball hits right wall bounce
            if(ball.getRect().right > screenX - 10){
                ball.setXVelocity(-200);
                ball.clearObstacleX(screenX - 22);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // If obstacle collides with left
            for(int i=0;i< numObstacles;i++){
                if(obstacles[i].getRect().left <0 ) obstacles[i].setObstacleMoving(2);
                if(obstacles[i].getRect().right > screenX ) obstacles[i].setObstacleMoving(1);
            }

            // Pause if cleared screen
            if(score == numObstacles * 10){
                paused = true;
                createObstaclesAndRestart();
            }

            // Update Ball
            ball.update(fps);

        }

        // Draw the newly updated scene
        public void draw() {

            // Make sure our drawing surface is valid or we crash
            if (ourHolder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();

                background = createScaledBitmap(background,screenX,screenY,false);
                paddlepic = createScaledBitmap(paddlepic, (int)paddle.getlength(),(int) paddle.getheight(),false);

                // Draw the background
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(background, (0), (0), null);


                // Choose the brush color for drawing
                paint.setColor(Color.argb(255,  255, 255, 255));


                // Draw the paddle
                //canvas.drawRect(paddle.getRect(), paint);
                canvas.drawBitmap(paddlepic, paddle.getX(),paddle.getY(),null);

                // Draw the ball
                canvas.drawRect(ball.getRect(), paint);

                // Change the obstacles color for drawing
                paint.setColor(Color.argb(255,  249, 129, 0));

                // Draw the obstacles if visible
                for(int i = 0; i < numObstacles; i++){
                    if(obstacles[i].getVisibility()) {
                        canvas.drawRect(obstacles[i].getRect(), paint);
                    }
                }

                // Draw the HUD
                // Choose the brush color for drawing
                paint.setColor(Color.argb(255,  255, 255, 255));

                // Draw the score, Lives, and Time played
                paint.setTextSize(40);

                canvas.drawText("Score: " + score + "   Lives: " + lives + "   Seconds Played: " + playtime, 10,50, paint);


                // Has the player cleared the screen?
                if(score == numObstacles * 10){
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE WON!", 10,screenY/2, paint);
                }

                // Has the player lost?
                if(lives <= 0){
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE LOST!", 10,screenY/2, paint);
                }

                // Draw everything to the screen
                ourHolder.unlockCanvasAndPost(canvas);
            }
            if ((System.currentTimeMillis()-starttime)/1000>playtime){
                playtime++;
                score++;
                System.out.println(playtime+" Sekunden gespielt");

            }

        }//draw

        // If SimpleGameEngine Activity is paused/stopped
        // shutdown our thread.
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }

        }

        // If SimpleGameEngine Activity is started theb
        // start our thread.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // The SurfaceView class implements onTouchListener
        // So we can override this method and detect screen touches.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:

                    paused = false;



                    if(motionEvent.getX() > screenX / 2){
                        paddle.setMovementState(paddle.RIGHT);
                    }
                    else{
                        paddle.setMovementState(paddle.LEFT);
                    }

                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }
            return true;
        }

    }
    // This is the end of our BouncingView inner class

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        bouncingView.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        bouncingView.pause();
    }

}
// This is the end of the BouncingGame class