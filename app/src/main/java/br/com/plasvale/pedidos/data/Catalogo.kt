package br.com.plasvale.pedidos.data

import android.content.Context
import br.com.plasvale.pedidos.model.Produto
import br.com.plasvale.pedidos.model.StNcm
import org.json.JSONObject

/**
 * Catálogo lido de assets/catalogo.json — gerado a partir da planilha
 * "FOLHA DE PEDIDOS TABELA 130 COM IPI 6,5 - SETEMBRO 2026".
 * Fica todo embarcado no aplicativo: nenhuma conexão é necessária.
 */
object Catalogo {

    var tabela: String = "130"; private set
    var referencia: String = ""; private set
    var ipiPadrao: Double = 6.5; private set
    var prazos: List<String> = emptyList(); private set
    var ufs: List<String> = emptyList(); private set
    var avisos: List<String> = emptyList(); private set
    var produtos: List<Produto> = emptyList(); private set
    var stPorNcm: List<StNcm> = emptyList(); private set

    private var carregado = false

    @Synchronized
    fun carregar(context: Context) {
        if (carregado) return
        val texto = context.assets.open("catalogo.json").bufferedReader().use { it.readText() }
        val raiz = JSONObject(texto)

        tabela = raiz.optString("tabela", "130")
        referencia = raiz.optString("referencia", "")
        ipiPadrao = raiz.optDouble("ipiPadrao", 6.5)

        prazos = raiz.optJSONArray("prazos").toStringList()
        ufs = raiz.optJSONArray("ufs").toStringList().sorted()
        avisos = raiz.optJSONArray("avisos").toStringList()

        val listaProdutos = mutableListOf<Produto>()
        raiz.optJSONArray("produtos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val precosJson = o.optJSONObject("precos")
                val precos = mutableMapOf<String, Double>()
                if (precosJson != null) {
                    val chaves = precosJson.keys()
                    while (chaves.hasNext()) {
                        val k = chaves.next()
                        if (!precosJson.isNull(k)) precos[k] = precosJson.optDouble(k, 0.0)
                    }
                }
                listaProdutos.add(
                    Produto(
                        referencia = o.optString("referencia"),
                        descricao = o.optString("descricao").trim(),
                        embalagem = o.optInt("embalagem"),
                        precos = precos
                    )
                )
            }
        }
        produtos = listaProdutos

        val listaSt = mutableListOf<StNcm>()
        raiz.optJSONArray("stPorNcm")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                listaSt.add(
                    StNcm(
                        ncm = o.optString("ncm"),
                        revenda = o.optJSONObject("revenda").toDoubleMap(),
                        nacional = o.optJSONObject("nacional").toDoubleMap()
                    )
                )
            }
        }
        stPorNcm = listaSt

        carregado = true
    }

    fun buscar(termo: String): List<Produto> =
        if (termo.isBlank()) produtos else produtos.filter { it.combina(termo) }

    fun porReferencia(referencia: String): Produto? =
        produtos.firstOrNull { it.referencia.equals(referencia, ignoreCase = true) }

    /**
     * ST sugerida para a UF, a partir da aba "BASE CONTÁBIL": usa o NCM 3924.10.00
     * (artigos de plástico para uso doméstico), que é o da maior parte da linha.
     * É apenas uma sugestão editável: a planilha calcula a ST por NCM de cada produto,
     * e a coluna de NCM não veio preenchida no arquivo original.
     * Em SP a orientação da aba "Importante" é desconsiderar a ST.
     */
    fun stSugeridaPara(uf: String): Double {
        if (uf.equals("SP", ignoreCase = true)) return 0.0
        val linha = stPorNcm.firstOrNull { it.ncm.replace(".", "") == "39241000" } ?: return 0.0
        val aliquota = linha.revenda[uf.uppercase()] ?: return 0.0
        return aliquota * 100.0
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until length()) out.add(optString(i))
        return out
    }

    private fun JSONObject?.toDoubleMap(): Map<String, Double> {
        if (this == null) return emptyMap()
        val out = mutableMapOf<String, Double>()
        val chaves = keys()
        while (chaves.hasNext()) {
            val k = chaves.next()
            out[k] = optDouble(k, 0.0)
        }
        return out
    }
}
