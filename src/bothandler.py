#! /usr/bin/env python3
import requests
import random
import json
import time
from requests import Request, Session
from babel.numbers import format_currency
from datetime import datetime
from pathlib import Path
from src import confighandler
from collections import Counter 

def updatePoller(config, verboseCarry, debugCarry, fullDict):
    global baseURL
    global token
    global reqPath
    global pizzaDict
    global extrasDict
    global verbose
    global debug
    global belagList

    debug = debugCarry
    verbose = verboseCarry

    if verbose: print("[{}] Starting bot... Press [CTRL+C] to stop\n".format(getTime()))

    pizzaDict = fullDict["pizza"]
    extrasDict = fullDict["extras"]
    belagList = getBelagList(pizzaDict)

    baseURL = config['TG_API'].get('baseURL')
    token = config['TG_API'].get('token')

    reqPath = baseURL + token

    pollingTimeout = 2000
    offset = None
 
    while True:
        try:
            params = {"timeout": str(pollingTimeout), "offset": str(offset)}
            resp = apiCall(reqPath, "getUpdates", params)

            if resp.status_code == 200:
                update_dict = json.loads(resp.text)

                if verbose: print("[{}] [Code {}] {}\n".format(getTime(), resp.status_code, update_dict))

                if (update_dict["result"]): offset = update_dict["result"][-1]["update_id"] + 1

                handleUpdate(update_dict)

            elif resp.status_code == 404:
                raise RuntimeError("API returned code 404. Did you set the bot token properly?")

            elif resp.status_code != 200:
                time.sleep(60)
                continue
          
            time.sleep(0.1)
        
        except KeyboardInterrupt:
            raise KeyboardInterrupt # lol


def apiCall(path, method, params):
    resp = requests.get(path + "/" + method, params=params)
    if verbose:
        print("[{}] [Code {}] {}\n".format(getTime(), resp.status_code, json.loads(resp.text)))
    return resp

def sendTyping(methodChatAction, chat_id):
    params = {}
    params["chat_id"] = chat_id
    params["action"] = "typing"
    apiCall(reqPath, methodChatAction, params)

def getTime():
    return str(datetime.now())

def getBelagListOld(pizDict):
    belagList = []
    for row in pizDict:
        for entry in row["Zutaten"]:
            if not ((entry in belagList) or (entry == "Tomatensoße") or (entry == "Käse")):
                belagList.append(entry)

    return belagList

def getBelagList(pizDict):
    belagListTemp = []
    belagList = []
    for row in pizDict:
        for entry in row["Zutaten"]:
            if not ((entry == "Tomatensoße") or (entry == "Käse")):
                belagListTemp.append(entry)

    # Sort by frequency https://www.geeksforgeeks.org/python-sort-list-elements-by-frequency/
    belagList = [item for items, c in Counter(belagListTemp).most_common() for item in [items] * c]
    
    belagListTemp = belagList
    belagList = []
    
    for entry in belagListTemp:
        if not ((entry in belagList)):
            belagList.append(entry)

    return belagList

def str2bool(string_, default='raise'):
    true = ['true', 't', '1', 'y', 'yes', 'enabled', 'enable', 'on']
    false = ['false', 'f', '0', 'n', 'no', 'disabled', 'disable', 'off']
    if string_.lower() in true:
        return True
    elif string_.lower() in false or (not default):
        return False
    else:
        raise ValueError('The value \'{}\' cannot be mapped to boolean.'
                         .format(string_))

def formatPrice(number):
    return format_currency(float(number), 'EUR', locale='de_DE')

