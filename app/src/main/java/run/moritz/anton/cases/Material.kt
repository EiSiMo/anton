package run.moritz.anton.cases

/** One level of the material pool, see the difficulty levels in the specification. */
internal class MaterialLevel(
    val surnames: List<String>,
    val femaleNames: List<String>,
    val maleNames: List<String>,
    val districts: List<String>,
    val serialLetters: IntRange,
    val serialDigits: IntRange
)

internal object Material {
    val levels = listOf(
        MaterialLevel(
            surnames = listOf(
                "Abel", "Adam", "Bach", "Bauer", "Beck", "Berg", "Bode", "Busch", "Dietz", "Ebert", "Engel", "Falk",
                "Fuchs", "Graf", "Haas", "Hahn", "Hartig", "Horn", "Huber", "Jahn", "Jung", "Kern", "Klein", "Koch",
                "Kraft", "Lang", "Lehmann", "Lorenz", "Maas", "Marx", "Neumann", "Noll", "Ott", "Pohl", "Ritter",
                "Rose", "Roth", "Sauer", "Seidel", "Stein", "Vogel", "Wagner", "Walter", "Weber", "Wolf", "Winkler"
            ),
            femaleNames = listOf(
                "Anna", "Berta", "Clara", "Dana", "Emma", "Erika", "Eva", "Frida", "Greta", "Hanna", "Helga", "Ida",
                "Ina", "Jana", "Karin", "Laura", "Lea", "Lena", "Lisa", "Marie", "Martha", "Mia", "Nina", "Paula",
                "Petra", "Rita", "Sara", "Sonja", "Tina", "Ulla", "Vera", "Wiebke"
            ),
            maleNames = listOf(
                "Anton", "Ben", "Bernd", "Carl", "David", "Dirk", "Emil", "Erik", "Felix", "Frank", "Hans", "Heiko",
                "Jan", "Jonas", "Karl", "Klaus", "Lars", "Leon", "Lukas", "Malte", "Max", "Moritz", "Nils", "Otto",
                "Paul", "Peter", "Ralf", "Stefan", "Sven", "Tim", "Tom", "Uwe"
            ),
            districts = listOf("B", "M", "K", "F", "S", "D", "H", "HH", "HB", "E", "DO", "L", "N", "P", "BN"),
            serialLetters = 1..2,
            serialDigits = 1..3
        ),
        MaterialLevel(
            surnames = listOf(
                "Meier", "Meyer", "Maier", "Mayer", "Mayr", "Schmidt", "Schmitt", "Schmid", "Schmied", "Schneider",
                "Müller", "Möller", "Hoffmann", "Hofmann", "Fischer", "Vogt", "Voigt", "Weiß", "Weiss", "Groß",
                "Gross", "Strauß", "Krauß", "Krause", "Schäfer", "Schaefer", "Köhler", "Koehler", "Krüger", "Jäger",
                "Bäcker", "Becker", "Bäumer", "Böhm", "Böhme", "Thiele", "Thomsen", "Thomson", "Philipp", "Filipp",
                "Heß", "Hesse", "Voß", "Vos", "Fuß", "Rösler", "Rößler", "Müller-Lang", "Meyer-Ott", "Schulz-Weber",
                "Bauer-Köhler", "Hoffmann-Groß", "Lüdtke", "Lüdecke", "Nöther", "Sänger", "Stöckl", "Dörfler"
            ),
            femaleNames = listOf(
                "Anne-Marie", "Käthe", "Jördis", "Gudrun", "Annegret", "Bärbel", "Christa", "Christine", "Kristin",
                "Maike", "Meike", "Mareike", "Sophie", "Sofie", "Stefanie",
                "Stephanie", "Thea", "Thekla", "Ute", "Änne", "Jutta", "Lieselotte", "Marie-Luise"
            ),
            maleNames = listOf(
                "Jürgen", "Jörg", "Jörn", "Sören", "Björn", "Günther", "Günter", "Hans-Peter", "Karl-Heinz", "Rüdiger",
                "Christoph", "Christof", "Kristof", "Matthias", "Mathias", "Philipp", "Stephan", "Thorsten", "Torsten",
                "Volker", "Walther", "Wolfgang", "Friedrich", "Heinrich", "Joachim", "Dieter", "Götz"
            ),
            districts = listOf(
                "GÖ", "LÖ", "TÜ", "WÜ", "FÜ", "MÜ", "BÖ", "DÜW", "SÜW", "KÖT", "SÖM", "HH", "HRO", "HGW", "HWI"
            ),
            serialLetters = 1..2,
            serialDigits = 2..4
        ),
        MaterialLevel(
            surnames = listOf(
                "Kowalczyk", "Kowalski", "Nowak", "Wojciechowski", "Szymanski", "Lewandowski", "Yilmaz", "Öztürk",
                "Kaya", "Demir", "Celik", "Aydin", "Yildiz", "Nguyen", "Tran", "Pham", "Hoang", "Ivanov", "Petrov",
                "Smirnov", "Popescu", "Ionescu", "Horvath", "Novak", "Rossi", "Ferrari", "Esposito", "Garcia",
                "Rodriguez", "Fernandez", "Gonzalez", "Papadopoulos", "Dimitriou", "Haddad", "Khoury", "Rahman",
                "Hussein", "Abdullah", "Mohammadi", "Hosseini", "Okonkwo", "Mensah", "Johansson", "Lindqvist",
                "van der Meer", "de Vries", "Dubois", "Lefebvre", "Mitrovic", "Jovanovic", "Kovacevic", "Zielinski"
            ),
            femaleNames = listOf(
                "Aleksandra", "Agnieszka", "Katarzyna", "Ayse", "Fatma", "Elif", "Zeynep", "Thi Mai", "Linh",
                "Svetlana",
                "Natalia", "Ioana", "Giulia", "Carmen", "Lucia", "Eleni", "Fatima", "Layla", "Amara", "Ingrid", "Chloe",
                "Sophie-Anne", "Mirjana", "Jelena"
            ),
            maleNames = listOf(
                "Krzysztof", "Wojciech", "Grzegorz", "Mehmet", "Mustafa", "Emre", "Yusuf", "Van Minh", "Duc", "Dmitri",
                "Sergej", "Alexandru", "Giuseppe", "Alejandro", "Javier", "Georgios", "Ahmed", "Mohammed", "Karim",
                "Kwame", "Lars-Erik", "Jean-Luc", "Dragan", "Nikola"
            ),
            districts = listOf(
                "MYK", "HVL", "OHV", "TF", "LOS", "SPN", "UM", "PR", "MOL", "BAR", "OSL", "EE", "EW", "AIC", "WAF"
            ),
            serialLetters = 1..2,
            serialDigits = 1..4
        ),
        MaterialLevel(
            surnames = listOf(
                "Quandt", "Quast", "Quirmbach", "Queckenberg", "Xylander", "Xanthopoulos", "Ypsilanti", "Yorck",
                "Zyprian", "Zschokke", "Zschäpel", "Pfitzner", "Pfeiffer", "Schwarzenbacher", "Wachtendonk",
                "Hohenzollern-Sigmaringen", "Oberkirchner-Quast", "Brzezinski-Yilmaz", "Czerwinski", "Tschirschky",
                "Mühlhäuser", "Kirchhöfer-Schmidt", "Rosenschöpfer", "Quackernack", "Vlaykov", "Jaxtheimer",
                "Wojtyczkowski", "Schwartzkopff", "Dzierzawa", "Przybylski", "Yxkull", "Qureshi"
            ),
            femaleNames = listOf(
                "Xenia", "Xaviera", "Yvonne", "Yasemin", "Ysabel", "Quirina", "Zoe", "Zita", "Philippa", "Maximiliane",
                "Friederike", "Wilhelmine", "Theodora", "Cäcilie", "Katharina-Yvonne", "Marie-Xenia"
            ),
            maleNames = listOf(
                "Xaver", "Xavier", "Yannick", "Yves", "Quirin", "Quentin", "Zacharias", "Zeno", "Maximilian",
                "Bartholomäus",
                "Theodor", "Cyprian", "Friedrich-Wilhelm", "Hans-Xaver", "Ypke", "Kilian-Quirin"
            ),
            districts = listOf(
                "QLB", "QFT", "KYF", "NVP", "VER", "VK", "ZW", "WW", "ZI", "SYK", "PCH", "VG", "VR", "WSF", "ZEL"
            ),
            serialLetters = 2..2,
            serialDigits = 3..4
        )
    )

    val birthPlaces = listOf(
        "BERLIN", "HAMBURG", "MÜNCHEN", "KÖLN", "FRANKFURT", "STUTTGART", "DÜSSELDORF", "LEIPZIG", "DRESDEN",
        "BREMEN", "HANNOVER", "NÜRNBERG", "POTSDAM", "ROSTOCK", "KASSEL", "ERFURT", "MAGDEBURG", "SAARBRÜCKEN"
    )

    /** Middle letters of a plate. Letters that are too easily confused are not issued. */
    const val SERIAL_ALPHABET = "ABCDEFGHIJKLMNOPRSTUVWXYZ"

    /** Letter combinations that German registration offices do not issue. */
    val forbiddenSerials = setOf("HJ", "KZ", "NS", "SA", "SS")
}
