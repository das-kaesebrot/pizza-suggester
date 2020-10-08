import csv
import os
from pathlib import Path
import time

class Pizza:
    def __init__(self,number,name,ingredients,price):
        self.number = number
        self.name = name
        self.ingredients = ingredients
        self.price = price
        pass
    
    def __repr__(self):
        return " - ".join([self.number, self.name, self.ingredients, self.price])

def pizzaListFromCSV(filename):
    
    pizzaList = []

    # filename = "kantine.csv"

    with open(os.path.join(filename),encoding='utf-8') as f:
        reader = csv.reader(f,delimiter=';')
        for x in reader:
            pizzaList.append(x)

    pizzaList = pizzaList[1:-1]
    
    pizzaList = [Pizza(p[0],p[1],p[3],p[2]) for p in pizzaList]

    #print(pizzaList)

    return pizzaList

def pizzaDictListFromCSV(filename):
    pizzaList = []
    with open(os.path.join(filename),encoding='utf-8-sig') as f:
        reader = csv.DictReader(f,delimiter=';')
        for row in reader:
            row["Zutaten"] = row["Zutaten"].split(",")
            pizzaList.append(row)
            
    return pizzaList

def extrasDictListFromCSV(filename):
    extrasList = []
    with open(os.path.join(filename),encoding='utf-8-sig') as f:
        reader = csv.DictReader(f,delimiter=';')
        for row in reader:
            extrasList.append(row)

    return extrasList

def combineDicts(pizzaDict, extrasDict):
    fullDict = {}
    fullDict["pizza"] = pizzaDict
    fullDict["extras"] = extrasDict
    return fullDict

def makeFullDict(file1, file2):
    return combineDicts(pizzaDictListFromCSV(file1), extrasDictListFromCSV(file2))

if __name__ == "__main__":
    for row in pizzaDictListFromCSV("assets/csv/kantine.csv"):
        print(row)