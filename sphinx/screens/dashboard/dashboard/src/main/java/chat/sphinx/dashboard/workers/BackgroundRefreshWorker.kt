package chat.sphinx.dashboard.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

internal class BackgroundRefreshWorker @Inject constructor(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var repositoryDashboard: RepositoryDashboardAndroid<Any>

    override suspend fun doWork(): Result {
        return fetchContactsAndNewMessages()
    }

    @SuppressLint("RestrictedApi")
    private suspend fun fetchContactsAndNewMessages(): Result {
        var result: Result = Result.Success()

        repositoryDashboard.networkRefreshLatestContacts.collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {}
                is Response.Success -> {}
                is Response.Error -> {
                    result = Result.Failure()
                }
            }
        }

        repositoryDashboard.networkRefreshMessages.collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {}
                is Response.Success -> {}
                is Response.Error -> {
                    result = Result.Failure()
                }
            }
        }

        return result
    }
}