package org.delcom.repositories

import org.delcom.dao.RumahAdatDAO
import org.delcom.entities.RumahAdat
import org.delcom.helpers.suspendTransaction
import org.delcom.tables.RumahAdatTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.UUID

class RumahAdatRepository(private val baseUrl: String) : IRumahAdatRepository {

    override suspend fun getRumahAdatList(search: String): List<RumahAdat> = suspendTransaction {
        if (search.isBlank()) {
            RumahAdatDAO.all()
                .orderBy(RumahAdatTable.createdAt to SortOrder.DESC)
                .limit(50)
                .map { daoToModel(it) }
        } else {
            val keyword = "%${search.lowercase()}%"
            RumahAdatDAO
                .find { RumahAdatTable.nama.lowerCase() like keyword }
                .orderBy(RumahAdatTable.nama to SortOrder.ASC)
                .limit(50)
                .map { daoToModel(it) }
        }
    }

    override suspend fun getRumahAdatById(id: String): RumahAdat? = suspendTransaction {
        RumahAdatDAO
            .find { RumahAdatTable.id eq UUID.fromString(id) }
            .limit(1)
            .map { daoToModel(it) }
            .firstOrNull()
    }

    override suspend fun getRumahAdatByNama(nama: String): RumahAdat? = suspendTransaction {
        RumahAdatDAO
            .find { RumahAdatTable.nama eq nama }
            .limit(1)
            .map { daoToModel(it) }
            .firstOrNull()
    }

    override suspend fun addRumahAdat(rumahAdat: RumahAdat): String = suspendTransaction {
        val dao = RumahAdatDAO.new {
            nama       = rumahAdat.nama
            daerah     = rumahAdat.daerah
            pathGambar = rumahAdat.pathGambar
            deskripsi  = rumahAdat.deskripsi
            keunikan   = rumahAdat.keunikan
            createdAt  = rumahAdat.createdAt
            updatedAt  = rumahAdat.updatedAt
        }
        dao.id.value.toString()
    }

    override suspend fun updateRumahAdat(id: String, newRumahAdat: RumahAdat): Boolean = suspendTransaction {
        val dao = RumahAdatDAO
            .find { RumahAdatTable.id eq UUID.fromString(id) }
            .limit(1)
            .firstOrNull()

        if (dao != null) {
            dao.nama       = newRumahAdat.nama
            dao.daerah     = newRumahAdat.daerah
            dao.pathGambar = newRumahAdat.pathGambar
            dao.deskripsi  = newRumahAdat.deskripsi
            dao.keunikan   = newRumahAdat.keunikan
            dao.updatedAt  = newRumahAdat.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun removeRumahAdat(id: String): Boolean = suspendTransaction {
        val rowsDeleted = RumahAdatTable.deleteWhere {
            RumahAdatTable.id eq UUID.fromString(id)
        }
        rowsDeleted == 1
    }

    // ─────────────────────────────────────────────
    // Private helper: konversi DAO → Entity
    // ─────────────────────────────────────────────
    private fun daoToModel(dao: RumahAdatDAO): RumahAdat {
        val relativePath = dao.pathGambar.removePrefix("uploads/")
        return RumahAdat(
            id         = dao.id.value.toString(),
            nama       = dao.nama,
            daerah     = dao.daerah,
            pathGambar = dao.pathGambar,
            gambar     = "$baseUrl/static/$relativePath",
            deskripsi  = dao.deskripsi,
            keunikan   = dao.keunikan,
            createdAt  = dao.createdAt,
            updatedAt  = dao.updatedAt,
        )
    }
}