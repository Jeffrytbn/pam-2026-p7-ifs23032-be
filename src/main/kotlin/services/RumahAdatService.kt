package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.RumahAdatRequest
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IRumahAdatRepository
import java.io.File
import java.util.*

class RumahAdatService(private val rumahAdatRepository: IRumahAdatRepository) {

    // ─────────────────────────────────────────────
    // GET /rumah-adat — Mengambil semua data rumah adat
    // ─────────────────────────────────────────────
    suspend fun getAllRumahAdat(call: ApplicationCall) {
        val search = call.request.queryParameters["search"] ?: ""

        val rumahAdatList = rumahAdatRepository.getRumahAdatList(search)

        val response = DataResponse(
            status  = "success",
            message = "Berhasil mengambil daftar rumah adat",
            data    = mapOf("rumahAdatList" to rumahAdatList)
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // GET /rumah-adat/{id} — Mengambil detail rumah adat
    // ─────────────────────────────────────────────
    suspend fun getRumahAdatById(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID rumah adat tidak boleh kosong!")

        val rumahAdat = rumahAdatRepository.getRumahAdatById(id)
            ?: throw AppException(404, "Data rumah adat tidak tersedia!")

        val response = DataResponse(
            status  = "success",
            message = "Berhasil mengambil data rumah adat",
            data    = mapOf("rumahAdat" to rumahAdat)
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // POST /rumah-adat — Menambahkan data rumah adat
    // ─────────────────────────────────────────────
    suspend fun createRumahAdat(call: ApplicationCall) {
        val rumahAdatReq = getRumahAdatRequest(call)

        validateRumahAdatRequest(rumahAdatReq)

        // Cek duplikat berdasarkan nama
        val existRumahAdat = rumahAdatRepository.getRumahAdatByNama(rumahAdatReq.nama)
        if (existRumahAdat != null) {
            cleanupTempFile(rumahAdatReq.pathGambar)
            throw AppException(409, "Rumah adat dengan nama ini sudah terdaftar!")
        }

        val rumahAdatId = rumahAdatRepository.addRumahAdat(rumahAdatReq.toEntity())

        val response = DataResponse(
            status  = "success",
            message = "Berhasil menambahkan data rumah adat",
            data    = mapOf("rumahAdatId" to rumahAdatId)
        )
        call.respond(HttpStatusCode.Created, response)
    }

    // ─────────────────────────────────────────────
    // PUT /rumah-adat/{id} — Mengubah data rumah adat
    // ─────────────────────────────────────────────
    suspend fun updateRumahAdat(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID rumah adat tidak boleh kosong!")

        val oldRumahAdat = rumahAdatRepository.getRumahAdatById(id)
            ?: throw AppException(404, "Data rumah adat tidak tersedia!")

        val rumahAdatReq = getRumahAdatRequest(call)

        // Gunakan path gambar lama jika tidak ada file baru yang diupload
        if (rumahAdatReq.pathGambar.isEmpty()) {
            rumahAdatReq.pathGambar = oldRumahAdat.pathGambar
        }

        validateRumahAdatRequest(rumahAdatReq)

        // Cek duplikat nama hanya jika nama diubah
        if (rumahAdatReq.nama != oldRumahAdat.nama) {
            val existRumahAdat = rumahAdatRepository.getRumahAdatByNama(rumahAdatReq.nama)
            if (existRumahAdat != null) {
                cleanupTempFile(rumahAdatReq.pathGambar)
                throw AppException(409, "Rumah adat dengan nama ini sudah terdaftar!")
            }
        }

        // Hapus gambar lama jika file baru berhasil diupload
        if (rumahAdatReq.pathGambar != oldRumahAdat.pathGambar) {
            cleanupTempFile(oldRumahAdat.pathGambar)
        }

        val isUpdated = rumahAdatRepository.updateRumahAdat(id, rumahAdatReq.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data rumah adat!")
        }

        val response = DataResponse(
            status  = "success",
            message = "Berhasil mengubah data rumah adat",
            data    = null
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // DELETE /rumah-adat/{id} — Menghapus data rumah adat
    // ─────────────────────────────────────────────
    suspend fun deleteRumahAdat(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID rumah adat tidak boleh kosong!")

        val oldRumahAdat = rumahAdatRepository.getRumahAdatById(id)
            ?: throw AppException(404, "Data rumah adat tidak tersedia!")

        val isDeleted = rumahAdatRepository.removeRumahAdat(id)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data rumah adat!")
        }

        // Hapus file gambar setelah data berhasil dihapus dari database
        cleanupTempFile(oldRumahAdat.pathGambar)

        val response = DataResponse(
            status  = "success",
            message = "Berhasil menghapus data rumah adat",
            data    = null
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Membaca data multipart dari request dan mengisinya ke RumahAdatRequest.
     * File yang diupload disimpan di folder "uploads/rumah_adat/".
     */
    private suspend fun getRumahAdatRequest(call: ApplicationCall): RumahAdatRequest {
        val req = RumahAdatRequest()

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "nama"      -> req.nama      = part.value.trim()
                        "daerah"    -> req.daerah    = part.value.trim()
                        "deskripsi" -> req.deskripsi = part.value
                        "keunikan"  -> req.keunikan  = part.value
                    }
                }

                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = "${UUID.randomUUID()}$ext"

                    // File disimpan di "uploads/rumah_adat/" agar dapat dilayani
                    // oleh Static Content plugin melalui URL "/static/rumah_adat/..."
                    val filePath = "uploads/rumah_adat/$fileName"
                    val file = File(filePath)
                    file.parentFile.mkdirs()

                    part.provider().copyAndClose(file.writeChannel())
                    req.pathGambar = filePath
                }

                else -> {}
            }
            part.dispose()
        }

        return req
    }

    /**
     * Memvalidasi data request rumah adat.
     */
    private fun validateRumahAdatRequest(req: RumahAdatRequest) {
        val validator = ValidatorHelper(req.toMap())
        validator.required("nama",      "Nama tidak boleh kosong")
        validator.required("daerah",    "Daerah tidak boleh kosong")
        validator.required("deskripsi", "Deskripsi tidak boleh kosong")
        validator.required("keunikan",  "Keunikan tidak boleh kosong")
        validator.required("pathGambar","Gambar tidak boleh kosong")
        validator.validate()

        val file = File(req.pathGambar)
        if (!file.exists()) {
            throw AppException(400, "Gambar rumah adat gagal diupload!")
        }
    }

    /**
     * Menghapus file dari disk (gambar lama / file gagal proses).
     */
    private fun cleanupTempFile(path: String) {
        if (path.isBlank()) return
        val file = File(path)
        if (file.exists()) file.delete()
    }
}