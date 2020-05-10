#!/usr/bin/python
# coding=utf8

import csv
import os
from pathlib import Path

class Pizza:
    def __init__(self,number,name,ingredients,price):
        self.number = number
        self.name = name
        self.ingredients = ingredients
        self.price = price
        pass
    
    def __repr__(self):
        return " - ".join([self.number, self.name, self.ingredients, self.price])

def pizzaListFromCsv():
    
    pizzaList = []

    filename = "kantine.csv"

    with open(os.path.join("csv",filename),encoding='utf-8') as f:
        reader = csv.reader(f,delimiter=';')
        for x in reader:
            pizzaList.append(x)

    pizzaList = pizzaList[1:-1]
    
    pizzaList = [Pizza(p[0],p[1],p[3],p[2]) for p in pizzaList]

    #print(pizzaList)

    return pizzaList

if __name__ == "__main__":
    
    print(pizzaListFromCsv())
    
    pass