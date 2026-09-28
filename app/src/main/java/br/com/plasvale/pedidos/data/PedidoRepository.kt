package br.com.plasvale.pedidos.data

import android.content.Context
import br.com.plasvale.pedidos.model.Pedido
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistência 100% local: um único arquivo JSON na memória interna do aparelho
 * (Context.filesDir), que não é visível para outros aplicativos.
 */
class PedidoRepository(context: Context) {

    private val arquivo = File(context.filesDir, "pedidos.json")

    fun listar(): MutableList<Pedido> {
        if (!arquivo.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(arquivo.readText())
            val lista = mutableListOf<Pedido>()
            for (i in 0 until arr.length()) lista.add(Pedido.fromJson(arr.getJSONObject(i)))
            lista.sortByDescending { it.dataMillis }
            lista
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun porId(id: String): Pedido? = listar().firstOrNull { it.id == id }

    fun salvar(pedido: Pedido) {
        val lista = listar()
        val indice = lista.indexOfFirst { it.id == pedido.id }
        if (indice >= 0) lista[indice] = pedido else lista.add(pedido)
        gravar(lista)
    }

    fun excluir(id: String) {
        val lista = listar().filter { it.id != id }.toMutableList()
        gravar(lista)
    }

    /** Próximo número sequencial de pedido, no formato 0001, 0002... */
    fun proximoNumero(): String {
        val maior = listar().mapNotNull { it.numero.filter { c -> c.isDigit() }.toIntOrNull() }.maxOrNull() ?: 0
        return String.format("%04d", maior + 1)
    }

    private fun gravar(lista: List<Pedido>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.toJson()) }
        arquivo.writeText(arr.toString())
    }

    /** Exporta todos os pedidos (backup) como JSON legível. */
    fun exportarJson(): String {
        val raiz = JSONObject()
        raiz.put("app", "Pedidos Plasvale")
        raiz.put("exportadoEm", System.currentTimeMillis())
        val arr = JSONArray()
        listar().forEach { arr.put(it.toJson()) }
        raiz.put("pedidos", arr)
        return raiz.toString(1)
    }
}
