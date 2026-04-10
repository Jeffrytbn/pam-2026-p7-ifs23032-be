package org.delcom.module

import io.ktor.server.application.*
import org.delcom.repositories.IPlantRepository
import org.delcom.repositories.IRumahAdatRepository
import org.delcom.repositories.PlantRepository
import org.delcom.repositories.RumahAdatRepository
import org.delcom.services.PlantService
import org.delcom.services.ProfileService
import org.delcom.services.RumahAdatService
import org.koin.dsl.module

fun appModule(application: Application) = module {
    val baseUrl = application.environment.config
        .property("ktor.app.baseUrl")
        .getString()
        .trimEnd('/')   // Pastikan tidak ada trailing slash

    // ── Plant ──────────────────────────────────────
    single<IPlantRepository> {
        PlantRepository(baseUrl)
    }
    single {
        PlantService(get())
    }

    // ── Rumah Adat ─────────────────────────────────
    single<IRumahAdatRepository> {
        RumahAdatRepository(baseUrl)
    }
    single {
        RumahAdatService(get())
    }

    // ── Profile ────────────────────────────────────
    single {
        ProfileService(baseUrl)
    }
}