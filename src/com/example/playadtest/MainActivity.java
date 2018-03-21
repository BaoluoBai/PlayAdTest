package com.example.playadtest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.example.util.SerialPortUtil;
import com.example.util.SerialPortUtil.OnDataReceiveListener;
import com.example.util.Util;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseIntArray;

import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;


public class MainActivity extends Activity implements SurfaceHolder.Callback, OnInfoListener{
	private ImageView iv_adpic;
	
	private VideoView vv_admed;
	
	private FrameLayout fl_outframe;
	
	
	public static final String ADFILE_PATH = "/sdcard/advertisement";
	
	List<String> ad_list = new ArrayList<String>();
	
	int flag_ad;
	
	int index_picture = 1;
	
	int index_video = 2;
	
	int index_monitor = 3;
	
	int length_adlist = 0;
	
	int position = 0;
	
	boolean isAd = false;
	
	Dialog dialog;
	
	private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private boolean isRecording = false;//标记是否已经在录制
    private MediaRecorder mRecorder;//音视频录制类
    private Camera mCamera = null;//相机
    private Camera.Size mSize = null;//相机的尺寸
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;//默认后置摄像头
    private static final SparseIntArray orientations = new SparseIntArray();//手机旋转对应的调整角度
    
    public SerialPortUtil serialPortOne = null;
    
    private static final String PORT_ONE = "/dev/ttymxc1";
	private static final int BAUDRATE = 9600;
	
	private ViewPager vpager_one;
    private ArrayList<View> aList;
    private MainScreenAdapter mAdapter;
    
    private DisplayManager mDisplayManager;// 屏幕管理
    private Display[] displays;// 屏幕数组
    private DifferentDisplay mPresentation;
    
