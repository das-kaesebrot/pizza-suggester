#! /usr/bin/env python3
import configparser
import os
import sys
from environs import Env
from pathlib import Path

def readConfig():
    env = Env()
    env.read_env()

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
    
    # Reads config file values
    else:
        config.read(configPathFull)

        if (config["TG_API"].get("token") == ""):
            config["TG_API"]["token"] = env("TGBOTTOKEN")

        return config

def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))