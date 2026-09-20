package com.securevault.passwordmanager.core.security

import java.security.SecureRandom

/**
 * Cryptographically secure password and passphrase generator using SecureRandom.
 */
object PasswordGenerator {

    private val secureRandom = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    private const val AMBIGUOUS = "0O1lI|"

    // Curated high-entropy wordlist for passphrases (Diceware style)
    val WORDLIST = listOf(
        "abacus", "abbey", "ability", "academy", "action", "advance", "airport", "alarm",
        "alchemy", "alien", "alpha", "amber", "anchor", "anthem", "arcade", "arctic",
        "arrow", "artist", "aspect", "astronomy", "atlas", "atomic", "august", "autumn",
        "avatar", "banner", "beacon", "beyond", "binary", "biotech", "breeze", "bridge",
        "bronze", "buffer", "cactus", "canyon", "captain", "carbon", "castle", "cedar",
        "celestial", "center", "champion", "chrome", "cipher", "circuit", "citizen", "clarity",
        "cloud", "cluster", "cobalt", "comet", "compass", "conduit", "copper", "coral",
        "cosmic", "cradle", "crystal", "current", "cyber", "daemon", "dancer", "delta",
        "desert", "diamond", "digital", "dolphin", "dragon", "dynamic", "eagle", "earth",
        "echo", "eclipse", "element", "emerald", "empire", "enigma", "entropy", "epoch",
        "essence", "eternal", "falcon", "fathom", "feather", "federal", "filter", "flame",
        "flare", "forest", "fossil", "frontier", "galaxy", "gateway", "glacier", "glimmer",
        "gravity", "harbor", "harmony", "haven", "hazard", "horizon", "hybrid", "hyper",
        "iceberg", "impact", "impulse", "infinity", "island", "jaguar", "jungle", "jupiter",
        "karma", "kinetic", "lagoon", "lantern", "laser", "legacy", "legend", "liberty",
        "light", "logic", "lunar", "magnet", "matrix", "meadow", "meteor", "miracle",
        "monarch", "mosaic", "mountain", "nebula", "neon", "nexus", "nomad", "nova",
        "oasis", "ocean", "omega", "orbit", "orchid", "origin", "outpost", "ozone",
        "pacific", "palace", "panther", "paradox", "patrol", "peak", "phantom", "phoenix",
        "pioneer", "planet", "plasma", "polaris", "prism", "pulsar", "quantum", "quartz",
        "radar", "radiant", "raptor", "realm", "rebel", "relic", "resonance", "ridge",
        "ripple", "robot", "rocket", "rover", "safari", "satellite", "sapphire", "scalar",
        "scanner", "shadow", "shield", "sierra", "signal", "silver", "solar", "solitude",
        "spark", "spectrum", "sphere", "spiral", "stellar", "summit", "sunlight", "super",
        "symbol", "tactic", "tempest", "terminal", "terra", "thunder", "timber", "titan",
        "topaz", "tornado", "tracer", "transit", "tribune", "trinity", "tsunami", "turbo",
        "twilight", "ultra", "umbrella", "uranium", "utopia", "valence", "vector", "velocity",
        "venture", "vertex", "vessel", "vibrant", "vintage", "violet", "vortex", "voyage",
        "wave", "wildlife", "wind", "winter", "wisdom", "zenith", "zephyr", "zodiac"
    )

    data class GeneratorConfig(
        val length: Int = 16,
        val includeUppercase: Boolean = true,
        val includeLowercase: Boolean = true,
        val includeNumbers: Boolean = true,
        val includeSymbols: Boolean = true,
        val excludeAmbiguous: Boolean = false
    )

    data class PassphraseConfig(
        val wordCount: Int = 4,
        val separator: String = "-",
        val capitalizeWords: Boolean = true,
        val includeNumber: Boolean = true
    )

    /**
     * Generates a random password guaranteed to include at least one character
     * from each enabled character set.
     */
    fun generatePassword(config: GeneratorConfig): String {
        require(config.length >= 4) { "Password length must be at least 4" }

        val poolBuilder = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        var upper = UPPERCASE
        var lower = LOWERCASE
        var num = NUMBERS
        var sym = SYMBOLS

        if (config.excludeAmbiguous) {
            upper = upper.filter { it !in AMBIGUOUS }
            lower = lower.filter { it !in AMBIGUOUS }
            num = num.filter { it !in AMBIGUOUS }
            sym = sym.filter { it !in AMBIGUOUS }
        }

        if (config.includeLowercase && lower.isNotEmpty()) {
            poolBuilder.append(lower)
            guaranteedChars.add(lower[secureRandom.nextInt(lower.length)])
        }
        if (config.includeUppercase && upper.isNotEmpty()) {
            poolBuilder.append(upper)
            guaranteedChars.add(upper[secureRandom.nextInt(upper.length)])
        }
        if (config.includeNumbers && num.isNotEmpty()) {
            poolBuilder.append(num)
            guaranteedChars.add(num[secureRandom.nextInt(num.length)])
        }
        if (config.includeSymbols && sym.isNotEmpty()) {
            poolBuilder.append(sym)
            guaranteedChars.add(sym[secureRandom.nextInt(sym.length)])
        }

        val pool = poolBuilder.toString()
        if (pool.isEmpty()) return ""

        val resultChars = ArrayList<Char>(config.length)
        resultChars.addAll(guaranteedChars)

        while (resultChars.size < config.length) {
            resultChars.add(pool[secureRandom.nextInt(pool.length)])
        }

        // Fisher-Yates shuffle using SecureRandom
        for (i in resultChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = resultChars[i]
            resultChars[i] = resultChars[j]
            resultChars[j] = temp
        }

        return resultChars.joinToString("")
    }

    /**
     * Generates a diceware-style passphrase.
     */
    fun generatePassphrase(config: PassphraseConfig): String {
        require(config.wordCount >= 2) { "Word count must be at least 2" }

        val selectedWords = mutableListOf<String>()
        for (i in 0 until config.wordCount) {
            val word = WORDLIST[secureRandom.nextInt(WORDLIST.size)]
            val processedWord = if (config.capitalizeWords) {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            } else {
                word
            }
            selectedWords.add(processedWord)
        }

        if (config.includeNumber) {
            val randomIdx = secureRandom.nextInt(selectedWords.size)
            val randomDigit = secureRandom.nextInt(100).toString()
            selectedWords[randomIdx] = selectedWords[randomIdx] + randomDigit
        }

        return selectedWords.joinToString(config.separator)
    }
}
