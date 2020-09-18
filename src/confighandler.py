#! /usr/bin/env python3
import configparser
import os
import sys
from pathlib import Path

def readConfig():
    config = configparser.ConfigParser()

    # Generates a config file next to the script if doesn't exist
    configname = "config.cfg"
    configPathFull = Path(Path(get_script_path()) / configname)

    if not os.path.isfile(configPathFull):
        print("Generating config file...\n")
        config['TG_API'] = {
                        'baseURL': 'https://api.telegram.org/bot',
                        'token': ''
                        }
        with open(configPathFull, 'w') as configfile:
            config.write(configfile)
            # print("wrote config file to " + str(configPathFull))
    
    # Reads config file values
    else:
        # print("Reading config file...\n")
        config.read(configPathFull)
        return config

def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))

