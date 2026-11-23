package com.karelisio.azur.update

/** Version sémantique X.Y.Z (préfixe « v » toléré). */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        fun parse(raw: String): SemVer? {
            val parts = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            val nums = parts.map { it.toIntOrNull() ?: return null }
            return SemVer(nums[0], nums.getOrElse(1) { 0 }, nums.getOrElse(2) { 0 })
        }
    }
}

fun isNewer(remoteTag: String, currentVersion: String): Boolean {
    val remote = SemVer.parse(remoteTag) ?: return false
    val current = SemVer.parse(currentVersion) ?: return true
    return remote > current
}
