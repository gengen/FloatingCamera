package org.g_okuyama.camera.floating;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class FloatingCameraPreference extends PreferenceActivity implements OnPreferenceChangeListener{
    public static final String TAG = "FloatingCamera";

    public static final String DEFAULT_SHOOT_NUM = "0";
    public static final String DEFAULT_INTERVAL = "0";
    public static final String DEFAULT_PREVIEW_SIZE = "2";
    
    static final int COLOR_EFFECT = 1;
    static final int SCENE_MODE = 2;
    static final int WHITE_BALANCE = 3;
    static final int PICTURE_SIZE = 4;
    static final int SHOOT_NUM = 5;
    static final int INTERVAL = 6;
    static final int PREVIEW_SIZE = 7;
    static String[] sSizeList = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.preference);
        
        Bundle extras = getIntent().getExtras();
        String[] effectList = null;
        String[] whiteList = null;
        String[] sceneList = null;
        if(extras != null){
            effectList = extras.getStringArray("effect");
            sceneList = extras.getStringArray("scene");
            whiteList = extras.getStringArray("white");
            sSizeList = extras.getStringArray("size");
        }

        //色合い
        ListPreference colorPref = (ListPreference)this.findPreference("color_effect");
        if(effectList != null){
        	colorPref.setOnPreferenceChangeListener(this);
        	colorPref.setSummary(getCurrentEffect(this));
        	colorPref.setEntries(effectList);
        	colorPref.setEntryValues(effectList);
        }
        else{
        	colorPref.setEnabled(false);
        }
                
        //シーン
        ListPreference scenePref = (ListPreference)this.findPreference("scene_mode");
        if(sceneList != null){
        	scenePref.setOnPreferenceChangeListener(this);
        	scenePref.setSummary(getCurrentSceneMode(this));
        	scenePref.setEntries(sceneList);
        	scenePref.setEntryValues(sceneList);
        }
        else{
        	scenePref.setEnabled(false);
        }

        //ホワイトバランス
        ListPreference whitePref = (ListPreference)this.findPreference("white_balance");
        if(whiteList != null){
        	whitePref.setOnPreferenceChangeListener(this);
        	whitePref.setSummary(getCurrentWhiteBalance(this));
        	whitePref.setEntries(whiteList);
        	whitePref.setEntryValues(whiteList);
        }
        else{
        	whitePref.setEnabled(false);
        }
        
        //画像サイズ
        ListPreference sizePref = (ListPreference)this.findPreference("picture_size");
        String size = getCurrentPictureSize(this);
        if(sSizeList != null){
            sizePref.setOnPreferenceChangeListener(this);
            if(!size.equals("0")){
                sizePref.setSummary(size);
            }
            else{
                sizePref.setSummary(getString(R.string.picture_size_summary));                
            }
            sizePref.setEntries(sSizeList);

            /*
            String[] valueList = new String[sSizeList.length];
            for(int i=0; i<sSizeList.length; i++){
                valueList[i] = String.valueOf(i);
            }
            Log.d(TAG, "size = " + valueList);
            */
            sizePref.setEntryValues(sSizeList);
        }
        else{
            sizePref.setEnabled(false);
        }
        
        //連写枚数
        ListPreference shootPref = (ListPreference)this.findPreference("shoot_num");
        shootPref.setOnPreferenceChangeListener(this);
        String str = getCurrentShootNum(this);
        if(str.equals("0")){
            shootPref.setSummary((CharSequence)getString(R.string.shoot_num_unlimited));
        }
        else{
            shootPref.setSummary(str);
        }

        //連写間隔
        ListPreference intPref = (ListPreference)this.findPreference("interval");
        intPref.setOnPreferenceChangeListener(this);
        String intStr = getCurrentInterval(this);
        if(intStr.equals("0")){
            intPref.setSummary((CharSequence)getString(R.string.interval_not_set));
        }
        else{
            intPref.setSummary(intStr + " " + getString(R.string.str_sec));
        }
        
        //プレビューサイズ
        ListPreference previewPref = (ListPreference)this.findPreference("preview_size");
        previewPref.setOnPreferenceChangeListener(this);
        String previewStr = getCurrentPreviewSize(this);
        String summaryStr = getString(R.string.preview_size_summary);
        if(previewStr.equals("1")){
            summaryStr = getString(R.string.preview_size_large);
        }
        else if(previewStr.equals("2")){
            summaryStr = getString(R.string.preview_size_middle);            
        }
        else if(previewStr.equals("3")){
            summaryStr = getString(R.string.preview_size_small);            
        }
        else if(previewStr.equals("4")){
            summaryStr = getString(R.string.preview_size_none);
        }
        previewPref.setSummary(summaryStr);
    }
    
    public static String getCurrentEffect(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("color_effect", /*default*/Camera.Parameters.EFFECT_NONE);
    }

    public static String getCurrentWhiteBalance(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("white_balance", /*default*/Camera.Parameters.WHITE_BALANCE_AUTO);
    }

    public static String getCurrentSceneMode(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("scene_mode", /*default*/Camera.Parameters.SCENE_MODE_AUTO);
    }

    public static String getCurrentPictureSize(Context c){
    	return /*String str = */PreferenceManager.getDefaultSharedPreferences(c)
    			.getString("picture_size", /*default*/"0");
    }
    
    public static String getCurrentShootNum(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("shoot_num", /*default*/DEFAULT_SHOOT_NUM);
    }

    public static String getCurrentInterval(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("interval", /*default*/DEFAULT_INTERVAL);
    }
    
    public static boolean isAutoShoot(Context c){
    	return PreferenceManager.getDefaultSharedPreferences(c)
    			.getBoolean("auto_shoot", false);
    }

    public static String getCurrentPreviewSize(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getString("preview_size", /*default*/DEFAULT_PREVIEW_SIZE);
    }
    
    public static boolean isSleepMode(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getBoolean("display_sleep", true);
    }

	public boolean onPreferenceChange(Preference pref, Object newValue) {
		final CharSequence value = (CharSequence)newValue;
		if(value == null){
			return false;
		}
		
		if(pref.getKey().equals("color_effect")){
		    //選択されたら、設定画面を終了し、即反映させる
            Intent intent = new Intent();
            intent.putExtra("effect", value);
            this.setResult(COLOR_EFFECT, intent);
            finish();
		}
		else if(pref.getKey().equals("scene_mode")){
		    Intent intent = new Intent();
		    intent.putExtra("scene", value);
		    this.setResult(SCENE_MODE, intent);
		    finish();
		}
        else if(pref.getKey().equals("white_balance")){
            Intent intent = new Intent();
            intent.putExtra("white", value);
            this.setResult(WHITE_BALANCE, intent);
            finish();
        }
		else if(pref.getKey().equals("picture_size")){
            Intent intent = new Intent();
            
            for(int i=0; i<sSizeList.length; i++){
            	if(sSizeList[i].equals(value)){
                    intent.putExtra("size", i);
                    
                    //Log.d(TAG, "result data = " + i);
                    
                    break;
            	}
            }
            this.setResult(PICTURE_SIZE, intent);
            finish();
		}
        else if(pref.getKey().equals("shoot_num")){
            Intent intent = new Intent();
            intent.putExtra("shoot", Integer.valueOf((String)value));
            this.setResult(SHOOT_NUM, intent);
            finish();
        }
        else if(pref.getKey().equals("interval")){
            Intent intent = new Intent();
            intent.putExtra("interval", Integer.valueOf((String)value));
            this.setResult(INTERVAL, intent);
            finish();
        }
        else if(pref.getKey().equals("preview_size")){
            Intent intent = new Intent();
            intent.putExtra("preview_size", Integer.valueOf((String)value));
            this.setResult(PREVIEW_SIZE, intent);
            finish();
        }
		
		return true;
	}
}
