package br.com.plasvale.pedidos.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import br.com.plasvale.pedidos.model.Pedido
import br.com.plasvale.pedidos.pdf.PdfPedidoGenerator
import java.io.File

/** Gera o PDF, informa onde ele foi salvo e oferece abrir/compartilhar. */
object PdfCompartilhamento {

    fun gerar(activity: Activity, pedido: Pedido): File? {
        if (pedido.itens.isEmpty()) {
            Toast.makeText(activity, "Adicione ao menos um item antes de gerar o PDF.", Toast.LENGTH_LONG).show()
            return null
        }
        return try {
            val gerador = PdfPedidoGenerator(activity)
            val arquivo = gerador.gerar(pedido)
            val copia = gerador.copiarParaDownloads(arquivo)
            val onde = copia ?: "Documentos do aplicativo: ${arquivo.absolutePath}"
            Toast.makeText(activity, "PDF salvo em $onde", Toast.LENGTH_LONG).show()
            arquivo
        } catch (e: Exception) {
            Toast.makeText(activity, "Falha ao gerar o PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    fun gerarECompartilhar(activity: Activity, pedido: Pedido) {
        val arquivo = gerar(activity, pedido) ?: return
        try {
            val uri = FileProvider.getUriForFile(
                activity,
                activity.packageName + ".fileprovider",
                arquivo
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Pedido ${pedido.numero} - ${pedido.cliente}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Compartilhar pedido"))
        } catch (e: Exception) {
            Toast.makeText(activity, "PDF gerado, mas não foi possível compartilhar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
