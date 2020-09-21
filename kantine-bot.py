#! /usr/bin/env python3
from pathlib import Path
import os
import sys
from src import pizza
from src import confighandler
from src import bothandler

if __name__ == "__main__":
    try:
        assetsfolder = "assets/csv"
        pizzaFile = "kantine.csv"
        extrasFile = "extras.csv"

        pizzaPath = os.path.join(confighandler.get_script_path(),assetsfolder,pizzaFile)
        extrasPath = os.path.join(confighandler.get_script_path(),assetsfolder,extrasFile)

        verbose = True
        config = confighandler.readConfig()

        bothandler.updatePoller(config, verbose, pizza.makeFullDict(pizzaPath, extrasPath))
        
    except KeyboardInterrupt as e:
        print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
        sys.exit(0)
    except Exception as e:
        print("\n[{}] [ERROR] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
    finally:
        print("Program stopped")