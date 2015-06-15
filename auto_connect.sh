#!/bin/bash
# A script to automatically ssh into the pi and go to the desktop dir
# Requires autoassh (apt-get install autossh or brew install autossh for osx)

/usr/local/bin/autossh -M 20000 -p 22 -D 1080 -t root@192.168.4.3 "cd /home/pi/Desktop ; exec \$SHELL -l"