def handleUpdate(update_dict):

    # EMOJI CODES
    emojiSweatSmile = u'\U0001F605'
    emojiPizza = u'\U0001F355'
    emojiDie = u'\U0001F3B2'
    emojiPin = u'\U0001F4CC'
    emojiFaceSavouringFood = u'\U0001F60B'
    emojiForkKnife = u'\U0001F374' # Variable name on point
    emojiSeedling = u'\U0001F331'
    emojiPoultryLeg = u'\U0001F357'
    emojiBacon = u'\U0001F953'
    emojiPig = u'\U0001F416'
    emojiRobot = u'\U0001F916'

    # Commands
    commandPizza = "/debug" # TEMP SWITCH
    commandDebug = "/pizza" # TEMP SWITCH
    commandStart = "/start"
    commandRandom = "/zufall"
    commandAddress = "/kontakt"
    commandHelp = "/help"
    commandCarlos = "/carlos"

    # Kantine data
    KantineLat = 49.9936668
    KantineLong = 8.4121598
    KantineTitle = "Die Holzofen Kantine"
    KantineAddress = "Marktstraße 23, 65428 Rüsselsheim am Main"
    KantineNumber = "+496142795232"

    # API related vars
    methodMsg = "sendMessage"
    methodContact = "sendContact"
    methodVenue = "sendVenue"
    methodChatAction = "sendChatAction"
    parseMode = "MarkdownV2"
    numberCells = 2
    numberRows = 2

    # Pre formatted strings
    TextSelectToppings = "Bitte Beläge auswählen und dann mit Button bestätigen"
    TextNichtVegetarisch = "\n{} _Nicht vegetarisch_".format(emojiPig)
    TextVegetarisch = "\n{} _Vegetarisch_".format(emojiSeedling)
    TextStart = """Hi, ich bins\\! Der *KantineBot* {emojiRobot}{emojiSweatSmile}
_by \\@das\\_kaesebrot_

Folgende Kommandos sind bei mir verfügbar:

{emojiPizza} /pizza \\- Die 3 günstigsten Pizzen nach Auswahl deiner Beläge
{emojiDie} /zufall \\- Lass dir vom Bot eine zufällige Pizza vorschlagen\\!

{emojiPin} /kontakt \\- Daten der Holzofen\\-Kantine

{emojiRobot} /help \\- Informationen zum Bot

Guten Appetit\\! {emojiPizza}""".format(emojiSweatSmile = emojiSweatSmile, emojiPizza = emojiPizza, emojiDie = emojiDie, emojiPin = emojiPin, emojiRobot = emojiRobot)


    TextPizza = """__*Nummer {nummer}*__
*{name}* mit {zutaten}
*Preis:* {preis}{vegetarisch}"""

    TextPizzaAndExtras = """__*Nummer {nummer}*__
*{name}* mit {zutaten}
*Extras:* {extras}
*Preis:* {preis}{vegetarisch}"""


    TextEnd = """Guten Appetit\\! {emojiFaceSavouringFood}{emojiForkKnife}""".format(emojiFaceSavouringFood = emojiFaceSavouringFood, emojiForkKnife = emojiForkKnife)

    TextGeneric = """{emojiPizza} Hier sind deine Pizzen:\n
{{TextPizza1}}\n
{{TextPizza2}}\n
{{TextPizza3}}\n
{TextEnd}""".format(emojiPizza = emojiPizza, TextEnd = TextEnd)

    TextRandom = """{emojiDie} Hier ist deine Zufallspizza:\n
{{TextPizza}}\n
{TextEnd}""".format(emojiDie = emojiDie, TextEnd = TextEnd)

    TextStandard = """{emojiPizza} Hier ist deine Pizza:\n
{{TextPizza}}\n
{TextEnd}""".format(emojiPizza = emojiPizza, TextEnd = TextEnd)

    TextAddress = """{emojiPin} Hier die Daten:""".format(emojiPin = emojiPin)


    ### MAIN UPDATE HANDLING THREAD ###
    for update in update_dict["result"]:

        # Check if update is a message
        if ("message" in update.keys()):
            
            if not (debug and not (update["message"]["from"]["id"] == 412041295)):

                # Response for /start or /help
                if (update["message"]["text"] == commandStart) or (update["message"]["text"] == commandHelp):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["text"] = TextStart
                    params["parse_mode"] = parseMode
                    apiCall(reqPath, methodMsg, params)

                # TEMP Response for /pizza
                elif (update["message"]["text"] == commandDebug):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["text"] = "Hi, leider funktioniert diese Funktion noch nicht\\. Ich arbeite dran, ok"
                    params["parse_mode"] = parseMode
                    apiCall(reqPath, methodMsg, params)

                # TEMP RESPONSE FOR /debug
                # Response for /pizza
                elif (update["message"]["text"] == commandPizza):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    InlineKeyboardButtonsAll = []
                    InlineKeyboardRow = []
                
                    params = {}
                    counter = 0
                    params["chat_id"] = update["message"]["from"]["id"]
                    # params["text"] = "Test"
                    params["text"] = TextSelectToppings
                    
                    # while (len(belagList) % numberCells) != 0:
                        # belagList.append("test")

                    for belag in belagList:

                        # TMP
                        # break
                    
                        InlineKeyboardButton= {}
                        InlineKeyboardButton["text"] = belag
                        InlineKeyboardButton["callback_data"] = str(counter)
                        InlineKeyboardRow.append(InlineKeyboardButton)
                        counter+=1
                        if (counter % numberCells) == 0:
                            InlineKeyboardButtonsAll.append(InlineKeyboardRow)
                            InlineKeyboardRow = []
                    
                    # InlineKeyboardRows = [keyboardButtons[x:x+2] for x in range(0, len(keyboardButtons), 2)]
                    InlineKeyboardMarkup = {}
                    InlineKeyboardMarkup["inline_keyboard"] = InlineKeyboardButtonsAll

                    params["reply_markup"] = json.dumps(InlineKeyboardMarkup, ensure_ascii=False)

                    print(InlineKeyboardMarkup)
                    print(params)

                    apiCall(reqPath, methodMsg, params)

                # Response for /zufall
                elif (update["message"]["text"] == commandRandom):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    pizzaNum = random.randint(0, len(pizzaDict)-1)
                    pizza = pizzaDict[pizzaNum]
                    nummer = pizza["Nummer"]
                    name = pizza["Name"]

                    zutaten = ""
                    vegetarisch = ""
                    # vorname = update["message"]["from"]["first_name"]

                    preis = formatPrice(pizza["Preis"])

                    if (len(pizza["Zutaten"]) >= 2):
                        zutaten += pizza["Zutaten"][0]
                        for zutat in pizza["Zutaten"][1:-1]:
                            zutaten = zutaten + ", " + zutat
                        zutaten += " und " + pizza["Zutaten"][-1]
                    else:
                        zutaten = "{} und {}".format(pizza["Zutaten"][0], pizza["Zutaten"][1])

                    if str2bool(pizza["Vegetarisch"]):
                        vegetarisch = TextVegetarisch
                    else:
                        vegetarisch = TextNichtVegetarisch

                    if "frisches" in zutaten:
                        zutaten = zutaten.replace("frisches", "frischem")
                    elif "frischer" in zutaten:
                        zutaten = zutaten.replace("frischer", "frischem")
                    
                    TextRandomFull = TextRandom.format(TextPizza = TextPizza.format(nummer = nummer, name = name, zutaten = zutaten, preis = preis, vegetarisch = vegetarisch))

                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["text"] = TextRandomFull
                    params["parse_mode"] = parseMode
                    apiCall(reqPath, methodMsg, params)

                # Response for /kontakt
                elif (update["message"]["text"] == commandAddress):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    # Send general info message
                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["text"] = TextAddress
                    params["parse_mode"] = parseMode
                    apiCall(reqPath, methodMsg, params)

                    # Send venue
                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["latitude"] = KantineLat
                    params["longitude"] = KantineLong
                    params["title"] = KantineTitle
                    params["address"] = KantineAddress
                    apiCall(reqPath, methodVenue, params)

                    # Send phone number as contact
                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["phone_number"] = KantineNumber
                    params["first_name"] = KantineTitle
                    apiCall(reqPath, methodContact, params)
                
                # Response for /carlos
                elif (update["message"]["text"] == commandCarlos) and (update["message"]["from"]["id"] == 26699016):
                    sendTyping(methodChatAction, update["message"]["from"]["id"])

                    pizzaNum = 3 # Number 3!!
                    pizza = pizzaDict[pizzaNum]
                    nummer = pizza["Nummer"]
                    name = pizza["Name"]

                    zutaten = ""
                    vegetarisch = ""
                    extras = "Büffelmozzarella"
                    # vorname = update["message"]["from"]["first_name"]

                    preis = formatPrice(pizza["Preis"])

                    if (len(pizza["Zutaten"]) >= 2):
                        zutaten += pizza["Zutaten"][0]
                        for zutat in pizza["Zutaten"][1:-1]:
                            zutaten = zutaten + ", " + zutat
                        zutaten += " und " + pizza["Zutaten"][-1]
                    else:
                        zutaten = "{} und {}".format(pizza["Zutaten"][0], pizza["Zutaten"][1])

                    if str2bool(pizza["Vegetarisch"]):
                        vegetarisch = TextVegetarisch
                    else:
                        vegetarisch = TextNichtVegetarisch

                    if "frisches" in zutaten:
                        zutaten = zutaten.replace("frisches", "frischem")
                    elif "frischer" in zutaten:
                        zutaten = zutaten.replace("frischer", "frischem")

                    params = {}
                    params["chat_id"] = update["message"]["from"]["id"]
                    params["text"] = TextStandard.format(TextPizza = TextPizzaAndExtras.format(nummer = nummer, name = name, zutaten = zutaten, preis = preis, vegetarisch = vegetarisch, extras = extras))
                    params["parse_mode"] = parseMode
                    apiCall(reqPath, methodMsg, params)