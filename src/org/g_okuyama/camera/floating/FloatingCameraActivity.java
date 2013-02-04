package org.g_okuyama.camera.floating;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

//import com.ad_stir.AdstirView;
//import com.ad_stir.AdstirTerminate;

public class FloatingCameraActivity extends Activity {
    private static final String TAG = "FloatingCamera";
    
    static final int MENU_DISP_GALLERY = 1;
    static final int MENU_DISP_SETTING = 2;

    static final int REQUEST_CODE = 1;
    static final int RESPONSE_COLOR_EFFECT = 1;
    static final int RESPONSE_SCENE_MODE = 2;
    static final int RESPONSE_WHITE_BALANCE = 3;
    static final int RESPONSE_PICTURE_SIZE = 4;
    static final int RESPONSE_SHOOT_NUM = 5;
    static final int RESPONSE_INTERVAL = 6;
    static final int RESPONSE_HIDDEN_SIZE = 7;
    
    SurfaceHolder mHolder;
    private int mCount = 0;
    private TextView mText;
    private Context mContext;
    private CameraPreview mPreview = null;
    SurfaceView mSurface;

    //撮影中か否か（0:停止中、1：撮影中）
    public int mMode = 0;
    private boolean mSleepFlag = false;
    private boolean mPinchFlag = false;
    
    private ImageButton mButton = null;
    private ImageButton mFocusButton = null;
    private ContentResolver mResolver;
    
    //全体の画面サイズ
    int mWidth = 0;
    int mHeight = 0;
    //プレビュー枠のサイズ
    int mPrevWidth = 0;
    int mPrevHeight = 0;
    
    int mHiddenSizeIdx = 0;
    
    ScaleGestureDetector mScaleGesture;    
    protected boolean isPinch = false;   

    //surfaceviewの左上の座標
    float mSurfaceX;
    float mSurfaceY;
    //surfaceviewのサイズ
    float mSurfaceWidth;
    float mSurfaceHeight;
    
    float mSpanPrev = 0.0f;
    
    //private AdstirView mAdstirView = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        mContext = getApplicationContext();
        
        mResolver = getContentResolver();
        
        //設定値の取得
        String effect = FloatingCameraPreference.getCurrentEffect(this);
        String scene = FloatingCameraPreference.getCurrentSceneMode(this);
        String white = FloatingCameraPreference.getCurrentWhiteBalance(this);
        String size = FloatingCameraPreference.getCurrentPictureSize(this);
        
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        mWidth = disp.getWidth();
        mHeight = disp.getHeight();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mSurface = (SurfaceView)findViewById(R.id.camera);
        mHolder = mSurface.getHolder();
        
        /*
         * プレビューサイズ設定
         * 大=4/5, 中=1/2, 小=1/4, 無し=1*1
         */
        int hide_width = 0;
        int hide_height = 0;
        mHiddenSizeIdx = Integer.parseInt(FloatingCameraPreference.getCurrentHiddenSize(this));
        switch(mHiddenSizeIdx){
            case 1:
                hide_width = mWidth * (4/5);
                hide_height = hide_width / 3 * 4;
                break;

            case 2:
                hide_width = mWidth / 2;
                hide_height = hide_width / 3 * 4;
                break;

            case 3:
                hide_width = mWidth / 4;
                hide_height = hide_width / 3 * 4;
                break;
                
            case 4:
                hide_height = 1;
                hide_width = 1;            
                break;                
        }

        mSurface.setLayoutParams(new FrameLayout.LayoutParams(hide_width, hide_height, Gravity.CENTER));
        
        mPreview = new CameraPreview(this);
        mPreview.setField(effect, scene, white, size, mWidth, mHeight);
        mHolder.addCallback(mPreview);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mText = (TextView)findViewById(R.id.text1);
    	mText.setText(/*mNum + System.getProperty("line.separator") + */"0" + " ");