    static {
        orientations.append(Surface.ROTATION_0, 90);
        orientations.append(Surface.ROTATION_90, 0);
        orientations.append(Surface.ROTATION_180, 270);
        orientations.append(Surface.ROTATION_270, 180);
    }
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        initView();
        serialPortOne = new SerialPortUtil(PORT_ONE, BAUDRATE);
        ad_list.clear();
        ad_list = Util.doSearchAd(ADFILE_PATH);
        if(!ad_list.isEmpty()){
        	length_adlist = ad_list.size();
    	}
        Timer time = new Timer();
        time.schedule(play, 200, 20000);
        serialPortOne.setOnDataReceiveListener(new OnDataReceiveListener() {
			
			@Override
			public void onDataReceive(byte[] buffer, int size) {
				// TODO Auto-generated method stub
				
			}
		});
//        startRecord();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        initCamera();
//        startRecord();
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
    }
    
    private void initView(){
    	mDisplayManager = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);
    	displays = mDisplayManager.getDisplays();
        if (displays.length > 1) {
            if (null == mPresentation) {

                mPresentation = new DifferentDisplay(getApplicationContext(), displays[1]);// displays[1]是副屏
                mPresentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                mPresentation.setContentView(R.layout.liststation);
                mPresentation.show();
            }
        }
    	vpager_one = (ViewPager) findViewById(R.id.viewpager);

        aList = new ArrayList<View>();
        LayoutInflater li = getLayoutInflater();
        aList.add(li.inflate(R.layout.route_line,null,false));
        aList.add(li.inflate(R.layout.next_station,null,false));
        mAdapter = new MainScreenAdapter(aList);
        vpager_one.setAdapter(mAdapter);
        vpager_one.setCurrentItem(1);  
    	fl_outframe =  (FrameLayout) findViewById(R.id.fl_outfram);
    	mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
    	iv_adpic = (ImageView) findViewById(R.id.iv_adpic);
    	
    	SurfaceHolder holder = mSurfaceView.getHolder();// 取得holder
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.setKeepScreenOn(true);
        holder.addCallback(this); // holder加入回调接口
    }
    
    TimerTask play = new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			runOnUiThread(new Runnable() {
				public void run() {
					playAd(ad_list);
				}
			});
		}
	};
	
    
    private void playAd(List<String> adlist){
    	if(isAd){
    		fl_outframe.setVisibility(View.VISIBLE);
    		isAd = false;
    		if(position<adlist.size()){
    			if(adlist.get(position).endsWith(".mp4")||adlist.get(position).endsWith(".avi")){
    				Log.d("视频路径", adlist.get(position));
    				playVideo(adlist.get(position));
    			}else if(adlist.get(position).endsWith(".jpg")||adlist.get(position).endsWith(".png")){
    				Log.d("图片路径", adlist.get(position));
    				playPic(adlist.get(position));
    			}
    		}else{
    			position = 0;
    			if(adlist.get(position).endsWith(".mp4")||adlist.get(position).endsWith(".avi")){
    				Log.d("视频路径", adlist.get(position));
    				playVideo(adlist.get(position));
    			}else if(adlist.get(position).endsWith(".jpg")||adlist.get(position).endsWith(".png")){
    				Log.d("图片路径", adlist.get(position));
    				playPic(adlist.get(position));
    			}
    		}
    	}else{
    		//播放监控
    		isAd = true;
//    		vv_admed.setVisibility(View.GONE);
    		iv_adpic.setVisibility(View.GONE);
    		if(dialog!=null){
    			dialog.hide();
    		}
    		
    	}
    }
    
    private void playVideo(String path){
    	position++;
    	if(dialog==null){
    		dialog = new Dialog(MainActivity.this);
    		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        	dialog.setContentView(R.layout.pop_video);
        	dialog.getWindow().setDimAmount(0);
    	}
    	
    	Window window = dialog.getWindow();
    	WindowManager.LayoutParams lp = window.getAttributes(); 
    	lp.x = 395; 
    	lp.y = -245; 
    	lp.height=591; 
    	lp.width=1067;  
    	window.setAttributes(lp); 
    	dialog.show();
    	vv_admed=(VideoView)dialog.findViewById(R.id.vv_admed);
    	vv_admed.setVideoPath(path);
    	vv_admed.getHolder().setFixedSize(960, 540);
    	vv_admed.start();
    	vv_admed.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				mp.setLooping(false);
			}
		});
    	vv_admed.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				
			}
		});
    	
