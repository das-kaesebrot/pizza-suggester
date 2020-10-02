#! /usr/bin/env python3
from environs import Env

def readEnv():
    env = Env()
    env.read_env()

    env_vars = {}
    env_vars["BASEURL"] = "https://api.telegram.org/bot"
    env_vars["TOKEN"] = env("TGBOTTOKEN")

    if not (env("API_BASEURL") == ""):
        env_vars["BASEURL"] = env("API_BASEURL")
    
    if (env("TGBOTTOKEN") == ""):
        raise Exception("Bot token variable has not been set. Please set the environment variable and restart the program.")

    return env_vars