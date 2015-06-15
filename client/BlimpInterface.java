/**
 *   BlimpInterface.java
 * 
 *   A GUI to control the Blimp.
 * 
 *   @author Eric Ciminelli <eciminelli@rollins.edu>
 *   Copyright (c) 2015 Mental Ergonomics. All rights reserved. 
 * 
 */

package com.mentalergonomics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.ProgressBarFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.widgets.Button;

public class BlimpInterface
{
    protected Shell shell;
    private Figure parent;
    private TankFigure batteryTank;
    private MeterFigure headingMeter;
    private ProgressBarFigure leftPropSpeed, rightPropSpeed, combinedSpeed, cameraX, cameraY, axleMeter;
	protected Label lblConnectionStatus;
		 
    private BlimpController blimpController; 
    private boolean[] keys;
    private boolean advancedModeToggle;
    private int camera0UL, camera1UL;
    private int camera0LL, camera1LL;
    private int axleLL, axleUL;
    
    public BlimpInterface()
    {
    	advancedModeToggle = true;
    	camera0LL = 17;
    	camera0UL = 90;
    	camera1LL = 75;
    	camera1UL = 99;
    	axleLL = 19;
    	axleUL = 66;
        blimpController = new BlimpController();
        updateOnlineStatus();
        
        /* [0 - Shift] [1 - Space] [2 - Up] [3 - Down] [4 - Left] [5 - Right] [6 - W] [7 - A] [8 - S] [9 - D] */
        keys = new boolean[10];
        
        /*
         * TODO 
         * 		make it so only the left shift sends a command
         * 
         */
    }
    
