package com.example.krebs.bouncing_th1ng;

import android.graphics.RectF;

public class Boost {

    //1 Energy      //2 mehr Punkte     //3 Hindernisse unsichtbar
    int typ = 0;

    private RectF rect;

    private float length;
    private float height;

    private float x;
    private int screenX;
    private float y;

    private boolean isVisible;
    public RectF getRect(){
        return this.rect;
    }
    public void setInvisible(){
        isVisible = false;
    }
    public boolean getVisibility(){
        return isVisible;
    }


    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getTyp() {
        return typ;
    }

    public void setTyp(int typ) {
        this.typ = typ;
    }

    public Boost(int screenX, int screenY){
        // 130 pixels wide and 20 pixels high
        length = 100;
        height = 150;

        x = (float) (screenX/((Math.random()*100%4.8)+1.2));
        y = (float) (screenY/((Math.random()*100%2.6)+1.4));

        isVisible=true;

        rect = new RectF(x, y, x + length, y + height);

    }
}
