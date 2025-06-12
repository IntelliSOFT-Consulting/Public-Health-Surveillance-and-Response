package com.icl.surveillance.utils

object Constants {

    //MOH 505
    const val COUNTY = "a4-county"
    const val SUB_COUNTY = "a3-sub-county"
    const val WARD = "819943434"
    const val HEALTH_FACILITY = "819946803677"
    const val FACILITY_TYPE = "438862163919"
    const val WEEK_ENDING_DATE = "728034137219"

    val FACILITY_DETAILS =
        listOf(COUNTY, SUB_COUNTY, WARD, HEALTH_FACILITY, FACILITY_TYPE, WEEK_ENDING_DATE)

    const val AEFI = "aefi-summary"
    const val BACTERIAL_MENINGITIS = "bacterial-meningitis-summary"
    const val ACUTE_JAUNDICE = "acute-jaundice-summary"
    const val NEONATAL_DEATHS = "neonatal-deaths-summary"
    const val ACUTE_MALNUTRITION = "acute-malnutrition-summary"
    const val CHIKUNGUNYA = "chikungunya-summary"
    const val COVID_19 = "covid--19-summary"
    const val SARI_CLUSTER = "sari-cluster-ge3-cases-summary"
    const val DENGUE = "dengue-summary"
    const val MEASLES = "measles-summary"
    const val RIFT_VALLEY_FEVER = "rift-valley-fever-summary"
    const val TYPHOID = "typhoid-summary"
    const val ANTHRAX = "anthrax-summary"
    const val GUINEA_WORM = "guinea-worm-disease-summary"
    const val VHF = "vhf-summary"
    const val ZIKA = "zika-virus-summary"
    const val SUSPECTED_MALARIA = "suspected-malaria-summary"
    const val YELLOW_FEVER = "yellow-fever-summary"
    const val SUSPECTED_MDR_XDR_TB = "suspected-mdr-xdr-tb-summary"
    const val OTHERS = "others-specify-summary"

    val ALL = listOf(
        AEFI,
        BACTERIAL_MENINGITIS,
        ACUTE_JAUNDICE,
        NEONATAL_DEATHS,
        ACUTE_MALNUTRITION,
        CHIKUNGUNYA,
        COVID_19,
        SARI_CLUSTER,
        DENGUE,
        MEASLES,
        RIFT_VALLEY_FEVER,
        TYPHOID,
        ANTHRAX,
        GUINEA_WORM,
        VHF,
        ZIKA,
        SUSPECTED_MALARIA,
        YELLOW_FEVER,
        SUSPECTED_MDR_XDR_TB,
        OTHERS
    )

    val ALL_LINK_IDS = FACILITY_DETAILS + ALL
}

