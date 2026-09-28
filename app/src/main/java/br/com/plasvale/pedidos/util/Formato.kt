package br.com.plasvale.pedidos.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formato {

    private val locale = Locale("pt", "BR")
    private val simbolos = DecimalFormatSymbols(locale).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }
    private val formatoMoeda = DecimalFormat("#,##0.00", simbolos)
    private val formatoNumero = DecimalFormat("#,##0.##", simbolos)
    private val formatoData = SimpleDateFormat("dd/MM/yyyy", locale)
    private val formatoDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)
    private val formatoArquivo = SimpleDateFormat("yyyyMMdd_HHmmss", locale)

    fun moeda(valor: Double): String = "R$ " + formatoMoeda.format(valor)

    fun valor(valor: Double): String = formatoMoeda.format(valor)

    fun numero(valor: Double): String = formatoNumero.format(valor)

    fun percentual(valor: Double): String = formatoNumero.format(valor) + "%"

    fun data(millis: Long): String = formatoData.format(Date(millis))

    fun dataHora(millis: Long): String = formatoDataHora.format(Date(millis))

    fun carimboArquivo(millis: Long): String = formatoArquivo.format(Date(millis))

    /** Aceita "12,5" e "12.5". Retorna [padrao] quando o texto não é um número. */
    fun paraDouble(texto: String?, padrao: Double = 0.0): Double {
        if (texto.isNullOrBlank()) return padrao
        var limpo = texto.trim()
            .replace("R$", "")
            .replace("%", "")
            .replace(" ", "")
        limpo = if (limpo.contains(',')) {
            // formato brasileiro: ponto é separador de milhar
            limpo.replace(".", "").replace(',', '.')
        } else {
            limpo
        }
        return limpo.toDoubleOrNull() ?: padrao
    }

    fun paraInt(texto: String?, padrao: Int = 0): Int {
        if (texto.isNullOrBlank()) return padrao
        return texto.trim().filter { it.isDigit() }.toIntOrNull() ?: padrao
    }

    /** Remove acentos e caracteres inválidos para nome de arquivo. */
    fun nomeArquivoSeguro(texto: String): String {
        val semAcento = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return semAcento.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).trim('_')
    }
}
