package br.com.plasvale.pedidos.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.plasvale.pedidos.R
import br.com.plasvale.pedidos.data.Catalogo
import br.com.plasvale.pedidos.data.PedidoRepository
import br.com.plasvale.pedidos.databinding.ActivityMainBinding
import br.com.plasvale.pedidos.model.Pedido
import br.com.plasvale.pedidos.util.Formato
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repositorio: PedidoRepository
    private lateinit var adapter: PedidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        Catalogo.carregar(this)
        repositorio = PedidoRepository(this)

        adapter = PedidoAdapter(
            emptyList(),
            aoAbrir = { abrirPedido(it.id) },
            aoGerarPdf = { PdfCompartilhamento.gerarECompartilhar(this, it) },
            aoDuplicar = { duplicar(it) },
            aoExcluir = { confirmarExclusao(it) }
        )
        binding.listaPedidos.layoutManager = LinearLayoutManager(this)
        binding.listaPedidos.adapter = adapter

        binding.botaoNovo.setOnClickListener { abrirPedido(null) }
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun atualizarLista() {
        val pedidos = repositorio.listar()
        adapter.atualizar(pedidos)
        binding.textoVazio.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
        val total = pedidos.sumOf { it.totalBruto }
        binding.textoResumo.text =
            "Tabela ${Catalogo.tabela} • ${Catalogo.referencia} • ${Catalogo.produtos.size} produtos\n" +
                "${pedidos.size} pedido(s) salvos no aparelho • total ${Formato.moeda(total)}"
    }

    private fun abrirPedido(id: String?) {
        val intent = Intent(this, PedidoActivity::class.java)
        if (id != null) intent.putExtra(PedidoActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    private fun duplicar(pedido: Pedido) {
        val copia = pedido.copy(
            id = UUID.randomUUID().toString(),
            numero = repositorio.proximoNumero(),
            dataMillis = System.currentTimeMillis(),
            itens = pedido.itens.map { it.copy() }.toMutableList()
        )
        repositorio.salvar(copia)
        atualizarLista()
    }

    private fun confirmarExclusao(pedido: Pedido) {
        AlertDialog.Builder(this)
            .setTitle("Excluir pedido")
            .setMessage("Excluir o pedido nº ${pedido.numero} de ${pedido.cliente.ifBlank { "cliente não informado" }}?")
            .setPositiveButton(R.string.excluir) { _, _ ->
                repositorio.excluir(pedido.id)
                atualizarLista()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acaoConfiguracoes -> {
                startActivity(Intent(this, ConfiguracaoActivity::class.java))
                true
            }
            R.id.acaoCatalogo -> {
                SeletorProduto.mostrar(this, "Catálogo — Tabela ${Catalogo.tabela}", "À Vista") { }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
