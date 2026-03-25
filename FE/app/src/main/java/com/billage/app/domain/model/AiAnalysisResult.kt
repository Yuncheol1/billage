package com.billage.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiAnalysisResult(
    val is_safe: Boolean = true,
    val warnings: List<AiWarning> = emptyList(),
    val tax_summary: AiTaxSummary? = null,
    val summary: String = ""
)

@Serializable
data class AiWarning(
    val risk_level: String = "LOW", // LOW, MEDIUM, HIGH
    val category: String = "",
    val clause_id: String? = null,
    val reason: String = "",
    val suggestion: String = ""
)

@Serializable
data class AiTaxSummary(
    val gift_tax_risk: Boolean = false,
    val estimated_gift_tax: Double? = 0.0,
    val withholding_tax_rate: String? = null
)
