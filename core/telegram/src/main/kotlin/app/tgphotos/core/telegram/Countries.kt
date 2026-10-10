package app.tgphotos.core.telegram

data class Country(val iso: String, val name: String, val callingCode: String)

/** Phase 1 starter choices. Full country/number validation belongs to the live auth adapter. */
object Countries {
    val choices = listOf(
        Country("BD", "Bangladesh", "+880"), Country("IN", "India", "+91"),
        Country("US", "United States", "+1"), Country("GB", "United Kingdom", "+44"),
        Country("PK", "Pakistan", "+92"), Country("NP", "Nepal", "+977"),
        Country("LK", "Sri Lanka", "+94"), Country("DE", "Germany", "+49"),
        Country("FR", "France", "+33"), Country("AE", "United Arab Emirates", "+971"),
        Country("SA", "Saudi Arabia", "+966"), Country("AU", "Australia", "+61"),
    )
    fun suggest(iso: String): Country = choices.firstOrNull { it.iso.equals(iso, ignoreCase = true) } ?: choices.first()
}
