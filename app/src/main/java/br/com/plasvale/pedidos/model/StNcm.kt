package br.com.plasvale.pedidos.model

/** Linha da aba "BASE CONTÁBIL": alíquotas de ST por NCM e UF. */
data class StNcm(
    val ncm: String,
    val revenda: Map<String, Double>,
    val nacional: Map<String, Double>
)
