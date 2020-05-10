#!/usr/bin/python
# coding=utf8

import csv
import os

class Pizza:
    def __init__(self,number,name,ingredients,price):
        self.number = number
        self.name = name
        self.ingredients = ingredients
        self.price = price
        pass
    
    def __repr__(self):
        return " - ".join([self.number, self.name, self.ingredients, self.price])

"""
    def pizzaList():

    # die Idee habe ich schon wieder verworfen

    myPizzaList = [
        Pizza(0, 'Pizzabrot', [], 3.00),
        Pizza(1, 'Pizza', ['Tomatensoße','Käse'], 4.00),
        Pizza(2, 'Pizza', ['Tomatensoße','Käse','Salami'], 4.00),
        Pizza(3, 'Pizza', ['Tomatensoße','Käse','Peperoniwurst'], 5.00),
        Pizza(4, 'Pizza', ['Tomatensoße','Käse','Sardellen', 'Zwiebeln'], 5.50),
        Pizza(5, 'Pizza', ['Tomatensoße','Käse', 'Champignon'], 0.00),
        '...',
        Pizza(6, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(7, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(8, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(9, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(10, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(11, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(12, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(13, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(14, 'Pizza', ['Tomatensoße','Käse'], 00),
        Pizza(15, 'Pizza', ['Tomatensoße','Käse'], 00)
    ]
    return myPizzaList
"""

def pizzaListFromCsv():
    
    pizzaList = []

    with open(r"src/pizzas.csv",encoding='utf-8') as f:
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