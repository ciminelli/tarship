#!/bin/bash

gst-launch-1.0 -v tcpclientsrc host=192.168.4.3 port=5000 ! gdpdepay !  rtph264depay ! avdec_h264 ! videoflip method=vertical-flip ! videoconvert ! autovideosink sync=false
