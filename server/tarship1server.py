#
#  tarship1Server.py
#
#  A server to control the blimp using JSON-RPC.
#
#  Created by Eric Ciminelli on 2/3/15.
#  Copyright (c) 2015 Mental Ergonomics. All rights reserved. <eciminelli@rollins.edu>
#


from jsonrpclib.SimpleJSONRPCServer import SimpleJSONRPCServer
import RPi.GPIO as GPIO
import socket
import time


# Upper limits (%)
_Servo0UL = 90 # X axis
_Servo1UL = 99 # Y axis
_Servo7UL = 66 # Max up

# Lower Limits (%)
_Servo0LL = 17 # X axis
_Servo1LL = 75 # Y axis
_Servo7LL = 19 # Max down

ServoBlaster = open('/dev/servoblaster', 'w')  # Used to control servos

#rightPropellorEnable  = 11  # Bridge 11-12 (@depreciated)
#leftPropellorEnable   = 15  # Bridge 15-16 (@depreciated)

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BOARD)

GPIO.setup(21, GPIO.OUT)  # Right propellor  - Forward   (pin 21)
GPIO.setup(22, GPIO.OUT)  # Right propellor  - Backward  (pin 22)
GPIO.setup(23, GPIO.OUT)  # Left propellor   - Forward   (pin 23)
GPIO.setup(12, GPIO.OUT)  # Left propellor   - Backward  (pin 12, 24 span)

# Globals
state               = "stop"
axlePosition        = 66
turnAxlePosition    = 0  # Tracks pre-turn axle pos allowing it to recover from turn, 42 default forward
camera0Position     = 50
camera1Position     = 75
combinedSpeed       = 0
rightMotorSpeed     = 0
leftMotorSpeed      = 0

# Adjustables
centerAxlePosition  = 50
leftForwardTurn     = 70
leftBackwardTurn    = 100
rightForwardTurn    = 70
rightBackwardTurn   = 100

leftPropellorForward    = GPIO.PWM(21,100)  # GPIO to spin right prop forward
leftPropellorBackward   = GPIO.PWM(22,100)  # GPIO to spin right prop backward
rightPropellorForward   = GPIO.PWM(23,100)  # GPIO to spin left prop forward
rightPropellorBackward  = GPIO.PWM(12,100)  # GPIO to spin left prop backward




#========================= Servo Control Methods =========================#


def SET_AXLE_POSITION(position):
    global axlePosition
    if (not state == "left") or (not state == "right"):
        if position >= _Servo7LL and position <= _Servo7UL:
            ServoBlaster.write('2=' + str(position) + '%\n')
            ServoBlaster.flush()
            axlePosition = position
    return;

def SET_CAMERA_0_POSITION(position):
    global camera0Position
    if position >= _Servo0LL and position <= _Servo0UL:
        ServoBlaster.write('0=' + str(position) + '%\n')
        ServoBlaster.flush()
        camera0Position = position
    return;

def SET_CAMERA_1_POSITION(position):
    global camera1Position
    if position >= _Servo1LL and position <= _Servo1UL:
        ServoBlaster.write('1=' + str(position) + '%\n')
        ServoBlaster.flush()
        camera1Position = position
    return;


def REPOSITION_AXLE():
    STOP()
    SET_AXLE_POSITION(turnAxlePosition)




#========================= Motor Control Methods =========================#


def SET_SPEED(n):
    global combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    combinedSpeed = n
    if state == "forward" or state == "simple_up" or state == "simple_down":
        rightPropellorForward.ChangeDutyCycle(10*n)
        leftPropellorForward.ChangeDutyCycle(10*n)
        rightMotorSpeed = n
        leftMotorSpeed = n
    elif state == "backward":
        rightPropellorBackward.ChangeDutyCycle(10*n)
        leftPropellorBackward.ChangeDutyCycle(10*n)
        rightMotorSpeed = n
        leftMotorSpeed = n


def FORWARD(speed):
    global state, combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        rightPropellorForward.start(10*speed)
        leftPropellorForward.start(10*speed)
        combinedSpeed = speed
        rightMotorSpeed = speed
        leftMotorSpeed = speed
        state = "forward"


def BACKWARD(speed):
    global state, combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        rightPropellorBackward.start(10*speed)
        leftPropellorBackward.start(10*speed)
        combinedSpeed = speed
        rightMotorSpeed = speed
        leftMotorSpeed = speed
        state = "backward"