//    	View contentView = LayoutInflater.from(MainActivity.this).inflate(R.layout.pop_video, null);  
////        mPopWindow = new PopupWindow(contentView, 960, 540, true);
//    	mPopWindow = new PopupWindow(contentView, 960, 540, true);
//        mPopWindow.setContentView(contentView);  
//        vv_admed = (VideoView) contentView.findViewById(R.id.vv_admed);
//    	vv_admed.setVisibility(View.VISIBLE);
//    	iv_adpic.setVisibility(View.GONE);
//    	vv_admed.setVideoPath(path);
//    	vv_admed.getHolder().setFixedSize(960, 540);
//    	vv_admed.start();
//    	vv_admed.setOnPreparedListener(new OnPreparedListener() {
//			
//			@Override
//			public void onPrepared(MediaPlayer mp) {
//				// TODO Auto-generated method stub
//				mp.setLooping(false);
//			}
//		});
//    	vv_admed.setOnCompletionListener(new OnCompletionListener() {
//			
//			@Override
//			public void onCompletion(MediaPlayer mp) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//    	View rootview = LayoutInflater.from(MainActivity.this).inflate(R.layout.activity_main, null);  
////        mPopWindow.showAtLocation(rootview, Gravity.TOP, 700, 0); 
//    	mPopWindow.showAtLocation(rootview, Gravity.CENTER, 0, 0);
    }
    
    private void playPic(String path){
    	iv_adpic.setVisibility(View.VISIBLE);
    	if(dialog!=null){
    		dialog.hide();
    	}
    	
//    	vv_admed.setVisibility(View.GONE);
    	position++;
    	BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeFile(path);
        iv_adpic.setImageBitmap(bm);
    }
    
    @SuppressWarnings("deprecation")
	private void initCamera() {
        if (Camera.getNumberOfCameras() == 2) {
            mCamera = Camera.open(mCameraFacing);
        } else {
            mCamera = Camera.open();
        }

        CameraSizeComparator sizeComparator = new CameraSizeComparator();
        Camera.Parameters parameters = mCamera.getParameters();

        if (mSize == null) {
            List<Camera.Size> vSizeList = parameters.getSupportedPreviewSizes();
            Collections.sort(vSizeList, sizeComparator);

            for (int num = 0; num < vSizeList.size(); num++) {
                Camera.Size size = vSizeList.get(num);
                Log.d("Size：", size.width+".."+size.height);
                if (size.width >= 800 && size.height >= 480) {
                    this.mSize = size;
                    break;
                }
            }
            mSize = vSizeList.get(0);

            List<String> focusModesList = parameters.getSupportedFocusModes();

            //增加对聚焦模式的判断
            if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = orientations.get(rotation);
        mCamera.setDisplayOrientation(orientation);
    }

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		// TODO Auto-generated method stub
		if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
			stopRecord();
			startRecord();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mSurfaceHolder = holder;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		// 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给mSurfaceHolder
        mSurfaceHolder = holder;
        if (mCamera == null) {
            return;
        }
        try {
            //设置显示
        	mCamera.stopPreview();
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
            finish();
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// surfaceDestroyed的时候同时对象设置为null
        if (isRecording && mCamera != null) {
            mCamera.lock();
        }
        mSurfaceView = null;
        mSurfaceHolder = null;
        releaseMediaRecorder();
        releaseCamera();
	}
	
	private void releaseMediaRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }
	
	private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.unlock();
                mCamera.release();
            }
        } catch (RuntimeException e) {
        } finally {
            mCamera = null;
        }
    }
	
	public void startRecord(){
		File file_record = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis()+".mp4");
		if (mRecorder == null) {
            mRecorder = new MediaRecorder(); // 创建MediaRecorder
        }
        if (mCamera != null) {
        	try {
				mCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            mCamera.stopPreview();
            mCamera.unlock();
            mRecorder.setCamera(mCamera);
        }
        try {
            // 设置音频采集方式
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            //设置视频的采集方式
            mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            //设置文件的输出格式
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//aac_adif， aac_adts， output_format_rtp_avp， output_format_mpeg2ts ，webm
            //设置audio的编码格式
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //设置video的编码格式
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //设置录制的视频编码比特率
            mRecorder.setVideoEncodingBitRate(1024 * 1024);
            //设置录制的视频帧率,注意文档的说明:
            mRecorder.setVideoFrameRate(15);
            //设置要捕获的视频的宽度和高度
            mSurfaceHolder.setFixedSize(720, 576);//最高只能设置640x480

            mRecorder.setVideoSize(720, 576);//最高只能设置640x480
            //设置记录会话的最大持续时间（毫秒）
            mRecorder.setMaxDuration(60 * 1000);
            mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
//            String path = getExternalCacheDir().getPath();
            if (file_record != null) {
//                File dir = new File(path + "/videos");
//                if (!dir.exists()) {
//                    dir.mkdir();
//                }
//                path = dir + "/" + getCurrentTime() + ".mp4";
                //设置输出文件的路径
            	Log.d("路径", file_record.getAbsolutePath());
                mRecorder.setOutputFile(file_record.getAbsolutePath());
                //准备录制
                mRecorder.prepare();
                //开始录制
                mRecorder.start();
                isRecording = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
    
	private class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }
	
	public void stopRecord(){
		 try {
	         //停止录制
			 mRecorder.stop();
	         //重置
	         mRecorder.reset();
	     } catch (Exception e) {
	         e.printStackTrace();
	     }
	     isRecording = false;
	}
	
}
