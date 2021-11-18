from pathlib import Path
import os
import sys
import json
import requests
from src import pizza
from src import envhandler
from src import bothandler
from src import utility as u

from flask import Flask, request, Response

app = Flask(__name__)

@app.route(f'/pizza-suggester/{envhandler.readEnv()["TOKEN"]}', methods=['POST'])
def respond():
    print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), "Received update", json.loads(request.data), indent=2))
    bothandler.handleAndBootstrapVars(envhandler.readEnv(), verbose, debug, pizza.makeFullDict(pizzaPath, extrasPath), json.loads(request.data))
    return Response(status=200)

if __name__ == "__main__":
    try:
        verbose = True
        debug = True

        resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "deleteWebhook")
        print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))

        resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "setWebhook", params = {
            "url": envhandler.readEnv()["HOOKSBASEURL"] + "/" + envhandler.readEnv()["TOKEN"]
        })
        print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))

        resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "getWebhookInfo")
        print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))

        assetsfolder = "assets/csv"
        pizzaFile = "kantine.csv"
        extrasFile = "extras.csv"

        pizzaPath = u.patthatcat(u.getscrpath(),assetsfolder,pizzaFile)
        extrasPath = u.patthatcat(u.getscrpath(),assetsfolder,extrasFile)
        
        app.run(host='0.0.0.0', port=8000)
        
    except KeyboardInterrupt as e:
        print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
        sys.exit(0)
    except Exception as e:
        print("\n[{}] [ERROR] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
    finally:
        print("Program stopped")