#! /usr/bin/env python3
from pathlib import Path
from src import pizza
from src import confighandler
from src import bothandler

if __name__ == "__main__":
    csvfile = "assets/csv/kantine.csv"

    verbose = False
    config = confighandler.readConfig()

    bothandler.longPollUpdates(config, verbose, pizza.pizzaDictListFromCSV(confighandler.get_script_path() + "/" + csvfile))