def RIGHT():
    global state, turnAxlePosition, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        turnAxlePosition = axlePosition
        SET_AXLE_POSITION(centerAxlePosition)
        state = "right"
        rightPropellorBackward.start(rightBackwardTurn)
        leftPropellorForward.start(rightForwardTurn)
        rightMotorSpeed = -rightBackwardTurn
        leftMotorSpeed = rightForwardTurn


def LEFT():
    global state, turnAxlePosition, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        turnAxlePosition = axlePosition
        SET_AXLE_POSITION(centerAxlePosition)
        state = "left"
        leftPropellorBackward.start(leftBackwardTurn)
        rightPropellorForward.start(leftForwardTurn)
        rightMotorSpeed = leftForwardTurn
        leftMotorSpeed = -leftBackwardTurn


def SIMPLE_UP(speed):
    global state, turnAxlePosition, combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        turnAxlePosition = axlePosition
        SET_AXLE_POSITION(_Servo7UL)
        state = "simple_up"
        rightPropellorForward.start(10*speed)
        leftPropellorForward.start(10*speed)
        combinedSpeed = speed
        rightMotorSpeed = speed
        leftMotorSpeed = speed

def SIMPLE_DOWN(speed):
    global state, turnAxlePosition, combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    if state == "stop":
        turnAxlePosition = axlePosition
        SET_AXLE_POSITION(_Servo7LL)
        state = "simple_up"
        rightPropellorForward.start(10*speed)
        leftPropellorForward.start(10*speed)
        combinedSpeed = speed
        rightMotorSpeed = speed
        leftMotorSpeed = speed


def STOP():
    global state, combinedSpeed, rightMotorSpeed, leftMotorSpeed
    
    rightPropellorForward.stop()
    rightPropellorBackward.stop()
    leftPropellorForward.stop()
    leftPropellorBackward.stop()
    combinedSpeed   = 0
    rightMotorSpeed = 0
    leftMotorSpeed  = 0
    state = "stop"




#======================= Return Methods & More =======================#


 # Determines the connection status of the server
def PING():
    return True

def GET_AXLE_POSITION():
    return axlePosition

def GET_SPEED():
    return speed

def GET_RIGHT_SPEED():
    return rightMotorSpeed

def GET_LEFT_SPEED():
    return leftMotorSpeed

def GET_CAMERA0_POSITION():
    return camera0Position

def GET_CAMERA1_POSITION():
    return camera1Position

def GET_STATE():
    return state


# Servo positioning startup sweeps
for x in xrange(_Servo7UL, _Servo7LL, -1):
    SET_AXLE_POSITION(x)
    time.sleep(.01)
for x in xrange(_Servo7LL, _Servo7UL):
    SET_AXLE_POSITION(x)
    time.sleep(.01)
for x in xrange(_Servo1UL, _Servo1LL, -1):
    SET_CAMERA_1_POSITION(x)
    time.sleep(.01)
for x in xrange(_Servo1LL, 75):
    SET_CAMERA_1_POSITION(x)
    time.sleep(.01)
for x in xrange(_Servo0UL, _Servo0LL, -1):
    SET_CAMERA_0_POSITION(x)
    time.sleep(.01)
for x in xrange(_Servo0LL, 50):
    SET_CAMERA_0_POSITION(x)
    time.sleep(.01)


if __name__=="__main__":
    
    # Start JSON-RPC server getting its own IP address
    server = SimpleJSONRPCServer((([(s.connect(('8.8.8.8', 80)), s.getsockname()[0], s.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]), 10101))
    
    server.register_function(SET_AXLE_POSITION)
    server.register_function(REPOSITION_AXLE)
    server.register_function(SET_SPEED)
    server.register_function(SET_CAMERA_0_POSITION)
    server.register_function(SET_CAMERA_1_POSITION)
    server.register_function(FORWARD)
    server.register_function(BACKWARD)
    server.register_function(SIMPLE_UP)
    server.register_function(SIMPLE_DOWN)
    server.register_function(RIGHT)
    server.register_function(LEFT)
    server.register_function(STOP)
    
    server.register_function(PING)
    server.register_function(GET_AXLE_POSITION)
    server.register_function(GET_SPEED)
    server.register_function(GET_RIGHT_SPEED)
    server.register_function(GET_LEFT_SPEED)
    server.register_function(GET_CAMERA0_POSITION)
    server.register_function(GET_CAMERA1_POSITION)
    server.register_function(GET_STATE)
    
    try:
        print "Server listening..."
        server.serve_forever()
    except KeyboardInterrupt:
        GPIO.cleanup()
    pass


















