package au.edu.jcu.wordkeep.domain

/**
 * Sublist 1 of the Academic Word List: the 60 most frequent word families in university texts
 * (A. Coxhead, "A New Academic Word List", TESOL Quarterly 34(2), 2000).
 *
 * Only the headwords are bundled. Definitions and audio are downloaded on demand, so the app
 * does not ship dictionary content it has no licence to redistribute.
 */
object StarterPack {
    const val NAME = "Academic Word List · Sublist 1"

    val words: List<String> = listOf(
        "analyse", "approach", "area", "assess", "assume", "authority", "available", "benefit",
        "concept", "consist", "constitute", "context", "contract", "create", "data", "define",
        "derive", "distribute", "economy", "environment", "establish", "estimate", "evident",
        "export", "factor", "finance", "formula", "function", "identify", "income", "indicate",
        "individual", "interpret", "involve", "issue", "labour", "legal", "legislate", "major",
        "method", "occur", "percent", "period", "policy", "principle", "proceed", "process",
        "require", "research", "respond", "role", "section", "sector", "significant", "similar",
        "source", "specific", "structure", "theory", "vary",
    )
}
