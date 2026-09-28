package br.com.plasvale.pedidos.ui

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import br.com.plasvale.pedidos.data.Catalogo
import br.com.plasvale.pedidos.databinding.DialogProdutosBinding
import br.com.plasvale.pedidos.model.Produto
import br.com.plasvale.pedidos.util.Formato

/** Diálogo de busca no catálogo embarcado (código ou descrição). */
object SeletorProduto {

    private class ProdutoAdapter(
        private val activity: Activity,
        var itens: List<Produto>,
        private val prazo: String
    ) : BaseAdapter() {

        override fun getCount(): Int = itens.size
        override fun getItem(position: Int): Produto = itens[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(activity)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            val produto = itens[position]
            view.findViewById<TextView>(android.R.id.text1).text =
                "${produto.referencia} — ${produto.descricao}"
            view.findViewById<TextView>(android.R.id.text2).text =
                "${Formato.moeda(produto.precoPara(prazo))}  •  M.V. ${produto.embalagem}  •  prazo $prazo"
            return view
        }
    }

    fun mostrar(activity: Activity, titulo: String, prazo: String, aoSelecionar: (Produto) -> Unit) {
        val binding = DialogProdutosBinding.inflate(LayoutInflater.from(activity))
        val adapter = ProdutoAdapter(activity, Catalogo.produtos, prazo)
        binding.listaProdutos.adapter = adapter
        binding.textoContador.text = "${Catalogo.produtos.size} produtos"

        binding.campoBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val filtrados = Catalogo.buscar(s?.toString().orEmpty())
                adapter.itens = filtrados
                adapter.notifyDataSetChanged()
                binding.textoContador.text = "${filtrados.size} produtos"
            }
        })

        val dialogo = AlertDialog.Builder(activity)
            .setTitle(titulo)
            .setView(binding.root)
            .setNegativeButton("Fechar", null)
            .create()

        binding.listaProdutos.setOnItemClickListener { _, _, posicao, _ ->
            val produto = adapter.getItem(posicao)
            dialogo.dismiss()
            aoSelecionar(produto)
        }

        dialogo.show()
    }
}
