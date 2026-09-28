package br.com.plasvale.pedidos.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Item do pedido. Reproduz uma linha da aba "FOLHA DE PEDIDO":
 *
 *  PREÇO UNI.   = PREÇO TABELA - (PREÇO TABELA * DESC.)
 *  PREÇO C/ IPI = PREÇO UNI. * (1 + IPI%)
 *  PREÇO C/ ST  = PREÇO C/ IPI * (1 + ST%)
 *  VALOR LÍQ.   = QTDE * PREÇO UNI.
 *  VALOR BRUTO  = QTDE * PREÇO C/ ST
 */
data class ItemPedido(
    var referencia: String = "",
    var descricao: String = "",
    var embalagem: Int = 0,
    var cor: String = "",
    var quantidade: Int = 0,
    var precoTabela: Double = 0.0,
    var descontoPercent: Double = 0.0,
    var ipiPercent: Double = 0.0,
    var stPercent: Double = 0.0
) {
    val precoUnitario: Double
        get() = precoTabela - (precoTabela * descontoPercent / 100.0)

    val precoComIpi: Double
        get() = precoUnitario * (1.0 + ipiPercent / 100.0)

    val precoComSt: Double
        get() = precoComIpi * (1.0 + stPercent / 100.0)

    val valorLiquido: Double
        get() = quantidade * precoUnitario

    val valorIpi: Double
        get() = quantidade * (precoComIpi - precoUnitario)

    val valorSt: Double
        get() = quantidade * (precoComSt - precoComIpi)

    val valorBruto: Double
        get() = quantidade * precoComSt

    /** A planilha usa M.V. (múltiplo de venda): a quantidade deveria ser múltipla da embalagem. */
    val multiploOk: Boolean
        get() = embalagem <= 0 || quantidade % embalagem == 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("referencia", referencia)
        put("descricao", descricao)
        put("embalagem", embalagem)
        put("cor", cor)
        put("quantidade", quantidade)
        put("precoTabela", precoTabela)
        put("descontoPercent", descontoPercent)
        put("ipiPercent", ipiPercent)
        put("stPercent", stPercent)
    }

    companion object {
        fun fromJson(o: JSONObject) = ItemPedido(
            referencia = o.optString("referencia"),
            descricao = o.optString("descricao"),
            embalagem = o.optInt("embalagem"),
            cor = o.optString("cor"),
            quantidade = o.optInt("quantidade"),
            precoTabela = o.optDouble("precoTabela", 0.0),
            descontoPercent = o.optDouble("descontoPercent", 0.0),
            ipiPercent = o.optDouble("ipiPercent", 0.0),
            stPercent = o.optDouble("stPercent", 0.0)
        )
    }
}

data class Pedido(
    var id: String = UUID.randomUUID().toString(),
    var numero: String = "",
    var dataMillis: Long = System.currentTimeMillis(),
    var cliente: String = "",
    var cnpj: String = "",
    var cidade: String = "",
    var uf: String = "",
    var telefone: String = "",
    var comprador: String = "",
    var representante: String = "",
    var tabela: String = "130",
    var prazo: String = "À Vista",
    var descontoPadrao: Double = 0.0,
    var ipiPercent: Double = 6.5,
    var stPercent: Double = 0.0,
    var transportadora: String = "",
    var observacao: String = "",
    var itens: MutableList<ItemPedido> = mutableListOf()
) {
    val totalLiquido: Double get() = itens.sumOf { it.valorLiquido }
    val totalIpi: Double get() = itens.sumOf { it.valorIpi }
    val totalSt: Double get() = itens.sumOf { it.valorSt }
    val totalBruto: Double get() = itens.sumOf { it.valorBruto }
    val totalPecas: Int get() = itens.sumOf { it.quantidade }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("numero", numero)
        put("dataMillis", dataMillis)
        put("cliente", cliente)
        put("cnpj", cnpj)
        put("cidade", cidade)
        put("uf", uf)
        put("telefone", telefone)
        put("comprador", comprador)
        put("representante", representante)
        put("tabela", tabela)
        put("prazo", prazo)
        put("descontoPadrao", descontoPadrao)
        put("ipiPercent", ipiPercent)
        put("stPercent", stPercent)
        put("transportadora", transportadora)
        put("observacao", observacao)
        val arr = JSONArray()
        itens.forEach { arr.put(it.toJson()) }
        put("itens", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Pedido {
            val itens = mutableListOf<ItemPedido>()
            val arr = o.optJSONArray("itens")
            if (arr != null) {
                for (i in 0 until arr.length()) itens.add(ItemPedido.fromJson(arr.getJSONObject(i)))
            }
            return Pedido(
                id = o.optString("id", UUID.randomUUID().toString()),
                numero = o.optString("numero"),
                dataMillis = o.optLong("dataMillis", System.currentTimeMillis()),
                cliente = o.optString("cliente"),
                cnpj = o.optString("cnpj"),
                cidade = o.optString("cidade"),
                uf = o.optString("uf"),
                telefone = o.optString("telefone"),
                comprador = o.optString("comprador"),
                representante = o.optString("representante"),
                tabela = o.optString("tabela", "130"),
                prazo = o.optString("prazo", "À Vista"),
                descontoPadrao = o.optDouble("descontoPadrao", 0.0),
                ipiPercent = o.optDouble("ipiPercent", 6.5),
                stPercent = o.optDouble("stPercent", 0.0),
                transportadora = o.optString("transportadora"),
                observacao = o.optString("observacao"),
                itens = itens
            )
        }
    }
}
