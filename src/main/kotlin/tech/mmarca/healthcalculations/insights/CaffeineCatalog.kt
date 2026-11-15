package tech.mmarca.healthcalculations.insights

import tech.mmarca.healthcalculations.model.CaffeineCatalogMatch
import tech.mmarca.healthcalculations.model.CaffeineEntry
import tech.mmarca.healthcalculations.model.CaffeineSourceCategory

/**
 * What a caffeinated drink is, by its name. The caffeine calculations ask it to
 * sort entries into sources; the drinks and their names are the app's data, so
 * the app supplies the catalog.
 */
interface CaffeineCatalog {
    /** The catalog drink [name] names, or null when it names none. */
    fun matchName(name: String?): CaffeineCatalogMatch?

    /** The source the words in [name] suggest, for a drink the catalog does not list. */
    fun inferGenericCategory(name: String?): CaffeineSourceCategory

    fun match(entry: CaffeineEntry): CaffeineCatalogMatch? = matchName(entry.name)

    /** [entry]'s source: its catalog drink's, else the one its name suggests. */
    fun categoryFor(entry: CaffeineEntry): CaffeineSourceCategory =
        match(entry)?.item?.category ?: inferGenericCategory(entry.name)
}
