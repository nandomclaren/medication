package com.medicontrol.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicontrol.app.MediControlApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * O AlarmManager perde todos os alarmes agendados quando o aparelho reinicia.
 * Este receiver reconcilia os horários a partir do banco assim que o sistema
 * termina de inicializar (mesma lógica usada ao salvar/excluir um medicamento).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
        if (intent.action !in validActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as MediControlApp).repository.reconcileAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
