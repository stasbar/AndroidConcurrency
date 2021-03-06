package com.stasbar.concurrency.benchmarks.proofofwork

import android.os.AsyncTask
import android.util.Log
import com.stasbar.concurrency.benchmarks.*
import kotlin.system.measureTimeMillis

@ExperimentalUnsignedTypes
class PoWAsyncTaskExecutor(
    difficulty: UInt,
    poolSize: UInt,
    jobSize: UInt,
    val onUpdate: (JobUpdate) -> Unit,
    val onComplete: (MiningResult) -> Unit
) : PoWExecutor(difficulty, poolSize, jobSize) {

    private val asyncTasks = mutableListOf<ProofOfWorkAsyncTask>()

    init {
        var from: ULong = searchStart
        repeat(jobSize.toInt()) {
            val task = ProofOfWorkAsyncTask(
                it,
                PoWParams(ULongRange(from, from + calculationsPerWorker), "stasbar", difficulty),
                onUpdate,
                { result ->
                    onComplete(result)
                    if (result is MiningResult.Success) cancel()
                })
            asyncTasks.add(task)
            from += calculationsPerWorker
        }
    }

    override fun execute() {
        asyncTasks.forEach { it.executeOnExecutor(threadPool) }
    }

    override fun cancel() {
        asyncTasks.forEach { it.cancel(true) }
    }
}

class PoWAlreadyFoundException : Exception()

@ExperimentalUnsignedTypes
private class ProofOfWorkAsyncTask(
    val id: Int,
    val arguments: PoWParams,
    val onUpdate: (JobUpdate) -> Unit,
    val onComplete: (MiningResult) -> Unit
) :
    AsyncTask<Any, ULong, MiningResult>() {

    lateinit var updater: Updater

    override fun doInBackground(vararg params: Any): MiningResult {
        val (searchRange, data, difficulty) = arguments
        val searchLength = searchRange.last - searchRange.first
        updater = Updater(id, searchLength, onUpdate)
        updater.start()

        Log.d("ProofOfWorkAsyncTask", "Thread name: ${Thread.currentThread().name} searchRange: $searchRange")
        var finalHash: String? = null
        val time = try {
            measureTimeMillis {
                val target = String(CharArray(difficulty.toInt())).replace('\u0000', '0')
                for (testNonce in searchRange) {
                    // Stop searching if pow is already found
                    if (isCancelled) throw PoWAlreadyFoundException()

                    val hash = calculateHashOf(data, testNonce)
                    if (hash.substring(0, difficulty.toInt()) == target) {
                        finalHash = hash
                        break
                    }
                    updater.incNonce()
                }
            }
        } catch (e: PoWAlreadyFoundException) {
            return MiningResult.NotFound(id)
        }

        return if (finalHash != null) {
            Log.d("PoWAsyncTask-$id", "Found PoW: $finalHash in ${ProofOfWorkActivity.measureFormat.format(time)}")
            MiningResult.Success(id, finalHash!!, time)
        } else {
            Log.d(
                "PoWAsyncTask-$id",
                "Didn't found PoW: $finalHash in $searchRange in time ${ProofOfWorkActivity.measureFormat.format(
                    time
                )}"
            )
            MiningResult.NotFound(id)
        }


    }

    override fun onPostExecute(result: MiningResult) {
        super.onPostExecute(result)
        onComplete(result)
        updater.interrupt()
    }

    override fun onCancelled() {
        super.onCancelled()
        Log.d("PoWAsyncTask-$id", " Cancelled !")
    }
}