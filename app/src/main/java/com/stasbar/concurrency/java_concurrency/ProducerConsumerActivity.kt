package com.stasbar.concurrency.java_concurrency

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.util.LogPrinter
import android.util.Printer
import com.stasbar.concurrency.LoggableActivity
import com.stasbar.concurrency.R
import com.stasbar.concurrency.data.Transacton
import kotlinx.android.synthetic.main.activity_producer_consumer.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread

class ProducerConsumerActivity : LoggableActivity() {
    override fun getLogger() = logger
    val queue: LinkedList<Transacton> = object : LinkedList<Transacton>() {
        override fun addLast(e: Transacton?) {
            super.addLast(e)
            synchronized(this) {
                (this as Object).notifyAll()
            }
        }
    }
    val countDownLatch: CountDownLatch = CountDownLatch(1)
    val semapthore: Semaphore = Semaphore(1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_producer_consumer)
        startProductionLooper()
        startProducing()
        startConsuming(2)

        //Looper.getMainLooper().setMessageLogging(LogPrinter(Log.DEBUG, "Looper"))

    }

    lateinit var producerHandler: Handler
    private fun startProductionLooper() {
        val thread = Thread {
            Looper.prepare()
            producerHandler = Handler()
            countDownLatch.countDown()
            Looper.myLooper().setMessageLogging(LogPrinter(Log.DEBUG, "ProductionLooper"))
            Looper.loop()
        }
        thread.start()

    }

    private fun startProducing() {
        countDownLatch.await()
        val producer = Thread({
            val rand = Random()

            while (true) {

                producerHandler.post {
                    val transcation = Transacton(UUID.randomUUID().toString(), UUID.randomUUID().toString(), BigDecimal(rand.nextDouble()))
                    queue.addLast(transcation)
                }

                Thread.sleep(1000)
            }
        })

        producer.start()

    }

    private fun startConsuming(consumerCount: Int) {
        countDownLatch.await()
        repeat(consumerCount) {

            val consumer = Thread {
                while (true) {

                    producerHandler.post {
                        var transction: Transacton? = null
                        synchronized(queue) {
                            while (queue.isEmpty()) {
                                log("Waiting for new product")
                                (queue as Object).wait()
                            }
                            transction = queue.remove()
                        }

                        log("Processing ${transction!!.hashCode()}")
                        Thread.sleep(700)
                        log("Done ${transction!!.hashCode()}")
                    }

                }
            }
            consumer.name = "Consumer-$it"
            consumer.start()
        }
    }


}
