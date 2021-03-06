package com.stasbar.concurrency.caching

import android.os.Bundle
import com.stasbar.concurrency.LoggableActivity
import com.stasbar.concurrency.R
import com.stasbar.concurrency.Square
import com.stasbar.concurrency.view.LogView
import kotlinx.android.synthetic.main.activity_cache.*


class CacheActivity : LoggableActivity() {
    override fun getLogger(): LogView = this.logger
    /**
     * SoftReference is a wrapper for an object
     * If all the references ti that object are soft references and
     * if the garbage collector needs more memory it will first collect soft references
     * Excellent for memory caches, that object can disappear at any time
     */
    private val simpleCache = SimpleCache()

    /**
     *  WeakReference is a wrapper for an object
     *  If all the references to that object are weak references then the object will automatically GC'ed (Garbage Collected)
     *  Avoids memory leaks
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache)

        val size = 10
        btnLoadObjectsToCache.setOnClickListener {
            for (i in 1..size)
                simpleCache.put(i.toString(), Square.Builder(i, i).build())

            val message = "added $size elements to cache"
            log(message)
        }
        btnClearCache.setOnClickListener {
            try {
                val ignored = arrayOfNulls<Any>(Runtime.getRuntime().maxMemory().toInt())
            } catch (e: OutOfMemoryError) {
                val message = e.message.toString()
                log(message)
            }
        }

        btnReadCache.setOnClickListener {
            val hitsCounter: Int = (1..size).sumBy { simpleCache.read(it.toString()) }
            val message = String.format("Cache hit %d/%d %d %%", hitsCounter, size, hitsCounter / size * 100)
            log(message)

        }


    }

}