    public static void main(String[] args) 
    {
        try {
            BlimpInterface window = new BlimpInterface();            
            window.open();         
        } catch (Exception e) {
            System.out.println("System Error: " + e.getMessage());
        }
    }
    /**
     * A thread to update the online status by pinging the server every 3 seconds.
     */
    public void updateOnlineStatus()
    {
    	new Thread(new Runnable() {
            public void run() {
                while (true) {
                	blimpController.serverOnline = blimpController.ping();       	
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                    	System.out.println("System Error: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    /**
     * Open the window, start threads to update the interface and control the servo's.
     */
    public void open() 
    {
    	Display display = Display.getDefault();
    	Listener keyListener = new KeyListener();
    	display.addFilter(SWT.KeyDown, keyListener);
    	try {
    		createContents();
    		try {
    			shell.open();
    			shell.layout();
    	        new Thread(new Runnable() {
    	            public void run() {
    	            	while(true) {
    	            		try {
    	            			updateThread();
    							Thread.sleep(10);
    						} catch (InterruptedException e) {
    							System.out.println(e.getMessage());
    						}
    	            	}
    	            }
    	        }).start();
    			while (!shell.isDisposed()) {
    				if (!display.readAndDispatch()) {
    					display.sleep();
    				}
    			}
    		} finally {
    			if (!shell.isDisposed()) {
    				shell.dispose();
    			}
    		}
    	} finally {
    		display.dispose();
    	}
        System.exit(0);
    }  
    /**
     *  Updates components of the UI. Needed for SWT.
     */
    private void updateThread() 
    {
    	Display.getDefault().asyncExec(new Runnable() {
            public void run() {
        		updateServoKeys();
        		
        		/* Set the connection status label in SWT */
            	if (blimpController.serverOnline && !lblConnectionStatus.getText().equals("ONLINE")) {
            		lblConnectionStatus.setText("ONLINE");
            		lblConnectionStatus.setForeground(SWTResourceManager.getColor(0, 255, 0));      
            	}
            	else if (!blimpController.serverOnline && !lblConnectionStatus.getText().equals("OFFLINE")) {
            		lblConnectionStatus.setText("OFFLINE");
            		lblConnectionStatus.setForeground(SWTResourceManager.getColor(255, 0, 0));     
            	}
            }
        });
    }
    /* 
     * 
     * TODO use the heading meter elsewhere
     * then get rid of this old method
     * 
     */
    public void updateSWT()
    {
    	// TODO produce an actual heading instead of just the speed, use a ScheduledExecutorService to smooth movement 
    	if(blimpController.rightSpeed > 0 && blimpController.leftSpeed < 0)
    		headingMeter.setValue(blimpController.leftSpeed * 10);
    	else if(blimpController.leftSpeed > 0 && blimpController.rightSpeed < 0)
    		headingMeter.setValue(-blimpController.rightSpeed * 10);
    	else
    		headingMeter.setValue(0);

    }
    public void updateServoKeys() 
    {
        if (keys[0] && advancedModeToggle) // Shift - axle down
        {    	
            if (blimpController.axlePosition > axleLL && advancedModeToggle) {
                blimpController.axlePosition = blimpController.axlePosition - 1;
                blimpController.setAxle(blimpController.axlePosition);
            	axleMeter.setValue(blimpController.axlePosition);
            }
        }
        if (keys[1] && advancedModeToggle) // Space - axle up
        {
            if (blimpController.axlePosition < axleUL) {
                blimpController.axlePosition = blimpController.axlePosition + 1;
                blimpController.setAxle(blimpController.axlePosition);
            	axleMeter.setValue(blimpController.axlePosition);
            }
        }
        if (keys[2]) // Up arrow - camera up
        {
            if (blimpController.camera1Position > camera1LL) {
                blimpController.camera1Position = blimpController.camera1Position - 1;
                blimpController.setCamera1Position(blimpController.camera1Position);
            	cameraY.setValue(174 - blimpController.camera1Position);
                }
        }
        if (keys[3]) // Down arrow - camera down
        {
            if (blimpController.camera1Position < camera1UL) {
                blimpController.camera1Position = blimpController.camera1Position + 1;
                blimpController.setCamera1Position(blimpController.camera1Position);
            	cameraY.setValue(174 - blimpController.camera1Position);
            }
        }
        if (keys[4]) // Left arrow - camera left
        {
            if (blimpController.camera0Position >= camera0LL) {
                blimpController.camera0Position = blimpController.camera0Position - 1;
                blimpController.setCamera0Position(blimpController.camera0Position);
            	cameraX.setValue(blimpController.camera0Position);
            }
        }
        if (keys[5]) // Right arrow - camera right
        {
            if (blimpController.camera0Position <= camera0UL) {
                blimpController.camera0Position = blimpController.camera0Position + 1;
                blimpController.setCamera0Position(blimpController.camera0Position);
            	cameraX.setValue(blimpController.camera0Position);
            }
        }
    }
    /**
     * Create contents of the window.
     */
    protected void createContents() 
    {
        shell = new Shell((SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.MIN ));
        shell.setBackground(SWTResourceManager.getColor(50, 50, 50));
        shell.setModified(true);
        shell.setSize(1228, 862);
        shell.setText("Tarship 1 Control Center");    
   
        shell.addKeyListener(new KeyAdapter()
        {	        	
        	public void keyPressed(KeyEvent e)
            {
        		if (blimpController.serverOnline)
        		{
            		if (!keys[0] && !keys[1] && !keys[6] && !keys[7] && !keys[8] && !keys[9]) // No axle or directional is already pressed
            		{ 
            	        /* Directional Controls */
                        if (e.keyCode == 119) // W forwards
                        {
                        	keys[6] = true;
                        	blimpController.forward(blimpController.combinedSpeed);
                        	leftPropSpeed.setValue(blimpController.combinedSpeed * 10);
                        	rightPropSpeed.setValue(blimpController.combinedSpeed * 10);
                        }
                        if (e.keyCode == 97 && !keys[0] && !keys[1])  // A left: axle isn't moving
                        {
                        	keys[7] = true;
                            blimpController.left();
                            leftPropSpeed.setValue(blimpController.leftSpeed * 10);
                        	rightPropSpeed.setValue(blimpController.rightSpeed * 10);
                        }
                        if (e.keyCode == 115) // S backwards
                        {
                        	keys[8] = true;
                        	blimpController.backward(blimpController.combinedSpeed);
                        	leftPropSpeed.setValue(-blimpController.combinedSpeed * 10);
                        	rightPropSpeed.setValue(-blimpController.combinedSpeed * 10);
                        }
                        if (e.keyCode == 100 && !keys[0] && !keys[1]) // D right: axle isn't moving
                        {
                        	keys[9] = true;
                            blimpController.right();
                            leftPropSpeed.setValue(blimpController.leftSpeed * 10);
                        	rightPropSpeed.setValue(blimpController.rightSpeed * 10);
                        }
            		}
            		/*
            		 * 
            		 * TODO
            		 * 
            		 * in simple mode the axle is held when you press a direction key
            		 * 
            		 * 
            		 */
            		
            		/* Axle Controls */
            		if(!keys[7] && !keys[9]) // Not in a turn
            		{
            			if (e.keyCode == 131072)
            			{ 
            				/* Simple down, no directional */
            				if(!advancedModeToggle && !keys[0] && !keys[1] && !keys[6] && !keys[8])
            				{
            					blimpController.simpleDown(blimpController.combinedSpeed);
            					leftPropSpeed.setValue(blimpController.combinedSpeed * 10);
                            	rightPropSpeed.setValue(blimpController.combinedSpeed * 10);
            				}
            				keys[0] = true;
            			}
            			if (e.keyCode == 32)
            	        {
            				/* Simple up, no directional */
            	        	if (!advancedModeToggle && !keys[0] && !keys[1] && !keys[6] && !keys[8]) 
            	        	{
                	        	blimpController.simpleUp(blimpController.combinedSpeed);
                	        	leftPropSpeed.setValue(blimpController.combinedSpeed * 10);
                            	rightPropSpeed.setValue(blimpController.combinedSpeed * 10);
            	        	}
            				keys[1] = true;
            	        }
            		}
        			
            		/* Camera Controls */
        			if (e.keyCode == 16777217) // Up arrow
        				keys[2] = true;
        			if (e.keyCode == 16777218) // Down arrow
        				keys[3] = true;
        			if (e.keyCode == 16777219) // Left arrow
        				keys[4] = true;
        			if (e.keyCode == 16777220) // Right arrow
        				keys[5] = true;
            		
        			/* Speed Controls */
                    if (e.keyCode == 49) {
                        blimpController.combinedSpeed = 2;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 50) {
                        blimpController.combinedSpeed = 3;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 51) {
                        blimpController.combinedSpeed = 4;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 52) {
                        blimpController.combinedSpeed = 5;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 53) {
                        blimpController.combinedSpeed = 6;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 54) {
                        blimpController.combinedSpeed = 7;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 55) {
                        blimpController.combinedSpeed = 8;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 56) {
                        blimpController.combinedSpeed = 9;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 57) {
                        blimpController.combinedSpeed = 10;
                        blimpController.setSpeed(blimpController.combinedSpeed);
                    	combinedSpeed.setValue(blimpController.combinedSpeed * 10);
                    }
                    if (e.keyCode == 48) 
                    {
                        blimpController.stop();
                        leftPropSpeed.setValue(0);
                    	rightPropSpeed.setValue(0);
                    	combinedSpeed.setValue(0);
                    }
        		}
            }

            /* Used to send stop commands or recover from a turn */
            public void keyReleased(KeyEvent e)
            {
            	if(blimpController.serverOnline)
            	{
            		/* Camera Controls */
        			if (e.keyCode == 16777217) // Up arrow
        				keys[2] = false;
        			if (e.keyCode == 16777218) // Down arrow
        				keys[3] = false;
        			if (e.keyCode == 16777219) // Left arrow
        				keys[4] = false;
        			if (e.keyCode == 16777220) // Right arrow
        				keys[5] = false;
        			
        			/* Axle Controls */
        			if (e.keyCode == 131072 && keys[0]) // Shift
        			{
        				keys[0] = false;
        				if(!advancedModeToggle && !keys[1] && !keys[6] && !keys[8]) // Simple down: not up, forwards, or backwards
        				{
        					blimpController.repositionAxleFromTurn();
        					blimpController.axlePosition = 42; // for SWT
        					leftPropSpeed.setValue(0);
                        	rightPropSpeed.setValue(0);
        				}
        			}
        	        if (e.keyCode == 32 && keys[1]) // Space
        	        { 
        	        	keys[1] = false;
        	        	if(!advancedModeToggle && !keys[0] && !keys[6] && !keys[8]) // Simple up: not down, forwards, or backwards
        	        	{
        	        		blimpController.repositionAxleFromTurn();
        	        		blimpController.axlePosition = 42; // for SWT
        	        		leftPropSpeed.setValue(0);
                        	rightPropSpeed.setValue(0);
        	        	}
        	        }
        	        
        	        /* Directional Controls */
        	        if (e.keyCode == 119 && keys[6]) // W forwards
                    { 
                    	keys[6] = false;
                    	if((!advancedModeToggle && !keys[0] && !keys[1]) || advancedModeToggle) // Simple, not up or down
        				{
                    		blimpController.stop();
                            blimpController.leftSpeed = 0;
                            blimpController.rightSpeed = 0;
                            leftPropSpeed.setValue(0);
                        	rightPropSpeed.setValue(0);
        				}
                    	
                    }
                    if (e.keyCode == 97 && keys[7]) // A left
                    { 
                    	keys[7] = false;
                    	if(!advancedModeToggle && !keys[0] && !keys[1] || advancedModeToggle) // Simple, not up or down
                    	{
                        	blimpController.repositionAxleFromTurn();
                            blimpController.leftSpeed = 0;
                            blimpController.rightSpeed = 0;
                            leftPropSpeed.setValue(0);
                        	rightPropSpeed.setValue(0);
                    	}
                    }
                    if (e.keyCode == 115 && keys[8]) // S backwards
                    { 
                    	keys[8] = false;
                    	if(!advancedModeToggle && !keys[0] && !keys[1] || advancedModeToggle) // Simple, not up or down
                    	{
                    		blimpController.stop();
                    		blimpController.leftSpeed = 0;
                    		blimpController.rightSpeed = 0;  
                    		leftPropSpeed.setValue(0);
                    		rightPropSpeed.setValue(0);
                    	}
                    }
                    if (e.keyCode == 100 && keys[9]) // D right
                    { 
                    	keys[9] = false;
                    	if(!advancedModeToggle && !keys[0] && !keys[1] || advancedModeToggle) // Simple, not up or down
                    	{
                    		blimpController.repositionAxleFromTurn();
                            blimpController.leftSpeed = 0;
                            blimpController.rightSpeed = 0;
                            leftPropSpeed.setValue(0);
                        	rightPropSpeed.setValue(0);
                    	}
                    }
            	}  
            }
        });
        
        Label lblNewLabel = new Label(shell, SWT.NONE);
        lblNewLabel.setForeground(SWTResourceManager.getColor(255, 255, 255));
        lblNewLabel.setAlignment(SWT.CENTER);
        lblNewLabel.setBounds(10, 10, 1208, 477);
        lblNewLabel.setText("Camera Direction (°)");
        
        Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        label.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        label.setBounds(10, 504, 1208, 8);
        
        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        //  A LightweightSystem is used to create the bridge between SWT and draw2D
        final LightweightSystem lws = new LightweightSystem(shell);
        
        Label label_1 = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
        label_1.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        label_1.setBounds(903, 512, 8, 318);
        
        Label label_2 = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
        label_2.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        label_2.setBounds(325, 512, 8, 318);
        
        Label lblRemainingBattery = new Label(shell, SWT.NONE);
        lblRemainingBattery.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblRemainingBattery.setBounds(29, 816, 123, 24);
        lblRemainingBattery.setText("Remaining Battery (%)");
        
        Label lblCurrentAxlePosition = new Label(shell, SWT.NONE);
        lblCurrentAxlePosition.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblCurrentAxlePosition.setBounds(208, 816, 96, 14);
        lblCurrentAxlePosition.setText("Axle Position (°)");
        
        Label lblLeftMotorPower = new Label(shell, SWT.NONE);
        lblLeftMotorPower.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblLeftMotorPower.setBounds(346, 785, 96, 14);
        lblLeftMotorPower.setText("Left Power (%)");
        
        Label lblRightMotorPower = new Label(shell, SWT.NONE);
        lblRightMotorPower.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblRightMotorPower.setText("Right Power (%)");
        lblRightMotorPower.setBounds(803, 785, 93, 33);
        
        Label lblCollectivePower = new Label(shell, SWT.NONE);
        lblCollectivePower.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblCollectivePower.setBounds(534, 804, 169, 14);
        lblCollectivePower.setText("    Current Power Level (%)");
        
        Label lblHeading = new Label(shell, SWT.NONE);
        lblHeading.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblHeading.setBounds(556, 692, 128, 21);
        lblHeading.setText("Heading Indicator (°)");
        
        Label lblNewLabel_1 = new Label(shell, SWT.NONE);
        lblNewLabel_1.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblNewLabel_1.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 16, SWT.NORMAL));
        lblNewLabel_1.setBounds(943, 521, 151, 24);
        lblNewLabel_1.setText("Connection Status: ");
        
        lblConnectionStatus = new Label(shell, SWT.NONE);
        lblConnectionStatus.setForeground(SWTResourceManager.getColor(255, 0, 0));
        lblConnectionStatus.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 16, SWT.BOLD));
        lblConnectionStatus.setBounds(1100, 521, 75, 24);
        lblConnectionStatus.setText("OFFLINE");
        
        Label label_3 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        label_3.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        label_3.setBounds(911, 549, 307, 8);
        
        Label lblControls = new Label(shell, SWT.NONE);
        lblControls.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblControls.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 12, SWT.BOLD));
        lblControls.setBounds(951, 569, 60, 14);
        lblControls.setText("Controls:");
        
        Label lblW = new Label(shell, SWT.NONE);
        lblW.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblW.setBounds(954, 609, 80, 14);
        lblW.setText("     w");
        
        Label lblA = new Label(shell, SWT.NONE);
        lblA.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblA.setBounds(945, 629, 35, 14);
        lblA.setText("a");
        
        Label lblD = new Label(shell, SWT.NONE);
        lblD.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblD.setBounds(1000, 629, 60, 14);
        lblD.setText("d");
        
        Label lblS = new Label(shell, SWT.NONE);
        lblS.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblS.setBounds(974, 629, 24, 14);
        lblS.setText("s");
        
        Label lblForwardsBackwardsLeft = new Label(shell, SWT.NONE);
        lblForwardsBackwardsLeft.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblForwardsBackwardsLeft.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblForwardsBackwardsLeft.setBounds(1083, 609, 135, 14);
        lblForwardsBackwardsLeft.setText("Forwards, backwards,");
        
        Label lblLeftRight = new Label(shell, SWT.NONE);
        lblLeftRight.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblLeftRight.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblLeftRight.setBounds(1083, 622, 88, 14);
        lblLeftRight.setText("left, and right");
        
        Label lblShift = new Label(shell, SWT.NONE);
        lblShift.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblShift.setBounds(963, 669, 60, 14);
        lblShift.setText("Shift");
        
        Label lblSpace = new Label(shell, SWT.NONE);
        lblSpace.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblSpace.setBounds(959, 685, 60, 28);
        lblSpace.setText("Space");
        
        final Label lblAxleDownAnd = new Label(shell, SWT.NONE);
        lblAxleDownAnd.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblAxleDownAnd.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblAxleDownAnd.setBounds(1083, 669, 131, 14);
        lblAxleDownAnd.setText("Axle down and up");
        
        Label label_4 = new Label(shell, SWT.NONE);
        label_4.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        label_4.setBounds(962, 722, 60, 14);
        label_4.setText("1 - 9");
        
        Label lblSpeedControl = new Label(shell, SWT.NONE);
        lblSpeedControl.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblSpeedControl.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblSpeedControl.setBounds(1083, 722, 115, 14);
        lblSpeedControl.setText("Speed control");
        
        Label lblArrowKeys = new Label(shell, SWT.NONE);
        lblArrowKeys.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        lblArrowKeys.setBounds(949, 755, 78, 24);
        lblArrowKeys.setText("Arrow Keys");
        
        Label lblCameraPanAnd = new Label(shell, SWT.NONE);
        lblCameraPanAnd.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblCameraPanAnd.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblCameraPanAnd.setBounds(1083, 755, 131, 14);
        lblCameraPanAnd.setText("Camera pan and tilt");
        
        Label label_5 = new Label(shell, SWT.NONE);
        label_5.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        label_5.setBounds(962, 785, 60, 14);
        label_5.setText("   0");
        
        Label lblSystemStop = new Label(shell, SWT.NONE);
        lblSystemStop.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        lblSystemStop.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.ITALIC));
        lblSystemStop.setBounds(1083, 785, 88, 14);
        lblSystemStop.setText("Stop motors");
        
        final Button modeToggle = new Button(shell, SWT.NONE);
        modeToggle.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.NORMAL));
        modeToggle.setBounds(1070, 564, 128, 28);
        modeToggle.setText("Advanced Mode");
        modeToggle.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              switch (e.type) {
              case SWT.Selection:
                if(advancedModeToggle)
                {
                	blimpController.setAxle(42);
					blimpController.axlePosition = 42;
                	advancedModeToggle = false;
                	modeToggle.setText("Simple Mode");
                    lblAxleDownAnd.setText("Blimp down and up");

                }
                else if(!advancedModeToggle)
                {
                	advancedModeToggle = true;
                	modeToggle.setText("Advanced Mode");
                    lblAxleDownAnd.setText("Axle down and up");

                }
                break;
              }
            }
          });
            
        parent = new Figure();
        parent.setLayoutManager(new XYLayout());
        lws.setContents(parent);
          
        batteryTank = new TankFigure();
        batteryTank.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(100, 100, 100));
        batteryTank.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 200));
		batteryTank.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		batteryTank.setHihiColor(XYGraphMediaFactory.getInstance().getColor(0, 255, 0));
		batteryTank.setHiColor(XYGraphMediaFactory.getInstance().getColor(0, 200, 0));
		batteryTank.setEffect3D(false);
		batteryTank.setRange(0, 100);
		batteryTank.setMajorTickMarkStepHint(30);
		batteryTank.setValue(90);
		parent.add(batteryTank, new Rectangle(47, 614, 100, 200));
        
        headingMeter = new MeterFigure(); // make final to run in scheduledExecuterService thread
		headingMeter.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		headingMeter.setNeedleColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		headingMeter.setHihiColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		headingMeter.setHiColor(XYGraphMediaFactory.getInstance().getColor(255, 192, 0));
		headingMeter.setLoloColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		headingMeter.setLoColor(XYGraphMediaFactory.getInstance().getColor(255, 192, 0));
		headingMeter.setGradient(true);
		headingMeter.setLogScale(false);
		headingMeter.setHihiLevel(170);
		headingMeter.setHiLevel(120);
		headingMeter.setLoloLevel(-170);
		headingMeter.setLoLevel(-120);
        headingMeter.setRange(-180, 180);
        headingMeter.setMajorTickMarkStepHint(50);
        headingMeter.setValue(0);
        parent.add(headingMeter, new Rectangle(415, 530, 400, 500));
        /*
        //Update the gauge in another thread.
      	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      	ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
      			
      		@Override
      		public void run() {
      			Display.getDefault().asyncExec(new Runnable() {					
      				@Override
      				public void run() {
     						headingMeter.setValue(Math.sin(meterPosition++/10.0)*180);						
   					}
     			});
      		}
   		}, 100, 100, TimeUnit.MILLISECONDS);
   		*/
        
        leftPropSpeed = new ProgressBarFigure();
        leftPropSpeed.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 200, 0));
        leftPropSpeed.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(65, 65, 65));
        leftPropSpeed.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
        leftPropSpeed.setEffect3D(false);
        leftPropSpeed.setRange(-100, 100);
        leftPropSpeed.setLoLevel(-50);
        leftPropSpeed.setLoloLevel(-80);
        leftPropSpeed.setHiLevel(60);
        leftPropSpeed.setHihiLevel(80);
		leftPropSpeed.setMajorTickMarkStepHint(50);
		leftPropSpeed.setOriginIgnored(true);
		leftPropSpeed.setValue(0);
		parent.add(leftPropSpeed, new Rectangle(355, 525, 100, 250));
        
        rightPropSpeed = new ProgressBarFigure();
        rightPropSpeed.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 200, 0));
        rightPropSpeed.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(65, 65, 65));
        rightPropSpeed.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
        rightPropSpeed.setEffect3D(false);
        rightPropSpeed.setRange(-100, 100);
        rightPropSpeed.setLoLevel(-50);
        rightPropSpeed.setLoloLevel(-80);
        rightPropSpeed.setHiLevel(60);
        rightPropSpeed.setHihiLevel(80);
        rightPropSpeed.setMajorTickMarkStepHint(50);
        rightPropSpeed.setOriginIgnored(true);
        rightPropSpeed.setValue(0);
		parent.add(rightPropSpeed, new Rectangle(805, 525, 100, 250));
		
		combinedSpeed = new ProgressBarFigure();
		combinedSpeed.setFillColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		combinedSpeed.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		combinedSpeed.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(65, 65, 65));
		combinedSpeed.setHihiColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		combinedSpeed.setHiColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		combinedSpeed.setLoColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		combinedSpeed.setLoloColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		combinedSpeed.setEffect3D(false);
		combinedSpeed.setRange(0, 100);
		combinedSpeed.setLoLevel(30);
		combinedSpeed.setLoloLevel(10);
		combinedSpeed.setHiLevel(70);
		combinedSpeed.setHihiLevel(90);
		combinedSpeed.setMajorTickMarkStepHint(40);
		combinedSpeed.setHorizontal(true);
		combinedSpeed.setOriginIgnored(true);
		combinedSpeed.setValue(50);
		parent.add(combinedSpeed, new Rectangle(445, 732, 340, 60));
		
		cameraX = new ProgressBarFigure();
		cameraX.setFillColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		cameraX.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		cameraX.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(50, 50, 50));
		cameraX.setOpaque(false);
		cameraX.setTransparent(true);
		cameraX.setShowMarkers(false);
		cameraX.setEffect3D(false);
		cameraX.setRange(17, 90);
		cameraX.setMajorTickMarkStepHint(50);
		cameraX.setHorizontal(true);
		cameraX.setOriginIgnored(true);
		cameraX.setIndicatorMode(true);
		cameraX.setValue(53.5);
		parent.add(cameraX, new Rectangle(120, 410, 1000, 80));
		
		cameraY = new ProgressBarFigure();
		cameraY.setFillColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		cameraY.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		cameraY.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(50, 50, 50));
		cameraY.setOpaque(false);
		cameraY.setTransparent(true);
		cameraY.setShowMarkers(false);
		cameraY.setEffect3D(false);
		cameraY.setRange(75, 99);
		cameraY.setMajorTickMarkStepHint(30);
		cameraY.setOriginIgnored(true);
		cameraY.setIndicatorMode(true);
		cameraY.setValue(87);
		parent.add(cameraY, new Rectangle(20, 10, 80, 455));
		
		axleMeter = new ProgressBarFigure();
		axleMeter.setFillColor(XYGraphMediaFactory.getInstance().getColor(215, 0, 0));
		axleMeter.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		axleMeter.setFillBackgroundColor(XYGraphMediaFactory.getInstance().getColor(50, 50, 50));
		axleMeter.setEffect3D(false);
		axleMeter.setShowMarkers(false);
		axleMeter.setRange(19, 66);
		axleMeter.setMajorTickMarkStepHint(30);
		axleMeter.setOriginIgnored(true);
		axleMeter.setIndicatorMode(true);
		axleMeter.setValue(42.5);
		parent.add(axleMeter, new Rectangle(190, 525, 120, 290));
    }
}

class KeyListener implements Listener
{
	@Override
	public void handleEvent(Event arg0)
	{
		if (arg0.stateMask == SWT.COMMAND && arg0.keyCode == 'w')
	    {
	    	System.exit(0);
	    }
		if (arg0.stateMask == SWT.COMMAND && arg0.keyCode == 'q')
	    {
	    	System.exit(0);
	    }
	}
}











