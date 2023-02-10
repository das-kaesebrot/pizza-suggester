import os
from os import environ
from environs import Env

def readEnv():
    # read environment variables from .env file
    env = Env()
    env.read_env()

    # create new dict to store vars in
    env_vars = {}
    env_vars["BASEURL"] = os.getenv("API_BASEURL", "https://api.telegram.org/bot")
    env_vars["TOKEN"] = os.getenv("TGBOTTOKEN")
    env_vars["HOOKSBASEURL"] = os.getenv("HOOKSBASEURL")
    
    if (env_vars["TOKEN"] == None) or (env_vars["TOKEN"] == ""):
        raise Exception("Bot token variable has not been set. Please set the environment variable TGBOTTOKEN and restart.")
    if (env_vars["HOOKSBASEURL"] == None) or (env_vars["HOOKSBASEURL"] == ""):
        raise Exception("Hooks base URL variable has not been set. Please set the environment variable HOOKSBASEURL and restart.")
    
    return env_vars