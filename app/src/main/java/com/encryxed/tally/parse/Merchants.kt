package com.encryxed.tally.parse

/** A shop we recognise by name, and what kind of spending it implies. */
data class KnownMerchant(val display: String, val category: Category)

/**
 * Strip a line down to comparable letters and digits.
 * "ALBERT HEIJN 1234" -> "ALBERTHEIJN1234"
 */
fun normalizeForMatch(text: String): String =
    text.uppercase().filter { it.isLetterOrDigit() }

/**
 * Chains matched by *containment*, so "ALBERT HEIJN 1043 AMSTERDAM" still hits.
 * Every key here is >= 4 characters; shorter names live in [SHORT_MERCHANTS]
 * where a sloppy substring match would cause havoc.
 *
 * Note the deliberate absence of the fuel brand "TOTAL" — it would collide
 * with the word "TOTAL" printed on nearly every receipt on earth.
 */
val KNOWN_MERCHANTS: Map<String, KnownMerchant> = buildMap {
    fun add(category: Category, vararg entries: Pair<String, String>) {
        entries.forEach { (key, display) -> put(key, KnownMerchant(display, category)) }
    }

    add(
        Category.GROCERIES,
        "ALBERTHEIJN" to "Albert Heijn", "JUMBO" to "Jumbo", "LIDL" to "Lidl",
        "ALDI" to "Aldi", "DIRKVANDENBROEK" to "Dirk", "SPAR" to "Spar",
        "VOMAR" to "Vomar", "HOOGVLIET" to "Hoogvliet", "EKOPLAZA" to "Ekoplaza",
        "PICNIC" to "Picnic", "CARREFOUR" to "Carrefour", "DELHAIZE" to "Delhaize",
        "COLRUYT" to "Colruyt", "TESCO" to "Tesco", "SAINSBURY" to "Sainsbury's",
        "MORRISONS" to "Morrisons", "WAITROSE" to "Waitrose", "ASDA" to "Asda",
        "REWE" to "REWE", "EDEKA" to "Edeka", "KAUFLAND" to "Kaufland",
        "PENNY" to "Penny", "NETTO" to "Netto", "WALMART" to "Walmart",
        "TARGET" to "Target", "KROGER" to "Kroger", "COSTCO" to "Costco",
        "TRADERJOE" to "Trader Joe's", "WHOLEFOODS" to "Whole Foods",
        "SAFEWAY" to "Safeway", "PUBLIX" to "Publix", "ALBERTSONS" to "Albertsons",
        "MERCADONA" to "Mercadona", "INTERMARCHE" to "Intermarché",
        "LECLERC" to "E.Leclerc", "AUCHAN" to "Auchan", "MIGROS" to "Migros",
        "COOP" to "Coop", "PLUSSUPERMARKT" to "PLUS",
    )
    add(
        Category.DINING,
        "MCDONALD" to "McDonald's", "BURGERKING" to "Burger King", "SUBWAY" to "Subway",
        "STARBUCKS" to "Starbucks", "DOMINO" to "Domino's", "PIZZAHUT" to "Pizza Hut",
        "NANDO" to "Nando's", "WENDY" to "Wendy's", "TACOBELL" to "Taco Bell",
        "CHIPOTLE" to "Chipotle", "DUNKIN" to "Dunkin'", "COSTACOFFEE" to "Costa Coffee",
        "PRETAMANGER" to "Pret A Manger", "GREGGS" to "Greggs", "FEBO" to "FEBO",
        "NEWYORKPIZZA" to "New York Pizza", "THUISBEZORGD" to "Thuisbezorgd",
        "UBEREATS" to "Uber Eats", "DELIVEROO" to "Deliveroo", "LAPLACE" to "La Place",
    )
    add(
        Category.FUEL,
        "SHELL" to "Shell", "TOTALENERGIES" to "TotalEnergies", "ESSO" to "Esso",
        "TEXACO" to "Texaco", "TANGO" to "Tango", "TINQ" to "Tinq", "ARAL" to "Aral",
        "GULF" to "Gulf", "CHEVRON" to "Chevron", "EXXON" to "Exxon",
        "CIRCLEK" to "Circle K", "SPEEDWAY" to "Speedway", "REPSOL" to "Repsol",
        "CEPSA" to "Cepsa", "FIREZONE" to "Firezone", "AVIA" to "Avia",
    )
    add(
        Category.TRANSPORT,
        "NEDERLANDSESPOORWEGEN" to "NS", "ARRIVA" to "Arriva",
        "CONNEXXION" to "Connexxion", "UBER" to "Uber", "BOLTOPERATIONS" to "Bolt",
        "LYFT" to "Lyft", "TRAINLINE" to "Trainline", "FLIXBUS" to "FlixBus",
        "DEUTSCHEBAHN" to "Deutsche Bahn", "RYANAIR" to "Ryanair",
        "EASYJET" to "easyJet", "TRANSAVIA" to "Transavia", "QPARK" to "Q-Park",
    )
    add(
        Category.HEALTH,
        "ETOS" to "Etos", "KRUIDVAT" to "Kruidvat", "APOTHEEK" to "Apotheek",
        "BOOTS" to "Boots", "SUPERDRUG" to "Superdrug", "WALGREENS" to "Walgreens",
        "ROSSMANN" to "Rossmann", "PHARMACY" to "Pharmacy",
    )
    add(
        Category.HOME,
        "IKEA" to "IKEA", "ACTION" to "Action", "HEMA" to "HEMA",
        "BLOKKER" to "Blokker", "XENOS" to "Xenos", "PRAXIS" to "Praxis",
        "GAMMA" to "GAMMA", "KARWEI" to "Karwei", "HORNBACH" to "Hornbach",
        "BAUHAUS" to "Bauhaus", "LEROYMERLIN" to "Leroy Merlin",
        "HOMEDEPOT" to "Home Depot", "LOWES" to "Lowe's", "JYSK" to "JYSK",
        "KWANTUM" to "Kwantum", "LEENBAKKER" to "Leen Bakker", "FLYINGTIGER" to "Flying Tiger",
    )
    add(
        Category.ELECTRONICS,
        "MEDIAMARKT" to "MediaMarkt", "COOLBLUE" to "Coolblue", "SATURN" to "Saturn",
        "CURRYS" to "Currys", "BESTBUY" to "Best Buy", "AMAZON" to "Amazon",
        "BOLCOM" to "bol", "ALTERNATE" to "Alternate", "BCC" to "BCC",
    )
    add(
        Category.CLOTHING,
        "PRIMARK" to "Primark", "ZARA" to "Zara", "UNIQLO" to "Uniqlo",
        "ZALANDO" to "Zalando", "DECATHLON" to "Decathlon", "JDSPORTS" to "JD Sports",
        "WEFASHION" to "WE Fashion", "BERSHKA" to "Bershka", "PULLBEAR" to "Pull&Bear",
        "MANGO" to "Mango", "NIKE" to "Nike", "ADIDAS" to "Adidas",
        "SCOTCHSODA" to "Scotch & Soda", "MASSIMODUTTI" to "Massimo Dutti",
    )
    add(
        Category.ENTERTAINMENT,
        "PATHE" to "Pathé", "KINEPOLIS" to "Kinepolis", "CINEVILLE" to "Cineville",
        "SPOTIFY" to "Spotify", "NETFLIX" to "Netflix", "STEAMGAMES" to "Steam",
        "GAMEMANIA" to "GameMania", "VUECINEMA" to "Vue",
    )
    add(
        Category.OTHER,
        "POSTNL" to "PostNL", "PRIMERA" to "Primera", "BRUNA" to "Bruna",
    )
}

