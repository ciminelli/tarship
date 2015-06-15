package com.mentalergonomics;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;

/**
 * A class that manages communication with the blimp (raspberry pi) using JSON-RPC.
 *
 * @authors Jordan Rickman, Eric Ciminelli
 *
 */
public class BlimpController
{
    public static final String SERVER_ADDR = "http://192.168.4.3:10101"; // Address of the raspberry pi

    private JSONRPC2Session rpc; 	// Connection to the server
    private int nextRequestID;		// Counter for IDs of the requests sent (we don't really care about this, but the protocol requires it)
    protected boolean serverOnline; // Is the server currently online?

    protected int axlePosition; 	// Current position of the axle (Servoblaster 15%-100%)
    protected int camera0Position, camera1Position; 	// Current position of the cameras (Servoblaster 10%-87%)
    protected int combinedSpeed, leftSpeed, rightSpeed; // 1-10 
    
    public BlimpController()
    {
        nextRequestID = 0;
    	if (rpc == null) // Initialization failed, server cannot be contacted
            serverOnline = false;
    	
        try {
            URL url = new URL(SERVER_ADDR);
            rpc = new JSONRPC2Session(url);
            serverOnline = true;
        } catch (Exception e) {
            e.printStackTrace();
            serverOnline = false;
        }

        /* Set the positions to default */
        axlePosition 	= 66;
        camera0Position = 52;
        camera1Position = 80;
        combinedSpeed 	= 5;
        leftSpeed 		= 0;
        rightSpeed 		= 0;
    }
    
    /* Ping the server to check if it is online */
    public boolean ping()
    {
        final boolean[] result = new boolean[1]; // Variable turned into single element array to avoid scope error
        try {
            new TimeLimit(2000, new ExceptionRunnable()
            {
                @Override
                public void run() throws Exception
                {
                    JSONRPC2Request request = new JSONRPC2Request("PING", ++nextRequestID);
                    JSONRPC2Response response = rpc.send(request);
                    if (response.getResult().equals(true))
                        result[0] = true;
                    else
                        result[0] = false;
                }
            }).run();

        } catch (Exception e) {
            return false;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result[0];
    }
    /* Ping the server without using a TimeLimit */
    public boolean simplePing()
    {
        boolean result = false;
        try {
            JSONRPC2Request request = new JSONRPC2Request("PING", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            if (response.getResult().equals(true))
                result = true;
            else
                result = false;

        } catch (Exception e) {
            return false;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result;
    }
    
    public void stop()
    {
        if (serverOnline)
        {
            JSONRPC2Request request = new JSONRPC2Request("STOP", ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void forward(int speed)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(speed);
            JSONRPC2Request request = new JSONRPC2Request("FORWARD", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            leftSpeed = speed;
            rightSpeed = speed;
        }
    }
    public void backward(int speed)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(speed);
            JSONRPC2Request request = new JSONRPC2Request("BACKWARD", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            leftSpeed = -speed;
            rightSpeed = -speed;
        }
    }
    public void left()
    {
        if (serverOnline)
        {
            JSONRPC2Request request = new JSONRPC2Request("LEFT", ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            leftSpeed = -10;
            rightSpeed = 7;
        }
    }
    public void right()
    {
        if (serverOnline)
        {
            JSONRPC2Request request = new JSONRPC2Request("RIGHT", ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            rightSpeed = -10;
            leftSpeed = 7;
        }
    }
    public void simpleUp(int speed)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(speed);
            JSONRPC2Request request = new JSONRPC2Request("SIMPLE_UP", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            axlePosition = 66;
        }
    }
    public void simpleDown(int speed)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(speed);
            JSONRPC2Request request = new JSONRPC2Request("SIMPLE_DOWN", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            axlePosition = 19;
        }
    }
    
    public void repositionAxleFromTurn()
    {
        if (serverOnline)
        {
            JSONRPC2Request request = new JSONRPC2Request("REPOSITION_AXLE", ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setAxle(int percentage)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(percentage);
            JSONRPC2Request request = new JSONRPC2Request("SET_AXLE_POSITION", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setCamera0Position(int percentage)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(percentage);
            JSONRPC2Request request = new JSONRPC2Request("SET_CAMERA_0_POSITION", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setCamera1Position(int percentage)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(percentage);
            JSONRPC2Request request = new JSONRPC2Request("SET_CAMERA_1_POSITION", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setSpeed(int speed)
    {
        if (serverOnline)
        {
            List<Object> params = new ArrayList<Object>();
            params.add(speed);
            JSONRPC2Request request = new JSONRPC2Request("SET_SPEED", params, ++nextRequestID);
            try {
                rpc.send(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public int getAxlePosition()
    {
    	int axlePosition = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_AXLE_POSITION", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            axlePosition = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return axlePosition;
    }
    public int getSpeed()
    {
    	int currentSpeed = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_SPEED", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            currentSpeed = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return currentSpeed;
    }
    public int getRightSpeed()
    {
    	int rightSpeed = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_RIGHT_SPEED", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            rightSpeed = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return rightSpeed;
    }
    public int getLeftSpeed()
    {
    	int leftSpeed = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_LEFT_SPEED", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            leftSpeed = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return leftSpeed;
    }
    public int getCamera0Position()
    {
    	int camera0Position = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_CAMERA0_POSITION", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            camera0Position = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return camera0Position;
    }
    public int getCamera1Position()
    {
    	int camera1Position = 0;
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_CAMERA1_POSITION", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            camera1Position = Integer.valueOf(response.getResult().toString());
            
        } catch (Exception e) {
            return 0;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return camera1Position;
    }
    public String getState()
    {
    	String state = "";
        try {
            JSONRPC2Request request = new JSONRPC2Request("GET_STATE", ++nextRequestID);
            JSONRPC2Response response = rpc.send(request);

            state = response.getResult().toString();
            
        } catch (Exception e) {
            return null;
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return state;
    }
}

/*
 * Limit the execution time of any method (used for the JSON request).
 * Before this, if the server was offline the entire program would hang waiting for the request to timeout.
 * 
 * Next time probably send the job to an executer...
 */
interface ExceptionRunnable
{
    public void run() throws Throwable;
}

class TimeLimit implements ExceptionRunnable
{
    private final ExceptionRunnable r;
    private final int limit;
    private Throwable t;

    public TimeLimit(final int limit, final ExceptionRunnable r)
    {
        this.limit = limit;
        this.r = r;
    }

    @SuppressWarnings("deprecation")
    public synchronized void run() throws Throwable
    {
        final Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    r.run();
                } catch (final Throwable t) {
                    TimeLimit.this.t = t;
                }
            }
        });
        thread.start();
        try {
            thread.join(limit);
            if(thread.isAlive())
            {
                thread.stop();
                throw new InterruptedException("Timeout");
            }
        } catch (final InterruptedException e) {
            if(t == null)
                t = e;
        }
        if(t != null)
        {
            final Throwable tt = t;
            t = null;
            throw tt;
        }
    }
}

















