#!/usr/bin/python
# coding=utf8

class Pizza:
    def __init__(self,number,name,ingredients,price):
        self.number = number
        self.name = name
        self.ingredients = ingredients
        pass

def pizzaList():
    myPizzaList = [
        Pizza(0, 'Pizzabrot', [], 3.00),
        Pizza(1, 'Pizza', ['Tomatensoße','Käse'], 4.00),
        Pizza(2, 'Pizza', ['Tomatensoße','Käse','Salami'], 4.00),
        Pizza(3, 'Pizza', ['Tomatensoße','Käse','Peperoniwurst'], 5.00),
        Pizza(4, 'Pizza', ['Tomatensoße','Käse','Sardellen', 'Zwiebeln'], 5.50),
        Pizza(5, 'Pizza', ['Tomatensoße','Käse', 'Champignon'], 0.00),
        'yea pls if you want to ...',
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

def smartPizzaList():
    # https://smallseotools.com/de/image-to-text-converter/
    rawData = "Pizza 00 Pizzabrot 01 Pizza Tomatensoße, 02 Pizza Tomatensoße, Salami 1 03 Pizza Tomatensoße, Peperoniwurst 04 Pizza Tomatensoße, Sardellen, Zwiebeln 05 Pizza Tomatensoße, Champignon 06 Pizza Tomatensoße, 3,00 € 4,00 € 5,00 5,00 € 5,50€ 5,00 € 19 20 21 22 23 24 25 26 27 28 29 30 31 32 Pizza Tomatensoße, Käse2, Champignon Pizza Tomatensoße, Käse2, Tunfisch, Zwiebeln Pizza Tomatensoße, Käse2, 6,00 € 6,00 € 7,50€ Schinkenl Salami Champignon, Ei Pizza Calzone1.2.3A Pizza Frutti di Mare 7,00€ 7,50€ Pasta 40 Spaghetti Napolitana 41 Spaghetti Bolognese 42 Spaghetti Diavolo 44 Spaghetti Aglio e Olio Pikante 1.2.3.4 45 Spaghetti Kantine mit Schinken, Champignon, Erbsen, Tomaten-Sahnesoße und Parmesan 46A Spaghetti Carbonara 46B Carbonara italienische Art Tomatensoße, Käse2, Meeresfrüchte Shrimps, Kirschtomaten, Basilikum Quattro Stagioni 7,00€ Tomatensoße, Käse2, Champignon, Paprika, Artischocken, Käse2, Käse2, Salami12'3'4, Champignon 07 Pizza Tomatensoße, Käse2, Salami12'3A, Champignon, Paprika, Peperoni Pizza Rudi (vegetarisch)2 mit frischem Gemüse Pizza Hawaii 7,50€ 6 00 € Tomatensoße, Käse2, Ananas, Schinkenl r F .1 .1 1 1 1 1 1 1 1 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 64 65 67 68 69 70 71 72 73 74 75 76 Spaghetti Puttanesca Spaghetti Rudi (vegetarisch) Spaghetti Mozzarella2 Spaghetti con Tonno Spaghetti Frutti di Mare 2 Tortelloni mit Steinpilzfüllung in Trüffelsoße Gnocchi mit Mozzarellafüllung in Tomatensoße2 Gnocchi mit Steinpilzfüllung in Trüffelsauce Maccheroni Napolitana Maccheroni Bolognese Maccheroni Paesana2 Maccheroni Gorgonzola2 5,00 € 6,00 € 5,00 € 5,00 € 7,00 € 6,50 € 7,50 6,50 7,00 6,50 € 6,50 € 7,00 € 9,00 € 7,00 7,50 € 5,00 6,00 7,00 € 6,00 € 08 Pizza Tomatensoße, Käse2, Schinken12,3A, Champignon, Zwiebeln 09 Pizza Tomatensoße, Käse2, Champignon, Paprika, Peperoni, Oliven, Sardellen, Zwiebeln 10 Pizza Tomatensoße, Käse2, 1.2.3.4 paprika, Zwiebeln Peperoniwurst II Pizza Tomatensoße, Käse2, Schinkenl 2,3.4 12 Pizza Tomatensoße, Käse2, Zwiebeln 13 Pizza Tomatensoße, Käse2, Sardellen 14 Pizza Tomatensoße, Käse2, Paprika, Zwiebel 15 Pizza Tomatensoße, Champignon, Paprika, 16 Pizza Tomatensoße, 6,50 7,00 6,00 € 5,00 € 4,50€ 5,00 € 5,00 € 6,50 € Pizza Tomatensoße, Käse2, Hackfieisch, Zwiebeln Pizza Kantina Tomatensoße, Käse2, Shrimps, Kirschtomten, Basilikum Pizza Tomatensoße, Käse2, frischer Spinat Pizza Tomatensoße, Mozzarella2, frisches Basilikum Pizza Tomatensoße, Mozzarella2, frische Tomaten, Basilikum Pizza Tomatensoße, Käse2, frischer Lachs, Lauchzwiebeln Peperoni, Zwiebeln 33A Pizza Tomatensoße, Mozzarella2, Rucola, Parmaschinken 33B Pizza Tomatensoße, Mozzarella2, Salami 1234 17 Pizza Tomatensoße, Käse2, Peperoniwurst 1 Paprika 18 Pizza Tomatensoße, Käse2, 6,00 € 5,50€ 6,50 € Salami 1,2.3.4, Champignon Extras 0,50 C: Oliven. eingelegte Paprika. Zwiebeln. Mais,Peperoni. Ei, Erbsen. extra Tomaten- Oder Sahnesoße Extras 1,00 C: Brokoli, Spinat, frische Paprika, Sardellen,T0mate. Kapern, Peperoniwurst, Sardinen, Schinken, Pilze, Aubergine, Basilikum, Artischocken, Ananas, aeriebener Parmasan Extras 1,50 Tunfisch, Rucola, gucuk, Mozzarella Extras 2,00 Meeresfrüchte, Lachs, Salsicca, parmesan, Schrimps, Putenstreifen, Goraonzola, Schafskäse Extras 2,50 C: Buffel Mozzarella 34 35 36 37 38 Rucola, Parmesan Pizza Tomatensoße, Mozzarella2, Rucola, Parmesan, Parmaschinken Pizza Quattro Formaggi Vier verschiedene Sorten Käse2 Pizza Salsicca Mozzarella2, Rucda, Salsicca-Wurst Pizza Sucuk Tomatensoße, Käse2, Sucukl'2'3'4 Pizza Rindersalami 6,50 € 7,50 € 6,00 € 5,50 € 7,00€ 8,00 8,00 € 8,00 9,00 € 7,00 € 8,00 € 6,00 € 6,00 € Maccheroni Quattro Formaggio 7,00 € (Vier versch. Käsesorten)2 Maccheroni Amatriciana 6,00 Maccheroni mit Spinat und 6,50 frischen Champignons 6,00 € Maccheroni mit Broccoli Maccheroni mit frischen Champignons und Sahne 6,00 € Maccheroni mit Lachs 8,00 € Maccheroni mit Gorgonzola 2 7,00 € und Broccoli Tagliatelle Bolognese 6,50 € Tagliatelle Paesana 7,50 € Tagliatelle mit Steinpilzen 7,00 € Tagliatelle mit Broccoli 6,00 Tagliatelle mit frischen 6,00 € Champignons und Sahne Tagliatelle mit Lachs 8,00 € Tagliatelle mit Putenbrust- 7,50 € streifen und Steinpilzen Tortellini Bolognese 6,50 123,4 Tortellini mit Sahne, Schinken 6,50 Tortellini al Gorgonzola2 6,50 Tomatensoße, Käse2, Rindersalami1Z3A"
    
    for d in rawData.split(','):
        print(d)


if __name__ == "__main__":
    
    smartPizzaList()

    
    pass