/**
 * Names too short to match safely by substring — a receipt containing the
 * letters "BP" anywhere would otherwise become a petrol station. These only
 * match when they are effectively the whole line.
 */
val SHORT_MERCHANTS: Map<String, KnownMerchant> = mapOf(
    "AH" to KnownMerchant("Albert Heijn", Category.GROCERIES),
    "PLUS" to KnownMerchant("PLUS", Category.GROCERIES),
    "DIRK" to KnownMerchant("Dirk", Category.GROCERIES),
    "BP" to KnownMerchant("BP", Category.FUEL),
    "Q8" to KnownMerchant("Q8", Category.FUEL),
    "JET" to KnownMerchant("JET", Category.FUEL),
    "NS" to KnownMerchant("NS", Category.TRANSPORT),
    "GVB" to KnownMerchant("GVB", Category.TRANSPORT),
    "RET" to KnownMerchant("RET", Category.TRANSPORT),
    "HTM" to KnownMerchant("HTM", Category.TRANSPORT),
    "DB" to KnownMerchant("Deutsche Bahn", Category.TRANSPORT),
    "KLM" to KnownMerchant("KLM", Category.TRANSPORT),
    "DA" to KnownMerchant("DA", Category.HEALTH),
    "DM" to KnownMerchant("dm", Category.HEALTH),
    "HM" to KnownMerchant("H&M", Category.CLOTHING),
    "CA" to KnownMerchant("C&A", Category.CLOTHING),
    "KFC" to KnownMerchant("KFC", Category.DINING),
    "BOL" to KnownMerchant("bol", Category.ELECTRONICS),
    "IKEA" to KnownMerchant("IKEA", Category.HOME),
)

