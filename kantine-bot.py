from pathlib import Path
import os
import sys
from src import pizza
from src import envhandler
from src import bothandler
from src import utility as u

from flask import Flask, request, Response

app = Flask(__name__)

@app.route(f'/pizza-suggester/{envhandler.readEnv()["TOKEN"]}', methods=['POST'])
def respond():
    bothandler.handleAndBootstrapVars(envhandler.readEnv(), verbose, debug, pizza.makeFullDict(pizzaPath, extrasPath, json.loads(request.data)))
    # bothandler.handleUpdate(envhandler.readEnv(), verbose, debug, pizza.makeFullDict(pizzaPath, extrasPath))
    print(request.json)
    return Response(status=200)

if __name__ == "__main__":
    try:
        verbose = True
        debug = True

        assetsfolder = "assets/csv"
        pizzaFile = "kantine.csv"
        extrasFile = "extras.csv"

        pizzaPath = u.patthatcat(u.getscrpath(),assetsfolder,pizzaFile)
        extrasPath = u.patthatcat(u.getscrpath(),assetsfolder,extrasFile)
        
        app.run()
        
    except KeyboardInterrupt as e:
        print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
        sys.exit(0)
    except Exception as e:
        print("\n[{}] [ERROR] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
    finally:
        print("Program stopped")