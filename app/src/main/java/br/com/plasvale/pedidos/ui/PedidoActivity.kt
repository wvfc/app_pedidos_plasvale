package br.com.plasvale.pedidos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.plasvale.pedidos.R
import br.com.plasvale.pedidos.data.Catalogo
import br.com.plasvale.pedidos.data.Configuracao
import br.com.plasvale.pedidos.data.PedidoRepository
import br.com.plasvale.pedidos.databinding.ActivityPedidoBinding
import br.com.plasvale.pedidos.databinding.DialogItemBinding
import br.com.plasvale.pedidos.databinding.ItemProdutoPedidoBinding
import br.com.plasvale.pedidos.model.ItemPedido
import br.com.plasvale.pedidos.model.Pedido
import br.com.plasvale.pedidos.model.Produto
import br.com.plasvale.pedidos.util.Formato

class PedidoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "pedido_id"
    }

    private lateinit var binding: ActivityPedidoBinding
    private lateinit var repositorio: PedidoRepository
    private lateinit var config: Configuracao
    private lateinit var pedido: Pedido
    private var novo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Catalogo.carregar(this)
        repositorio = PedidoRepository(this)
        config = Configuracao(this)

        val id = intent.getStringExtra(EXTRA_ID)
        val existente = id?.let { repositorio.porId(it) }
        novo = existente == null
        pedido = existente ?: Pedido(
            numero = repositorio.proximoNumero(),
            uf = config.ufPadrao,
            prazo = config.prazoPadrao,
            descontoPadrao = config.descontoPadrao,
            ipiPercent = config.ipiPadrao,
            stPercent = config.stDaUf(config.ufPadrao),
            representante = config.representante,
            tabela = Catalogo.tabela
        )

        supportActionBar?.title = if (novo) "Novo pedido" else "Pedido nº ${pedido.numero}"

        configurarSpinners()
        preencherCampos()
        configurarBotoes()
        redesenharItens()
    }

    private fun configurarSpinners() {
        val adapterUf = ArrayAdapter(this, android.R.layout.simple_spinner_item, Catalogo.ufs)
        adapterUf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUf.adapter = adapterUf
        val posicaoUf = Catalogo.ufs.indexOf(pedido.uf)
        if (posicaoUf >= 0) binding.spinnerUf.setSelection(posicaoUf)

        val adapterPrazo = ArrayAdapter(this, android.R.layout.simple_spinner_item, Catalogo.prazos)
        adapterPrazo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPrazo.adapter = adapterPrazo
        val posicaoPrazo = Catalogo.prazos.indexOf(pedido.prazo)
        if (posicaoPrazo >= 0) binding.spinnerPrazo.setSelection(posicaoPrazo)

        binding.spinnerUf.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val uf = Catalogo.ufs[position]
                if (uf != pedido.uf) {
                    coletarCampos()
                    pedido.uf = uf
                    pedido.stPercent = config.stDaUf(uf)
                    binding.campoSt.setText(Formato.numero(pedido.stPercent))
                    aplicarCondicoesNosItens(silencioso = true)
                }
                atualizarAviso()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerPrazo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val prazo = Catalogo.prazos[position]
                if (prazo != pedido.prazo) {
                    pedido.prazo = prazo
                    reprecificarItens()
                    Toast.makeText(
                        this@PedidoActivity,
                        "Preços atualizados para o prazo $prazo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun preencherCampos() {
        binding.campoCliente.setText(pedido.cliente)
        binding.campoCnpj.setText(pedido.cnpj)
        binding.campoCidade.setText(pedido.cidade)
        binding.campoTelefone.setText(pedido.telefone)
        binding.campoComprador.setText(pedido.comprador)
        binding.campoTransportadora.setText(pedido.transportadora)
        binding.campoObservacao.setText(pedido.observacao)
        binding.campoDesconto.setText(Formato.numero(pedido.descontoPadrao))
        binding.campoIpi.setText(Formato.numero(pedido.ipiPercent))
        binding.campoSt.setText(Formato.numero(pedido.stPercent))
        binding.textoTabela.text = "Tabela ${pedido.tabela} (${Catalogo.referencia})"
        atualizarAviso()
    }

    private fun atualizarAviso() {
        val avisos = mutableListOf<String>()
        if (pedido.uf.equals("SP", ignoreCase = true)) {
            avisos.add("Região de SP: desconsiderar a ST.")
        }
        avisos.add("Pedido mínimo: R$ 3.000,00 (pequeno e médio varejo) / R$ 5.000,00 (supermercados e grandes redes).")
        avisos.add("Tabela não válida para clientes do Simples Nacional.")
        binding.textoAviso.text = avisos.joinToString("\n")
    }

    private fun configurarBotoes() {
        binding.botaoAdicionarItem.setOnClickListener {
            coletarCampos()
            SeletorProduto.mostrar(this, "Escolher produto", pedido.prazo) { produto ->
                abrirDialogoItem(produto, null)
            }
        }

        binding.botaoAplicarCondicoes.setOnClickListener {
            coletarCampos()
            aplicarCondicoesNosItens(silencioso = false)
        }

        binding.botaoSalvar.setOnClickListener {
            if (salvar()) {
                Toast.makeText(this, "Pedido salvo no aparelho.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.botaoPdf.setOnClickListener {
            if (salvar()) PdfCompartilhamento.gerarECompartilhar(this, pedido)
        }
    }

    private fun coletarCampos() {
        pedido.cliente = binding.campoCliente.text.toString().trim()
        pedido.cnpj = binding.campoCnpj.text.toString().trim()
        pedido.cidade = binding.campoCidade.text.toString().trim()
        pedido.telefone = binding.campoTelefone.text.toString().trim()
        pedido.comprador = binding.campoComprador.text.toString().trim()
        pedido.transportadora = binding.campoTransportadora.text.toString().trim()
        pedido.observacao = binding.campoObservacao.text.toString().trim()
        pedido.descontoPadrao = Formato.paraDouble(binding.campoDesconto.text.toString(), 0.0)
        pedido.ipiPercent = Formato.paraDouble(binding.campoIpi.text.toString(), Catalogo.ipiPadrao)
        pedido.stPercent = Formato.paraDouble(binding.campoSt.text.toString(), 0.0)
        pedido.uf = binding.spinnerUf.selectedItem?.toString() ?: pedido.uf
        pedido.prazo = binding.spinnerPrazo.selectedItem?.toString() ?: pedido.prazo
        if (pedido.representante.isBlank()) pedido.representante = config.representante
    }

    private fun salvar(): Boolean {
        coletarCampos()
        if (pedido.cliente.isBlank()) {
            binding.campoCliente.error = "Informe o cliente"
            binding.campoCliente.requestFocus()
            return false
        }
        repositorio.salvar(pedido)
        novo = false
        return true
    }

    /** Reaplica desconto, IPI e ST informados no cabeçalho a todos os itens. */
    private fun aplicarCondicoesNosItens(silencioso: Boolean) {
        pedido.itens.forEach {
            it.descontoPercent = pedido.descontoPadrao
            it.ipiPercent = pedido.ipiPercent
            it.stPercent = pedido.stPercent
        }
        redesenharItens()
        if (!silencioso) {
            Toast.makeText(this, "Condições aplicadas a ${pedido.itens.size} item(ns).", Toast.LENGTH_SHORT).show()
        }
    }

    /** Ao trocar o prazo, o preço de tabela de cada item passa a ser o da nova coluna. */
    private fun reprecificarItens() {
        pedido.itens.forEach { item ->
            val produto = Catalogo.porReferencia(item.referencia)
            if (produto != null) item.precoTabela = produto.precoPara(pedido.prazo)
        }
        redesenharItens()
    }

    private fun abrirDialogoItem(produto: Produto, itemExistente: ItemPedido?) {
        val dialogBinding = DialogItemBinding.inflate(LayoutInflater.from(this))
        val precoTabela = itemExistente?.precoTabela ?: produto.precoPara(pedido.prazo)

        dialogBinding.textoProduto.text = "${produto.referencia} — ${produto.descricao}"
        dialogBinding.textoInfoProduto.text =
            "Preço de tabela (${pedido.prazo}): ${Formato.moeda(precoTabela)}  •  M.V. ${produto.embalagem}"

        dialogBinding.campoQuantidade.setText(
            if (itemExistente != null) itemExistente.quantidade.toString()
            else if (produto.embalagem > 0) produto.embalagem.toString() else "1"
        )
        dialogBinding.campoCor.setText(itemExistente?.cor ?: "")
        dialogBinding.campoDescontoItem.setText(
            Formato.numero(itemExistente?.descontoPercent ?: pedido.descontoPadrao)
        )
        dialogBinding.campoIpiItem.setText(Formato.numero(itemExistente?.ipiPercent ?: pedido.ipiPercent))
        dialogBinding.campoStItem.setText(Formato.numero(itemExistente?.stPercent ?: pedido.stPercent))

        fun montarItem(): ItemPedido = ItemPedido(
            referencia = produto.referencia,
            descricao = produto.descricao,
            embalagem = produto.embalagem,
            cor = dialogBinding.campoCor.text.toString().trim(),
            quantidade = Formato.paraInt(dialogBinding.campoQuantidade.text.toString(), 0),
            precoTabela = precoTabela,
            descontoPercent = Formato.paraDouble(dialogBinding.campoDescontoItem.text.toString(), 0.0),
            ipiPercent = Formato.paraDouble(dialogBinding.campoIpiItem.text.toString(), 0.0),
            stPercent = Formato.paraDouble(dialogBinding.campoStItem.text.toString(), 0.0)
        )

        fun atualizarPrevia() {
            val previa = montarItem()
            dialogBinding.textoPreviaItem.text = buildString {
                append("Preço unitário: ").append(Formato.moeda(previa.precoUnitario)).append("\n")
                append("Com IPI: ").append(Formato.moeda(previa.precoComIpi))
                append("   •   Com ST: ").append(Formato.moeda(previa.precoComSt)).append("\n")
                append("Valor líquido: ").append(Formato.moeda(previa.valorLiquido)).append("\n")
                append("Valor bruto: ").append(Formato.moeda(previa.valorBruto))
                if (!previa.multiploOk) {
                    append("\nAtenção: a quantidade não é múltiplo da embalagem (${produto.embalagem}).")
                }
            }
        }

        val observador = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = atualizarPrevia()
        }
        dialogBinding.campoQuantidade.addTextChangedListener(observador)
        dialogBinding.campoDescontoItem.addTextChangedListener(observador)
        dialogBinding.campoIpiItem.addTextChangedListener(observador)
        dialogBinding.campoStItem.addTextChangedListener(observador)
        atualizarPrevia()

        AlertDialog.Builder(this)
            .setTitle(if (itemExistente == null) "Adicionar item" else "Editar item")
            .setView(dialogBinding.root)
            .setPositiveButton("Confirmar") { _, _ ->
                val novoItem = montarItem()
                if (novoItem.quantidade <= 0) {
                    Toast.makeText(this, "Informe uma quantidade maior que zero.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (itemExistente == null) {
                    pedido.itens.add(novoItem)
                } else {
                    val indice = pedido.itens.indexOf(itemExistente)
                    if (indice >= 0) pedido.itens[indice] = novoItem
                }
                redesenharItens()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun redesenharItens() {
        binding.containerItens.removeAllViews()
        binding.textoSemItens.visibility = if (pedido.itens.isEmpty()) View.VISIBLE else View.GONE

        pedido.itens.forEachIndexed { indice, item ->
            val itemBinding = ItemProdutoPedidoBinding.inflate(
                LayoutInflater.from(this), binding.containerItens, false
            )
            itemBinding.textoDescricao.text = "${indice + 1}. ${item.referencia} — ${item.descricao}"
            itemBinding.textoValor.text = Formato.moeda(item.valorBruto)
            itemBinding.textoDetalhe.text = buildString {
                append(item.quantidade).append(" un")
                if (item.cor.isNotBlank()) append(" • ").append(item.cor)
                append(" • tab. ").append(Formato.moeda(item.precoTabela))
                append(" • desc. ").append(Formato.numero(item.descontoPercent)).append("%")
                append(" • uni. ").append(Formato.moeda(item.precoUnitario))
                append("\nIPI ").append(Formato.numero(item.ipiPercent)).append("%")
                append(" • ST ").append(Formato.numero(item.stPercent)).append("%")
                append(" • líq. ").append(Formato.moeda(item.valorLiquido))
            }
            if (!item.multiploOk) {
                itemBinding.textoAlerta.visibility = View.VISIBLE
                itemBinding.textoAlerta.text = "Quantidade não é múltiplo da embalagem (${item.embalagem})."
            } else {
                itemBinding.textoAlerta.visibility = View.GONE
            }
            itemBinding.botaoEditar.setOnClickListener {
                val produto = Catalogo.porReferencia(item.referencia)
                    ?: Produto(item.referencia, item.descricao, item.embalagem, mapOf(pedido.prazo to item.precoTabela))
                abrirDialogoItem(produto, item)
            }
            itemBinding.botaoRemover.setOnClickListener {
                pedido.itens.remove(item)
                redesenharItens()
            }
            binding.containerItens.addView(itemBinding.root)
        }

        atualizarTotais()
    }

    private fun atualizarTotais() {
        binding.textoTotais.text = buildString {
            append("Líquido ").append(Formato.moeda(pedido.totalLiquido))
            append("   •   IPI ").append(Formato.moeda(pedido.totalIpi))
            append("   •   ST ").append(Formato.moeda(pedido.totalSt))
            append("\nTOTAL ").append(Formato.moeda(pedido.totalBruto))
            append("   (").append(pedido.itens.size).append(" itens / ").append(pedido.totalPecas).append(" pçs)")
        }
    }

    private fun confirmarSaida() {
        coletarCampos()
        AlertDialog.Builder(this)
            .setTitle("Sair do pedido")
            .setMessage("Deseja salvar antes de sair?")
            .setPositiveButton("Salvar") { _, _ -> if (salvar()) finish() }
            .setNegativeButton("Sair sem salvar") { _, _ -> finish() }
            .setNeutralButton(R.string.cancelar, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pedido, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acaoExcluirPedido -> {
                AlertDialog.Builder(this)
                    .setTitle("Excluir pedido")
                    .setMessage("Excluir definitivamente este pedido do aparelho?")
                    .setPositiveButton(R.string.excluir) { _, _ ->
                        repositorio.excluir(pedido.id)
                        finish()
                    }
                    .setNegativeButton(R.string.cancelar, null)
                    .show()
                true
            }
            android.R.id.home -> {
                confirmarSaida()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        confirmarSaida()
    }
}
