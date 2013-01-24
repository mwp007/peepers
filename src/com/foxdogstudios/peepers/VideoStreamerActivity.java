package com.foxdogstudios.peepers;

import java.io.InputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class VideoStreamerActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = VideoStreamerActivity.class.getSimpleName();
    private static final String LOCAL_SERVER_SOCKET_NAME = "com.foxdogstudios.peepers";

    private LocalServerSocket mServerSocket = null;
    private LocalSocket mReceiver = null;
    private LocalSocket mSender = null;

    private boolean mIsRecording = false;
    private MediaRecorder mRecorder = null;
    private RtpStreamer mRtpStreamer = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (tryOpenSockets() && tryStartRtpStreamer())
        {
            initialisePreviewDisplay();
        } // if
    } // onCreate(Bundle)

    private boolean tryOpenSockets()
    {
        try
        {
            mServerSocket = new LocalServerSocket(LOCAL_SERVER_SOCKET_NAME);
        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "An exception occured while creating the local server socket", e);
        } // catch

        if (mServerSocket != null)
        {
            mReceiver = new LocalSocket();
            try
            {
                mReceiver.connect(mServerSocket.getLocalSocketAddress());
            } // try
            catch (final IOException e)
            {
                Log.e(TAG, "An exception occured while connecting to the local server socket", e);
                mReceiver = null;
            } // catch
        } // if

        if (mReceiver != null)
        {
            try
            {
                mSender = mServerSocket.accept();
            } // try
            catch (final IOException e)
            {
                Log.e(TAG, "An exception occured while waiting for a client to connect to the "
                        + "local server socket", e);
            } // catch
        } // if

        return mServerSocket != null && mReceiver != null && mServerSocket != null;
    } // tryOpenSockets()

    private boolean tryStartRtpStreamer()
    {
        final InputStream videoStream;
        try
        {
            videoStream = mReceiver.getInputStream();
        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "An exception occurred while getting the receive socket input stream", e);
            return false;
        } // catch
        mRtpStreamer = new RtpStreamer(videoStream);
        mRtpStreamer.start();
        return true;
    } // start

    private void initialisePreviewDisplay()
    {
        final SurfaceHolder holder = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);
    } // initialisePreviewDisplay()

    @Override
    protected void onDestroy()
    {
        if (mRtpStreamer != null)
        {
            mRtpStreamer.stop();
            mRtpStreamer = null;
        } // if

        if (mSender != null)
        {
            try
            {
                mSender.close();
            } // try
            catch (final IOException e)
            {
                Log.e(TAG, "An exception occured when attempting to close the sender socket", e);
            } // catch
            mSender = null;
        } // if

        if (mReceiver != null)
        {
            try
            {
                mReceiver.close();
            } // try
            catch (final IOException e)
            {
                Log.e(TAG, "An exception occured when attempting to close the receiver socket", e);
            } // catch
            mReceiver = null;
        } // if

        if (mServerSocket != null)
        {
            try
            {
                mServerSocket.close();
            } // try
            catch (final IOException e)
            {
                Log.e(TAG, "An exception occured when attempting to close server socket", e);
            } // catch
            mServerSocket = null;
        } // if

        super.onDestroy();
    } // onDestroy()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
            final int height)
    {
        // Ignore
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mRecorder = new MediaRecorder();
        mRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mRecorder.setOutputFile(mSender.getFileDescriptor());
        mRecorder.setPreviewDisplay(holder.getSurface());

        try
        {
            mRecorder.prepare();
        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "Cannot start recording", e);
            mRecorder.release();
            mRecorder = null;
            return;
        } // catch

        mRecorder.start();
        mIsRecording = true;
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        if (mIsRecording)
        {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            mIsRecording = false;
        } // if
    } // surfaceDestroyed(SurfaceHolder)


} // class VideoStreamerActivity

