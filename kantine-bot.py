#!/usr/bin/env python3

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

verbose = True
debug = True

assetsfolder = os.path.join(os.getenv('WORKDIR_APP', '/srv/kantinebot'), 'assets/csv')
pizzaFile = "kantine.csv"
extrasFile = "extras.csv"

pizzaPath = u.patthatcat(assetsfolder, pizzaFile)
extrasPath = u.patthatcat(assetsfolder, extrasFile)

def bootstrap():
    try:
        initWebhook = True

        if initWebhook:
            resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "deleteWebhook")
            print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))

            resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "setWebhook", params = {
                "url": envhandler.readEnv()["HOOKSBASEURL"] + "/" + envhandler.readEnv()["TOKEN"]
            })
            print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))

            resp = requests.post(envhandler.readEnv()["BASEURL"] + envhandler.readEnv()["TOKEN"] + "/" + "getWebhookInfo")
            print("[{}] [Code {}] {}\n".format(bothandler.getTime(), resp.status_code, json.loads(resp.text)))
        
        global repliesDict
        repliesDict = {}
        
        
    except KeyboardInterrupt as e:
        print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
        sys.exit(0)
    except Exception as e:
        print("\n[{}] [ERROR] [{}] {}".format(bothandler.getTime(), type(e).__name__, e))
    finally:
        print("Bootstrap completed")
        
bootstrap()

@app.route(f'/pizza-suggester/{envhandler.readEnv()["TOKEN"]}', methods=['POST'])
def respond():
    print("\n[{}] [OK] [{}] {}".format(bothandler.getTime(), "Received update", json.loads(request.data), indent=2))
    bothandler.handleAndBootstrapVars(envhandler.readEnv(), verbose, debug, pizza.makeFullDict(pizzaPath, extrasPath), json.loads(request.data), repliesDict)
    return Response(status=200)