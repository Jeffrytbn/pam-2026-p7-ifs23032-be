package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RumahAdatTable : UUIDTable("rumah_adat") {
    val nama       = varchar("nama", 150)
    val daerah     = varchar("daerah", 100)
    val pathGambar = varchar("path_gambar", 255)
    val deskripsi  = text("deskripsi")
    val keunikan   = text("keunikan")
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}
