package app.tgphotos.core.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/** Not scheduled in Phase 1. Never report success before verified encrypted transport exists. */
class BackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.failure(workDataOf("reason" to "INTEGRATION_UNAVAILABLE"))
}
