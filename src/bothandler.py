#! /usr/bin/env python3
import requests
from requests import Request, Session
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

    pollingTimeout = 1000
    offset = None
 
    while True:
        params = {"timeout": str(pollingTimeout), "offset": str(offset)}
        resp = apiCall(reqPath, "getUpdates", params)
        if resp.status_code == 200:
            update_dict = json.loads(resp.text)

            print(update_dict)

            if (update_dict["result"]): offset = update_dict["result"][-1]["update_id"] + 1

            handleUpdate(update_dict)
        """
        elif resp.status_code != 304:
            time.sleep(60)
            continue
        """
        time.sleep(0.1)

def apiCall(path, method, params):
    return requests.get(path + "/" + method, params=params)

def getBelagList(pizDict):
    belagList = []
    for row in pizDict:
        for entry in row["Zutaten"]:
            if not ((entry in belagList) or (entry == "Tomatensoße") or (entry == "Käse")):
                belagList.append(entry)

    return belagList

def handleUpdate(update_dict):

    commandPizza = "/pizza"
    numberCells = 3
    methodMsg = "sendMessage"
    question = "Bitte Beläge auswählen und dann mit Button bestätigen"

    for update in update_dict["result"]:
        if "message" in update.keys():
            if (update["message"]["text"] == commandPizza):
                keyboardButtonsAll = []
                keyboardRow = []

                belagList = getBelagList(pizzaDict)
            
                params = {}
                counter = 0
                params["chat_id"] = update["message"]["from"]["id"]
                params["text"] = question
                params["reply_to_message_id"] = update["message"]["message_id"]
                
                while (len(belagList) % numberCells) != 0:
                    belagList.append("test")

                for belag in belagList:
                    keyboardButton= {}
                    keyboardButton["text"] = belag
                    keyboardButton["callback_data"] = str(counter)
                    keyboardRow.append(json.dumps(keyboardButton, ensure_ascii=False))
                    counter+=1
                    if (counter % numberCells) == 0:
                        keyboardButtonsAll.append(keyboardRow)
                        keyboardRow = []
                
                # keyboardRows = [keyboardButtons[x:x+2] for x in range(0, len(keyboardButtons), 2)]
                inlineKeyboard = {}
                inlineKeyboard["inline_keyboard"] = keyboardButtonsAll

                params["reply_markup"] = str(json.dumps(inlineKeyboard, ensure_ascii=False))
                
                print(apiCall(reqPath, methodMsg, params).text)
            

        # elif (update["message"] ):