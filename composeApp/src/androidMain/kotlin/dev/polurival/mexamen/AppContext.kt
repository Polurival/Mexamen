package dev.polurival.mexamen

import android.content.Context

/**
 * Application context для платформенных factory-функций (expect/actual), у которых
 * нет доступа к Compose/Activity (напр. [dev.polurival.mexamen.data.defaultQuestionStatsStore]).
 * Выставляется в [MainActivity.onCreate] до `setContent`, до первого Koin `get()`.
 */
object AppContext {
    lateinit var instance: Context
}