/**
 * A stable key for "which shop is this receipt from", used to remember the
 * user's corrections.
 *
 * It keys off whatever the parser *guessed*, not off the correct answer: the
 * same till prints the same layout every time, so a shop the parser reads
 * wrongly is read wrongly in the same way each visit. Correct it once and the
 * signature matches forever after.
 */
fun receiptSignature(guess: String?, rawText: String): String {
    val basis = guess?.takeIf { it.isNotBlank() }
        ?: rawText.lineSequence().firstOrNull { normalizeForMatch(it).length >= 4 }
        ?: return ""
    return normalizeForMatch(basis).take(24)
}

/**
 * Boilerplate that gets printed at the top of receipts but is never the shop's
 * name. Without this the parser happily decides you shopped at "Kassabon".
 */
val HEADER_NOISE = listOf(
    "kassabon", "kassa ", "bonnummer", "factuur", "receipt", "invoice",
    "customer copy", "merchant copy", "klantticket", "pinbon", "duplicate",
    // "welcome to <shop>" is deliberately absent: the parser unwraps the
    // greeting and keeps the shop name rather than discarding the line.
    "btw-bon", "vat receipt", "tax invoice", "bedankt", "thank you",
    "openingstijden", "opening hours", "tot ziens",
    "terminal", "transactie", "transaction", "afrekening", "bewijs",
    "please retain", "keep this", "your receipt", "order number", "bestelnummer",
    "tafel", "table", "kassier", "cashier", "medewerker", "operator",
)

/**
 * Fallback categorisation when the shop is unknown: look at what was bought.
 * Checked in declaration order, first hit wins.
 */
private val CATEGORY_KEYWORDS: List<Pair<Category, List<String>>> = listOf(
    Category.FUEL to listOf(
        "euro95", "euro 95", "diesel", "benzine", "unleaded", "brandstof",
        "gasoline", "liter", "pompnr", "pomp ", "fuel", "ltr",
    ),
    Category.GROCERIES to listOf(
        "brood", "melk", "kaas", "groente", "boodschappen", "statiegeld",
        "milk", "bread", "cheese", "produce", "grocery", "eieren", "yoghurt",
    ),
    Category.DINING to listOf(
        "koffie", "coffee", "burger", "pizza", "menu", "frites", "cola",
        "restaurant", "cafe", "eetcafe", "bier", "beer", "wine", "lunch", "dinner",
    ),
    Category.TRANSPORT to listOf(
        "parkeren", "parking", "ticket", "reis", "traject", "enkele reis",
        "ov-chip", "ovchip", "trein", "bus ", "metro", "taxi", "toll",
    ),
    Category.HEALTH to listOf(
        "apotheek", "pharmacy", "shampoo", "tandpasta", "toothpaste",
        "vitamine", "medicijn", "paracetamol", "recept",
    ),
    Category.HOME to listOf(
        "schroef", "verf", "paint", "hout", "timber", "gereedschap", "tool",
        "lamp", "kussen", "meubel",
    ),
    Category.ELECTRONICS to listOf(
        "kabel", "cable", "usb", "hdmi", "laptop", "telefoon", "phone",
        "koptelefoon", "headphone", "batterij", "battery", "adapter", "ssd",
    ),
    Category.CLOTHING to listOf(
        "shirt", "broek", "jeans", "jas", "jacket", "schoen", "shoe",
        "sokken", "socks", "trui", "sweater", "maat ",
    ),
    Category.ENTERTAINMENT to listOf(
        "bioscoop", "cinema", "film", "movie", "game", "concert", "museum",
    ),
)

/** Guess a category from the receipt body when the shop name tells us nothing. */
fun categoryFromKeywords(rawText: String): Category {
    val lower = rawText.lowercase()
    for ((category, keywords) in CATEGORY_KEYWORDS) {
        if (keywords.any { lower.contains(it) }) return category
    }
    return Category.OTHER
}
