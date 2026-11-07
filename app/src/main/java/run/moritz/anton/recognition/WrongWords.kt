package run.moritz.anton.recognition

/**
 * Words a player might say instead of a spelling word: first names, other spelling tables (DIN 5009 cities,
 * NATO, Austrian and older German forms), number words and hyphen mistakes. Hearing them reports a wrong word
 * instead of forcing the speech onto the nearest correct word.
 *
 * Every word has to exist in the speech model, and each one takes recognition accuracy from the correct words.
 * The list was therefore selected by measurement: names that sound like a spelling word or digit are left out on
 * purpose, for example Emilie, Marta, Fanny or Paul. Measure again before adding words.
 */
internal val WRONG_WORDS: List<String> = """
    Aachen Abel Adalbert Adam Adelheid Adriana Alan Albert Albrecht Alena Alessandro Alex Alexa
    Alexander Alexandra Alfons Alfred Alicia Alina Alma Alois Alpha Alwin Amalia Anastasia Angela
    Angelika Anja Anke Anna Annabel Annelie Annemarie Astrid August Auguste Augustin Balthasar Barbara
    Barnabas Bartholomäus Bastian Beatrice Beatrix Benedikt Benjamin Berlin Bernd Bernhard Bianca Björn
    Bogdan Bonifatius Boris Bravo Bremen Brigitte Bruno Carl Carla Carolin Carolina Caroline Carsten
    Chantal Charles Charlie Chemnitz Christian Christiane Christina Christine Christopher Cindy Claudia
    Claudio Claus Clemens Conrad Constantin Constanze Corinna Cornelia Cornelius Cosima Cristina Curt
    Dagmar Daniel Daniela Danielle Danny David Delta Detlev Diana Dieter Dominik Dominique Dorit dreißig
    Dresden Dustin Düsseldorf Echo Eddi Eddie Edgar Edmund Eduard Edwin Elena elf Elfriede Elisa
    Elisabeth Elli Elmar Else Elvira Emanuel Emma Emmanuel Engelbert Erhard Eric Erika Ernst Erwin Essen
    Ewald Fabrizio Ferdinand Flora Florian Foxtrott Franca Franka Frankfurt Franz Franziska Frauke
    Friedemann fünfzehn Gabi Gabriel Gabriela Gabriele Gaby Georg Gerald Gerhard Gerlinde Gerold Gertrud
    Gesine Gilbert Gloria Godehard Golf Goslar Gotthard Gregor Gudrun Gundula Gunnar Gunter Gunther
    Hamburg Hannelore Hannover Hans Hansjörg Helga Hella Helmut Henri Henry Hildegard Hilmar Holger
    Horst Hubert hundert Immanuel Ines Ingeborg Ingelheim Isaak Isabel Isabella Isolde Jacob Jacqueline
    Jakob James Jana Jane Janina Janine Janosch Jaroslav Jasmin Jean Jeanette Jenny Jeremy Jonathan
    Josef Josefine Josephine Joshua Juliane Justin Jutta Jürgen Karen Karina Karl Karla Karlheinz
    Karoline Karsten Katerina Katharina Kathrin Katrin Kirsten Klaus Klemens Konrad Konstantin Korbinian
    Kornelius Kristina Kurt Köln Lars Leah Leipzig Lena Leni Lennard Lennart Lenny Leonardo Leonhard
    Leopold Liese Lilli Lilly Lima Lisa Lise Lucas Luisa Luitpold Madeleine Magdalena Magnus Maike
    Manfred Mara Marcel Marcus Maren Margareta Margarete Maria Marianne Marie Mario Marius Mark Markus
    Marlene Marvin Mary Maurice Mechthild Meike Melanie Melina Melissa Melvin Michael Michel Miles Mina
    Minus Miriam Mirjam Mohammed Mona Monika Moritz Moses München Nadine Nana Nancy Natalie Natascha
    Nathan Nelly neunzehn Niclas Nicolai Nicolas Nicole Nikita Niklas Nikola Nikolai Nikolas Nikolaus
    Norbert November Nürnberg Offenbach Olga Oliver Olivia Omar Oscar Oskar Oswald Otmar Ottmar Pamela
    Papa Pascal Patricia Patrizia Pauline Peggy Peter Pius Potsdam Quasi Quentin Quickborn Raimund
    Ramona Rasmus Regina Reinhard Renate Robert Robin Rochus Roland Romeo Ronald Ronja Ronny Rosalie
    Rosemarie Rosina Rostock Rudi Rudolf Rupert Ruprecht Sabina Sabine Sabrina Sebastian Selim Selma
    Sibylle Siegfried Sieglinde Sierra Sonja Stefan Stefanie Stella Stephanie Strich Susanna Svenja
    Tango Tatjana Thomas Thorsten Timo Timothy Titus Traugott Trier Tristan Trude Tübingen Ueli Uniform
    Unna Ursel Ursula Uschi Valentin Valentina Valerie Veronika Vincenz Vinzenz Vivian Völklingen
    Walburga Waldemar Wally Walter Wendelin Werner Whiskey Whisky Wiebke Wieland Wolfgang Wolfhard
    Wolfram Wuppertal Xavier Xenia Yankee Yasemin Yasmin Yvonne Zentrum Zeppelin zwanzig Zwickau
""".trim().split(Regex("\\s+")) + listOf("zwei", "bindestrich")
