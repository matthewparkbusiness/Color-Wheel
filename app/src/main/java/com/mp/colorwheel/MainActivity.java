package com.mp.colorwheel;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity {

    public static MainActivity context;
    public static boolean starting = true;
    public static int highestScore = 0;

    public static SharedPreferences sharedPref;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if(sharedPref.contains(getString(R.string.save_data28))) {
            highestScore = sharedPref.getInt(getString(R.string.save_data28), -1);

        }
        else{
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.save_data28), 0);
            editor.commit();

        }

        if(sharedPref.contains(getString(R.string.mode))) {
            GameView.mode = sharedPref.getInt(getString(R.string.mode), 1);

        }
        else{
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.mode), 1);
            editor.commit();

            GameView.mode = 1;

        }
        GameView.initMode(GameView.mode);

        GameView gv = new GameView(this);
        this.setContentView(gv);
    }

    public void onPause(){
        super.onPause();
        System.exit(0);
    }


}
