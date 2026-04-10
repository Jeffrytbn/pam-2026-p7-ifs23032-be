package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.RumahAdat

@Serializable
data class RumahAdatRequest(
    var nama: String = "",
    var daerah: String = "",
    var deskripsi: String = "",
    var keunikan: String = "",
    var pathGambar: String = "",
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "nama"       to nama,
            "daerah"     to daerah,
            "deskripsi"  to deskripsi,
            "keunikan"   to keunikan,
            "pathGambar" to pathGambar,
        )
    }

    fun toEntity(): RumahAdat {
        return RumahAdat(
            nama       = nama,
            daerah     = daerah,
            deskripsi  = deskripsi,
            keunikan   = keunikan,
            pathGambar = pathGambar,
        )
    }
}
