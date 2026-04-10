package org.delcom.repositories

import org.delcom.entities.RumahAdat

interface IRumahAdatRepository {
    suspend fun getRumahAdatList(search: String): List<RumahAdat>
    suspend fun getRumahAdatById(id: String): RumahAdat?
    suspend fun getRumahAdatByNama(nama: String): RumahAdat?
    suspend fun addRumahAdat(rumahAdat: RumahAdat): String
    suspend fun updateRumahAdat(id: String, newRumahAdat: RumahAdat): Boolean
    suspend fun removeRumahAdat(id: String): Boolean
}