package com.edu.unal.directorio.data.db

import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.CompanyDraft

/**
 * The 8 fictional companies loaded once, when the database file is created (design D8).
 *
 * The data is visible content, so names and products are in Spanish. Emails and URLs use the
 * reserved `example.com` domain and phones use the `+57` prefix with `555-01xx` example numbers.
 * Each has exactly one classification: 2 Consulting, 3 CustomDevelopment, 3 SoftwareFactory.
 */
val SAMPLE_COMPANIES: List<CompanyDraft> = listOf(
    CompanyDraft(
        name = "Andes Consultores",
        email = "contacto@andesconsultores.example.com",
        url = "https://andesconsultores.example.com",
        phone = "+57 300 555-0101",
        products = "Diagnóstico de procesos\nPlanes de transformación digital",
        classification = Consulting,
    ),
    CompanyDraft(
        name = "Bosque Digital",
        email = "contacto@bosquedigital.example.com",
        url = "https://bosquedigital.example.com",
        phone = "+57 301 555-0102",
        products = "Aplicaciones móviles a la medida",
        classification = CustomDevelopment,
    ),
    CompanyDraft(
        name = "Cumbre Software",
        email = "contacto@cumbresoftware.example.com",
        url = "https://cumbresoftware.example.com",
        phone = "+57 302 555-0103",
        products = "Consultoría de arquitectura\nDesarrollo a la medida\nFábrica de pruebas automatizadas",
        classification = SoftwareFactory,
    ),
    // Empty URL and phone.
    CompanyDraft(
        name = "Delta Soluciones",
        email = "contacto@deltasoluciones.example.com",
        url = "",
        phone = "",
        products = "Soporte técnico de aplicaciones",
        classification = SoftwareFactory,
    ),
    // Name with an accent and a capital eñe (normalization).
    CompanyDraft(
        name = "Ñandú Tecnología",
        email = "contacto@nandutecnologia.example.com",
        url = "https://nandutecnologia.example.com",
        phone = "+57 304 555-0105",
        products = "Plataformas web\nIntegración de sistemas\nMantenimiento evolutivo",
        classification = CustomDevelopment,
    ),
    CompanyDraft(
        name = "Horizonte Ágil",
        email = "contacto@horizonteagil.example.com",
        url = "https://horizonteagil.example.com",
        phone = "+57 305 555-0106",
        products = "Células de desarrollo dedicadas",
        classification = Consulting,
    ),
    // Empty phone only.
    CompanyDraft(
        name = "Lumbre Sistemas",
        email = "contacto@lumbresistemas.example.com",
        url = "https://lumbresistemas.example.com",
        phone = "",
        products = "Sistemas de inventario",
        classification = CustomDevelopment,
    ),
    // Long multi-line products.
    CompanyDraft(
        name = "Zafiro Labs",
        email = "contacto@zafirolabs.example.com",
        url = "https://zafirolabs.example.com",
        phone = "+57 307 555-0108",
        products = "Analítica de datos\nAutomatización de procesos\nAplicaciones a la medida\nCapacitación en ingeniería de software",
        classification = SoftwareFactory,
    ),
)
