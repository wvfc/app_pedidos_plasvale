package br.com.plasvale.pedidos.data

import android.content.Context

/** Preferências locais do representante (SharedPreferences, apenas no aparelho). */
class Configuracao(context: Context) {

    private val prefs = context.getSharedPreferences("config_pedidos", Context.MODE_PRIVATE)

    var representante: String
        get() = prefs.getString("representante", "") ?: ""
        set(v) = prefs.edit().putString("representante", v).apply()

    var telefoneRepresentante: String
        get() = prefs.getString("telefone_rep", "") ?: ""
        set(v) = prefs.edit().putString("telefone_rep", v).apply()

    var emailRepresentante: String
        get() = prefs.getString("email_rep", "") ?: ""
        set(v) = prefs.edit().putString("email_rep", v).apply()

    var ipiPadrao: Double
        get() = prefs.getFloat("ipi_padrao", 6.5f).toDouble()
        set(v) = prefs.edit().putFloat("ipi_padrao", v.toFloat()).apply()

    var descontoPadrao: Double
        get() = prefs.getFloat("desconto_padrao", 20f).toDouble()
        set(v) = prefs.edit().putFloat("desconto_padrao", v.toFloat()).apply()

    var prazoPadrao: String
        get() = prefs.getString("prazo_padrao", "À Vista") ?: "À Vista"
        set(v) = prefs.edit().putString("prazo_padrao", v).apply()

    var ufPadrao: String
        get() = prefs.getString("uf_padrao", "SP") ?: "SP"
        set(v) = prefs.edit().putString("uf_padrao", v).apply()

    /** ST em % gravada por UF (o usuário informa; SP fica sempre em 0 por orientação da planilha). */
    fun stDaUf(uf: String): Double = prefs.getFloat("st_$uf", -1f).let {
        if (it < 0f) Catalogo.stSugeridaPara(uf) else it.toDouble()
    }

    fun definirStDaUf(uf: String, valor: Double) {
        prefs.edit().putFloat("st_$uf", valor.toFloat()).apply()
    }
}
