#! /usr/bin/env python3
import requests
import json
import time
from pathlib import Path
from src import confighandler

def longPollUpdates(config, verbose, pDict):
    global baseURL
    global token
    global reqPath
    global pizzaDict
    
    pizzaDict = pDict
    baseURL = config['TG_API'].get('baseURL')
    token = config['TG_API'].get('token')

    reqPath = baseURL + token

    pollingTimeout = 120
    offset = None
 
    while True:
        params = {"timeout": str(pollingTimeout), "offset": str(offset)}
        resp = apiCall(reqPath, "getUpdates", params)
        if resp.status_code == 200:
            update_dict = json.loads(resp.text)

            handleUpdate(update_dict)

            offset = update_dict["result"][-1]["update_id"] + 1

        elif resp.status_code != 304:
            time.sleep(60)
            continue
        time.sleep(0.1)

def apiCall(path, method, params):
    return requests.get(path + "/" + method, params=params)

def getBelagList(pizDict):
    belagList = []
    for row in pizDict:
        for entry in row["Zutaten"]:
            if not entry in belagList:
                belagList.append(entry)
    print(belagList)
    return json.dumps(belagList)

def handleUpdate(update_dict):

    commandPizza = "/pizza"
    methodPoll = "sendPoll"
    question = "Bitte Beläge auswählen und dann Umfrage abschicken"

    for update in update_dict["result"]:
        if (update["message"]["text"] == commandPizza):
            params = {}

            params["chat_id"] = update["message"]["from"]["id"]
            params["question"] = question
            params["allows_multiple_answers"] = True
            params["options"] = getBelagList(pizzaDict)
            params["reply_to_message_id"] = update["message"]["from"]["id"]

            print(apiCall(reqPath, methodPoll, params))
        # elif (update["message"] ):