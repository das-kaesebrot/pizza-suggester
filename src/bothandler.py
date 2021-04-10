import random
import json
import time
import telegram
import requests
import random
import collections
from requests import Request, Session
from babel.numbers import format_currency
from datetime import datetime
from pathlib import Path
from src import envhandler
from collections import Counter
from collections import OrderedDict
from operator import itemgetter

# https://github.com/python-telegram-bot/python-telegram-bot/wiki

def updatePoller(env_vars, verboseCarry, debugCarry, fullDict):
    global baseURL
    global token
    global reqPath
    global pizzaDict
    global extrasDict
    global verbose
    global debug
    global belagList
    global repliesDict

    repliesDict = {}
    debug = debugCarry
    verbose = verboseCarry

    if verbose: print("[{}] Starting bot... Press [CTRL+C] to stop\n".format(getTime()))

    pizzaDict = fullDict["pizza"]
    extrasDict = fullDict["extras"]
    belagList = getBelagList(pizzaDict)

    baseURL = env_vars["BASEURL"]
    token = env_vars["TOKEN"]

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
    emojiSOSSign = u'\U0001F198'
    emojiWarningSign = u'\u26A0'
    
    emojiRightArrow = u'\u27A1'
    emojiLeftArrow = u'\u2B05'
    emojiiOKButton = u'\U0001F197'

    emojiCheckMark = u'\u2714'

    checkMarkUnicode = u'\u2713'
    rightArrowUnicode = u'\u2192'
    leftArrowUnicode = u'\u2190'

    # Commands
    commandPizza = "/pizza" # TEMP SWITCH
    commandDebug = "/debug" # TEMP SWITCH
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
    methodEditMsgReplyMarkup = "editMessageReplyMarkup"
    methodEditMsg = "editMessageText"
    methodAnswerCallbackQuery = "answerCallbackQuery"
    parseMode = "MarkdownV2"
    numberCells = 2
    numberRows = 3

    # Pre formatted strings
    TextSelectToppings = f"{emojiPizza} Bitte wähle deine Beläge:"
    TextSelectedToppingsHeader = f"""Gewählte Beläge:
{{Toppings}}"""
    TextSelectToppingsWithFeedBack = f"{emojiPizza} {TextSelectedToppingsHeader}"
    TextFinalSelection = f"""Vielen Dank! Ich versuche nun, die günstigsten drei Pizzen mit den folgenden Belägen zu finden:
{{Toppings}}"""
    TextButtonBestätigen = f"{checkMarkUnicode}"
    TextButtonNext = f"{rightArrowUnicode}"
    TextButtonPrevious = f"{leftArrowUnicode}"
    TextNichtVegetarisch = f"\n{emojiPig} _Nicht vegetarisch_"
    TextVegetarisch = f"\n{emojiSeedling} _Vegetarisch_"
    TextStart = f"""Hi, ich bins\\! Der *KantineBot* {emojiRobot}{emojiSweatSmile}
_by \\@das\\_kaesebrot_

Folgende Kommandos sind bei mir verfügbar:

{emojiPizza} /pizza \\- Die 3 günstigsten Pizzen nach Auswahl deiner Beläge
{emojiDie} /zufall \\- Lass dir vom Bot eine zufällige Pizza vorschlagen\\!

{emojiPin} /kontakt \\- Daten der Holzofen\\-Kantine

{emojiRobot} /help \\- Informationen zum Bot

Guten Appetit\\! {emojiPizza}"""


    TextPizza = """__*Nummer {nummer}*__
*{name}* mit {zutaten}
*Preis:* {preis}{vegetarisch}"""

    TextPizzaAndExtras = """__*Nummer {nummer}*__
*{name}* mit {zutaten}
*Extras:* {extras}
*Preis:* {preis}{vegetarisch}"""


    TextEnd = f"Guten Appetit\\! {emojiFaceSavouringFood}{emojiForkKnife}"

    TextGeneric = f"""{emojiPizza} Hier sind deine Pizzen:\n
{{TextPizzaMulti}}{TextEnd}"""

    TextFail = f"""{emojiWarningSign} Es tut mir Leid\\, leider konnte ich keine passenden Pizzen finden\\.

Probiere es bitte noch einmal:
{emojiPizza} /pizza
"""

    TextRandom = f"""{emojiDie} Hier ist deine Zufallspizza:\n
{{TextPizza}}\n
{TextEnd}"""

    TextStandard = f"""{emojiPizza} Hier ist deine Pizza:\n
{{TextPizza}}\n
{TextEnd}"""

    TextAddress = f"{emojiPin} Hier die Daten:"


    ### MAIN UPDATE HANDLING THREAD ###
    for update in update_dict["result"]:

        # Check if update is a callback query
        if ("callback_query" in update.keys()):
            from_id = update["callback_query"]["from"]["id"]
            message_id = update["callback_query"]["message"]["message_id"]
            
            if from_id in repliesDict.keys():
                selectedUser = repliesDict[from_id]

                if not (message_id in selectedUser.keys()):
                    break
                
                # TODO implement keyboard callback response
                if "data" in update["callback_query"]:
                    
                    selectedDict = selectedUser[message_id]

                    data = update["callback_query"]["data"]
                    params = {}
                    params["chat_id"] = from_id
                    params["message_id"] = update["callback_query"]["message"]["message_id"]
                    params["reply_markup"] = update["callback_query"]["message"]["reply_markup"]

                    paramsCallbackQueryAnswer = {
                        "callback_query_id": update["callback_query"]["id"]
                    }
                 
                    currPage = int(update["callback_query"]["message"]["reply_markup"]["inline_keyboard"][-1][-2]["callback_data"][1:-1])
                    lastPage = len(selectedDict["pages"])-1

                    lastRow = []


                    if (data == "next"):
                        if not (currPage == lastPage):
                            currPage += 1

                        else:
                            paramsCallbackQueryAnswer["text"] = f"Du bist schon auf der letzten Seite!"

                        params["reply_markup"]["inline_keyboard"][-1][-2]["text"] = f"Seite {currPage+1}"
                        params["reply_markup"]["inline_keyboard"][-1][-2]["callback_data"] = f"p{currPage}p"
                        lastRow = params["reply_markup"]["inline_keyboard"][-1]
                        params["reply_markup"]["inline_keyboard"] = []
                        params["reply_markup"]["inline_keyboard"] = selectedDict["pages"][currPage]
                        params["reply_markup"]["inline_keyboard"].append(lastRow)

                        if len(params["reply_markup"]["inline_keyboard"]) > numberRows+1:
                            del params["reply_markup"]["inline_keyboard"][-1]

                        params["reply_markup"] = json.dumps(params.get("reply_markup"))


                    elif (data == "previous"):
                        if not (currPage == 0):
                            currPage -= 1

                        else:
                            paramsCallbackQueryAnswer["text"] = f"Du bist schon auf der ersten Seite!"
                        
                        
                        params["reply_markup"]["inline_keyboard"][-1][-2]["text"] = f"Seite {currPage+1}"
                        params["reply_markup"]["inline_keyboard"][-1][-2]["callback_data"] = f"p{currPage}p"
                        lastRow = params["reply_markup"]["inline_keyboard"][-1]
                        params["reply_markup"]["inline_keyboard"] = []
                        params["reply_markup"]["inline_keyboard"] = selectedDict["pages"][currPage]
                        params["reply_markup"]["inline_keyboard"].append(lastRow)

                        if len(params["reply_markup"]["inline_keyboard"]) > numberRows+1:
                            del params["reply_markup"]["inline_keyboard"][-1]

                        params["reply_markup"] = json.dumps(params.get("reply_markup"))


                    elif (data == "confirm"):
                        paramsCallbackQueryAnswer["text"] = ""
                        # apiCall(reqPath, "deleteMessage", params)
                        strToppings = ""
                        for x in range(len(selectedDict["selected"])):
                            entry = int(selectedDict["selected"][x])
                            strToppings += belagList[entry]
                            if not len(selectedDict["selected"]) == 1:
                                if not x == len(selectedDict["selected"])-1:
                                    strToppings += ", "
                        
                        belaegeFinal = []
                        for x in range(len(selectedDict["selected"])):
                            entry = int(selectedDict["selected"][x])
                            belaegeFinal.append(belagList[entry])

                        params["text"] = TextFinalSelection.format(Toppings=strToppings)
                        # params["parse_mode"] = parseMode
                        params.pop("reply_markup")

                        apiCall(reqPath, methodEditMsg, params)

                        repliesDict[from_id].pop(message_id)

                        matches = []

                        for entry in pizzaDict:
                            if all(item in entry["Zutaten"] for item in belaegeFinal):
                                matches.append(entry)

                        matches = sorted(matches, key=lambda k: k['Preis'])[:3]
                        print(matches)
                        
                        params= {
                                "chat_id": from_id
                        }                       
                        params["parse_mode"] = parseMode

                        if len(matches) == 0:                            
                            params["text"] = TextFail
                            apiCall(reqPath, methodMsg, params)
                        
                        else:
                            pizzaStr = ""

                            for pizza in matches:
                                zutaten = ""
                                vegetarisch = ""

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
                                
                                pizzaStr += TextPizza.format(nummer = pizza["Nummer"], name = pizza["Name"], zutaten = zutaten, preis = preis, vegetarisch = vegetarisch)
                                pizzaStr += "\n\n"
                            
                            params["text"] = TextGeneric.format(TextPizzaMulti = pizzaStr)
                            apiCall(reqPath, methodMsg, params)


                    elif "p" == data[0] and "p" == data[-1]:
                        paramsCallbackQueryAnswer["text"] = "Dieser Button ist nutzlos."


                    elif "b" == data[0] and "b" == data[-1]:
                        belagNum = int(data[1:-1])

                        if belagNum in selectedDict["selected"]:
                            repliesDict[from_id][message_id]["selected"].remove(belagNum)
                        else:
                            repliesDict[from_id][message_id]["selected"].append(belagNum)
                        
                        strToppings = ""
                        for x in range(len(selectedDict["selected"])):
                            entry = int(selectedDict["selected"][x])
                            strToppings += belagList[entry]
                            if not len(selectedDict["selected"]) == 1:
                                if not x == len(selectedDict["selected"])-1:
                                    strToppings += ", "


                        params["text"] = TextSelectToppingsWithFeedBack.format(Toppings=strToppings)
                        params["parse_mode"] = parseMode

                        params["reply_markup"] = json.dumps(params.get("reply_markup"))

                        apiCall(reqPath, methodEditMsg, params)

                        paramsCallbackQueryAnswer["text"] = ""
                    


                    if not "text" in paramsCallbackQueryAnswer.keys():
                        apiCall(reqPath, methodEditMsgReplyMarkup, params)

                    apiCall(reqPath, methodAnswerCallbackQuery, paramsCallbackQueryAnswer)

        # Check if update is a message
        if ("message" in update.keys()):

            from_id = update["message"]["from"]["id"]

            paramsDefault = {"chat_id": from_id}

            # Response for /start or /help
            if (update["message"]["text"] == commandStart) or (update["message"]["text"] == commandHelp):
                sendTyping(methodChatAction, from_id)

                params = paramsDefault
                params["text"] = TextStart
                params["parse_mode"] = parseMode
                apiCall(reqPath, methodMsg, params)

            # Response for /debug
            elif (update["message"]["text"] == commandDebug) and False:
                sendTyping(methodChatAction, from_id)

                params = paramsDefault
                params["text"] = "Hi, leider funktioniert diese Funktion noch nicht\\. Ich arbeite dran, ok"
                params["parse_mode"] = parseMode
                apiCall(reqPath, methodMsg, params)

            # Response for /pizza
            elif (update["message"]["text"] == commandPizza):

                tempDict = {}
                # overwrite chat id so that the bot only handles one request per user at a time
                # TODO track corresponding requests...properly!
                tempDict["selected"] = []

                sendTyping(methodChatAction, from_id)

                InlineKeyboardButtonsAll = []
                InlineKeyboardButtonsShown = []
                InlineKeyboardRow = []
                buttonPrevious = {"text": TextButtonPrevious, "callback_data": "previous"}
                buttonNext = {"text": TextButtonNext, "callback_data": "next"}
                # TODO fix page tracking
                buttonCurrentSite = {"text": "Seite 1", "callback_data": "p0p"}
                buttonConfirm = {"text": TextButtonBestätigen, "callback_data": "confirm"}

                rowLast = [buttonPrevious, buttonNext, buttonCurrentSite, buttonConfirm]
            
                params = paramsDefault
                counter = 0
                params["text"] = TextSelectToppings
                
                # while (len(belagList) % numberCells) != 0:
                    # belagList.append("test")

                for belag in belagList:
                
                    InlineKeyboardButton= {}
                    InlineKeyboardButton["text"] = belag
                    InlineKeyboardButton["callback_data"] = f"b{counter}b"
                    InlineKeyboardRow.append(InlineKeyboardButton)
                    counter+=1
                    if (counter % numberCells) == 0:
                        InlineKeyboardButtonsAll.append(InlineKeyboardRow)
                        InlineKeyboardRow = []

                for x in range(numberRows):
                    InlineKeyboardButtonsShown.append(InlineKeyboardButtonsAll[x])

                InlineKeyboardPages = []

                for i in range(0, len(InlineKeyboardButtonsAll), numberRows):
                    InlineKeyboardPages.append(InlineKeyboardButtonsAll[i:i+numberRows])

                
                InlineKeyboardButtonsShown.append(rowLast)

                # InlineKeyboardRows = [keyboardButtons[x:x+2] for x in range(0, len(keyboardButtons), 2)]
                InlineKeyboardMarkup = {}
                InlineKeyboardMarkup["inline_keyboard"] = InlineKeyboardButtonsShown

                params["reply_markup"] = json.dumps(InlineKeyboardMarkup, ensure_ascii=False)

                resp = json.loads(apiCall(reqPath, methodMsg, params).text)

                # tempDict["inline_keyboard"] = InlineKeyboardMarkup
                tempDict["pages"] = InlineKeyboardPages
                # tempDict["row_last"] = rowLast
                # tempDict["message_id"] = resp["result"]["message_id"]
                
                if from_id in repliesDict.keys():
                    repliesDict[from_id][resp["result"]["message_id"]] = tempDict
                else:
                    repliesDict[from_id] = {resp["result"]["message_id"]: tempDict}

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

                params = paramsDefault
                params["text"] = TextRandomFull
                params["parse_mode"] = parseMode
                apiCall(reqPath, methodMsg, params)

            # Response for /kontakt
            elif (update["message"]["text"] == commandAddress):
                sendTyping(methodChatAction, from_id)

                # Send general info message
                params = paramsDefault
                params["text"] = TextAddress
                params["parse_mode"] = parseMode
                apiCall(reqPath, methodMsg, params)

                # Send venue
                params = paramsDefault
                params["latitude"] = KantineLat
                params["longitude"] = KantineLong
                params["title"] = KantineTitle
                params["address"] = KantineAddress
                apiCall(reqPath, methodVenue, params)

                # Send phone number as contact
                params = paramsDefault
                params["phone_number"] = KantineNumber
                params["first_name"] = KantineTitle
                apiCall(reqPath, methodContact, params)
            
            # Response for /carlos
            elif (update["message"]["text"] == commandCarlos):
                sendTyping(methodChatAction, from_id)

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
                
                params = paramsDefault
                params["text"] = TextStandard.format(TextPizza = TextPizzaAndExtras.format(nummer = nummer, name = name, zutaten = zutaten, preis = preis, vegetarisch = vegetarisch, extras = extras))
                params["parse_mode"] = parseMode
                apiCall(reqPath, methodMsg, params)
            
            elif "time" in update["message"]["text"].lower() or "zeit" in update["message"]["text"].lower():
                params = paramsDefault
                if random.randrange(2) == 1:
                    params["text"] = "https://www.youtube.com/watch?v=TRgdA9_FsXM"
                    apiCall(reqPath, methodMsg, params)
                else:
                    params["animation"] = "https://tenor.com/view/pizza-peter-parker-pizza-time-spiderman-smile-gif-5523507"
                    apiCall(reqPath, "sendAnimation", params)
