package run.moritz.anton.cases

import android.content.Context
import java.time.LocalDate
import org.json.JSONArray

/** The fictional persons that ship with the app, each with a portrait in the assets. */
object PersonCatalog {
    private const val CATALOG = "persons/persons.json"
    const val PORTRAIT_DIRECTORY = "persons/portraits"

    fun load(context: Context): List<Person> =
        parse(context.assets.open(CATALOG).bufferedReader().use { it.readText() })

    fun parse(json: String): List<Person> {
        val entries = JSONArray(json)
        return List(entries.length()) { index ->
            val entry = entries.getJSONObject(index)
            Person(
                surname = entry.getString("surname"),
                firstName = entry.getString("firstName"),
                sex = Sex.valueOf(entry.getString("sex")),
                birthDate = LocalDate.parse(entry.getString("birthDate")),
                birthPlace = entry.getString("birthPlace"),
                expiryDate = LocalDate.parse(entry.getString("expiryDate")),
                level = entry.getInt("level"),
                portrait = entry.getString("portrait")
            )
        }
    }
}
