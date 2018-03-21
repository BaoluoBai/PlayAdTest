package com.example.playadtest;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;


/**
 * Created by Administrator on 2017/9/22 0022.
 */
public class DifferentDisplay extends Presentation{
    public DifferentDisplay(Context outerContext, Display display) {
        super(outerContext, display);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.differ_layout);
    }
}
