package br.com.plasvale.pedidos.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import br.com.plasvale.pedidos.model.ItemPedido
import br.com.plasvale.pedidos.model.Pedido
import br.com.plasvale.pedidos.util.Formato
import java.io.File
import java.io.FileInputStream

/**
 * Gera o PDF do pedido usando apenas a API nativa do Android (android.graphics.pdf),
 * sem bibliotecas externas e sem internet. O arquivo é salvo no próprio aparelho.
 */
class PdfPedidoGenerator(private val context: Context) {

    // A4 paisagem, em pontos (1/72").
    private val larguraPagina = 842
    private val alturaPagina = 595
    private val margem = 24f

    private val azul = Color.parseColor("#0B5394")
    private val azulClaro = Color.parseColor("#E3EDF7")
    private val cinzaLinha = Color.parseColor("#C9D2DA")
    private val cinzaTexto = Color.parseColor("#5F6B76")

    private val pTitulo = Paint().apply {
        color = Color.WHITE; textSize = 16f; isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val pSubtitulo = Paint().apply {
        color = Color.WHITE; textSize = 9f; isAntiAlias = true
    }
    private val pRotulo = Paint().apply {
        color = cinzaTexto; textSize = 7f; isAntiAlias = true
    }
    private val pCampo = Paint().apply {
        color = Color.BLACK; textSize = 9f; isAntiAlias = true
    }
    private val pCabecalhoTabela = Paint().apply {
        color = Color.WHITE; textSize = 7.5f; isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val pCelula = Paint().apply {
        color = Color.BLACK; textSize = 8f; isAntiAlias = true
    }
    private val pCelulaForte = Paint().apply {
        color = Color.BLACK; textSize = 8f; isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val pLinha = Paint().apply {
        color = cinzaLinha; strokeWidth = 0.5f
    }
    private val pFundo = Paint()
    private val pRodape = Paint().apply {
        color = cinzaTexto; textSize = 7f; isAntiAlias = true
    }

    /** Colunas: rótulo, largura em pontos, alinhamento à direita. */
    private val colunas = listOf(
        Coluna("#", 24f, false),
        Coluna("CÓDIGO", 52f, false),
        Coluna("DESCRIÇÃO", 195f, false),
        Coluna("M.V.", 30f, true),
        Coluna("COR", 45f, false),
        Coluna("QTDE", 38f, true),
        Coluna("PREÇO TAB.", 58f, true),
        Coluna("DESC.", 38f, true),
        Coluna("PREÇO UNI.", 58f, true),
        Coluna("IPI", 32f, true),
        Coluna("ST", 32f, true),
        Coluna("PREÇO C/ST", 60f, true),
        Coluna("VALOR LÍQ.", 66f, true),
        Coluna("VALOR BRUTO", 66f, true)
    )

    private data class Coluna(val titulo: String, val largura: Float, val direita: Boolean)

    fun gerar(pedido: Pedido): File {
        val documento = PdfDocument()
        var numeroPagina = 1
        var pagina = novaPagina(documento, numeroPagina)
        var canvas = pagina.canvas

        var y = desenharCabecalho(canvas, pedido, numeroPagina)
        y = desenharCabecalhoTabela(canvas, y)

        val alturaLinha = 14f
        val limiteY = alturaPagina - 60f

        pedido.itens.forEachIndexed { indice, item ->
            if (y + alturaLinha > limiteY) {
                desenharRodape(canvas, numeroPagina)
                documento.finishPage(pagina)
                numeroPagina++
                pagina = novaPagina(documento, numeroPagina)
                canvas = pagina.canvas
                y = desenharCabecalho(canvas, pedido, numeroPagina)
                y = desenharCabecalhoTabela(canvas, y)
            }
            y = desenharLinhaItem(canvas, y, alturaLinha, indice + 1, item, indice % 2 == 1)
        }

        // Totais (nova página se não couber)
        val alturaTotais = 110f
        if (y + alturaTotais > limiteY) {
            desenharRodape(canvas, numeroPagina)
            documento.finishPage(pagina)
            numeroPagina++
            pagina = novaPagina(documento, numeroPagina)
            canvas = pagina.canvas
            y = desenharCabecalho(canvas, pedido, numeroPagina)
        }
        desenharTotais(canvas, y + 10f, pedido)
        desenharRodape(canvas, numeroPagina)
        documento.finishPage(pagina)

        val arquivo = arquivoDestino(pedido)
        arquivo.parentFile?.mkdirs()
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return arquivo
    }

    private fun novaPagina(documento: PdfDocument, numero: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(larguraPagina, alturaPagina, numero).create()
        val pagina = documento.startPage(info)
        pFundo.color = Color.WHITE
        pagina.canvas.drawRect(0f, 0f, larguraPagina.toFloat(), alturaPagina.toFloat(), pFundo)
        return pagina
    }

    private fun desenharCabecalho(canvas: Canvas, pedido: Pedido, numeroPagina: Int): Float {
        // Faixa azul
        pFundo.color = azul
        canvas.drawRect(0f, 0f, larguraPagina.toFloat(), 46f, pFundo)
        canvas.drawText("PLASVALE", margem, 22f, pTitulo)
        canvas.drawText(
            "FOLHA DE PEDIDO  •  TABELA ${pedido.tabela}  •  IPI ${Formato.numero(pedido.ipiPercent)}%",
            margem, 36f, pSubtitulo
        )

        val direita = larguraPagina - margem
        val pDireita = Paint(pSubtitulo).apply { textAlign = Paint.Align.RIGHT }
        val pDireitaForte = Paint(pTitulo).apply { textAlign = Paint.Align.RIGHT; textSize = 12f }
        canvas.drawText("PEDIDO Nº ${pedido.numero}", direita, 20f, pDireitaForte)
        canvas.drawText(Formato.dataHora(pedido.dataMillis) + "   |   pág. $numeroPagina", direita, 36f, pDireita)

        var y = 62f

        // Bloco do cliente
        pFundo.color = azulClaro
        canvas.drawRect(margem, y - 12f, larguraPagina - margem, y + 46f, pFundo)

        val col1 = margem + 6f
        val col2 = margem + 300f
        val col3 = margem + 560f

        campo(canvas, col1, y, "CLIENTE", pedido.cliente)
        campo(canvas, col2, y, "CNPJ", pedido.cnpj)
        campo(canvas, col3, y, "TELEFONE", pedido.telefone)

        campo(canvas, col1, y + 22f, "CIDADE / UF", listOf(pedido.cidade, pedido.uf).filter { it.isNotBlank() }.joinToString(" / "))
        campo(canvas, col2, y + 22f, "COMPRADOR", pedido.comprador)
        campo(canvas, col3, y + 22f, "TRANSPORTADORA", pedido.transportadora)

        y += 60f

        // Condições comerciais
        // O desconto padrão não é impresso: ele aparece item a item na coluna DESC.
        campo(canvas, col1, y, "PRAZO DE PAGAMENTO", pedido.prazo)
        campo(canvas, col1 + 170f, y, "IPI", Formato.percentual(pedido.ipiPercent))
        campo(canvas, col1 + 270f, y, "ST", Formato.percentual(pedido.stPercent))
        campo(canvas, col1 + 370f, y, "REPRESENTANTE", pedido.representante)

        return y + 18f
    }

    private fun campo(canvas: Canvas, x: Float, y: Float, rotulo: String, valor: String) {
        canvas.drawText(rotulo, x, y, pRotulo)
        canvas.drawText(if (valor.isBlank()) "-" else valor, x, y + 11f, pCampo)
    }

    private fun desenharCabecalhoTabela(canvas: Canvas, yInicial: Float): Float {
        val y = yInicial + 8f
        val altura = 16f
        pFundo.color = azul
        canvas.drawRect(margem, y, larguraPagina - margem, y + altura, pFundo)

        var x = margem
        colunas.forEach { coluna ->
            val texto = coluna.titulo
            if (coluna.direita) {
                val p = Paint(pCabecalhoTabela).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(texto, x + coluna.largura - 4f, y + 11f, p)
            } else {
                canvas.drawText(texto, x + 4f, y + 11f, pCabecalhoTabela)
            }
            x += coluna.largura
        }
        return y + altura
    }

    private fun desenharLinhaItem(
        canvas: Canvas,
        y: Float,
        altura: Float,
        numero: Int,
        item: ItemPedido,
        zebra: Boolean
    ): Float {
        if (zebra) {
            pFundo.color = Color.parseColor("#F5F8FB")
            canvas.drawRect(margem, y, larguraPagina - margem, y + altura, pFundo)
        }

        val valores = listOf(
            numero.toString(),
            item.referencia,
            item.descricao,
            if (item.embalagem > 0) item.embalagem.toString() else "-",
            item.cor,
            item.quantidade.toString(),
            Formato.valor(item.precoTabela),
            Formato.numero(item.descontoPercent) + "%",
            Formato.valor(item.precoUnitario),
            Formato.numero(item.ipiPercent) + "%",
            Formato.numero(item.stPercent) + "%",
            Formato.valor(item.precoComSt),
            Formato.valor(item.valorLiquido),
            Formato.valor(item.valorBruto)
        )

        var x = margem
        valores.forEachIndexed { indice, valor ->
            val coluna = colunas[indice]
            val paint = if (indice >= 12) pCelulaForte else pCelula
            desenharCelula(canvas, x, y + altura - 4f, coluna, valor, paint)
            x += coluna.largura
        }

        canvas.drawLine(margem, y + altura, larguraPagina - margem, y + altura, pLinha)
        return y + altura
    }

    private fun desenharCelula(canvas: Canvas, x: Float, baseline: Float, coluna: Coluna, texto: String, paint: Paint) {
        val disponivel = coluna.largura - 8f
        val visivel = truncar(texto, paint, disponivel)
        if (coluna.direita) {
            val p = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(visivel, x + coluna.largura - 4f, baseline, p)
        } else {
            canvas.drawText(visivel, x + 4f, baseline, paint)
        }
    }

    private fun truncar(texto: String, paint: Paint, larguraMaxima: Float): String {
        if (paint.measureText(texto) <= larguraMaxima) return texto
        var corte = texto.length
        while (corte > 1 && paint.measureText(texto.substring(0, corte) + "…") > larguraMaxima) corte--
        return texto.substring(0, corte) + "…"
    }

    private fun desenharTotais(canvas: Canvas, yInicial: Float, pedido: Pedido) {
        var y = yInicial
        val larguraCaixa = 260f
        val x = larguraPagina - margem - larguraCaixa

        pFundo.color = azulClaro
        canvas.drawRect(x, y, larguraPagina - margem, y + 82f, pFundo)

        val pRotuloTotal = Paint(pCampo).apply { textSize = 9f }
        val pValorTotal = Paint(pCelulaForte).apply { textSize = 9f; textAlign = Paint.Align.RIGHT }

        var linha = y + 15f
        val direita = larguraPagina - margem - 8f

        canvas.drawText("Total de itens", x + 8f, linha, pRotuloTotal)
        canvas.drawText("${pedido.itens.size}  (${pedido.totalPecas} pçs)", direita, linha, pValorTotal)
        linha += 14f
        canvas.drawText("Total líquido (sem IPI/ST)", x + 8f, linha, pRotuloTotal)
        canvas.drawText(Formato.moeda(pedido.totalLiquido), direita, linha, pValorTotal)
        linha += 14f
        canvas.drawText("IPI", x + 8f, linha, pRotuloTotal)
        canvas.drawText(Formato.moeda(pedido.totalIpi), direita, linha, pValorTotal)
        linha += 14f
        canvas.drawText("ST", x + 8f, linha, pRotuloTotal)
        canvas.drawText(Formato.moeda(pedido.totalSt), direita, linha, pValorTotal)
        linha += 16f

        pFundo.color = azul
        canvas.drawRect(x, linha - 12f, larguraPagina - margem, linha + 6f, pFundo)
        val pTotalBrancoEsq = Paint(pCabecalhoTabela).apply { textSize = 10f }
        val pTotalBrancoDir = Paint(pCabecalhoTabela).apply { textSize = 11f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL DO PEDIDO", x + 8f, linha + 1f, pTotalBrancoEsq)
        canvas.drawText(Formato.moeda(pedido.totalBruto), direita, linha + 1f, pTotalBrancoDir)

        // Observações à esquerda
        val larguraObs = larguraPagina - (2 * margem) - larguraCaixa - 16f
        canvas.drawText("OBSERVAÇÕES", margem, y + 10f, pRotulo)
        var yObs = y + 22f
        val textoObs = if (pedido.observacao.isBlank()) "-" else pedido.observacao
        quebrarLinhas(textoObs, pCampo, larguraObs).take(4).forEach {
            canvas.drawText(it, margem, yObs, pCampo)
            yObs += 11f
        }

        y += 96f
        canvas.drawLine(margem, y, margem + 200f, y, pLinha)
        canvas.drawText("Assinatura do cliente", margem, y + 10f, pRotulo)
        canvas.drawLine(margem + 260f, y, margem + 460f, y, pLinha)
        canvas.drawText("Assinatura do representante", margem + 260f, y + 10f, pRotulo)
    }

    private fun quebrarLinhas(texto: String, paint: Paint, largura: Float): List<String> {
        val linhas = mutableListOf<String>()
        texto.split("\n").forEach { paragrafo ->
            var atual = StringBuilder()
            paragrafo.split(" ").forEach { palavra ->
                val teste = if (atual.isEmpty()) palavra else "$atual $palavra"
                if (paint.measureText(teste) > largura && atual.isNotEmpty()) {
                    linhas.add(atual.toString())
                    atual = StringBuilder(palavra)
                } else {
                    atual = StringBuilder(teste)
                }
            }
            linhas.add(atual.toString())
        }
        return linhas
    }

    private fun desenharRodape(canvas: Canvas, numeroPagina: Int) {
        val y = alturaPagina - 16f
        canvas.drawLine(margem, y - 10f, larguraPagina - margem, y - 10f, pLinha)
        canvas.drawText(
            "Pedidos Plasvale • documento gerado no aparelho, sem envio de dados • página $numeroPagina",
            margem, y, pRodape
        )
    }

    private fun arquivoDestino(pedido: Pedido): File {
        val pasta = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "")
        val cliente = Formato.nomeArquivoSeguro(pedido.cliente.ifBlank { "cliente" })
        val nome = "Pedido_${pedido.numero}_${cliente}_${Formato.carimboArquivo(pedido.dataMillis)}.pdf"
        return File(pasta, nome)
    }

    /**
     * Copia o PDF para a pasta pública "Downloads/Pedidos Plasvale" (Android 10+),
     * para que fique acessível pelo gerenciador de arquivos do aparelho.
     * Retorna null quando não foi possível (nesse caso o arquivo interno continua válido).
     */
    fun copiarParaDownloads(arquivo: File): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, arquivo.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Pedidos Plasvale")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { saida ->
                FileInputStream(arquivo).use { entrada -> entrada.copyTo(saida) }
            }
            "Downloads/Pedidos Plasvale/${arquivo.name}"
        } catch (e: Exception) {
            null
        }
    }
}
