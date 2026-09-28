package br.com.plasvale.pedidos.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.plasvale.pedidos.databinding.ItemPedidoBinding
import br.com.plasvale.pedidos.model.Pedido
import br.com.plasvale.pedidos.util.Formato

class PedidoAdapter(
    private var pedidos: List<Pedido>,
    private val aoAbrir: (Pedido) -> Unit,
    private val aoGerarPdf: (Pedido) -> Unit,
    private val aoDuplicar: (Pedido) -> Unit,
    private val aoExcluir: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.Holder>() {

    class Holder(val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root)

    fun atualizar(novos: List<Pedido>) {
        pedidos = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = pedidos.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pedido = pedidos[position]
        with(holder.binding) {
            textoCliente.text = pedido.cliente.ifBlank { "(sem cliente)" }
            textoNumero.text = "Nº ${pedido.numero}"
            textoDetalhes.text = buildString {
                append(Formato.dataHora(pedido.dataMillis))
                if (pedido.uf.isNotBlank()) append("  •  ").append(pedido.uf)
                append("  •  ").append(pedido.prazo)
                append("  •  ").append(pedido.itens.size).append(" itens")
            }
            textoTotal.text = "Total ${Formato.moeda(pedido.totalBruto)}  (líq. ${Formato.moeda(pedido.totalLiquido)})"
            root.setOnClickListener { aoAbrir(pedido) }
            botaoPdf.setOnClickListener { aoGerarPdf(pedido) }
            botaoDuplicar.setOnClickListener { aoDuplicar(pedido) }
            botaoExcluir.setOnClickListener { aoExcluir(pedido) }
        }
    }
}
