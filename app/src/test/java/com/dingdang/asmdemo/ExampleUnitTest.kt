package com.dingdang.asmdemo

import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testAdditionIsCorrect() {
        assertEquals(5, 2 + 2)
    }

    @Test
    fun testCoroutine(){
//        val coroutineScop= CoroutineScope(Job() + Dispatchers.IO)
//        val job=coroutineScop.launch {
//            println("coroutineScop CoroutineScope(Job() + Dispatchers.IO) context=${this.coroutineContext}")
//            println("thread=${Thread.currentThread().name}")
//        }
//        CoroutineScope(Job() + Dispatchers.IO).launch(Dispatchers.Default) {
//            println("coroutineScop CoroutineScope(Job() + Dispatchers.IO).launch.Dispatchers.Main context=${this.coroutineContext}")
//            println("thread=${Thread.currentThread().name}")
//        }
//        GlobalScope.launch {
//            println("GlobalScope context=${this.coroutineContext}")
//        }
        runBlocking {
            println("runBlocking context=${this.coroutineContext}")
            withContext(Dispatchers.Default) {
                delay(1000)
                println("job1 coroutineContext=${this.coroutineContext}")
            }
            val job2=launch {
                delay(2000)
                println("job2 coroutineContext=${this.coroutineContext}")
            }
            job2.join()
            launch {
                println("job3 coroutineContext=${this.coroutineContext}")
            }
        }
    }
}