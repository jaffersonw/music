package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.music.MusicCoverView;
import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.service.AudioPlayService;
import me.zhengken.lyricview.LyricView;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private SeekBar mSeekBar;
    private TextView mCurrentTextView;
    private TextView mDurationTextView;
    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private MusicCoverView mAlbumImageView;
    private FrameLayout mFrameLayout;
    private ImageButton mPauseButton;
    private ImageButton mNextButton;
    private ImageButton mPreviousButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mReturnButton;
    private LyricView mLyricView;
    private View mPauseButtonBackground;

    private Object mLock = new Object();

    private String mTitle;
    private String mArtist;
    private int mDuration;
    private String mPath;

    private boolean onDrag = false;
    private boolean mIsShuffle;
    private boolean mIsPlay;

    private boolean mIsLoadAlbum = false;

    private int mLoopWay;

    private boolean mIsAlbum;


    private BroadcastReceiver mPlayingReceiver;
    private BroadcastReceiver mVisualizerReceiver;
    private BroadcastReceiver mPlayEventReceiver;

    private long mStartTime;

    /**
     * UI更新
     * @param bundle 包含音乐信息的bundle
     */
    private void updateUI(Bundle bundle) {
        synchronized (mLock) {
            String title = bundle.getString(AudioPlayService.AUDIO_TITLE_STR);
            String artist = bundle.getString(AudioPlayService.AUDIO_ARTIST_STR);
            String path = bundle.getString(AudioPlayService.AUDIO_PATH_STR);

            if (mTitle == null || !mTitle.equals(title)) {
                mTitleTextView.setText(mTitle = title);
            }
            if (mArtist == null || !mArtist.equals(artist)) {
                mArtistTextView.setText(mArtist = artist);
            }


            // 特殊处理，停止旋转需要时间
            boolean isPlay = bundle.getBoolean(AudioPlayService.AUDIO_IS_PLAYING_BOOL, false);
            if (isPlay != mAlbumImageView.isRunning()) {
               if (isPlay) {
                   mAlbumImageView.start();
               } else {
                   mAlbumImageView.stop();
               }
            }

            int duration = bundle.getInt(AudioPlayService.AUDIO_DURATION_INT, 0);
            int current = Math.min(bundle.getInt(AudioPlayService.AUDIO_CURRENT_INT, 0), duration);

            if (!onDrag) {
                int min = 0, max = mSeekBar.getMax();
                int pos = 0;
                if (duration != 0 && (max - min) != 0) {
                    pos = (int) ((current * 1.0 / duration) * (max - min));
                }
                mSeekBar.setProgress(pos);
                mLyricView.setCurrentTimeMillis(current);
            }

            int totalSecond = current / 1000;
            int minute = totalSecond / 60;
            int second = totalSecond % 60;
            if (!onDrag) {
                mCurrentTextView.setText(String.format("%02d:%02d", minute, second));
            }
            if (mDuration != duration) {
                totalSecond = (mDuration = duration) / 1000;
                minute = totalSecond / 60;
                second = totalSecond % 60;
                mDurationTextView.setText(String.format("%02d:%02d", minute, second));
            }
        }
    }

    // 下一首
    private void nextMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.NEXT_ACTION);
        startService(intent);
    }

    // 上一首
    private void previousMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PREVIOUS_ACTION);
        startService(intent);
    }

    // seek歌曲
    private void seekMusic(int seekTo) {
        Intent intent = new Intent(PlayerActivity.this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.SEEK_ACTION);
        intent.putExtra(AudioPlayService.AUDIO_SEEK_POS_INT, seekTo);
        startService(intent);
    }

    // 切换暂停
    private void pauseMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        if (mIsPlay) {
            intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PAUSE_ACTION);
        } else {
            intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.REPLAY_ACTION);
        }

        enableButton(false, false);
        startService(intent);
    }


    /**
     * 切换播放顺序
     */
    private void changeListOrder() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LIST_SHUFFLE_ACTION);
        intent.putExtra(AudioPlayService.LIST_SHUFFLE_BOOL, mIsShuffle = !mIsShuffle);
        startService(intent);
        Toast.makeText(this, "切换到" + (mIsShuffle ? "随机播放" : "顺序播放"), Toast.LENGTH_SHORT).show();
        if (mIsShuffle) {
            mShuffleButton.setImageResource(R.drawable.btn_playback_shuffle_all);
        } else {
            mShuffleButton.setImageResource(R.drawable.shuffle);
        }
    }

    /**
     * 切换循环方式
     */
    public void changeLoopWay() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LOOP_ACTION);
        Toast.makeText(this, "切换到" + (mIsShuffle ? "单曲循环" : "循环列表"), Toast.LENGTH_SHORT).show();

        if (mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
            mLoopWay = AudioPlayService.LIST_LOOP;
            mRepeatButton.setImageResource(R.drawable.btn_playback_repeat_all);
        } else if (mLoopWay == AudioPlayService.LIST_LOOP) {
            mLoopWay = AudioPlayService.AUDIO_REPEAT;
            mRepeatButton.setImageResource(R.drawable.btn_playback_repeat_one);
        } else {
            mLoopWay = AudioPlayService.LIST_NOT_LOOP;
            mRepeatButton.setImageResource(R.drawable.repeat);
        }
        intent.putExtra(AudioPlayService.LOOP_WAY_INT, mLoopWay);
        startService(intent);
    }

    /** 切换按钮的可用状态 */
    public void enableButton(boolean enable) {
        enableButton(enable, false);
    }
    public void enableButton(boolean enable, boolean grey) {
        mPauseButton.setEnabled(enable);
        mPauseButtonBackground.setEnabled(enable);
        mNextButton.setEnabled(enable);
        mPreviousButton.setEnabled(enable);

        if (grey && !enable) {
            mPauseButtonBackground.setBackgroundResource(R.drawable.shadowed_circle_grey);
        } else {
            mPauseButtonBackground.setBackgroundResource(R.drawable.shadowed_circle_red);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mIsShuffle = getIntent().getBooleanExtra(AudioPlayService.LIST_SHUFFLE_BOOL, false);
        mLoopWay = getIntent().getIntExtra(AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
        mPath = getIntent().getStringExtra(AudioPlayService.AUDIO_PATH_STR);
        mIsPlay = getIntent().getBooleanExtra(AudioPlayService.AUDIO_IS_PLAYING_BOOL, false);

        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mCurrentTextView = (TextView) findViewById(R.id.current);
        mDurationTextView = (TextView) findViewById(R.id.duration);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mArtistTextView = (TextView) findViewById(R.id.artist);
        mFrameLayout = (FrameLayout) findViewById(R.id.album);
        mPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        mNextButton = (ImageButton) findViewById(R.id.nextButton);
        mPreviousButton = (ImageButton) findViewById(R.id.previousButton);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatButton);
        mShuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
        mReturnButton = (ImageButton) findViewById(R.id.returnButton);
        mLyricView = (LyricView) findViewById(R.id.lyric_view);
        mPauseButtonBackground = findViewById(R.id.playPauseButtonBackground);

        mTitleTextView.setHorizontallyScrolling(true);
        mTitleTextView.setSelected(true);
        mArtistTextView.setHorizontallyScrolling(true);
        mArtistTextView.setSelected(true);

        mPauseButton.setOnClickListener(this);
        mPauseButtonBackground.setOnClickListener(this);
        mRepeatButton.setOnClickListener(this);
        mShuffleButton.setOnClickListener(this);
        mReturnButton.setOnClickListener(this);


        mIsAlbum = true;
        mAlbumImageView = new MusicCoverView(this);
        mAlbumImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mAlbumImageView.setCallbacks(new MusicCoverView.Callbacks() {
            @Override
            public void onMorphEnd(MusicCoverView coverView) {
            }

            @Override
            public void onRotateEnd(MusicCoverView coverView) {
                enableButton(true, true);
            }
        });
        mAlbumImageView.setShape(MusicCoverView.SHAPE_CIRCLE);
        mFrameLayout.addView(mAlbumImageView);
        // 进度条事件
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int min = 0, max = seekBar.getMax();
                    int changedCurrent = (int) (mDuration * 1.0 / (max - min) * progress);
                    int totalSecond = changedCurrent / 1000;
                    int minute = totalSecond / 60;
                    int second = totalSecond % 60;
                    mCurrentTextView.setText(String.format("%02d:%02d", minute, second));
                    mLyricView.setCurrentTimeMillis(changedCurrent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                synchronized (mLock) {
                    onDrag = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int min = 0, max = seekBar.getMax();
                int changedCurrent = (int) (mDuration * 1.0 / (max - min) * seekBar.getProgress());
                seekMusic(changedCurrent);
                synchronized (mLock) {
                    onDrag = false;
                }
            }
        });



        updateUI(getIntent().getExtras());

        // 更新UI广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent.getExtras());
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));





        // 事件广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayEventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(AudioPlayService.EVENT_KEY);
                if (event == null) {
                    return;
                }
                switch (event) {
                    case AudioPlayService.PAUSE_EVENT:
                        synchronized (mLock) {
                            mPauseButton.setImageResource(R.drawable.play_light);
                            mAlbumImageView.stop();
                            enableButton(true);

                        }
                        mIsPlay = false;
                        break;
                    case AudioPlayService.REPLAY_EVENT:
                        synchronized (mLock) {
                            mPauseButton.setImageResource(R.drawable.pause_light);
                            mAlbumImageView.start();
                            enableButton(true);
                        }
                        mIsPlay = true;
                        break;
                    case AudioPlayService.PLAY_EVENT:
                        synchronized (mLock) {
                            boolean isPlay = intent.getBooleanExtra(AudioPlayService.AUDIO_PLAY_NOW_BOOL, false);
                            if (isPlay) {
                                mPauseButton.setImageResource(R.drawable.pause_light);
                                mAlbumImageView.start();
                                enableButton(true);
                                mIsPlay = true;
                            } else {
                                mPauseButton.setImageResource(R.drawable.play_light);
                                if (mAlbumImageView.isRunning()) {
                                    if (mIsAlbum) {
                                        enableButton(false, true);
                                        mAlbumImageView.stop();
                                    } else {
                                        enableButton(true);
                                    }
                                } else {
                                    enableButton(true);
                                }
                                mIsPlay = false;
                            }
                        }
                        break;
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayingReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVisualizerReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayEventReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUI(intent.getExtras());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton: case R.id.playPauseButtonBackground:
                pauseMusic();
                break;
            case R.id.nextButton:
                nextMusic();
                break;
            case R.id.previousButton:
                previousMusic();
                break;
            case R.id.shuffleButton:
                changeListOrder();
                break;
            case R.id.repeatButton:
                changeLoopWay();
                break;
            case R.id.returnButton:
                finish();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
