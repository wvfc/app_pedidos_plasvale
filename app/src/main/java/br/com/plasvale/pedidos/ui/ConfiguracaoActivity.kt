package br.com.plasvale.pedidos.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import br.com.plasvale.pedidos.data.Catalogo
import br.com.plasvale.pedidos.data.Configuracao
import br.com.plasvale.pedidos.data.PedidoRepository
import br.com.plasvale.pedidos.databinding.ActivityConfiguracaoBinding
import br.com.plasvale.pedidos.databinding.ItemStUfBinding
import br.com.plasvale.pedidos.util.Formato
import java.io.File

class ConfiguracaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracaoBinding
    private lateinit var config: Configuracao
    private val camposSt = mutableMapOf<String, ItemStUfBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Catalogo.carregar(this)
        config = Configuracao(this)

        binding.campoRepresentante.setText(config.representante)
        binding.campoTelefoneRep.setText(config.telefoneRepresentante)
        binding.campoEmailRep.setText(config.emailRepresentante)
        binding.campoIpiPadrao.setText(Formato.numero(config.ipiPadrao))
        binding.campoDescontoPadrao.setText(Formato.numero(config.descontoPadrao))

        val adapterUf = ArrayAdapter(this, android.R.layout.simple_spinner_item, Catalogo.ufs)
        adapterUf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUfPadrao.adapter = adapterUf
        val posUf = Catalogo.ufs.indexOf(config.ufPadrao)
        if (posUf >= 0) binding.spinnerUfPadrao.setSelection(posUf)

        val adapterPrazo = ArrayAdapter(this, android.R.layout.simple_spinner_item, Catalogo.prazos)
        adapterPrazo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPrazoPadrao.adapter = adapterPrazo
        val posPrazo = Catalogo.prazos.indexOf(config.prazoPadrao)
        if (posPrazo >= 0) binding.spinnerPrazoPadrao.setSelection(posPrazo)

        montarCamposSt()

        binding.textoCatalogo.text =
            "Tabela ${Catalogo.tabela} • ${Catalogo.referencia}\n" +
                "${Catalogo.produtos.size} produtos • prazos: ${Catalogo.prazos.joinToString(", ")}\n" +
                Catalogo.avisos.joinToString("\n")

        binding.botaoSalvarConfig.setOnClickListener { salvar() }
        binding.botaoBackup.setOnClickListener { exportarBackup() }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun montarCamposSt() {
        binding.containerSt.removeAllViews()
        camposSt.clear()
        Catalogo.ufs.forEach { uf ->
            val linha = ItemStUfBinding.inflate(layoutInflater, binding.containerSt, false)
            linha.textoUf.text = uf
            linha.campoStUf.setText(Formato.numero(config.stDaUf(uf)))
            camposSt[uf] = linha
            binding.containerSt.addView(linha.root)
        }
    }

    private fun salvar() {
        config.representante = binding.campoRepresentante.text.toString().trim()
        config.telefoneRepresentante = binding.campoTelefoneRep.text.toString().trim()
        config.emailRepresentante = binding.campoEmailRep.text.toString().trim()
        config.ipiPadrao = Formato.paraDouble(binding.campoIpiPadrao.text.toString(), Catalogo.ipiPadrao)
        config.descontoPadrao = Formato.paraDouble(binding.campoDescontoPadrao.text.toString(), 0.0)
        config.ufPadrao = binding.spinnerUfPadrao.selectedItem?.toString() ?: "SP"
        config.prazoPadrao = binding.spinnerPrazoPadrao.selectedItem?.toString() ?: "À Vista"
        camposSt.forEach { (uf, linha) ->
            config.definirStDaUf(uf, Formato.paraDouble(linha.campoStUf.text.toString(), 0.0))
        }
        Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun exportarBackup() {
        try {
            val repositorio = PedidoRepository(this)
            val pasta = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "")
            pasta.mkdirs()
            val arquivo = File(pasta, "backup_pedidos_${Formato.carimboArquivo(System.currentTimeMillis())}.json")
            arquivo.writeText(repositorio.exportarJson())
            val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", arquivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Enviar backup"))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