    	//連写枚数設定
        String num = FloatingCameraPreference.getCurrentShootNum(this);
        if(!num.equals("0")){
            mPreview.setShootNum(Integer.valueOf(num));
        }
        
        //連写間隔設定
        String interval = FloatingCameraPreference.getCurrentInterval(this);
        if(!interval.equals("0")){
            mPreview.setInterval(Integer.valueOf(interval));
        }

        //register UI Listener
    	setListener();
    	
        mScaleGesture = new ScaleGestureDetector(this, new MySimpleOnScaleGestureListener());
        //SurfaceView上でタッチしたときのみViewを動かせる
        mSurface.setOnTouchListener(new MyOnTouchListener());        
    }
    
    class MyOnTouchListener implements OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //Log.d(TAG, "onTouch");

            //指の数によって呼ばれるリスナを変更
            int num = event.getPointerCount();
            if(num == 1){
                mGesture.onTouchEvent(event);
            }
            else if(num == 2){
                mScaleGesture.onTouchEvent(event);

                if(!mPinchFlag){
                    return true;
                }
                /*
                Log.d(TAG, "width=" + view.getWidth() + ",height=" + view.getHeight());
                Log.d(TAG, "x1=" + event.getX(0) + ",y1=" + event.getY(0));
                Log.d(TAG, "x2=" + event.getX(1) + ",y2=" + event.getY(1));
                Log.d(TAG, "rawx=" + event.getRawX() + ",rawy=" + event.getRawY());
                */

                //タッチの座標と現在のViewの大きさから左上の座標を求める(view.getX/getYはAPIレベル11からのため他の方法で行う)
                //float curWidth = view.getWidth();
                //float curHeight = view.getHeight();
                mSurfaceWidth = Math.abs(event.getX(1) - event.getX(0));
                mSurfaceHeight = Math.abs(event.getY(1) - event.getY(0));
                mSurfaceX = event.getRawX();
                mSurfaceY = event.getRawY() - mSurfaceHeight;
                //Log.d(TAG, "curWidth=" + curWidth + ",curHeight=" + curHeight);
                //Log.d(TAG, "mSurfaceX=" + mSurfaceX + ",mSurfaceY=" + mSurfaceY);
                //Log.d(TAG, "mSurfaceWidth=" + mSurfaceWidth + ",mSurfaceHeight=" + mSurfaceHeight);
            }               

            //return event.getPointerCount() == 1 ? mGesture.onTouchEvent(event) : mScaleGesture.onTouchEvent(event);
            return true;
        }
    }
    
    //ドラッグアンドドロップ用
    GestureDetector mGesture = new GestureDetector(mContext, new OnGestureListener(){
        int offsetX;
        int offsetY;
        int currentX;
        int currentY;

        @Override
        public boolean onDown(MotionEvent e) {
            offsetX = (int) e.getRawX();
            offsetY = (int) e.getRawY();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            int x = (int) e2.getRawX();
            int y = (int) e2.getRawY();
            int diffX = offsetX - x;
            int diffY = offsetY - y;

            currentX -= diffX;
            currentY -= diffY;
            mSurface.layout(currentX, currentY,
                    currentX + mSurface.getWidth(),
                    currentY + mSurface.getHeight());

            offsetX = x;
            offsetY = y;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return false;
        }

    });

    class MySimpleOnScaleGestureListener extends SimpleOnScaleGestureListener {   
        @Override  
        public boolean onScale(ScaleGestureDetector detector) {   
            //Log.d(TAG, "onScale");
            // 2つの指の距離を使って遊びを持たせる（一定の距離以下ならピンチとみなさない）
            float spanCurr = detector.getCurrentSpan();   
            if (Math.abs(spanCurr - mSpanPrev) < 30) {   
                return false;   
            }
            
            mPinchFlag = true;

            mSurface.layout(
                    (int)mSurfaceX,
                    (int)mSurfaceY,
                    (int)mSurfaceX + (int)mSurfaceWidth,
                    (int)mSurfaceY + (int)mSurfaceHeight);

            mSpanPrev = spanCurr; 
            return super.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //Log.d(TAG, "onScaleBegin");
            return super.onScaleBegin(detector);
        }
    }
    
    private void setListener(){
        mButton = (ImageButton)findViewById(R.id.imgbtn);
        mButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				if(mPreview != null){
					if(mMode == 0){
						mPreview.resumeShooting();
						mMode = 1;
                        //フォーカスボタンを見えなくする
                        mFocusButton.setVisibility(View.INVISIBLE);
					}
					else{
						mPreview.stopShooting();
						mMode = 0;
                        //フォーカスボタンを見えるようにする
                        mFocusButton.setVisibility(View.VISIBLE);
					}
				}
			}
        });
        
        mFocusButton = (ImageButton)findViewById(R.id.focusbtn);
        mFocusButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				if(mPreview != null){
						mPreview.doAutoFocus();
				}
			}
        });
    }
        
    public void onStart(){
    	//Log.d(TAG, "enter ContShooting#onStart");
    	
        super.onStart();
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            new AlertDialog.Builder(this)
            .setTitle(R.string.sc_alert_title)
            .setMessage(getString(R.string.sc_alert_sd))
            .setPositiveButton(R.string.sc_alert_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(RESULT_OK);
                }
            })
            .show();
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //オプションメニュー(ギャラリー)
        MenuItem prefGallery = menu.add(0, MENU_DISP_GALLERY, 0, R.string.sc_menu_gallery);
        prefGallery.setIcon(android.R.drawable.ic_menu_gallery);

        //オプションメニュー(設定)
        MenuItem prefSetting = menu.add(0, MENU_DISP_SETTING, 0, R.string.sc_menu_setting);
        prefSetting.setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }
    
    //オプションメニュー選択時のリスナ
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DISP_GALLERY:        	
        	startGallery();
            break;
            
        case MENU_DISP_SETTING:
            displaySettings();
        	break;
            
        default:
            //何もしない
        }

        return true;
    }
    
    private void startGallery(){
    	// ギャラリー表示
    	Intent intent = null;
    	try{
    	    // for Honeycomb
    	    intent = new Intent();
    	    intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.Gallery");
    	    startActivity(intent);
    	    return;
    	}
    	catch(Exception e){
    	    try{
    	        // for Recent device
    	        intent = new Intent();
    	        intent.setClassName("com.cooliris.media", "com.cooliris.media.Gallery");
    	        startActivity(intent);
    	    }
    	    catch(ActivityNotFoundException e1){
    	        try
    	        {
    	            // for Other device except HTC
    	            intent = new Intent(Intent.ACTION_VIEW);
    	            intent.setData(Uri.parse("content://media/external/images/media"));
    	            startActivity(intent);
    	        }
    	        catch (ActivityNotFoundException e2){
    	        	try{
    	        		// for HTC
    	        		intent = new Intent();
    	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumTabSwitchActivity");
    	        		startActivity(intent);
    	        	}
    	        	catch(ActivityNotFoundException e3){
        	        	try{
        	        		// for HTC
        	        		intent = new Intent();
        	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumMain.ActivityMainDropList");
        	        		startActivity(intent);
        	        	}
        	        	catch(ActivityNotFoundException e4){
        	            	Toast.makeText(this, R.string.sc_menu_gallery_ng, Toast.LENGTH_SHORT).show();
        	        	}
    	        	}
    	        }
    	    }
    	}
    }
    
    private void displaySettings(){
        Intent pref_intent = new Intent(this, FloatingCameraPreference.class);

        //色合い設定のリストを作成する
        List<String> effectList = null;
        if(mPreview != null){
            effectList = mPreview.getEffectList();
        }
        if(effectList != null){
            pref_intent.putExtra("effect", (String[])effectList.toArray(new String[0]));
        }

        //シーン
        List<String> sceneList = null;
        if(mPreview != null){
            sceneList = mPreview.getSceneModeList();
        }
        if(sceneList != null){
            pref_intent.putExtra("scene", (String[])sceneList.toArray(new String[0]));
        }

        //ホワイトバランス
        List<String> whiteList = null;
        if(mPreview != null){
            whiteList = mPreview.getWhiteBalanceList();
        }
        if(whiteList != null){
            pref_intent.putExtra("white", (String[])whiteList.toArray(new String[0]));
        }
        
        //画像サイズ
        List<String> sizeList = null;
        if(mPreview != null){
            sizeList = mPreview.getSizeList();
        }
        if(sizeList != null){
            pref_intent.putExtra("size", (String[])sizeList.toArray(new String[0]));
        }
        
        startActivityForResult(pref_intent, REQUEST_CODE);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        
        if(requestCode == REQUEST_CODE){
            if(resultCode == RESPONSE_COLOR_EFFECT){
                if(mPreview != null){
                    mPreview.setColorValue(data.getStringExtra("effect"));
                }            	
            }
            if(resultCode == RESPONSE_SCENE_MODE){
            	if(mPreview != null){
                    mPreview.setSceneValue(data.getStringExtra("scene"));
                }            	            	
            }
            if(resultCode == RESPONSE_WHITE_BALANCE){
            	if(mPreview != null){
                    mPreview.setWhiteValue(data.getStringExtra("white"));
                }            	
            }
            if(resultCode == RESPONSE_PICTURE_SIZE){
                if(mPreview != null){
                    mPreview.setSizeValue(data.getIntExtra("size", 0));
                }
            }
            if(resultCode == RESPONSE_SHOOT_NUM){
                if(mPreview != null){
                    mPreview.setShootNum(data.getIntExtra("shoot", 0));
                }
            }
            if(resultCode == RESPONSE_INTERVAL){
                if(mPreview != null){
                    mPreview.setInterval(data.getIntExtra("interval", 0));
                }
            }
            if(resultCode == RESPONSE_HIDDEN_SIZE){
                //隠しモードサイズ設定
                mHiddenSizeIdx = data.getIntExtra("preview_size", 0);
            }
        }
    }
    
    public void count(){
    	mText.setText(Integer.toString(++mCount) + " ");
    }
    
    public void displayStart(){
    	mButton.setImageResource(R.drawable.start);
    }
    
    public void displayStop(){
        mButton.setImageResource(R.drawable.stop);
    }
    
    public void saveGallery(ContentValues values){
		mResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
    }
    
    public void setMode(int mode){
        mMode = mode;
    }
    
    protected void onPause(){
        //Log.d(TAG, "enter ContShooting#onPause");    	
    	super.onPause();
    	
    	if(mSleepFlag){
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSleepFlag = false;
    	}
    	
        if(mPreview != null){
            mPreview.release();
        }
    	
        //アプリのキャッシュ削除
        deleteCache(getCacheDir());    	
    }
    
    protected void onResume(){
        super.onResume();
        
        //adstir設定→onCreateからonResumeに移動
        /*
        mAdstirView = new AdstirView(this, "74792bcf", 1);
        LinearLayout layout = (LinearLayout)findViewById(R.id.adspace);
        layout.addView(mAdstirView);
        */
        
        if(FloatingCameraPreference.isSleepMode(this)){
            if(!mSleepFlag){
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mSleepFlag = true;                
            }
        }
        else{
            if(mSleepFlag){
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mSleepFlag = false;                
            }
        }
    }
    
    protected void onDestroy(){
        //Log.d(TAG, "enter ContShooting#onDestroy");
    	super.onDestroy();
    	//AdstirTerminate.init(this);
    }
    
    protected void onRestart(){
    	super.onRestart();
    }
    
    public void finish(){
		System.exit(RESULT_OK);
    }
    
    public static boolean deleteCache(File dir) {
        if(dir==null) {
            return false;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteCache(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}