package br.com.plasvale.pedidos.model

/**
 * Produto da aba "Tabela 130" da planilha.
 *
 * [precos] guarda o preço de tabela para cada prazo de pagamento
 * ("À Vista", "14", "21", ... "90"), exatamente como as colunas D..N da planilha.
 */
data class Produto(
    val referencia: String,
    val descricao: String,
    val embalagem: Int,
    val precos: Map<String, Double>
) {
    fun precoPara(prazo: String): Double = precos[prazo] ?: 0.0

    fun combina(termo: String): Boolean {
        if (termo.isBlank()) return true
        val t = termo.trim().uppercase()
        return referencia.uppercase().contains(t) || descricao.uppercase().contains(t)
    }
}
