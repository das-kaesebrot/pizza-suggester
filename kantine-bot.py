from pathlib import Path
import os
import sys
from src import pizza
from src import envhandler
from src import bothandler
from src import utility as u

if __name__ == "__main__":
    try:
        verbose = True
        debug = True

        assetsfolder = "assets/csv"
        pizzaFile = "kantine.csv"
        extrasFile = "extras.csv"

        pizzaPath = u.patthatcat(u.getscrpath(),assetsfolder,pizzaFile)
        extrasPath = u.patthatcat(u.getscrpath(),assetsfolder,extrasFile)

        bothandler.updatePoller(envhandler.readEnv(), verbose, debug, pizza.makeFullDict(pizzaPath, extrasPath))
        
    except KeyboardInterrupt as e:
        print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
        sys.exit(0)
    except Exception as e:
        print("\n[{}] [ERROR] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
    finally:
        print("Program stopped")