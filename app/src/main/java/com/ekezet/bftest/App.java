package com.ekezet.bftest;

import android.app.Application;

public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Config.initialize(this);
    }
}
